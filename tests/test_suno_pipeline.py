import hashlib
import json
from pathlib import Path
from typing import Any

import pytest

from budget.suno_pipeline import (
    AlbumCandidate,
    build_ready_albums,
    main,
    prepare_album_candidates,
    render_ready_videos,
    run_preflight,
)
from budget.suno_album import AlbumPolicy, AlbumSpec, AlbumTrackInput
from budget.suno_release import ReleaseMetadata
from budget.youtube_upload import YOUTUBE_UPLOAD_SCOPE


def test_run_preflight_reports_missing_paths_without_creating_output(
    tmp_path: Path,
) -> None:
    input_dir = tmp_path / "missing-input"
    output_dir = tmp_path / "output"
    defaults_path = tmp_path / "missing-defaults.json"

    issues = run_preflight(input_dir, output_dir, defaults_path)

    assert {issue.code for issue in issues} == {
        "input_missing",
        "defaults_missing",
    }
    assert not output_dir.exists()


def test_run_preflight_requires_ffmpeg_for_video_work(
    tmp_path: Path,
    monkeypatch: Any,
) -> None:
    input_dir = tmp_path / "input"
    input_dir.mkdir()
    defaults_path = tmp_path / "defaults.json"
    defaults_path.write_text("{}", encoding="utf-8")
    monkeypatch.setattr("budget.suno_pipeline.shutil.which", lambda _: None)

    issues = run_preflight(
        input_dir,
        tmp_path / "output",
        defaults_path,
        needs_render=True,
    )

    assert any(issue.code == "ffmpeg_missing" for issue in issues)


def test_run_preflight_requires_youtube_credentials_only_for_real_upload(
    tmp_path: Path,
) -> None:
    input_dir = tmp_path / "input"
    input_dir.mkdir()
    defaults_path = tmp_path / "defaults.json"
    defaults_path.write_text("{}", encoding="utf-8")

    dry_run_issues = run_preflight(
        input_dir,
        tmp_path / "output",
        defaults_path,
        needs_upload=True,
        dry_run=True,
        client_secrets_path=tmp_path / "missing-client.json",
        token_path=tmp_path / "missing-token.json",
    )
    real_upload_issues = run_preflight(
        input_dir,
        tmp_path / "output",
        defaults_path,
        needs_upload=True,
        client_secrets_path=tmp_path / "missing-client.json",
        token_path=tmp_path / "missing-token.json",
    )

    assert not any(issue.code == "youtube_credentials_missing" for issue in dry_run_issues)
    assert any(issue.code == "youtube_credentials_missing" for issue in real_upload_issues)


def test_run_preflight_requires_token_for_noninteractive_upload(tmp_path: Path) -> None:
    input_dir = tmp_path / "input"
    input_dir.mkdir()
    defaults_path = tmp_path / "defaults.json"
    defaults_path.write_text("{}", encoding="utf-8")
    client_path = tmp_path / "client.json"
    client_path.write_text("{}", encoding="utf-8")

    issues = run_preflight(
        input_dir,
        tmp_path / "output",
        defaults_path,
        needs_upload=True,
        require_youtube_token=True,
        client_secrets_path=client_path,
        token_path=tmp_path / "missing-token.json",
    )

    assert any(issue.code == "youtube_token_missing" for issue in issues)


def test_run_preflight_rejects_invalid_youtube_token_json(tmp_path: Path) -> None:
    input_dir = tmp_path / "input"
    input_dir.mkdir()
    defaults_path = tmp_path / "defaults.json"
    defaults_path.write_text("{}", encoding="utf-8")
    token_path = tmp_path / "token.json"
    token_path.write_text(json.dumps({"scopes": ["profile"]}), encoding="utf-8")

    issues = run_preflight(
        input_dir,
        tmp_path / "output",
        defaults_path,
        needs_upload=True,
        require_youtube_token=True,
        token_path=token_path,
    )

    assert any(issue.code == "youtube_token_invalid" for issue in issues)


def test_run_preflight_accepts_upload_token_with_required_scope(tmp_path: Path) -> None:
    input_dir = tmp_path / "input"
    input_dir.mkdir()
    defaults_path = tmp_path / "defaults.json"
    defaults_path.write_text("{}", encoding="utf-8")
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

    issues = run_preflight(
        input_dir,
        tmp_path / "output",
        defaults_path,
        needs_upload=True,
        require_youtube_token=True,
        token_path=token_path,
    )

    assert not any(issue.code.startswith("youtube_token") for issue in issues)


