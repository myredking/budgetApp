import json
import threading
import wave
from pathlib import Path

import pytest
from PIL import Image

from budget.suno_album import (
    AlbumPolicy,
    AlbumTrackInput,
    build_album_package,
    build_ffmpeg_command,
    generate_album_cover,
    group_tracks_into_albums,
    load_ingested_tracks,
    render_album_video,
    validate_album,
)
from budget.suno_release import ReleaseMetadata


def make_audio(path: Path) -> None:
    with wave.open(str(path), "wb") as audio:
        audio.setnchannels(1)
        audio.setsampwidth(2)
        audio.setframerate(44100)
        audio.writeframes(b"\0\0" * 44100)


def make_metadata(
    tmp_path: Path,
    track_id: str,
    created_at: str = "2026-09-08T10:00:00+09:00",
    **overrides: object,
) -> ReleaseMetadata:
    values: dict[str, object] = {
        "artist_name": "Neon Harbor",
        "track_title": f"Track {track_id}",
        "primary_genre": "Electronic",
        "songwriter_name": "Kim Jae-seop",
        "suno_plan": "Pro",
        "generated_at": created_at,
        "downloaded_at": "2026-09-08T10:05:00+09:00",
        "lyrics_by_ai": True,
        "music_by_ai": True,
        "audio_scope": "all",
        "rights_confirmed": True,
        "rights_evidence_path": str(Path(__file__)),
        "artwork_reviewed": True,
        "audio_reviewed": True,
        "artist_persona_confirmed": True,
    }
    values.update(overrides)
    return ReleaseMetadata(**values)


def make_track(tmp_path: Path, track_id: str, **overrides: object) -> AlbumTrackInput:
    audio_path = tmp_path / f"{track_id}.wav"
    lyrics_path = tmp_path / f"{track_id}.txt"
    make_audio(audio_path)
    lyrics_path.write_text(f"Lyrics for {track_id}\n", encoding="utf-8")
    metadata = make_metadata(tmp_path, track_id, **overrides)
    return AlbumTrackInput(metadata, audio_path, lyrics_path)


def test_group_tracks_into_albums_filters_and_chunks_by_month(tmp_path: Path) -> None:
    tracks = [make_track(tmp_path, f"track-{number}") for number in range(1, 6)]
    tracks.append(
        make_track(
            tmp_path,
            "next-month",
            created_at="2026-10-01T10:00:00+09:00",
        )
    )

    albums = group_tracks_into_albums(tracks, AlbumPolicy(min_tracks=4, max_tracks=4))

    assert len(albums) == 1
    assert albums[0].album_id == "neon-harbor-electronic-2026-09-01"
    assert [track.metadata.track_title for track in albums[0].tracks] == [
        "Track track-1",
        "Track track-2",
        "Track track-3",
        "Track track-4",
    ]


def test_group_tracks_does_not_create_album_below_minimum(tmp_path: Path) -> None:
    tracks = [make_track(tmp_path, f"track-{number}") for number in range(1, 4)]

    albums = group_tracks_into_albums(tracks, AlbumPolicy(min_tracks=4))

    assert albums == []


def test_load_ingested_tracks_applies_shared_rights_defaults(tmp_path: Path) -> None:
    for number in range(1, 5):
        track_id = f"clip-{number}"
        audio_path = tmp_path / f"{track_id}.mp3"
        lyrics_path = tmp_path / f"{track_id}.txt"
        audio_path.write_bytes(b"audio")
        lyrics_path.write_text("lyrics", encoding="utf-8")
        sidecar = {
            "track_id": track_id,
            "title": f"Track {number}",
            "genre": "Electronic",
            "audio_path": audio_path.name,
            "lyrics_path": lyrics_path.name,
            "created_at": "2026-09-08T10:00:00+09:00",
            "downloaded_at": "2026-09-08T10:05:00+09:00",
        }
        (tmp_path / f"{track_id}.track.json").write_text(
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
        "rights_confirmed": True,
        "rights_evidence_path": str(Path(__file__)),
        "artwork_reviewed": True,
        "audio_reviewed": True,
        "artist_persona_confirmed": True,
    }

    tracks = load_ingested_tracks(tmp_path, defaults)

    assert len(tracks) == 4
    assert tracks[0].track_id == "clip-1"
    assert tracks[0].metadata.artist_name == "Neon Harbor"
    assert tracks[0].metadata.track_title == "Track 1"


