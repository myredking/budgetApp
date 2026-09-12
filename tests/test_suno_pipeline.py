import hashlib
import json
import subprocess
import threading
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
    upload_ready_videos,
    write_pipeline_report,
)
from budget.suno_album import AlbumPolicy, AlbumSpec, AlbumTrackInput
from budget.suno_release import ReleaseMetadata
from budget.youtube_upload import (
    YOUTUBE_UPLOAD_SCOPE,
    YouTubeUploadResult,
    YouTubeVideo,
)


def write_valid_defaults(path: Path) -> None:
    path.write_text(
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


def make_empty_candidate(tmp_path: Path, album_id: str = "album-01") -> AlbumCandidate:
    album = AlbumSpec(album_id, "Album", "Artist", "Electronic", "2026-09", ())
    return AlbumCandidate(album, tmp_path / f"{album_id}-cover.jpg", ())


def make_report_candidates(tmp_path: Path) -> list[AlbumCandidate]:
    return [
        make_empty_candidate(tmp_path, "ready-album"),
        AlbumCandidate(
            AlbumSpec("review-album", "Review Album", "Artist", "Electronic", "2026-09", ()),
            tmp_path / "review-cover.jpg",
            ({"code": "rights_not_confirmed", "message": "검토 필요"},),
        ),
    ]


def make_report_video(tmp_path: Path) -> tuple[Path, Path]:
    package_path = tmp_path / "ready-album"
    video_path = package_path / "youtube" / "ready-album.mp4"
    video_path.parent.mkdir(parents=True)
    video_path.write_bytes(b"video")
    return package_path, video_path


def write_youtube_metadata(package_path: Path) -> None:
    metadata_path = package_path / "youtube" / "metadata.json"
    metadata_path.parent.mkdir(parents=True, exist_ok=True)
    metadata_path.write_text(
        json.dumps({"title": "Album", "description": "Description"}),
        encoding="utf-8",
    )


def make_two_candidates(tmp_path: Path) -> list[AlbumCandidate]:
    return [
        make_empty_candidate(tmp_path, "album-01"),
        make_empty_candidate(tmp_path, "album-02"),
    ]


def make_pipeline_args(
    input_dir: Path,
    output_dir: Path,
    defaults_path: Path,
    report_path: Path,
    *flags: str,
) -> list[str]:
    return [
        "--input",
        str(input_dir),
        "--output",
        str(output_dir),
        "--metadata-defaults",
        str(defaults_path),
        *flags,
        "--report",
        str(report_path),
    ]


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
    report_path = tmp_path / "report.json"

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
                "--report",
                str(report_path),
            ]
        )

    assert not called
    report = json.loads(report_path.read_text(encoding="utf-8"))
    assert report["run_status"] == "failed"
    assert "사전 점검 실패" in report["error"]


def test_main_writes_failed_report_after_pipeline_error(
    tmp_path: Path,
    monkeypatch: Any,
) -> None:
    input_dir = tmp_path / "input"
    input_dir.mkdir()
    defaults_path = tmp_path / "defaults.json"
    write_valid_defaults(defaults_path)
    candidate = make_empty_candidate(tmp_path)

    monkeypatch.setattr(
        "budget.suno_pipeline.prepare_album_candidates",
        lambda *args, **kwargs: [candidate],
    )

    def fail_build(*args: Any, **kwargs: Any) -> list[Path]:
        kwargs["on_package"](tmp_path / "output" / "album-01")
        raise subprocess.CalledProcessError(1, ["ffmpeg"])

    monkeypatch.setattr("budget.suno_pipeline.build_ready_albums", fail_build)
    report_path = tmp_path / "report.json"

    with pytest.raises(SystemExit):
        main(
            [
                "--input",
                str(input_dir),
                "--output",
                str(tmp_path / "output"),
                "--metadata-defaults",
                str(defaults_path),
                "--build",
                "--report",
                str(report_path),
            ]
        )

    report = json.loads(report_path.read_text(encoding="utf-8"))
    assert report["run_status"] == "failed"
    assert "returned non-zero" in report["error"]
    assert report["summary"]["candidate_count"] == 1
    assert report["summary"]["package_count"] == 1


