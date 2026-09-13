import json
from pathlib import Path
from typing import Any

import pytest

from budget.suno_ingest import TrackRecord, TrackRequest
from budget.suno_generate import (
    PendingGenerationError,
    generate_queue,
    main,
    resolve_pending_generation,
    write_generation_report,
)


class FakeClient:
    def __init__(self, records: list[TrackRecord]) -> None:
        self.records = records
        self.calls: list[TrackRequest] = []
        self.session = object()

    def generate_and_wait(self, request: TrackRequest) -> list[TrackRecord]:
        self.calls.append(request)
        return self.records


class FakeQuota:
    def __init__(self) -> None:
        self.track_ids: list[str] = []

    def reserve(self, track_id: str) -> int:
        self.track_ids.append(track_id)
        return len(self.track_ids)


def write_queue(path: Path, job_id: str = "job-1") -> None:
    payload = [
        {
            "job_id": job_id,
            "title": "Midnight Signal",
            "genre": "Electronic",
            "prompt": "Warm nocturnal synths with emotional vocals",
            "custom_mode": True,
        }
    ]
    path.write_text(json.dumps(payload), encoding="utf-8")


def write_two_job_queue(path: Path) -> None:
    payload = {
        "jobs": [
            {
                "job_id": "job-1",
                "title": "First Signal",
                "genre": "Electronic",
                "prompt": "Warm synths",
            },
            {
                "job_id": "job-2",
                "title": "Second Signal",
                "genre": "Electronic",
                "prompt": "Wide pads",
            },
        ]
    }
    path.write_text(json.dumps(payload), encoding="utf-8")


def make_records() -> list[TrackRecord]:
    return [
        TrackRecord("clip-1", "Midnight Signal", "audio-1", "cover-1", "lyrics"),
        TrackRecord("clip-2", "Midnight Signal Alt", "audio-2", "cover-2", "lyrics"),
    ]


def save_fake_assets(
    session: object,
    record: TrackRecord,
    output_dir: Path,
) -> dict[str, Path]:
    del session
    output_dir.mkdir(parents=True, exist_ok=True)
    metadata = output_dir / f"{record.track_id}.track.json"
    metadata.write_text(record.track_id, encoding="utf-8")
    return {"metadata": metadata}


def test_generate_queue_saves_tracks_and_skips_completed_job(
    tmp_path: Path,
) -> None:
    queue_path = tmp_path / "queue.json"
    state_path = tmp_path / "generation.json"
    output_dir = tmp_path / "downloads"
    write_queue(queue_path)
    client = FakeClient(make_records())
    quota = FakeQuota()

    first = generate_queue(
        queue_path,
        output_dir,
        state_path,
        client,
        quota,
        save_assets=save_fake_assets,
    )
    second = generate_queue(
        queue_path,
        output_dir,
        state_path,
        client,
        quota,
        save_assets=save_fake_assets,
    )

    assert len(first) == 2
    assert second == []
    assert len(client.calls) == 1
    assert quota.track_ids == ["clip-1", "clip-2"]
    state = json.loads(state_path.read_text(encoding="utf-8"))
    assert state["completed"]["job-1"]["track_ids"] == ["clip-1", "clip-2"]


def test_pending_job_blocks_automatic_duplicate_generation(tmp_path: Path) -> None:
    queue_path = tmp_path / "queue.json"
    state_path = tmp_path / "generation.json"
    write_queue(queue_path)

    class FailingClient(FakeClient):
        def generate_and_wait(self, request: TrackRequest) -> list[TrackRecord]:
            self.calls.append(request)
            raise RuntimeError("provider connection lost")

    client = FailingClient(make_records())
    with pytest.raises(RuntimeError, match="provider connection lost"):
        generate_queue(
            queue_path,
            tmp_path / "downloads",
            state_path,
            client,
            FakeQuota(),
            save_assets=save_fake_assets,
        )

    with pytest.raises(PendingGenerationError, match="job-1"):
        generate_queue(
            queue_path,
            tmp_path / "downloads",
            state_path,
            FakeClient(make_records()),
            FakeQuota(),
            save_assets=save_fake_assets,
        )


def test_resolve_pending_generation_marks_job_without_api_call(
    tmp_path: Path,
) -> None:
    queue_path = tmp_path / "queue.json"
    state_path = tmp_path / "generation.json"
    write_queue(queue_path)
    with pytest.raises(RuntimeError):
        generate_queue(
            queue_path,
            tmp_path / "downloads",
            state_path,
            FailingClient(make_records()),
            FakeQuota(),
            save_assets=save_fake_assets,
        )

    resolve_pending_generation(state_path, "job-1", ["clip-1", "clip-2"])
    client = FakeClient(make_records())
    assert generate_queue(
        queue_path,
        tmp_path / "downloads",
        state_path,
        client,
        FakeQuota(),
        save_assets=save_fake_assets,
    ) == []
    assert client.calls == []