def test_main_preflight_failure_stops_before_pipeline(tmp_path: Path, monkeypatch: Any) -> None:
    called = False

    def forbidden_prepare(*args: Any, **kwargs: Any) -> list[Any]:
        nonlocal called
        called = True
        return []

    monkeypatch.setattr("budget.suno_pipeline.prepare_album_candidates", forbidden_prepare)

    with pytest.raises(SystemExit):
        main(
            [
                "--input",
                str(tmp_path / "missing-input"),
                "--output",
                str(tmp_path / "output"),
                "--metadata-defaults",
                str(tmp_path / "missing-defaults.json"),
                "--build",
                "--preflight",
            ]
        )

    assert not called


def test_main_preflight_only_stops_after_successful_check(
    tmp_path: Path,
    monkeypatch: Any,
) -> None:
    input_dir = tmp_path / "input"
    input_dir.mkdir()
    defaults_path = tmp_path / "defaults.json"
    defaults_path.write_text(
        json.dumps(
            {
                "artist_name": "Artist",
                "songwriter_name": "Writer",
                "suno_plan": "Pro",
                "lyrics_by_ai": True,
                "music_by_ai": True,
            }
        ),
        encoding="utf-8",
    )

    def fail_prepare(*args: Any, **kwargs: Any) -> list[Any]:
        raise AssertionError("not called")

    monkeypatch.setattr(
        "budget.suno_pipeline.prepare_album_candidates",
        fail_prepare,
    )

    result = main(
        [
            "--input",
            str(input_dir),
            "--output",
            str(tmp_path / "output"),
            "--metadata-defaults",
            str(defaults_path),
            "--preflight-only",
        ]
    )

    assert result == 0


def test_prepare_album_candidates_creates_cover_and_review_report(tmp_path: Path) -> None:
    input_dir = tmp_path / "input"
    output_dir = tmp_path / "output"
    input_dir.mkdir()
    for number in range(1, 5):
        track_id = f"clip-{number}"
        (input_dir / f"{track_id}.wav").write_bytes(b"audio")
        (input_dir / f"{track_id}.txt").write_text("lyrics", encoding="utf-8")
        sidecar = {
            "track_id": track_id,
            "title": f"Track {number}",
            "genre": "Electronic",
            "audio_path": f"{track_id}.wav",
            "lyrics_path": f"{track_id}.txt",
            "created_at": "2026-09-08T10:00:00+09:00",
            "downloaded_at": "2026-09-08T10:05:00+09:00",
        }
        (input_dir / f"{track_id}.track.json").write_text(
            json.dumps(sidecar),
            encoding="utf-8",
        )
    defaults = {
        "artist_name": "Neon Harbor",
        "songwriter_name": "Kim Jae-seop",
        "suno_plan": "Pro",
        "lyrics_by_ai": True,
        "music_by_ai": True,
        "audio_scope": "all",
        "rights_confirmed": False,
        "rights_evidence_path": "",
        "artwork_reviewed": False,
        "audio_reviewed": False,
        "artist_persona_confirmed": False,
    }

    candidates = prepare_album_candidates(
        input_dir,
        output_dir,
        defaults,
        AlbumPolicy(min_tracks=4),
    )

    assert len(candidates) == 1
    assert candidates[0].cover_path.is_file()
    report = output_dir / "plans" / f"{candidates[0].album.album_id}.json"
    assert report.is_file()
    payload = json.loads(report.read_text(encoding="utf-8"))
    assert payload["track_count"] == 4
    assert payload["issues"]


def test_render_ready_videos_uses_only_candidates_without_issues(tmp_path: Path) -> None:
    metadata = ReleaseMetadata(
        artist_name="Artist",
        track_title="Track",
        primary_genre="Electronic",
        songwriter_name="Writer",
        suno_plan="Pro",
        generated_at="2026-09-08T10:00:00+09:00",
        downloaded_at="2026-09-08T10:05:00+09:00",
        lyrics_by_ai=True,
        music_by_ai=True,
        audio_scope="all",
        artist_persona_confirmed=True,
    )
    track = AlbumTrackInput(metadata, tmp_path / "audio.wav", tmp_path / "lyrics.txt")
    album = AlbumSpec("artist-album-01", "Album", "Artist", "Electronic", "2026-09", (track,))
    candidate = AlbumCandidate(album, tmp_path / "cover.jpg", ())
    calls: list[list[str]] = []

    def fake_runner(command: list[str], check: bool) -> None:
        calls.append(command)
        output_path = Path(command[-1])
        output_path.parent.mkdir(parents=True, exist_ok=True)
        output_path.write_bytes(b"rendered")

    videos = render_ready_videos(
        [candidate],
        [tmp_path / album.album_id],
        fake_runner,
    )

    assert videos == [tmp_path / album.album_id / "youtube" / "artist-album-01.mp4"]
    assert calls