def test_main_report_preserves_video_before_render_failure(
    tmp_path: Path,
    monkeypatch: Any,
) -> None:
    input_dir = tmp_path / "input"
    input_dir.mkdir()
    defaults_path = tmp_path / "defaults.json"
    write_valid_defaults(defaults_path)
    candidates = make_two_candidates(tmp_path)
    packages = [tmp_path / "output" / "album-01", tmp_path / "output" / "album-02"]
    videos = [packages[0] / "youtube" / "album-01.mp4"]
    monkeypatch.setattr("budget.suno_pipeline.shutil.which", lambda _: "ffmpeg")
    monkeypatch.setattr(
        "budget.suno_pipeline.prepare_album_candidates",
        lambda *args, **kwargs: candidates,
    )
    monkeypatch.setattr(
        "budget.suno_pipeline.build_ready_albums",
        lambda *args, **kwargs: packages,
    )

    def fail_render(*args: Any, **kwargs: Any) -> list[Path]:
        kwargs["on_video"](videos[0])
        raise subprocess.CalledProcessError(1, ["ffmpeg"])

    monkeypatch.setattr("budget.suno_pipeline.render_ready_videos", fail_render)
    report_path = tmp_path / "report.json"

    with pytest.raises(SystemExit):
        main(
            make_pipeline_args(
                input_dir,
                tmp_path / "output",
                defaults_path,
                report_path,
                "--render-video",
            )
        )

    report = json.loads(report_path.read_text(encoding="utf-8"))
    assert report["run_status"] == "failed"
    assert report["summary"]["package_count"] == 2
    assert report["summary"]["video_count"] == 1
    assert report["albums"][0]["video_path"] == str(videos[0])
    assert report["albums"][1]["video_path"] == ""
    assert "returned non-zero" in report["error"]


def test_main_report_preserves_upload_before_upload_failure(
    tmp_path: Path,
    monkeypatch: Any,
) -> None:
    input_dir = tmp_path / "input"
    input_dir.mkdir()
    defaults_path = tmp_path / "defaults.json"
    write_valid_defaults(defaults_path)
    candidates = make_two_candidates(tmp_path)
    packages = [tmp_path / "output" / "album-01", tmp_path / "output" / "album-02"]
    videos = [package / "youtube" / f"{package.name}.mp4" for package in packages]
    monkeypatch.setattr("budget.suno_pipeline.shutil.which", lambda _: "ffmpeg")
    monkeypatch.setattr(
        "budget.suno_pipeline.prepare_album_candidates",
        lambda *args, **kwargs: candidates,
    )
    monkeypatch.setattr(
        "budget.suno_pipeline.build_ready_albums",
        lambda *args, **kwargs: packages,
    )
    monkeypatch.setattr(
        "budget.suno_pipeline.render_ready_videos",
        lambda *args, **kwargs: videos,
    )

    def fail_upload(*args: Any, **kwargs: Any) -> list[YouTubeUploadResult]:
        kwargs["on_upload"](YouTubeUploadResult("video123456", "https://youtu.be/video123456"))
        raise RuntimeError("upload failed")
    monkeypatch.setattr("budget.suno_pipeline.upload_ready_videos", fail_upload)
    report_path = tmp_path / "report.json"

    with pytest.raises(SystemExit):
        main(
            make_pipeline_args(
                input_dir,
                tmp_path / "output",
                defaults_path,
                report_path,
                "--upload-youtube",
                "--dry-run",
            )
        )

    report = json.loads(report_path.read_text(encoding="utf-8"))
    assert report["run_status"] == "failed"
    assert report["summary"]["video_count"] == 2
    assert report["summary"]["youtube_result_count"] == 1
    assert report["albums"][0]["youtube"]["video_id"] == "video123456"
    assert report["albums"][1]["youtube"] is None
    assert report["error"] == "upload failed"


