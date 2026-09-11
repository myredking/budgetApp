import json
import sys
from pathlib import Path
from types import ModuleType
from typing import Any

import pytest

from budget.youtube_upload import (
    YOUTUBE_UPLOAD_SCOPE,
    YouTubeUploadPolicy,
    YouTubeVideo,
    YouTubeUploader,
    load_youtube_video,
)


class FakeUploadRequest:
    def __init__(self) -> None:
        self.calls = 0

    def next_chunk(self) -> tuple[None, dict[str, str] | None]:
        self.calls += 1
        if self.calls == 1:
            raise OSError("temporary network failure")
        return None, {"id": "video-123"}


class EmptyUploadRequest:
    def next_chunk(self) -> tuple[None, None]:
        return None, None


class FakeVideos:
    def __init__(self) -> None:
        self.body: dict[str, Any] = {}
        self.request = FakeUploadRequest()

    def insert(self, **kwargs: Any) -> FakeUploadRequest:
        self.body = kwargs
        return self.request


class FakeService:
    def __init__(self) -> None:
        self.api = FakeVideos()

    def videos(self) -> FakeVideos:
        return self.api


def make_video(tmp_path: Path) -> Path:
    path = tmp_path / "album.mp4"
    path.write_bytes(b"video")
    return path


def make_metadata() -> YouTubeVideo:
    return YouTubeVideo(
        title="Neon Harbor - September Sessions (Full Album)",
        description="AI-generated music album.\n\nTracklist:\n1. Night Transit",
        tags=("Neon Harbor", "Electronic", "AI music"),
    )


def test_youtube_policy_defaults_to_private_and_disclosed(tmp_path: Path) -> None:
    service = FakeService()
    uploader = YouTubeUploader(
        service,
        policy=YouTubeUploadPolicy(),
        state_path=tmp_path / "youtube.json",
        sleep_fn=lambda _: None,
    )

    result = uploader.upload(make_video(tmp_path), make_metadata())

    assert result.video_id == "video-123"
    assert result.url == "https://youtu.be/video-123"
    body = service.api.body["body"]
    assert body["status"]["privacyStatus"] == "private"
    assert body["status"]["containsSyntheticMedia"] is True
    assert service.api.body["notifySubscribers"] is False


def test_youtube_uploader_deduplicates_by_file_hash(tmp_path: Path) -> None:
    state_path = tmp_path / "youtube.json"
    service = FakeService()
    uploader = YouTubeUploader(service, state_path=state_path, sleep_fn=lambda _: None)
    video_path = make_video(tmp_path)

    first = uploader.upload(video_path, make_metadata())
    second = uploader.upload(video_path, make_metadata())

    assert first == second
    assert service.api.request.calls == 2
    assert json.loads(state_path.read_text(encoding="utf-8"))["uploads"]


def test_youtube_policy_rejects_public_upload_by_default(tmp_path: Path) -> None:
    service = FakeService()
    policy = YouTubeUploadPolicy(privacy_status="public")
    uploader = YouTubeUploader(service, policy=policy, state_path=tmp_path / "state.json")

    with pytest.raises(ValueError, match="공개 업로드"):
        uploader.upload(make_video(tmp_path), make_metadata())


def test_youtube_dry_run_does_not_call_service(tmp_path: Path) -> None:
    service = FakeService()
    uploader = YouTubeUploader(service, state_path=tmp_path / "state.json")

    result = uploader.upload(make_video(tmp_path), make_metadata(), dry_run=True)

    assert result.dry_run is True
    assert service.api.request.calls == 0


def test_youtube_resumable_upload_rejects_empty_chunk_response(tmp_path: Path) -> None:
    uploader = YouTubeUploader(state_path=tmp_path / "state.json")

    with pytest.raises(RuntimeError, match="빈 응답"):
        uploader._resumable_upload(EmptyUploadRequest())


def test_youtube_upload_serializes_state_access_with_lock(tmp_path: Path) -> None:
    class TrackingLock:
        entered = 0

        def __enter__(self) -> "TrackingLock":
            self.entered += 1
            return self

        def __exit__(self, *args: Any) -> None:
            return None

    lock = TrackingLock()
    service = FakeService()
    uploader = YouTubeUploader(service, state_path=tmp_path / "state.json", sleep_fn=lambda _: None)
    uploader._state_lock = lambda: lock  # type: ignore[method-assign]

    uploader.upload(make_video(tmp_path), make_metadata())

    assert lock.entered == 1


