import json
from datetime import datetime, timezone
from pathlib import Path
import random
import sys
from types import ModuleType
from typing import Any

import pytest

from budget.suno_ingest import (
    GoogleSheetsRepository,
    SunoDownloadQuota,
    SunoQuotaExceeded,
    TrackRecord,
    build_sheet_row,
    parse_completed_tracks,
    save_track_assets,
)


class FakeFileLock:
    def __init__(self, path: str) -> None:
        self.path = path

    def __enter__(self) -> "FakeFileLock":
        return self

    def __exit__(
        self,
        exc_type: Any,
        exc_value: Any,
        traceback: Any,
    ) -> None:
        return None


def install_fake_filelock(monkeypatch: Any) -> None:
    module = ModuleType("filelock")
    setattr(module, "FileLock", FakeFileLock)
    monkeypatch.setitem(sys.modules, "filelock", module)


def test_parse_completed_tracks_maps_suno_response_to_track_records() -> None:
    payload: dict[str, Any] = {
        "data": {
            "status": "SUCCESS",
            "response": {
                "sunoData": [
                    {
                        "id": "clip-1",
                        "title": "Midnight Signal",
                        "audioUrl": "https://cdn.example/track.mp3",
                        "imageUrl": "https://cdn.example/cover.jpeg",
                        "prompt": "[Verse]\nMidnight signal",
                    }
                ]
            },
        }
    }

    records = parse_completed_tracks(payload, "Fallback title", False)

    assert records == [
        TrackRecord(
            track_id="clip-1",
            title="Midnight Signal",
            audio_url="https://cdn.example/track.mp3",
            cover_url="https://cdn.example/cover.jpeg",
            lyrics="[Verse]\nMidnight signal",
        )
    ]


def test_suno_client_retries_rate_limit_before_polling() -> None:
    from budget.suno_ingest import SunoApiClient, TrackRequest

    class FakeResponse:
        def __init__(self, status_code: int, payload: dict[str, Any]) -> None:
            self.status_code = status_code
            self.headers: dict[str, str] = {}
            self.text = str(payload)
            self._payload = payload

        def json(self) -> dict[str, Any]:
            return self._payload

    class FakeSession:
        def __init__(self) -> None:
            self.responses = [
                FakeResponse(429, {}),
                FakeResponse(200, {"data": {"taskId": "task-1"}}),
                FakeResponse(
                    200,
                    {
                        "data": {
                            "status": "SUCCESS",
                            "response": {
                                "sunoData": [
                                    {
                                        "id": "clip-1",
                                        "audioUrl": (
                                            "https://cdn.example/"
                                            "a.mp3"
                                        ),
                                        "imageUrl": (
                                            "https://cdn.example/"
                                            "c.jpg"
                                        ),
                                    }
                                ]
                            },
                        }
                    },
                ),
            ]

        def request(
            self, method: str, url: str, **kwargs: Any
        ) -> FakeResponse:
            return self.responses.pop(0)

    client = SunoApiClient(
        "key",
        base_url="https://api.example/v1",
        session=FakeSession(),
        poll_interval=0.01,
        poll_timeout=1.0,
        min_delay=0.0,
        max_delay=0.0,
        sleep_fn=lambda _: None,
        rng=random.Random(0),
    )

    records = client.generate_and_wait(TrackRequest("Title", "Pop", "prompt"))

    assert records[0].track_id == "clip-1"


def test_parse_completed_tracks_clears_instrumental_lyrics() -> None:
    payload: dict[str, Any] = {
        "data": {
            "response": {
                "sunoData": [
                    {
                        "id": "clip-2",
                        "audio_url": "https://cdn.example/instrumental.mp3",
                        "image_url": "https://cdn.example/cover.jpg",
                    }
                ]
            }
        }
    }

    records = parse_completed_tracks(payload, "Instrumental Theme", True)

    assert records[0].title == "Instrumental Theme"
    assert records[0].lyrics == ""


def test_build_sheet_row_matches_tracks_header_order() -> None:
    record = TrackRecord(
        track_id="clip-3",
        title="Neon Harbor",
        genre="Electronic",
        audio_url="https://cdn.example/track.mp3",
        cover_url="https://cdn.example/cover.jpg",
        lyrics="lyrics",
        created_at="2026-09-08T10:00:00+09:00",
    )

    assert build_sheet_row(record) == [
        "clip-3",
        "Neon Harbor",
        "Electronic",
        "https://cdn.example/track.mp3",
        "https://cdn.example/cover.jpg",
        "lyrics",
        "Pending",
        "2026-09-08T10:00:00+09:00",
    ]


def test_download_asset_writes_file_atomically(tmp_path: Path) -> None:
    from budget.suno_ingest import download_asset

    class FakeResponse:
        status_code = 200
        headers: dict[str, str] = {}

        def iter_content(self, chunk_size: int) -> list[bytes]:
            return [b"audio", b"-data"]

        def raise_for_status(self) -> None:
            return None

    class FakeSession:
        def get(self, url: str, **kwargs: Any) -> FakeResponse:
            return FakeResponse()

    target = tmp_path / "track.mp3"

    download_asset(FakeSession(), "https://cdn.example/track.mp3", target)

    assert target.read_bytes() == b"audio-data"
    assert not target.with_name(target.name + ".part").exists()


