"""Generate Suno tracks from a local queue and save their assets."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import sys
import uuid
from collections.abc import Callable, Mapping
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Any, Protocol

from .suno_ingest import (
    DEFAULT_SUNO_BASE_URL,
    SunoApiClient,
    SunoDownloadQuota,
    SunoQuotaExceeded,
    SunoApiError,
    TrackRecord,
    TrackRequest,
    save_track_assets,
)


class GenerationClient(Protocol):
    """Subset of the Suno client needed by the queue runner."""

    session: Any

    def generate_and_wait(self, request: TrackRequest) -> list[TrackRecord]:
        """Generate and wait for all clips in one queue job."""


class PendingGenerationError(RuntimeError):
    """Raised when a previous generation needs manual reconciliation."""


@dataclass(frozen=True)
class GenerationJob:
    """One local prompt and its stable identity."""

    job_id: str
    request: TrackRequest

    def fingerprint(self) -> str:
        """Return a digest that detects edits to an existing job."""
        payload = {
            "title": self.request.title,
            "genre": self.request.genre,
            "prompt": self.request.prompt,
            "instrumental": self.request.instrumental,
            "custom_mode": self.request.custom_mode,
            "model": self.request.model,
        }
        serialized = json.dumps(payload, ensure_ascii=False, sort_keys=True)
        return hashlib.sha256(serialized.encode("utf-8")).hexdigest()


def load_generation_jobs(path: Path) -> tuple[GenerationJob, ...]:
    """Load and validate a JSON list of local generation jobs."""
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        raise ValueError(f"생성 큐를 읽을 수 없습니다: {error}") from error
    raw_jobs = payload.get("jobs") if isinstance(payload, dict) else payload
    if not isinstance(raw_jobs, list):
        raise ValueError("생성 큐는 jobs 목록 또는 JSON 목록이어야 합니다.")
    jobs: list[GenerationJob] = []
    seen_ids: set[str] = set()
    for raw_job in raw_jobs:
        job = _parse_job(raw_job)
        if job.job_id in seen_ids:
            raise ValueError(f"생성 큐에 중복 job_id가 있습니다: {job.job_id}")
        seen_ids.add(job.job_id)
        jobs.append(job)
    return tuple(jobs)


def generate_queue(
    queue_path: Path,
    output_dir: Path,
    state_path: Path,
    client: GenerationClient,
    quota: SunoDownloadQuota,
    *,
    save_assets: Callable[[Any, TrackRecord, Path], dict[str, Path]] = save_track_assets,
    on_saved: Callable[[Path], None] | None = None,
) -> list[Path]:
    """Generate pending jobs and return the saved sidecar paths."""
    jobs = load_generation_jobs(queue_path)
    if not jobs:
        return []
    output_dir.mkdir(parents=True, exist_ok=True)
    saved_paths: list[Path] = []
    with _run_lock(state_path):
        state = _read_state(state_path)
        for job in jobs:
            if _skip_completed_or_raise_pending(state, job):
                continue
            _mark_pending(state, state_path, job)
            records = client.generate_and_wait(job.request)
            paths = _save_records(
                client, records, output_dir, quota, save_assets, on_saved
            )
            saved_paths.extend(paths)
            _mark_completed(state, state_path, job, records)
    return saved_paths


def resolve_pending_generation(
    state_path: Path,
    job_id: str,
    track_ids: list[str],
) -> None:
    """Record a manually verified pending job without calling the provider."""
    normalized_ids = list(dict.fromkeys(item.strip() for item in track_ids))
    if not job_id.strip() or not normalized_ids or "" in normalized_ids:
        raise ValueError("수동 복구에는 job_id와 하나 이상의 Track_ID가 필요합니다.")
    with _run_lock(state_path):
        state = _read_state(state_path)
        pending = state["pending"].get(job_id)
        if not isinstance(pending, dict):
            raise ValueError(f"pending 생성 작업을 찾을 수 없습니다: {job_id}")
        if job_id in state["completed"]:
            raise ValueError(f"이미 완료 처리된 생성 작업입니다: {job_id}")
        state["completed"][job_id] = {
            "fingerprint": pending["fingerprint"],
            "track_ids": normalized_ids,
            "reconciled": True,
        }
        state["pending"].pop(job_id)
        _write_state(state_path, state)


def write_generation_report(
    path: Path,
    state_path: Path,
    saved_paths: list[Path],
    *,
    run_status: str = "completed",
    error: str = "",
) -> Path:
    """Write a local report for successful or failed generation runs."""
    completed, pending = _state_summary(state_path)
    payload: dict[str, Any] = {
        "generated_at": datetime.now().astimezone().isoformat(timespec="seconds"),
        "run_status": run_status,
        "summary": {
            "saved_track_count": len(saved_paths),
            "completed_job_count": completed,
            "pending_job_count": len(pending),
        },
        "saved_metadata_paths": [str(item) for item in saved_paths],
        "pending_job_ids": pending,
    }
    if error:
        payload["error"] = error
    with _report_lock(path):
        _write_json_file(path, payload)
    return path


def main(argv: list[str] | None = None) -> int:
    """Generate queued tracks using environment-configured API credentials."""
    _load_environment()
    parser = _build_parser()
    args = parser.parse_args(argv)
    state_path = Path(args.state)
    report_path = Path(args.report) if args.report else None
    saved_paths: list[Path] = []
    try:
        if args.resolve_pending_job:
            resolve_pending_generation(
                state_path, args.resolve_pending_job, args.resolve_track_id
            )
            print(f"pending 생성 작업을 수동 복구했습니다: {args.resolve_pending_job}")
            return 0
        if not args.queue or not args.output:
            parser.error("일반 생성에는 --queue와 --output이 필요합니다.")
        queue_path = Path(args.queue)
        if not load_generation_jobs(queue_path):
            if report_path:
                write_generation_report(report_path, state_path, [])
            print("생성 큐가 비어 있습니다.")
            return 0
        client = SunoApiClient(
            _required_env("SUNO_API_KEY"),
            base_url=os.getenv("SUNO_API_BASE_URL", DEFAULT_SUNO_BASE_URL),
        )
        quota = SunoDownloadQuota.from_environment()
        paths = generate_queue(
            queue_path,
            Path(args.output),
            state_path,
            client,
            quota,
            on_saved=saved_paths.append,
        )
    except (OSError, RuntimeError, SunoApiError, SunoQuotaExceeded, ValueError) as error:
        if report_path:
            _write_failure_report(report_path, state_path, saved_paths, error)
        print(f"오류: {error}", file=sys.stderr)
        return 1
    if report_path:
        write_generation_report(report_path, state_path, paths)
    print(f"생성·저장 완료: 새 트랙 {len(paths)}개")
    return 0


def _parse_job(raw_job: object) -> GenerationJob:
    if not isinstance(raw_job, Mapping):
        raise ValueError("생성 큐의 각 항목은 JSON 객체여야 합니다.")
    job_id = _required_text(raw_job, "job_id")
    request = TrackRequest(
        title=_required_text(raw_job, "title"),
        genre=_required_text(raw_job, "genre"),
        prompt=_required_text(raw_job, "prompt"),
        instrumental=_optional_bool(raw_job, "instrumental", False),
        custom_mode=_optional_bool(raw_job, "custom_mode", True),
        model=_optional_text(raw_job, "model", "V4_5ALL"),
    )
    return GenerationJob(job_id, request)


def _required_text(payload: Mapping[str, Any], name: str) -> str:
    value = payload.get(name)
    if not isinstance(value, str) or not value.strip():
        raise ValueError(f"생성 큐의 {name}은(는) 비어 있지 않은 문자열이어야 합니다.")
    return value.strip()


def _optional_text(payload: Mapping[str, Any], name: str, default: str) -> str:
    value = payload.get(name, default)
    if not isinstance(value, str) or not value.strip():
        raise ValueError(f"생성 큐의 {name}은(는) 문자열이어야 합니다.")
    return value.strip()


def _optional_bool(payload: Mapping[str, Any], name: str, default: bool) -> bool:
    value = payload.get(name, default)
    if not isinstance(value, bool):
        raise ValueError(f"생성 큐의 {name}은(는) true 또는 false여야 합니다.")
    return value


def _skip_completed_or_raise_pending(
    state: dict[str, Any],
    job: GenerationJob,
) -> bool:
    fingerprint = job.fingerprint()
    completed = state["completed"].get(job.job_id)
    pending = state["pending"].get(job.job_id)
    for entry in (completed, pending):
        if isinstance(entry, dict) and entry.get("fingerprint") != fingerprint:
            raise ValueError(f"기존 job_id의 프롬프트가 변경되었습니다: {job.job_id}")
    if isinstance(completed, dict):
        return True
    if isinstance(pending, dict):
        raise PendingGenerationError(
            f"중단된 생성 작업이 있어 수동 확인이 필요합니다: {job.job_id}"
        )
    return False


def _mark_pending(
    state: dict[str, Any],
    state_path: Path,
    job: GenerationJob,
) -> None:
    state["pending"][job.job_id] = {
        "fingerprint": job.fingerprint(),
        "started_at": datetime.now().astimezone().isoformat(timespec="seconds"),
    }
    _write_state(state_path, state)


def _save_records(
    client: GenerationClient,
    records: list[TrackRecord],
    output_dir: Path,
    quota: SunoDownloadQuota,
    save_assets: Callable[[Any, TrackRecord, Path], dict[str, Path]],
    on_saved: Callable[[Path], None] | None,
) -> list[Path]:
    if not records:
        raise RuntimeError("Suno 생성 응답에 저장할 트랙이 없습니다.")
    metadata_paths: list[Path] = []
    for record in records:
        quota.reserve(record.track_id)
        assets = save_assets(client.session, record, output_dir)
        metadata_path = assets.get("metadata")
        if not isinstance(metadata_path, Path):
            raise RuntimeError("음원 저장 결과에 metadata 경로가 없습니다.")
        metadata_paths.append(metadata_path)
        if on_saved:
            on_saved(metadata_path)
    return metadata_paths


def _mark_completed(
    state: dict[str, Any],
    state_path: Path,
    job: GenerationJob,
    records: list[TrackRecord],
) -> None:
    state["pending"].pop(job.job_id, None)
    state["completed"][job.job_id] = {
        "fingerprint": job.fingerprint(),
        "track_ids": [record.track_id for record in records],
        "completed_at": datetime.now().astimezone().isoformat(timespec="seconds"),
    }
    _write_state(state_path, state)


def _read_state(path: Path) -> dict[str, Any]:
    if not path.is_file():
        return {"completed": {}, "pending": {}}
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        raise RuntimeError(f"생성 상태 파일을 읽을 수 없습니다: {error}") from error
    if not isinstance(payload, dict):
        raise RuntimeError("생성 상태 파일 형식이 올바르지 않습니다.")
    for name in ("completed", "pending"):
        if not isinstance(payload.get(name, {}), dict):
            raise RuntimeError("생성 상태 파일 형식이 올바르지 않습니다.")
    payload.setdefault("completed", {})
    payload.setdefault("pending", {})
    return payload


def _write_state(path: Path, payload: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    partial = path.with_name(f"{path.name}.{uuid.uuid4().hex}.part")
    try:
        partial.write_text(
            json.dumps(payload, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )
        partial.replace(path)
    finally:
        partial.unlink(missing_ok=True)


def _state_summary(path: Path) -> tuple[int, list[str]]:
    try:
        state = _read_state(path)
    except (OSError, RuntimeError):
        return 0, []
    return len(state["completed"]), sorted(state["pending"])


def _write_failure_report(
    path: Path,
    state_path: Path,
    saved_paths: list[Path],
    error: Exception,
) -> None:
    try:
        write_generation_report(
            path,
            state_path,
            saved_paths,
            run_status="failed",
            error=str(error),
        )
    except (OSError, RuntimeError):
        return


def _report_lock(path: Path) -> Any:
    try:
        from filelock import FileLock
    except ModuleNotFoundError as error:
        raise RuntimeError("filelock이 필요합니다. requirements.txt를 설치하세요.") from error
    path.parent.mkdir(parents=True, exist_ok=True)
    return FileLock(str(path.with_name(path.name + ".lock")))


def _write_json_file(path: Path, payload: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    partial = path.with_name(f"{path.name}.{uuid.uuid4().hex}.part")
    try:
        partial.write_text(
            json.dumps(payload, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )
        partial.replace(path)
    finally:
        partial.unlink(missing_ok=True)


def _run_lock(path: Path) -> Any:
    try:
        from filelock import FileLock
    except ModuleNotFoundError as error:
        raise RuntimeError("filelock이 필요합니다. requirements.txt를 설치하세요.") from error
    path.parent.mkdir(parents=True, exist_ok=True)
    return FileLock(str(path.with_name(path.name + ".run.lock")))


def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Suno 생성 큐 자동 처리")
    parser.add_argument("--queue", help="생성 작업 JSON 경로")
    parser.add_argument("--output", help="생성 음원 저장 폴더")
    parser.add_argument("--state", default=".state/suno-generation.json")
    parser.add_argument("--report", help="생성 운영 보고서 JSON 경로")
    parser.add_argument("--resolve-pending-job")
    parser.add_argument("--resolve-track-id", action="append", default=[])
    return parser


def _required_env(name: str) -> str:
    value = os.getenv(name, "").strip()
    if not value:
        raise ValueError(f"환경변수가 필요합니다: {name}")
    return value


def _load_environment() -> None:
    try:
        from dotenv import load_dotenv
    except ModuleNotFoundError:
        return
    load_dotenv()


if __name__ == "__main__":
    raise SystemExit(main())