def test_validate_album_reports_track_scoped_policy_issues(tmp_path: Path) -> None:
    tracks = [
        make_track(tmp_path, "ok"),
        make_track(tmp_path, "free", suno_plan="Basic"),
    ]
    albums = group_tracks_into_albums(tracks, AlbumPolicy(min_tracks=1))

    issues = validate_album(albums[0], generate_album_cover("Album", "Artist", tmp_path / "cover.jpg"))

    assert any(issue.code == "suno_commercial_rights" for issue in issues)
    assert any("free" in issue.message for issue in issues)


def test_validate_album_blocks_missing_lyrics_before_copying(tmp_path: Path) -> None:
    track = make_track(tmp_path, "missing-lyrics")
    missing = AlbumTrackInput(track.metadata, track.audio_path, tmp_path / "missing.txt")
    album = group_tracks_into_albums([missing], AlbumPolicy(min_tracks=1))[0]
    cover_path = generate_album_cover("Album", "Artist", tmp_path / "cover.jpg")

    issues = validate_album(album, cover_path)

    assert any(issue.code == "lyrics_missing" for issue in issues)


def test_generate_album_cover_is_distrokid_ready_and_deterministic(tmp_path: Path) -> None:
    first = generate_album_cover("Night Transit", "Neon Harbor", tmp_path / "first.jpg")
    second = generate_album_cover("Night Transit", "Neon Harbor", tmp_path / "second.jpg")

    with Image.open(first) as image:
        assert image.size == (3000, 3000)
        assert image.mode == "RGB"
        assert image.format == "JPEG"
    assert first.read_bytes() == second.read_bytes()


def test_build_album_package_writes_tracklist_and_youtube_manifest(tmp_path: Path) -> None:
    tracks = [make_track(tmp_path, f"track-{number}") for number in range(1, 5)]
    album = group_tracks_into_albums(tracks, AlbumPolicy(min_tracks=4))[0]
    cover_path = generate_album_cover(album.album_title, album.artist_name, tmp_path / "cover.jpg")

    package = build_album_package(album, cover_path, tmp_path / "output")

    assert (package / "artwork" / "cover.jpg").is_file()
    assert (package / "distrokid" / "upload-checklist.md").is_file()
    assert (package / "youtube" / "metadata.json").is_file()
    album_payload = json.loads((package / "metadata" / "album.json").read_text(encoding="utf-8"))
    assert album_payload["track_count"] == 4
    assert album_payload["tracks"][0]["track_number"] == 1
    assert (package / "youtube" / "concat.txt").is_file()


def test_build_album_package_hard_blocks_policy_issues(tmp_path: Path) -> None:
    tracks = [make_track(tmp_path, f"track-{number}") for number in range(1, 5)]
    tracks[0] = make_track(tmp_path, "unreviewed", audio_reviewed=False)
    album = group_tracks_into_albums(tracks, AlbumPolicy(min_tracks=4))[0]
    cover_path = generate_album_cover(album.album_title, album.artist_name, tmp_path / "cover.jpg")

    with pytest.raises(ValueError, match="처음부터 끝까지"):
        build_album_package(album, cover_path, tmp_path / "output")


def test_build_album_package_rejects_reused_cover(tmp_path: Path) -> None:
    tracks = [make_track(tmp_path, f"track-{number}") for number in range(1, 5)]
    album = group_tracks_into_albums(tracks, AlbumPolicy(min_tracks=4))[0]
    cover_path = generate_album_cover(album.album_title, album.artist_name, tmp_path / "cover.jpg")
    output_root = tmp_path / "output"
    build_album_package(album, cover_path, output_root)
    other_album = album.__class__(
        "second-album",
        "Second Album",
        album.artist_name,
        album.primary_genre,
        album.month,
        album.tracks,
    )

    with pytest.raises(ValueError, match="이미 사용한 커버"):
        build_album_package(other_album, cover_path, output_root)