def test_save_track_assets_keeps_url_extensions(tmp_path: Path) -> None:
    class FakeResponse:
        status_code = 200
        headers: dict[str, str] = {}

        def iter_content(self, chunk_size: int) -> list[bytes]:
            return [b"asset"]

        def raise_for_status(self) -> None:
            return None

    class FakeSession:
        def get(self, url: str, **kwargs: Any) -> FakeResponse:
            return FakeResponse()

    record = TrackRecord(
        track_id="clip-4",
        title="Alternate Format",
        genre="Ambient",
        audio_url="https://cdn.example/clip-4.m4a?download=1",
        cover_url="https://cdn.example/clip-4.png",
        lyrics="",
    )

    paths = save_track_assets(FakeSession(), record, tmp_path)

    assert paths["audio"].suffix == ".m4a"
    assert paths["cover"].suffix == ".png"


def test_save_track_assets_writes_local_sidecar_for_album_pipeline(
    tmp_path: Path,
) -> None:
    class FakeResponse:
        status_code = 200

        def __init__(self, content_type: str) -> None:
            self.headers = {"Content-Type": content_type}

        def iter_content(self, chunk_size: int) -> list[bytes]:
            return [b"asset"]

        def raise_for_status(self) -> None:
            return None

    class FakeSession:
        def get(self, url: str, **kwargs: Any) -> FakeResponse:
            content_type = "audio/mpeg" if "audio" in url else "image/jpeg"
            return FakeResponse(content_type)

    record = TrackRecord(
        track_id="clip-sidecar",
        title="Sidecar Track",
        genre="Electronic",
        audio_url="https://cdn.example/audio.mp3",
        cover_url="https://cdn.example/cover.jpg",
        lyrics="Lyrics",
        created_at="2026-09-08T10:00:00+09:00",
    )

    paths = save_track_assets(FakeSession(), record, tmp_path)

    sidecar = json.loads(paths["metadata"].read_text(encoding="utf-8"))
    assert sidecar["track_id"] == "clip-sidecar"
    assert sidecar["title"] == "Sidecar Track"
    assert sidecar["audio_path"] == paths["audio"].name
    assert sidecar["lyrics_path"] == paths["lyrics"].name


def test_save_track_assets_cleans_downloads_when_sidecar_pipeline_fails(
    tmp_path: Path,
    monkeypatch: Any,
) -> None:
    class FakeResponse:
        status_code = 200

        def __init__(self, content_type: str) -> None:
            self.headers = {"Content-Type": content_type}

        def iter_content(self, chunk_size: int) -> list[bytes]:
            return [b"asset"]

        def raise_for_status(self) -> None:
            return None

    class FakeSession:
        def get(self, url: str, **kwargs: Any) -> FakeResponse:
            content_type = "audio/mpeg" if "audio" in url else "image/jpeg"
            return FakeResponse(content_type)

    original_write_text = Path.write_text

    def fail_lyrics_write(self: Path, *args: Any, **kwargs: Any) -> int:
        if self.suffix == ".txt":
            raise OSError("lyrics disk failure")
        return original_write_text(self, *args, **kwargs)

    monkeypatch.setattr(Path, "write_text", fail_lyrics_write)
    record = TrackRecord(
        track_id="clip-cleanup",
        title="Cleanup Track",
        audio_url="https://cdn.example/audio.mp3",
        cover_url="https://cdn.example/cover.jpg",
        lyrics="Lyrics",
    )

    with pytest.raises(OSError, match="lyrics disk failure"):
        save_track_assets(FakeSession(), record, tmp_path)

    assert not list(tmp_path.glob("clip-cleanup*"))


def test_opaque_urls_use_mime_types(tmp_path: Path) -> None:
    class FakeResponse:
        def __init__(self, content_type: str) -> None:
            self.status_code = 200
            self.headers = {"Content-Type": content_type}

        def iter_content(self, chunk_size: int) -> list[bytes]:
            return [b"asset"]

        def raise_for_status(self) -> None:
            return None

    class FakeSession:
        def get(self, url: str, **kwargs: Any) -> FakeResponse:
            content_type = "audio/mp4" if "audio" in url else "image/png"
            return FakeResponse(content_type)

    record = TrackRecord(
        track_id="clip-opaque",
        title="Opaque URLs",
        genre="Ambient",
        audio_url="https://cdn.example/audio?id=4",
        cover_url="https://cdn.example/cover?id=4",
        lyrics="",
    )

    paths = save_track_assets(FakeSession(), record, tmp_path)

    assert paths["audio"].suffix == ".m4a"
    assert paths["cover"].suffix == ".png"