def test_build_ready_albums_rejects_stale_existing_package(tmp_path: Path) -> None:
    metadata = ReleaseMetadata(
        artist_name="Artist",
        track_title="Track",
        primary_genre="Electronic",
        songwriter_name="Writer",
        suno_plan="Pro",
        generated_at="2026-09-08T10:00:00+09:00",
        downloaded_at="2026-09-08T10:05:00+09:00",
        lyrics_by_ai=True,
        music_by_ai=True,
        audio_scope="all",
        artist_persona_confirmed=True,
    )
    track = AlbumTrackInput(metadata, tmp_path / "audio.wav", tmp_path / "lyrics.txt", "new")
    album = AlbumSpec("artist-album-01", "Album", "Artist", "Electronic", "2026-09", (track,))
    candidate = AlbumCandidate(album, tmp_path / "cover.jpg", ())
    package = tmp_path / album.album_id / "metadata"
    package.mkdir(parents=True)
    (package / "album.json").write_text(
        json.dumps({"album_id": album.album_id, "tracks": [{"track_id": "old"}]}),
        encoding="utf-8",
    )

    with pytest.raises(ValueError, match="트랙 구성"):
        build_ready_albums([candidate], tmp_path)


def test_build_ready_albums_rejects_incomplete_matching_package(tmp_path: Path) -> None:
    metadata = ReleaseMetadata(
        artist_name="Artist",
        track_title="Track",
        primary_genre="Electronic",
        songwriter_name="Writer",
        suno_plan="Pro",
        generated_at="2026-09-08T10:00:00+09:00",
        downloaded_at="2026-09-08T10:05:00+09:00",
        lyrics_by_ai=True,
        music_by_ai=True,
        audio_scope="all",
        artist_persona_confirmed=True,
    )
    track = AlbumTrackInput(metadata, tmp_path / "audio.wav", tmp_path / "lyrics.txt", "track")
    album = AlbumSpec("artist-album-01", "Album", "Artist", "Electronic", "2026-09", (track,))
    candidate = AlbumCandidate(album, tmp_path / "cover.jpg", ())
    package = tmp_path / album.album_id
    (package / "metadata").mkdir(parents=True)
    (package / "metadata" / "album.json").write_text(
        json.dumps(
            {
                "album_id": album.album_id,
                "track_count": 1,
                "tracks": [{"track_id": "track", "audio": "tracks/audio.wav", "lyrics": "tracks/lyrics.txt"}],
            }
        ),
        encoding="utf-8",
    )

    with pytest.raises(ValueError, match="트랙 구성"):
        build_ready_albums([candidate], tmp_path)


def test_build_ready_albums_reuses_complete_matching_package(tmp_path: Path) -> None:
    metadata = ReleaseMetadata(
        artist_name="Artist",
        track_title="Track",
        primary_genre="Electronic",
        songwriter_name="Writer",
        suno_plan="Pro",
        generated_at="2026-09-08T10:00:00+09:00",
        downloaded_at="2026-09-08T10:05:00+09:00",
        lyrics_by_ai=True,
        music_by_ai=True,
        audio_scope="all",
        artist_persona_confirmed=True,
    )
    track = AlbumTrackInput(metadata, tmp_path / "audio.wav", tmp_path / "lyrics.txt", "track")
    album = AlbumSpec("artist-album-01", "Album", "Artist", "Electronic", "2026-09", (track,))
    candidate = AlbumCandidate(album, tmp_path / "cover.jpg", ())
    package = tmp_path / album.album_id
    rows = [{"track_id": "track", "audio": "tracks/audio.wav", "lyrics": "tracks/lyrics.txt"}]
    for relative_path in (
        "artwork/cover.jpg",
        "youtube/metadata.json",
        "youtube/concat.txt",
        "distrokid/upload-checklist.md",
        "tracks/audio.wav",
        "tracks/lyrics.txt",
        "tracks/metadata.json",
    ):
        path = package / relative_path
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(b"content")
    (package / "tracks" / "metadata.json").write_text("{}", encoding="utf-8")
    (package / "youtube" / "metadata.json").write_text(
        json.dumps(
            {
                "title": "Album",
                "description": "Description",
                "tags": ["Artist", "Electronic", "AI music"],
                "category_id": "10",
                "privacy_status": "private",
                "contains_synthetic_media": True,
                "notify_subscribers": False,
            }
        ),
        encoding="utf-8",
    )
    (package / "youtube" / "concat.txt").write_text(
        "file '../tracks/audio.wav'\n",
        encoding="utf-8",
    )
    metadata_path = package / "metadata" / "album.json"
    metadata_path.parent.mkdir(parents=True, exist_ok=True)
    metadata_path.write_text(
        json.dumps({"album_id": album.album_id, "track_count": 1, "tracks": rows}),
        encoding="utf-8",
    )

    assert build_ready_albums([candidate], tmp_path) == [package]
    (package / "youtube" / "metadata.json").write_text("broken", encoding="utf-8")

    with pytest.raises(ValueError, match="트랙 구성"):
        build_ready_albums([candidate], tmp_path)


