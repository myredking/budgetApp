"""Upload album listening videos through the official YouTube Data API."""

from __future__ import annotations

import hashlib
import json
import random
import time
import argparse
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Callable


YOUTUBE_UPLOAD_SCOPE = "https://www.googleapis.com/auth/youtube.upload"
RETRYABLE_HTTP_STATUSES = {408, 429, 500, 502, 503, 504}


@dataclass(frozen=True)
class YouTubeUploadPolicy:
    """Safe defaults for AI music uploads."""

    privacy_status: str = "private"
    contains_synthetic_media: bool = True
    notify_subscribers: bool = False
    made_for_kids: bool = False
    category_id: str = "10"
    allow_public: bool = False

    def validate(self) -> None:
        if self.privacy_status not in {"private", "unlisted", "public"}:
            raise ValueError("YouTube 공개 범위는 private, unlisted, public 중 하나여야 합니다.")
        if self.privacy_status == "public" and not self.allow_public:
            raise ValueError("공개 업로드는 정책상 허용되지 않습니다. 검토 후 직접 공개하세요.")
        if not self.contains_synthetic_media:
            raise ValueError("Suno 음원은 합성 미디어 표시를 켜야 합니다.")


@dataclass(frozen=True)
class YouTubeVideo:
    """Metadata sent with one YouTube video upload."""

    title: str
    description: str
    tags: tuple[str, ...] = ()
    category_id: str = "10"
    default_language: str = "ko"


@dataclass(frozen=True)
class YouTubeUploadResult:
    """Stable result stored for idempotent retries."""

    video_id: str
    url: str
    dry_run: bool = False


def load_youtube_video(path: Path) -> YouTubeVideo:
    """Load the YouTube metadata manifest written into an album package."""
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        raise ValueError(f"YouTube metadata를 읽을 수 없습니다: {error}") from error
    if not isinstance(payload, dict):
        raise ValueError("YouTube metadata는 객체여야 합니다.")
    title = _required_text(payload, "title")
    description = _required_text(payload, "description")
    tags = payload.get("tags", [])
    if not isinstance(tags, list) or not all(isinstance(tag, str) for tag in tags):
        raise ValueError("YouTube tags는 문자열 목록이어야 합니다.")
    return YouTubeVideo(
        title,
        description,
        tuple(tags),
        str(payload.get("category_id", "10")),
        str(payload.get("default_language", "ko")),
    )


class YouTubeUploader:
    """Perform resumable, deduplicated uploads with injected or OAuth service."""

    def __init__(
        self,
        service: Any | None = None,
        policy: YouTubeUploadPolicy | None = None,
        state_path: Path = Path(".state/youtube-uploads.json"),
        client_secrets_path: Path = Path("secrets/youtube-client.json"),
        token_path: Path = Path(".state/youtube-token.json"),
        max_attempts: int = 5,
        sleep_fn: Callable[[float], None] = time.sleep,
        rng: random.Random | None = None,
    ) -> None:
        self._service = service
        self._policy = policy or YouTubeUploadPolicy()
        self._state_path = state_path
        self._client_secrets_path = client_secrets_path
        self._token_path = token_path
        self._max_attempts = max_attempts
        self._sleep_fn = sleep_fn
        self._rng = rng or random.Random()

    def upload(
        self,
        video_path: Path,
        video: YouTubeVideo,
        *,
        dry_run: bool = False,
    ) -> YouTubeUploadResult:
        """Upload a video once, or return its previously recorded result."""
        self._policy.validate()
        if not video_path.is_file():
            raise ValueError(f"YouTube 영상 파일을 찾을 수 없습니다: {video_path}")
        file_hash = _file_sha256(video_path)
        with self._state_lock():
            previous = self._find_previous(file_hash)
            if previous:
                return previous
            if dry_run:
                return YouTubeUploadResult("", "", dry_run=True)
            service = self._service or self._build_service()
            body = self._video_body(video)
            result = self._send_upload(service, video_path, body)
            self._record(file_hash, video, result)
            return result

    def _video_body(self, video: YouTubeVideo) -> dict[str, Any]:
        return {
            "snippet": {
                "title": video.title,
                "description": video.description,
                "tags": list(dict.fromkeys(video.tags)),
                "categoryId": video.category_id or self._policy.category_id,
                "defaultLanguage": video.default_language,
            },
            "status": {
                "privacyStatus": self._policy.privacy_status,
                "selfDeclaredMadeForKids": self._policy.made_for_kids,
                "containsSyntheticMedia": self._policy.contains_synthetic_media,
            },
        }

    def _send_upload(
        self,
        service: Any,
        video_path: Path,
        body: dict[str, Any],
    ) -> YouTubeUploadResult:
        try:
            from googleapiclient.http import MediaFileUpload
        except ModuleNotFoundError as error:
            raise RuntimeError("google-api-python-client 설치가 필요합니다.") from error
        request = service.videos().insert(
            part="snippet,status",
            body=body,
            notifySubscribers=self._policy.notify_subscribers,
            media_body=MediaFileUpload(str(video_path), chunksize=-1, resumable=True),
        )
        response = self._resumable_upload(request)
        video_id = str(response.get("id", "")).strip()
        if not video_id:
            raise RuntimeError("YouTube 업로드 응답에 video id가 없습니다.")
        return YouTubeUploadResult(video_id, f"https://youtu.be/{video_id}")

    def _resumable_upload(self, request: Any) -> dict[str, Any]:
        for attempt in range(1, self._max_attempts + 1):
            try:
                response = self._finish_chunks(request)
                return response
            except Exception as error:
                if not _is_retryable(error) or attempt == self._max_attempts:
                    raise
                self._sleep_fn(_backoff_seconds(attempt, self._rng))
        raise RuntimeError("YouTube 업로드가 완료되지 않았습니다.")

    def _finish_chunks(self, request: Any) -> dict[str, Any]:
        response: dict[str, Any] | None = None
        while response is None:
            status, response = request.next_chunk()
            if status is None and response is None:
                raise RuntimeError("YouTube 업로드 청크 응답이 빈 응답입니다.")
        return response

    def _state_lock(self) -> Any:
        try:
            from filelock import FileLock
        except ModuleNotFoundError as error:
            raise RuntimeError("filelock 설치가 필요합니다.") from error
        lock_path = self._state_path.with_suffix(self._state_path.suffix + ".lock")
        lock_path.parent.mkdir(parents=True, exist_ok=True)
        return FileLock(str(lock_path))

    def _find_previous(self, file_hash: str) -> YouTubeUploadResult | None:
        payload = _read_state(self._state_path)
        record = payload.get("uploads", {}).get(file_hash)
        if not isinstance(record, dict):
            return None
        video_id = str(record.get("video_id", "")).strip()
        url = str(record.get("url", "")).strip()
        if not video_id or not url:
            return None
        return YouTubeUploadResult(video_id, url)

    def _record(
        self,
        file_hash: str,
        video: YouTubeVideo,
        result: YouTubeUploadResult,
    ) -> None:
        payload = _read_state(self._state_path)
        uploads = payload.setdefault("uploads", {})
        uploads[file_hash] = {
            "video_id": result.video_id,
            "url": result.url,
            "title": video.title,
        }
        self._state_path.parent.mkdir(parents=True, exist_ok=True)
        partial = self._state_path.with_name(self._state_path.name + ".part")
        partial.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        partial.replace(self._state_path)

    def _build_service(self) -> Any:
        try:
            from google.auth.transport.requests import Request
            from google.oauth2.credentials import Credentials
            from google_auth_oauthlib.flow import InstalledAppFlow
            from googleapiclient.discovery import build
        except ModuleNotFoundError as error:
            raise RuntimeError("YouTube OAuth 패키지를 설치해야 합니다.") from error
        credentials = _load_credentials(self._token_path, Credentials, Request)
        if credentials is None or not credentials.valid:
            flow = InstalledAppFlow.from_client_secrets_file(
                str(self._client_secrets_path),
                [YOUTUBE_UPLOAD_SCOPE],
            )
            credentials = flow.run_local_server(port=0)
            self._token_path.parent.mkdir(parents=True, exist_ok=True)
            self._token_path.write_text(credentials.to_json(), encoding="utf-8")
        return build("youtube", "v3", credentials=credentials, cache_discovery=False)