@pytest.mark.parametrize(
    ("audio_url", "content_type"),
    [
        ("https://cdn.example/audio?id=5", "application/octet-stream"),
        ("https://cdn.example/audio.mp3", "image/png"),
    ],
)
def test_save_track_assets_rejects_ambiguous_media(
    tmp_path: Path,
    audio_url: str,
    content_type: str,
) -> None:
    class FakeResponse:
        status_code = 200

        def __init__(self) -> None:
            self.headers = {"Content-Type": content_type}

        def iter_content(self, chunk_size: int) -> list[bytes]:
            return [b"asset"]

        def raise_for_status(self) -> None:
            return None

    class FakeSession:
        def get(self, url: str, **kwargs: Any) -> FakeResponse:
            return FakeResponse()

    record = TrackRecord(
        track_id="clip-invalid",
        title="Invalid Format",
        genre="Ambient",
        audio_url=audio_url,
        cover_url="https://cdn.example/cover.jpg",
        lyrics="",
    )

    with pytest.raises(OSError):
        save_track_assets(FakeSession(), record, tmp_path)


def test_google_append_is_raw_and_deduplicated(
    tmp_path: Path,
    monkeypatch: Any,
) -> None:
    install_fake_filelock(monkeypatch)

    class FakeValues:
        def __init__(self) -> None:
            self.arguments: dict[str, Any] = {}
            self.rows: list[list[str]] = []
            self.operation = ""

        def get(self, **kwargs: Any) -> "FakeValues":
            self.operation = "get"
            return self

        def append(self, **kwargs: Any) -> "FakeValues":
            self.arguments = kwargs
            self.operation = "append"
            return self

        def execute(self) -> dict[str, Any]:
            if self.operation == "get":
                return {"values": self.rows}
            self.rows.extend(self.arguments["body"]["values"])
            return {}

    class FakeSpreadsheets:
        def __init__(self) -> None:
            self.values_api = FakeValues()

        def values(self) -> FakeValues:
            return self.values_api

    class FakeService:
        def __init__(self) -> None:
            self.spreadsheets_api = FakeSpreadsheets()

        def spreadsheets(self) -> FakeSpreadsheets:
            return self.spreadsheets_api

    service = FakeService()
    repository = GoogleSheetsRepository(
        service,
        "spreadsheet-id",
        lock_path=tmp_path / "tracks.lock",
    )
    record = TrackRecord(
        track_id="clip-5",
        title="=IMPORTXML(\"https://bad.example\",\"//a\")",
        audio_url="https://cdn.example/a.mp3",
        cover_url="https://cdn.example/c.jpg",
        lyrics="+malicious-formula",
    )

    assert repository.append_pending(record) is True
    assert repository.append_pending(record) is False

    assert (
        service.spreadsheets_api.values_api.arguments["valueInputOption"]
        == "RAW"
    )


def test_google_append_retries_transient_error(
    tmp_path: Path,
    monkeypatch: Any,
) -> None:
    install_fake_filelock(monkeypatch)

    class FakeValues:
        def __init__(self) -> None:
            self.operation = ""
            self.append_attempts = 0

        def get(self, **kwargs: Any) -> "FakeValues":
            self.operation = "get"
            return self

        def append(self, **kwargs: Any) -> "FakeValues":
            self.operation = "append"
            return self

        def execute(self) -> dict[str, Any]:
            if self.operation == "get":
                return {"values": []}
            self.append_attempts += 1
            if self.append_attempts == 1:
                raise OSError("temporary network failure")
            return {}

    class FakeSpreadsheets:
        def __init__(self) -> None:
            self.values_api = FakeValues()

        def values(self) -> FakeValues:
            return self.values_api

    class FakeService:
        def __init__(self) -> None:
            self.spreadsheets_api = FakeSpreadsheets()

        def spreadsheets(self) -> FakeSpreadsheets:
            return self.spreadsheets_api

    service = FakeService()
    repository = GoogleSheetsRepository(
        service,
        "spreadsheet-id",
        max_attempts=2,
        sleep_fn=lambda _: None,
        lock_path=tmp_path / "tracks.lock",
    )
    record = TrackRecord(
        track_id="clip-retry",
        title="Retry",
        audio_url="https://cdn.example/a.mp3",
        cover_url="https://cdn.example/c.jpg",
        lyrics="",
    )

    assert repository.append_pending(record) is True
    assert service.spreadsheets_api.values_api.append_attempts == 2


def test_suno_download_quota_counts_unique_tracks_and_allows_retry(
    tmp_path: Path,
    monkeypatch: Any,
) -> None:
    install_fake_filelock(monkeypatch)

    quota = SunoDownloadQuota(
        tmp_path / "suno-quota.json",
        limit=20,
        billing_day=15,
    )
    before_reset = datetime(2026, 9, 14, tzinfo=timezone.utc)
    after_reset = datetime(2026, 9, 15, tzinfo=timezone.utc)

    assert quota.reserve("clip-1", before_reset) == 1
    assert quota.reserve("clip-1", before_reset) == 1

    for number in range(2, 21):
        assert quota.reserve(f"clip-{number}", before_reset) == number

    with pytest.raises(SunoQuotaExceeded):
        quota.reserve("clip-21", before_reset)

    assert quota.reserve("clip-1", after_reset) == 1
