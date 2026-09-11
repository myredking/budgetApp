"""Generate Suno tracks and append normalized rows to Google Sheets."""

from __future__ import annotations

import argparse
import calendar
import json
import os
import random
import re
import sys
import time
from collections.abc import Callable, Mapping
from dataclasses import dataclass
from datetime import datetime, timedelta
from pathlib import Path
from typing import Any, Protocol
from urllib.parse import urlparse

import requests


SHEETS_SCOPE = "https://www.googleapis.com/auth/spreadsheets"
DEFAULT_SUNO_BASE_URL = "https://api.sunoapi.org/api/v1"
DEFAULT_SUNO_DOWNLOAD_LIMIT = 20
DEFAULT_SUNO_BILLING_DAY = 1
DEFAULT_SUNO_QUOTA_STATE = Path(".state/suno.downloads.json")
RETRYABLE_STATUS_CODES = {408, 429, 430, 455, 500, 502, 503, 504}
AUDIO_EXTENSIONS = {".mp3", ".wav", ".flac", ".m4a", ".aiff", ".wma"}
COVER_EXTENSIONS = {".jpg", ".jpeg", ".png", ".webp"}
AUDIO_MIME_EXTENSIONS = {
    "audio/mpeg": (".mp3",),
    "audio/wav": (".wav",),
    "audio/x-wav": (".wav",),
    "audio/flac": (".flac",),
    "audio/mp4": (".m4a",),
}
COVER_MIME_EXTENSIONS = {
    "image/jpeg": (".jpg", ".jpeg"),
    "image/png": (".png",),
    "image/webp": (".webp",),
}
FAILED_STATUSES = {
    "CREATE_TASK_FAILED",
    "GENERATE_AUDIO_FAILED",
    "CALLBACK_EXCEPTION",
    "SENSITIVE_WORD_ERROR",
    "FAILED",
}


class HttpSession(Protocol):
    """Subset of requests.Session needed by this module."""

    def request(
        self, method: str, url: str, **kwargs: Any
    ) -> requests.Response:
        """Send an HTTP request."""

    def get(self, url: str, **kwargs: Any) -> requests.Response:
        """Send an HTTP GET request."""


@dataclass(frozen=True)
class TrackRequest:
    """Input used for one Suno generation task."""

    title: str
    genre: str
    prompt: str
    instrumental: bool = False
    custom_mode: bool = False
    model: str = "V4_5ALL"


@dataclass(frozen=True)
class TrackRecord:
    """One normalized track row and its remote assets."""

    track_id: str
    title: str
    audio_url: str
    cover_url: str
    lyrics: str
    genre: str = ""
    created_at: str = ""


class SunoApiError(RuntimeError):
    """Raised when generation or polling cannot complete."""


class GoogleSheetsError(RuntimeError):
    """Raised when a track row cannot be written to Google Sheets."""


class SunoQuotaExceeded(RuntimeError):
    """Raised when the local Suno download guard reaches its limit."""