def test_build_ffmpeg_command_uses_local_assets_only(tmp_path: Path) -> None:
    album = group_tracks_into_albums(
        [make_track(tmp_path, f"track-{number}") for number in range(1, 5)],
        AlbumPolicy(min_tracks=4),
    )[0]

    command = build_ffmpeg_command(tmp_path / "package", album)

    assert command[0] == "ffmpeg"
    assert "-loop" in command
    assert "youtube/concat.txt" in " ".join(command)
    assert "https://" not in " ".join(command)


def test_render_album_video_executes_local_ffmpeg_command(tmp_path: Path) -> None:
    tracks = [make_track(tmp_path, f"track-{number}") for number in range(1, 5)]
    album = group_tracks_into_albums(tracks, AlbumPolicy(min_tracks=4))[0]
    calls: list[list[str]] = []

    def fake_runner(command: list[str], check: bool) -> None:
        calls.append(command)
        assert check is True
        output_path = Path(command[-1])
        output_path.parent.mkdir(parents=True, exist_ok=True)
        output_path.write_bytes(b"rendered")

    output = render_album_video(tmp_path / "package", album, fake_runner)

    assert output.name == f"{album.album_id}.mp4"
    assert calls and calls[0][0] == "ffmpeg"


def test_render_album_video_removes_partial_file_after_failure(tmp_path: Path) -> None:
    tracks = [make_track(tmp_path, f"track-{number}") for number in range(1, 5)]
    album = group_tracks_into_albums(tracks, AlbumPolicy(min_tracks=4))[0]

    def failing_runner(command: list[str], check: bool) -> None:
        output_path = Path(command[-1])
        output_path.parent.mkdir(parents=True, exist_ok=True)
        output_path.write_bytes(b"partial")
        raise RuntimeError("ffmpeg failed")

    with pytest.raises(RuntimeError, match="ffmpeg failed"):
        render_album_video(tmp_path / "package", album, failing_runner)

    output_path = tmp_path / "package" / "youtube" / f"{album.album_id}.mp4"
    marker_path = output_path.with_name(output_path.name + ".complete")
    assert not output_path.exists()
    assert not marker_path.exists()


def test_render_album_video_serializes_concurrent_runs(tmp_path: Path) -> None:
    tracks = [make_track(tmp_path, f"track-{number}") for number in range(1, 5)]
    album = group_tracks_into_albums(tracks, AlbumPolicy(min_tracks=4))[0]
    state_lock = threading.Lock()
    first_entered = threading.Event()
    second_entered = threading.Event()
    release_first = threading.Event()
    active_runs = 0
    maximum_active_runs = 0
    runner_calls = 0

    def concurrent_runner(command: list[str], check: bool) -> None:
        nonlocal active_runs, maximum_active_runs, runner_calls
        output_path = Path(command[-1])
        output_path.parent.mkdir(parents=True, exist_ok=True)
        with state_lock:
            runner_calls += 1
            call_number = runner_calls
            active_runs += 1
            maximum_active_runs = max(maximum_active_runs, active_runs)
        if call_number == 1:
            first_entered.set()
            if not release_first.wait(2):
                raise AssertionError("첫 번째 렌더링 해제 신호가 없습니다.")
        else:
            second_entered.set()
        output_path.write_bytes(b"rendered")
        with state_lock:
            active_runs -= 1

    errors: list[BaseException] = []

    def render_once() -> None:
        try:
            render_album_video(tmp_path / "package", album, concurrent_runner)
        except BaseException as error:
            errors.append(error)

    first_thread = threading.Thread(target=render_once)
    second_thread = threading.Thread(target=render_once)
    first_thread.start()
    second_started = False
    try:
        assert first_entered.wait(2)
        second_thread.start()
        second_started = True
        assert not second_entered.wait(0.1)
    finally:
        release_first.set()
        first_thread.join(3)
        if second_started:
            second_thread.join(3)

    assert not first_thread.is_alive()
    assert not second_started or not second_thread.is_alive()

    assert errors == []
    assert maximum_active_runs == 1