def test_noninteractive_uploader_rejects_missing_token(
    tmp_path: Path,
    monkeypatch: Any,
) -> None:
    monkeypatch.setattr("budget.youtube_upload._load_credentials", lambda *args: None)
    uploader = YouTubeUploader(
        allow_interactive_oauth=False,
        client_secrets_path=tmp_path / "client.json",
        token_path=tmp_path / "token.json",
    )

    with pytest.raises(RuntimeError, match="비대화형"):
        uploader._build_service()


def test_noninteractive_uploader_rejects_invalid_token_json(
    tmp_path: Path,
) -> None:
    token_path = tmp_path / "token.json"
    token_path.write_text(json.dumps({"scopes": ["profile"]}), encoding="utf-8")
    uploader = YouTubeUploader(
        allow_interactive_oauth=False,
        client_secrets_path=tmp_path / "client.json",
        token_path=token_path,
    )

    with pytest.raises(RuntimeError, match="token JSON"):
        uploader._build_service()


def test_noninteractive_uploader_does_not_fallback_for_invalid_credentials(
    tmp_path: Path,
    monkeypatch: Any,
) -> None:
    token_path = tmp_path / "token.json"
    token_path.write_text(
        json.dumps(
            {
                "token": "access",
                "refresh_token": "refresh",
                "token_uri": "https://oauth2.googleapis.com/token",
                "client_id": "client",
                "client_secret": "secret",
                "scopes": [YOUTUBE_UPLOAD_SCOPE],
            }
        ),
        encoding="utf-8",
    )

    class InvalidCredentials:
        valid = False

    class FakeFlow:
        called = False

        @classmethod
        def from_client_secrets_file(cls, *args: Any, **kwargs: Any) -> "FakeFlow":
            cls.called = True
            raise AssertionError("browser OAuth must not be called")

    class FakeRequest:
        pass

    class FakeCredentials:
        pass

    def fake_build(*args: Any, **kwargs: Any) -> object:
        return object()

    modules = {
        "google": ModuleType("google"),
        "google.auth": ModuleType("google.auth"),
        "google.auth.transport": ModuleType("google.auth.transport"),
        "google.auth.transport.requests": ModuleType("google.auth.transport.requests"),
        "google.oauth2": ModuleType("google.oauth2"),
        "google.oauth2.credentials": ModuleType("google.oauth2.credentials"),
        "google_auth_oauthlib": ModuleType("google_auth_oauthlib"),
        "google_auth_oauthlib.flow": ModuleType("google_auth_oauthlib.flow"),
        "googleapiclient": ModuleType("googleapiclient"),
        "googleapiclient.discovery": ModuleType("googleapiclient.discovery"),
    }
    modules["google.auth.transport.requests"].Request = FakeRequest
    modules["google.oauth2.credentials"].Credentials = FakeCredentials
    modules["google_auth_oauthlib.flow"].InstalledAppFlow = FakeFlow
    modules["googleapiclient.discovery"].build = fake_build
    for name, module in modules.items():
        monkeypatch.setitem(sys.modules, name, module)
    monkeypatch.setattr(
        "budget.youtube_upload._load_credentials",
        lambda *args: InvalidCredentials(),
    )
    uploader = YouTubeUploader(
        allow_interactive_oauth=False,
        client_secrets_path=tmp_path / "client.json",
        token_path=token_path,
    )

    with pytest.raises(RuntimeError, match="비대화형"):
        uploader._build_service()

    assert FakeFlow.called is False


def test_load_youtube_video_reads_album_metadata(tmp_path: Path) -> None:
    path = tmp_path / "metadata.json"
    path.write_text(
        json.dumps(
            {
                "title": "Album",
                "description": "Description",
                "tags": ["one", "two"],
                "category_id": "10",
            }
        ),
        encoding="utf-8",
    )

    video = load_youtube_video(path)

    assert video.title == "Album"
    assert video.tags == ("one", "two")