class SunoDownloadQuota:
    """Persist a billing-period limit for unique Suno track downloads."""

    def __init__(
        self,
        state_path: Path = DEFAULT_SUNO_QUOTA_STATE,
        limit: int = DEFAULT_SUNO_DOWNLOAD_LIMIT,
        billing_day: int = DEFAULT_SUNO_BILLING_DAY,
        lock_path: Path | None = None,
    ) -> None:
        if limit < 1:
            raise ValueError("Suno 다운로드 한도는 1 이상이어야 합니다.")
        if not 1 <= billing_day <= 31:
            raise ValueError("Suno 결제일은 1에서 31 사이여야 합니다.")
        self._state_path = state_path
        self._limit = limit
        self._billing_day = billing_day
        self._lock_path = lock_path or state_path.with_suffix(".lock")

    @classmethod
    def from_environment(cls) -> SunoDownloadQuota:
        """Build a quota guard using the configured environment variables."""
        limit = _environment_int(
            "SUNO_MONTHLY_DOWNLOAD_LIMIT", DEFAULT_SUNO_DOWNLOAD_LIMIT
        )
        billing_day = _environment_int(
            "SUNO_BILLING_DAY", DEFAULT_SUNO_BILLING_DAY
        )
        state_path = Path(
            os.getenv(
                "SUNO_QUOTA_STATE_FILE", str(DEFAULT_SUNO_QUOTA_STATE)
            )
        )
        return cls(state_path, limit=limit, billing_day=billing_day)

    def reserve(self, track_id: str, now: datetime | None = None) -> int:
        """Reserve one unique track and return the used count."""
        normalized_id = track_id.strip()
        if not normalized_id:
            raise ValueError("다운로드 한도 기록에는 Track_ID가 필요합니다.")
        period = self._period_key(now or datetime.now().astimezone())
        with self._state_lock():
            track_ids = self._read_track_ids(period)
            if normalized_id in track_ids:
                return len(track_ids)
            if len(track_ids) >= self._limit:
                raise SunoQuotaExceeded(
                    f"Suno 다운로드 한도 초과: {self._limit}곡 ({period})"
                )
            track_ids.append(normalized_id)
            self._write_state(period, track_ids)
            return len(track_ids)

    @property
    def limit(self) -> int:
        """Return the configured maximum unique tracks per billing period."""
        return self._limit

    def _period_key(self, now: datetime) -> str:
        month_days = calendar.monthrange(now.year, now.month)[1]
        reset_day = min(self._billing_day, month_days)
        if now.day >= reset_day:
            return now.strftime("%Y-%m")
        previous_month = now.replace(day=1) - timedelta(days=1)
        return previous_month.strftime("%Y-%m")

    def _read_track_ids(self, period: str) -> list[str]:
        if not self._state_path.exists():
            return []
        try:
            payload = json.loads(self._state_path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as error:
            raise SunoQuotaExceeded(
                f"Suno 다운로드 상태 파일을 읽을 수 없습니다: {error}"
            ) from error
        if payload.get("period") != period:
            return []
        track_ids = payload.get("track_ids", [])
        if not isinstance(track_ids, list) or not all(
            isinstance(value, str) for value in track_ids
        ):
            raise SunoQuotaExceeded("Suno 다운로드 상태 형식이 올바르지 않습니다.")
        return list(dict.fromkeys(track_ids))

    def _write_state(self, period: str, track_ids: list[str]) -> None:
        self._state_path.parent.mkdir(parents=True, exist_ok=True)
        partial_path = self._state_path.with_name(
            self._state_path.name + ".part"
        )
        payload = {"period": period, "track_ids": track_ids}
        partial_path.write_text(
            json.dumps(payload, ensure_ascii=False, indent=2),
            encoding="utf-8",
        )
        partial_path.replace(self._state_path)

    def _state_lock(self) -> Any:
        try:
            from filelock import FileLock
        except ModuleNotFoundError as error:
            raise SunoQuotaExceeded(
                "filelock이 필요합니다. requirements.txt를 설치하세요."
            ) from error
        self._lock_path.parent.mkdir(parents=True, exist_ok=True)
        return FileLock(str(self._lock_path))


def parse_completed_tracks(
    payload: Mapping[str, Any],
    fallback_title: str,
    instrumental: bool,
    genre: str = "",
    created_at: str = "",
) -> list[TrackRecord]:
    """Convert a completed provider response into normalized track records."""
    data = _as_mapping(payload.get("data", payload))
    response = _as_mapping(data.get("response", {}))
    items = response.get("sunoData", response.get("data", []))
    if isinstance(items, Mapping):
        items = [items]
    if not isinstance(items, list) or not items:
        raise SunoApiError("완료 응답에 음원 결과가 없습니다.")
    records: list[TrackRecord] = []
    for index, item in enumerate(items, start=1):
        records.append(
            _parse_track_item(
                _as_mapping(item),
                fallback_title,
                instrumental,
                genre,
                created_at,
                index,
            )
        )
    return records


def build_sheet_row(record: TrackRecord, status: str = "Pending") -> list[str]:
    """Return a row in the exact Tracks sheet A:H order."""
    created_at = record.created_at or datetime.now().astimezone().isoformat(
        timespec="seconds"
    )
    return [
        record.track_id,
        record.title,
        record.genre,
        record.audio_url,
        record.cover_url,
        record.lyrics,
        status,
        created_at,
    ]


def download_asset(
    session: HttpSession,
    url: str,
    destination: Path,
    timeout_seconds: float = 60.0,
    allowed_content_types: tuple[str, ...] = (),
    require_content_type: bool = False,
) -> str:
    """Download one asset to a temporary file, then atomically rename it."""
    destination.parent.mkdir(parents=True, exist_ok=True)
    partial_path = destination.with_name(destination.name + ".part")
    try:
        response = session.get(url, stream=True, timeout=timeout_seconds)
        response.raise_for_status()
        content_type = response.headers.get("Content-Type", "").split(
            ";", 1
        )[0].casefold()
        _validate_content_type(
            content_type,
            allowed_content_types,
            require_content_type,
        )
        with partial_path.open("wb") as output:
            for chunk in response.iter_content(chunk_size=1024 * 1024):
                if chunk:
                    output.write(chunk)
        if partial_path.stat().st_size == 0:
            raise OSError(f"빈 파일이 다운로드되었습니다: {url}")
        partial_path.replace(destination)
        return content_type
    except (OSError, requests.RequestException):
        partial_path.unlink(missing_ok=True)
        raise


class SunoApiClient:
    """Small client for a documented Suno-compatible provider API."""

    def __init__(
        self,
        api_key: str,
        base_url: str = DEFAULT_SUNO_BASE_URL,
        session: HttpSession | None = None,
        max_attempts: int = 5,
        request_timeout: float = 60.0,
        poll_interval: float = 15.0,
        poll_jitter: float = 3.0,
        poll_timeout: float = 900.0,
        min_delay: float = 10.0,
        max_delay: float = 25.0,
        sleep_fn: Callable[[float], None] = time.sleep,
        rng: random.Random | None = None,
    ) -> None:
        self._api_key = api_key
        self._base_url = base_url.rstrip("/")
        self._session = session or requests.Session()
        self._max_attempts = max_attempts
        self._request_timeout = request_timeout
        self._poll_interval = poll_interval
        self._poll_jitter = poll_jitter
        self._poll_timeout = poll_timeout
        self._min_delay = min_delay
        self._max_delay = max_delay
        self._sleep_fn = sleep_fn
        self._rng = rng or random.Random()

    def generate_and_wait(self, request: TrackRequest) -> list[TrackRecord]:
        """Create a task, poll it, and return all generated clips."""
        self._sleep_random_delay()
        response = self._request_json(
            "POST", "generate", body=self._payload(request)
        )
        task_id = _extract_task_id(response)
        deadline = time.monotonic() + self._poll_timeout
        while time.monotonic() < deadline:
            status_response = self._request_json(
                "GET",
                "generate/record-info",
                params={"taskId": task_id},
            )
            status = _extract_status(status_response)
            if status == "SUCCESS":
                return parse_completed_tracks(
                    status_response,
                    request.title,
                    request.instrumental,
                    request.genre,
                    datetime.now().astimezone().isoformat(timespec="seconds"),
                )
            if status in FAILED_STATUSES:
                message = _extract_error_message(status_response, status)
                raise SunoApiError(f"Suno 생성 실패: {message}")
            poll_delay = self._poll_interval + self._rng.uniform(
                0, self._poll_jitter
            )
            self._sleep_fn(poll_delay)
        raise SunoApiError(f"Suno 작업 시간 초과: {task_id}")

    @property
    def session(self) -> HttpSession:
        """Return the HTTP session used for asset downloads."""
        return self._session

    def _payload(self, request: TrackRequest) -> dict[str, Any]:
        payload: dict[str, Any] = {
            "customMode": request.custom_mode,
            "instrumental": request.instrumental,
            "model": request.model,
            "prompt": request.prompt,
        }
        if request.custom_mode:
            payload["style"] = request.genre
            payload["title"] = request.title
        callback_url = os.getenv("SUNO_CALLBACK_URL", "").strip()
        if callback_url:
            payload["callBackUrl"] = callback_url
        return payload

    def _request_json(
        self,
        method: str,
        path: str,
        *,
        params: dict[str, str] | None = None,
        body: dict[str, Any] | None = None,
    ) -> dict[str, Any]:
        url = f"{self._base_url}/{path.lstrip('/')}"
        headers = {"Authorization": f"Bearer {self._api_key}"}
        for attempt in range(1, self._max_attempts + 1):
            response = self._request_response(
                method,
                url,
                headers,
                params,
                body,
                attempt,
            )
            if response is None:
                continue
            payload = _json_payload(response)
            provider_code = _safe_int(payload.get("code"), response.status_code)
            if self._retry_response(response, provider_code, attempt):
                continue
            self._raise_api_error(response, payload, provider_code)
            return payload
        raise SunoApiError("Suno API 요청이 완료되지 않았습니다.")

    def _request_response(
        self,
        method: str,
        url: str,
        headers: dict[str, str],
        params: dict[str, str] | None,
        body: dict[str, Any] | None,
        attempt: int,
    ) -> requests.Response | None:
        try:
            return self._session.request(
                method,
                url,
                headers=headers,
                params=params,
                json=body,
                timeout=self._request_timeout,
            )
        except requests.RequestException as error:
            if attempt == self._max_attempts:
                raise SunoApiError(f"Suno 네트워크 오류: {error}") from error
            self._sleep_backoff(attempt)
            return None

    def _retry_response(
        self,
        response: requests.Response,
        provider_code: int,
        attempt: int,
    ) -> bool:
        if not _is_retryable(response.status_code, provider_code):
            return False
        if attempt == self._max_attempts:
            raise SunoApiError(f"Suno 재시도 한도 초과: {provider_code}")
        self._sleep_backoff(attempt, response.headers.get("Retry-After"))
        return True

    def _raise_api_error(
        self,
        response: requests.Response,
        payload: dict[str, Any],
        provider_code: int,
    ) -> None:
        if response.status_code < 400 and provider_code < 400:
            return
        message = str(payload.get("msg", response.text[:300]))
        raise SunoApiError(f"Suno API 오류 {provider_code}: {message}")

    def _sleep_backoff(
        self, attempt: int, retry_after: str | None = None
    ) -> None:
        delay = _backoff_seconds(
            attempt,
            retry_after,
            self._rng,
            base=2.0,
            maximum=60.0,
        )
        self._sleep_fn(delay)

    def _sleep_random_delay(self) -> None:
        if self._max_delay <= 0:
            return
        lower = max(0.0, min(self._min_delay, self._max_delay))
        delay = self._rng.uniform(lower, self._max_delay)
        self._sleep_fn(delay)


class GoogleSheetsRepository:
    """Append deduplicated rows to the Tracks worksheet."""

    def __init__(
        self,
        service: Any,
        spreadsheet_id: str,
        sheet_name: str = "Tracks",
        lock_path: Path = Path(".state/tracks.append.lock"),
        max_attempts: int = 5,
        sleep_fn: Callable[[float], None] = time.sleep,
        rng: random.Random | None = None,
    ) -> None:
        self._service = service
        self._spreadsheet_id = spreadsheet_id
        self._sheet_name = sheet_name
        self._max_attempts = max_attempts
        self._sleep_fn = sleep_fn
        self._rng = rng or random.Random()
        self._lock_path = lock_path

    @classmethod
    def from_service_account_file(
        cls,
        credentials_path: Path,
        spreadsheet_id: str,
        sheet_name: str = "Tracks",
        lock_path: Path = Path(".state/tracks.append.lock"),
    ) -> GoogleSheetsRepository:
        """Build a Sheets client from a service-account JSON key."""
        from google.oauth2 import service_account
        from googleapiclient.discovery import build

        credentials = service_account.Credentials.from_service_account_file(
            str(credentials_path),
            scopes=[SHEETS_SCOPE],
        )
        service = build(
            "sheets",
            "v4",
            credentials=credentials,
            cache_discovery=False,
        )
        return cls(service, spreadsheet_id, sheet_name, lock_path=lock_path)

    def append_pending(self, record: TrackRecord) -> bool:
        """Append a Pending row unless its Track_ID is already present."""
        with self._append_lock():
            if record.track_id in self._read_track_ids():
                return False
            return self._append_with_retry(record)

    def _append_with_retry(self, record: TrackRecord) -> bool:
        row = build_sheet_row(record)
        for attempt in range(1, self._max_attempts + 1):
            try:
                self._append_once(row)
                return True
            except Exception as error:
                if record.track_id in self._read_track_ids():
                    return False
                if not _is_retryable_google_error(error):
                    raise GoogleSheetsError(
                        f"Google Sheets 기록 실패: {error}"
                    ) from error
                if attempt == self._max_attempts:
                    raise GoogleSheetsError(
                        "Google Sheets 재시도 한도 초과"
                    ) from error
                self._sleep_fn(_backoff_seconds(attempt, None, self._rng))
        return False

    def _append_lock(self) -> Any:
        try:
            from filelock import FileLock
        except ModuleNotFoundError as error:
            raise GoogleSheetsError(
                "filelock이 필요합니다. requirements.txt를 설치하세요."
            ) from error
        self._lock_path.parent.mkdir(parents=True, exist_ok=True)
        return FileLock(str(self._lock_path))

    def _read_track_ids(self) -> set[str]:
        response = self._execute_google(self._read_ids_once)
        values = response.get("values", [])
        return {
            str(row[0])
            for row in values
            if isinstance(row, list) and row and str(row[0]).strip()
        }

    def _read_ids_once(self) -> Any:
        return (
            self._service.spreadsheets()
            .values()
            .get(
                spreadsheetId=self._spreadsheet_id,
                range=f"'{self._sheet_name}'!A2:A",
            )
            .execute()
        )

    def _append_once(self, row: list[str]) -> None:
        (
            self._service.spreadsheets()
            .values()
            .append(
                spreadsheetId=self._spreadsheet_id,
                range=f"'{self._sheet_name}'!A:H",
                valueInputOption="RAW",
                insertDataOption="INSERT_ROWS",
                body={"majorDimension": "ROWS", "values": [row]},
            )
            .execute()
        )

    def _execute_google(self, operation: Callable[[], Any]) -> Any:
        for attempt in range(1, self._max_attempts + 1):
            try:
                return operation()
            except Exception as error:
                if not _is_retryable_google_error(error):
                    raise GoogleSheetsError(
                        f"Google Sheets 읽기 실패: {error}"
                    ) from error
                if attempt == self._max_attempts:
                    raise GoogleSheetsError(
                        "Google Sheets 읽기 재시도 한도 초과"
                    ) from error
                self._sleep_fn(_backoff_seconds(attempt, None, self._rng))
        raise GoogleSheetsError("Google Sheets 요청이 완료되지 않았습니다.")


def save_track_assets(
    session: HttpSession,
    record: TrackRecord,
    output_dir: Path,
) -> dict[str, Path]:
    """Download audio, cover, and lyrics for a generated track."""
    stem = _safe_filename(record.track_id or record.title)
    existing_paths = set(output_dir.glob(f"{stem}*"))
    assets: dict[str, Path] = {}
    try:
        assets["audio"] = _download_with_extension(
            session,
            record.audio_url,
            output_dir,
            stem,
            "audio",
            AUDIO_EXTENSIONS,
            AUDIO_MIME_EXTENSIONS,
        )
        assets["cover"] = _download_with_extension(
            session,
            record.cover_url,
            output_dir,
            stem,
            "cover",
            COVER_EXTENSIONS,
            COVER_MIME_EXTENSIONS,
        )
        assets["lyrics"] = output_dir / f"{stem}.txt"
        assets["lyrics"].write_text(record.lyrics, encoding="utf-8")
        assets["metadata"] = output_dir / f"{stem}.track.json"
        _write_track_sidecar(record, assets, assets["metadata"])
        return assets
    except (OSError, requests.RequestException):
        _remove_new_assets(list(assets.values()), existing_paths)
        raise


def _remove_new_assets(paths: list[Path], existing_paths: set[Path]) -> None:
    for path in paths:
        if path not in existing_paths:
            path.unlink(missing_ok=True)


def _write_track_sidecar(
    record: TrackRecord,
    assets: dict[str, Path],
    metadata_path: Path,
) -> None:
    payload = {
        "track_id": record.track_id,
        "title": record.title,
        "genre": record.genre,
        "audio_url": record.audio_url,
        "cover_url": record.cover_url,
        "audio_path": assets["audio"].name,
        "cover_path": assets["cover"].name,
        "lyrics_path": assets["lyrics"].name,
        "lyrics": record.lyrics,
        "created_at": record.created_at,
        "downloaded_at": datetime.now().astimezone().isoformat(timespec="seconds"),
    }
    partial_path = metadata_path.with_name(metadata_path.name + ".part")
    partial_path.write_text(
        json.dumps(payload, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    partial_path.replace(metadata_path)


def main(argv: list[str] | None = None) -> int:
    """Run one generation task from environment variables and CLI options."""
    _load_environment()
    args = _build_parser().parse_args(argv)
    try:
        request = TrackRequest(
            title=args.title,
            genre=args.genre,
            prompt=args.prompt,
            instrumental=args.instrumental,
            custom_mode=args.custom_mode,
            model=args.model,
        )
        client = SunoApiClient(
            _required_env("SUNO_API_KEY"),
            base_url=os.getenv("SUNO_API_BASE_URL", DEFAULT_SUNO_BASE_URL),
            min_delay=args.min_delay,
            max_delay=args.max_delay,
            poll_interval=args.poll_interval,
            poll_timeout=args.poll_timeout,
        )
        repository = GoogleSheetsRepository.from_service_account_file(
            Path(_required_env("GOOGLE_SERVICE_ACCOUNT_FILE")),
            _required_env("GOOGLE_SPREADSHEET_ID"),
            os.getenv("GOOGLE_SHEET_NAME", "Tracks"),
        )
        quota = SunoDownloadQuota.from_environment()
        for record in client.generate_and_wait(request):
            _save_and_record(
                client.session,
                repository,
                quota,
                record,
                args.output,
            )
    except (
        OSError,
        SunoApiError,
        GoogleSheetsError,
        SunoQuotaExceeded,
        ValueError,
    ) as error:
        print(f"오류: {error}", file=sys.stderr)
        return 1
    return 0


def _save_and_record(
    session: HttpSession,
    repository: GoogleSheetsRepository,
    quota: SunoDownloadQuota,
    record: TrackRecord,
    output_dir: str,
) -> None:
    used = quota.reserve(record.track_id)
    print(f"Suno 다운로드 한도 사용량: {used}/{quota.limit}")
    try:
        paths = save_track_assets(session, record, Path(output_dir))
        print(f"다운로드 완료: {paths['audio']}")
    except (OSError, requests.RequestException) as error:
        print(f"경고: 로컬 다운로드 실패, URL은 시트에 기록합니다: {error}")
    if repository.append_pending(record):
        print(f"시트 기록 완료: {record.track_id}")
    else:
        print(f"중복 건너뜀: {record.track_id}")


def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Suno 음원 생성 및 Google Sheets 기록"
    )
    parser.add_argument("--title", default="Suno Track")
    parser.add_argument("--genre", required=True)
    parser.add_argument("--prompt", required=True)
    parser.add_argument("--model", default="V4_5ALL")
    parser.add_argument("--instrumental", action="store_true")
    parser.add_argument("--custom-mode", action="store_true")
    parser.add_argument("--output", default="downloads/suno")
    parser.add_argument("--min-delay", type=float, default=10.0)
    parser.add_argument("--max-delay", type=float, default=25.0)
    parser.add_argument("--poll-interval", type=float, default=15.0)
    parser.add_argument("--poll-timeout", type=float, default=900.0)
    return parser


def _required_env(name: str) -> str:
    value = os.getenv(name, "").strip()
    if not value:
        raise ValueError(f"환경변수가 필요합니다: {name}")
    return value


def _environment_int(name: str, default: int) -> int:
    value = os.getenv(name, "").strip()
    if not value:
        return default
    try:
        return int(value)
    except ValueError as error:
        raise ValueError(f"정수 환경변수가 필요합니다: {name}") from error


def _load_environment() -> None:
    try:
        from dotenv import load_dotenv
    except ModuleNotFoundError:
        return
    load_dotenv()


def _parse_track_item(
    item: Mapping[str, Any],
    fallback_title: str,
    instrumental: bool,
    genre: str,
    created_at: str,
    index: int,
) -> TrackRecord:
    audio_url = _first_text(item, ("audioUrl", "audio_url"))
    if not audio_url:
        raise SunoApiError(f"결과 {index}에 audio URL이 없습니다.")
    track_id = _first_text(item, ("id", "clipId", "clip_id"))
    default_title = (
        fallback_title if index == 1 else f"{fallback_title} {index}"
    )
    title = _first_text(item, ("title",)) or default_title
    lyrics = "" if instrumental else _first_text(item, ("lyrics", "prompt"))
    return TrackRecord(
        track_id or f"generated-{index}",
        title,
        audio_url,
        _first_text(item, ("imageUrl", "image_url")),
        lyrics,
        genre,
        created_at,
    )


def _extract_task_id(payload: Mapping[str, Any]) -> str:
    data = _as_mapping(payload.get("data", payload))
    task_id = _first_text(data, ("taskId", "task_id"))
    if not task_id:
        raise SunoApiError("Suno 응답에 taskId가 없습니다.")
    return task_id


def _extract_status(payload: Mapping[str, Any]) -> str:
    data = _as_mapping(payload.get("data", payload))
    return _first_text(data, ("status",)).upper()


def _extract_error_message(payload: Mapping[str, Any], fallback: str) -> str:
    data = _as_mapping(payload.get("data", payload))
    return (
        _first_text(data, ("errorMessage", "error_message", "msg"))
        or fallback
    )


def _json_payload(response: requests.Response) -> dict[str, Any]:
    try:
        payload = response.json()
    except ValueError as error:
        raise SunoApiError("Suno 응답이 JSON이 아닙니다.") from error
    if not isinstance(payload, dict):
        raise SunoApiError("Suno 응답 형식이 올바르지 않습니다.")
    return payload


def _is_retryable(http_status: int, provider_code: int) -> bool:
    return (
        http_status in RETRYABLE_STATUS_CODES
        or provider_code in RETRYABLE_STATUS_CODES
    )


def _is_retryable_google_error(error: Exception) -> bool:
    response = getattr(error, "resp", None)
    status = getattr(response, "status", None)
    if status in {429, 500, 502, 503, 504}:
        return True
    return isinstance(error, (OSError, TimeoutError))


def _backoff_seconds(
    attempt: int,
    retry_after: str | None,
    rng: random.Random,
    base: float = 1.0,
    maximum: float = 30.0,
) -> float:
    if retry_after:
        try:
            return min(maximum, max(0.0, float(retry_after)))
        except ValueError:
            pass
    exponential = min(maximum, base * (2 ** (attempt - 1)))
    return min(maximum, exponential + rng.uniform(0, base))


def _as_mapping(value: Any) -> Mapping[str, Any]:
    return value if isinstance(value, Mapping) else {}


def _first_text(mapping: Mapping[str, Any], keys: tuple[str, ...]) -> str:
    for key in keys:
        value = mapping.get(key)
        if value is not None and str(value).strip():
            return str(value).strip()
    return ""


def _safe_int(value: Any, fallback: int) -> int:
    try:
        return int(value)
    except (TypeError, ValueError):
        return fallback


def _safe_filename(value: str) -> str:
    cleaned = re.sub(r"[^\w.-]+", "-", value, flags=re.UNICODE).strip(".-")
    return cleaned or "untitled"


def _url_extension(url: str, default: str, allowed: set[str]) -> str:
    extension = Path(urlparse(url).path).suffix.casefold()
    return extension if extension in allowed else default


def _validate_content_type(
    content_type: str,
    allowed_content_types: tuple[str, ...],
    require_content_type: bool,
) -> None:
    """Reject media types that do not match the requested category."""
    if require_content_type and not content_type:
        raise OSError("Content-Type이 없어 파일 형식을 확인할 수 없습니다.")
    if not content_type or not allowed_content_types:
        return
    matches_type = any(
        content_type.startswith(value) for value in allowed_content_types
    )
    if not matches_type:
        raise OSError(f"예상하지 못한 Content-Type입니다: {content_type}")


def _download_with_extension(
    session: HttpSession,
    url: str,
    output_dir: Path,
    stem: str,
    asset_kind: str,
    allowed_extensions: set[str],
    mime_extensions: dict[str, tuple[str, ...]],
) -> Path:
    """Download an asset and choose its extension from URL or Content-Type."""
    url_extension = _url_extension(url, "", allowed_extensions)
    temporary_path = output_dir / f"{stem}.{asset_kind}.download"
    try:
        content_type = download_asset(
            session,
            url,
            temporary_path,
            allowed_content_types=(
                "audio/" if asset_kind == "audio" else "image/",
            ),
            require_content_type=not bool(url_extension),
        )
        known_extensions = mime_extensions.get(content_type, ())
        if (
            url_extension
            and known_extensions
            and url_extension not in known_extensions
        ):
            raise OSError(f"URL 확장자와 Content-Type이 다릅니다: {url}")
        extension = url_extension or (
            known_extensions[0] if known_extensions else ""
        )
        if not extension:
            raise OSError(f"파일 확장자를 판별할 수 없습니다: {url}")
        destination = output_dir / f"{stem}{extension}"
        temporary_path.replace(destination)
        return destination
    finally:
        temporary_path.unlink(missing_ok=True)


if __name__ == "__main__":
    raise SystemExit(main())