def test_build_ready_albums_rejects_malformed_matching_metadata(tmp_path: Path) -> None:
    metadata = ReleaseMetadata(
        artist_name="Artist",
        track_title="Track",
        primary_genre="Electronic",
        songwriter_name="Writer",
        suno_plan="Pro",
        generated_at="2026-09-08T10:00:00+09:00",
        downloaded_at="2026-09-08T10:05:00+09:00",
        lyrics_by_ai=True,
        music_by_ai=True,
        audio_scope="all",
        artist_persona_confirmed=True,
    )
    track = AlbumTrackInput(metadata, tmp_path / "audio.wav", tmp_path / "lyrics.txt", "track")
    album = AlbumSpec("artist-album-01", "Album", "Artist", "Electronic", "2026-09", (track,))
    candidate = AlbumCandidate(album, tmp_path / "cover.jpg", ())
    package = tmp_path / album.album_id
    rows = [{"track_id": "track", "audio": "../outside.wav", "lyrics": "tracks/lyrics.txt"}]
    for relative_path in (
        "artwork/cover.jpg",
        "youtube/metadata.json",
        "youtube/concat.txt",
        "distrokid/upload-checklist.md",
        "tracks/lyrics.txt",
        "tracks/metadata.json",
    ):
        path = package / relative_path
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(b"content")
    metadata_path = package / "metadata" / "album.json"
    metadata_path.parent.mkdir(parents=True, exist_ok=True)
    metadata_path.write_text(
        json.dumps({"album_id": album.album_id, "track_count": True, "tracks": rows}),
        encoding="utf-8",
    )

    with pytest.raises(ValueError, match="트랙 구성"):
        build_ready_albums([candidate], tmp_path)


def test_render_ready_videos_skips_existing_nonempty_video(tmp_path: Path) -> None:
    metadata = ReleaseMetadata(
        artist_name="Artist",
        track_title="Track",
        primary_genre="Electronic",
        songwriter_name="Writer",
        suno_plan="Pro",
        generated_at="2026-09-08T10:00:00+09:00",
        downloaded_at="2026-09-08T10:05:00+09:00",
        lyrics_by_ai=True,
        music_by_ai=True,
        audio_scope="all",
        artist_persona_confirmed=True,
    )
    track = AlbumTrackInput(metadata, tmp_path / "audio.wav", tmp_path / "lyrics.txt")
    album = AlbumSpec("artist-album-01", "Album", "Artist", "Electronic", "2026-09", (track,))
    candidate = AlbumCandidate(album, tmp_path / "cover.jpg", ())
    video = tmp_path / album.album_id / "youtube" / f"{album.album_id}.mp4"
    video.parent.mkdir(parents=True)
    video.write_bytes(b"rendered")
    video_hash = hashlib.sha256(b"rendered").hexdigest()
    (video.parent / f"{video.name}.complete").write_text(video_hash + "\n", encoding="utf-8")
    calls: list[list[str]] = []

    def fake_runner(command: list[str], check: bool) -> None:
        calls.append(command)

    videos = render_ready_videos(
        [candidate],
        [tmp_path / album.album_id],
        fake_runner,
    )

    assert videos == [video]
    assert calls == []