def _load_credentials(token_path: Path, credentials_type: Any, request_type: Any) -> Any:
    if not token_path.is_file():
        return None
    credentials = credentials_type.from_authorized_user_file(str(token_path), [YOUTUBE_UPLOAD_SCOPE])
    if credentials.expired and credentials.refresh_token:
        credentials.refresh(request_type())
        token_path.write_text(credentials.to_json(), encoding="utf-8")
    return credentials


def _read_state(path: Path) -> dict[str, Any]:
    if not path.is_file():
        return {"uploads": {}}
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        raise RuntimeError(f"YouTube 상태 파일을 읽을 수 없습니다: {error}") from error
    if not isinstance(payload, dict) or not isinstance(payload.get("uploads", {}), dict):
        raise RuntimeError("YouTube 상태 파일 형식이 올바르지 않습니다.")
    return payload


def _file_sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def _is_retryable(error: Exception) -> bool:
    status = getattr(getattr(error, "resp", None), "status", None)
    return status in RETRYABLE_HTTP_STATUSES or isinstance(error, (OSError, TimeoutError))


def _backoff_seconds(attempt: int, rng: random.Random) -> float:
    return min(60.0, (2 ** (attempt - 1)) + rng.uniform(0.0, 1.0))


def _required_text(payload: dict[str, Any], name: str) -> str:
    value = payload.get(name)
    if not isinstance(value, str) or not value.strip():
        raise ValueError(f"YouTube metadata에 {name} 값이 필요합니다.")
    return value.strip()


def main(argv: list[str] | None = None) -> int:
    """Upload one rendered album video with safe defaults."""
    parser = argparse.ArgumentParser(description="YouTube 앨범 영상 업로드")
    parser.add_argument("--video", required=True)
    parser.add_argument("--metadata", required=True)
    parser.add_argument("--state", default=".state/youtube-uploads.json")
    parser.add_argument("--client-secrets", default="secrets/youtube-client.json")
    parser.add_argument("--token", default=".state/youtube-token.json")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args(argv)
    try:
        result = YouTubeUploader(
            state_path=Path(args.state),
            client_secrets_path=Path(args.client_secrets),
            token_path=Path(args.token),
        ).upload(
            Path(args.video),
            load_youtube_video(Path(args.metadata)),
            dry_run=args.dry_run,
        )
    except (OSError, RuntimeError, ValueError) as error:
        parser.error(str(error))
    message = "점검 완료" if result.dry_run else f"업로드 완료: {result.url}"
    print(message)
    return 0


if __name__ == "__main__":  # pragma: no cover
    raise SystemExit(main())