def test_generation_report_records_pending_job_and_error(tmp_path: Path) -> None:
    state_path = tmp_path / "generation.json"
    state_path.write_text(
        json.dumps(
            {
                "completed": {},
                "pending": {"job-1": {"fingerprint": "digest"}},
            }
        ),
        encoding="utf-8",
    )
    report_path = tmp_path / "report.json"

    write_generation_report(
        report_path,
        state_path,
        [tmp_path / "clip-1.track.json"],
        run_status="failed",
        error="provider connection lost",
    )

    report = json.loads(report_path.read_text(encoding="utf-8"))
    assert report["run_status"] == "failed"
    assert report["summary"]["saved_track_count"] == 1
    assert report["pending_job_ids"] == ["job-1"]
    assert report["error"] == "provider connection lost"


def test_main_writes_generation_failure_report(
    tmp_path: Path,
    monkeypatch: Any,
) -> None:
    queue_path = tmp_path / "queue.json"
    state_path = tmp_path / "generation.json"
    report_path = tmp_path / "report.json"
    write_queue(queue_path)
    monkeypatch.setenv("SUNO_API_KEY", "key")
    monkeypatch.setattr(
        "budget.suno_generate.SunoApiClient",
        lambda *args, **kwargs: FailingClient(make_records()),
    )

    result = main(
        [
            "--queue",
            str(queue_path),
            "--output",
            str(tmp_path / "downloads"),
            "--state",
            str(state_path),
            "--report",
            str(report_path),
        ]
    )

    assert result == 1
    report = json.loads(report_path.read_text(encoding="utf-8"))
    assert report["run_status"] == "failed"
    assert report["pending_job_ids"] == ["job-1"]
    assert report["error"] == "provider connection lost"


def test_main_preserves_partial_generation_report(
    tmp_path: Path,
    monkeypatch: Any,
) -> None:
    queue_path = tmp_path / "queue.json"
    state_path = tmp_path / "generation.json"
    report_path = tmp_path / "report.json"
    write_two_job_queue(queue_path)
    partial_path = tmp_path / "clip-1.track.json"
    monkeypatch.setenv("SUNO_API_KEY", "key")
    monkeypatch.setattr(
        "budget.suno_generate.SunoApiClient",
        lambda *args, **kwargs: FakeClient(make_records()),
    )

    def fail_after_one(*args: Any, **kwargs: Any) -> list[Path]:
        kwargs["on_saved"](partial_path)
        raise RuntimeError("second job failed")

    monkeypatch.setattr("budget.suno_generate.generate_queue", fail_after_one)
    result = main(
        [
            "--queue",
            str(queue_path),
            "--output",
            str(tmp_path / "downloads"),
            "--state",
            str(state_path),
            "--report",
            str(report_path),
        ]
    )

    assert result == 1
    report = json.loads(report_path.read_text(encoding="utf-8"))
    assert report["summary"]["saved_track_count"] == 1
    assert report["saved_metadata_paths"] == [str(partial_path)]
    assert report["error"] == "second job failed"


class FailingClient(FakeClient):
    def generate_and_wait(self, request: TrackRequest) -> list[TrackRecord]:
        self.calls.append(request)
        raise RuntimeError("provider connection lost")


class PartialClient(FakeClient):
    def generate_and_wait(self, request: TrackRequest) -> list[TrackRecord]:
        self.calls.append(request)
        if len(self.calls) == 2:
            raise RuntimeError("second job failed")
        return [self.records[0]]


def test_generate_queue_preserves_partial_saved_paths_on_failure(
    tmp_path: Path,
) -> None:
    queue_path = tmp_path / "queue.json"
    state_path = tmp_path / "generation.json"
    write_two_job_queue(queue_path)
    saved_paths: list[Path] = []

    with pytest.raises(RuntimeError, match="second job failed"):
        generate_queue(
            queue_path,
            tmp_path / "downloads",
            state_path,
            PartialClient(make_records()),
            FakeQuota(),
            save_assets=save_fake_assets,
            on_saved=saved_paths.append,
        )

    assert len(saved_paths) == 1
    assert saved_paths[0].name == "clip-1.track.json"