def test_write_pipeline_report_is_safe_for_concurrent_runs(tmp_path: Path) -> None:
    candidate = make_empty_candidate(tmp_path)
    report_path = tmp_path / "report.json"
    barrier = threading.Barrier(6)
    errors: list[BaseException] = []

    def write_report() -> None:
        try:
            barrier.wait()
            write_pipeline_report(report_path, [candidate], [], [], [])
        except BaseException as error:
            errors.append(error)

    threads = [threading.Thread(target=write_report) for _ in range(6)]
    for thread in threads:
        thread.start()
    for thread in threads:
        thread.join(3)

    assert errors == []
    assert all(not thread.is_alive() for thread in threads)
    report = json.loads(report_path.read_text(encoding="utf-8"))
    assert report["run_status"] == "completed"
    assert not list(tmp_path.glob("report.json*.part"))


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


def test_write_pipeline_report_summarizes_review_and_upload_results(
    tmp_path: Path,
) -> None:
    candidates = make_report_candidates(tmp_path)
    package_path, video_path = make_report_video(tmp_path)
    report_path = tmp_path / "reports" / "pipeline.json"

    result = write_pipeline_report(
        report_path,
        candidates,
        [package_path],
        [video_path],
        [YouTubeUploadResult("video123456", "https://youtu.be/video123456")],
    )

    assert result == report_path
    payload = json.loads(report_path.read_text(encoding="utf-8"))
    assert payload["run_status"] == "review_required"
    assert payload["summary"] == {
        "candidate_count": 2,
        "review_required_count": 1,
        "package_count": 1,
        "video_count": 1,
        "youtube_result_count": 1,
    }
    assert payload["albums"][0]["video_path"] == str(video_path)
    assert payload["albums"][0]["youtube"]["video_id"] == "video123456"
    assert payload["albums"][1]["status"] == "review_required"
    assert not report_path.with_name("pipeline.json.part").exists()


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


def test_render_ready_videos_preserves_completed_callbacks_on_failure(
    tmp_path: Path,
) -> None:
    candidates = [
        make_empty_candidate(tmp_path, "album-01"),
        make_empty_candidate(tmp_path, "album-02"),
    ]
    package_paths = [tmp_path / "album-01", tmp_path / "album-02"]
    completed: list[Path] = []
    calls = 0

    def runner(command: list[str], check: bool) -> None:
        nonlocal calls
        calls += 1
        if calls == 2:
            raise subprocess.CalledProcessError(1, command)
        output_path = Path(command[-1])
        output_path.parent.mkdir(parents=True, exist_ok=True)
        output_path.write_bytes(b"rendered")

    with pytest.raises(subprocess.CalledProcessError):
        render_ready_videos(
            candidates,
            package_paths,
            runner,
            on_video=completed.append,
        )

    assert len(completed) == 1
    assert completed[0].name == "album-01.mp4"


def test_upload_ready_videos_preserves_completed_callbacks_on_failure(
    tmp_path: Path,
) -> None:
    candidates = [
        make_empty_candidate(tmp_path, "album-01"),
        make_empty_candidate(tmp_path, "album-02"),
    ]
    package_paths = [tmp_path / "album-01", tmp_path / "album-02"]
    for package_path in package_paths:
        write_youtube_metadata(package_path)
    completed: list[YouTubeUploadResult] = []

    class FakeUploader:
        def __init__(self) -> None:
            self.calls = 0

        def upload(
            self,
            video_path: Path,
            video: YouTubeVideo,
            *,
            dry_run: bool = False,
        ) -> YouTubeUploadResult:
            self.calls += 1
            if self.calls == 2:
                raise RuntimeError("upload failed")
            return YouTubeUploadResult("video123456", "https://youtu.be/video123456")

    uploader = FakeUploader()

    with pytest.raises(RuntimeError, match="upload failed"):
        upload_ready_videos(
            candidates,
            package_paths,
            uploader,
            on_upload=completed.append,
        )

    assert len(completed) == 1
    assert completed[0].video_id == "video123456"


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
