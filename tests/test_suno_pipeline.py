import json
from pathlib import Path

from budget.suno_pipeline import (
    AlbumCandidate,
    prepare_album_candidates,
    render_ready_videos,
)
from budget.suno_album import AlbumPolicy, AlbumSpec, AlbumTrackInput
from budget.suno_release import ReleaseMetadata


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

    videos = render_ready_videos(
        [candidate],
        [tmp_path / album.album_id],
        fake_runner,
    )

    assert videos == [tmp_path / album.album_id / "youtube" / "artist-album-01.mp4"]
    assert calls
