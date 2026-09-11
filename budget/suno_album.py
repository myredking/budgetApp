"""Build policy-checked album packages from Suno track exports."""

from __future__ import annotations

import argparse
import hashlib
import json
import random
import re
import shutil
import subprocess
import tempfile
from collections import defaultdict
from dataclasses import asdict, dataclass
from datetime import datetime
from pathlib import Path
from typing import Any, Iterable

from PIL import Image, ImageDraw

from .suno_release import (
    ReleaseMetadata,
    ValidationIssue,
    get_ai_credit_labels,
    validate_release,
)


@dataclass(frozen=True)
class AlbumPolicy:
    """Album grouping rules used by the local automation pipeline."""

    min_tracks: int = 4
    max_tracks: int = 12
    album_prefix: str = "Suno Album"

    def __post_init__(self) -> None:
        if self.min_tracks < 1:
            raise ValueError("앨범 최소 곡 수는 1 이상이어야 합니다.")
        if self.max_tracks < self.min_tracks:
            raise ValueError("앨범 최대 곡 수는 최소 곡 수 이상이어야 합니다.")


@dataclass(frozen=True)
class AlbumTrackInput:
    """One local audio and lyric pair with DistroKid metadata."""

    metadata: ReleaseMetadata
    audio_path: Path
    lyrics_path: Path
    track_id: str = ""


@dataclass(frozen=True)
class AlbumSpec:
    """A stable album unit ready for validation and packaging."""

    album_id: str
    album_title: str
    artist_name: str
    primary_genre: str
    month: str
    tracks: tuple[AlbumTrackInput, ...]


def group_tracks_into_albums(
    tracks: Iterable[AlbumTrackInput],
    policy: AlbumPolicy,
) -> list[AlbumSpec]:
    """Group tracks by artist, genre, and generation month."""
    groups: dict[tuple[str, str, str], list[AlbumTrackInput]] = defaultdict(list)
    for track in tracks:
        key = (
            track.metadata.artist_name.strip(),
            track.metadata.primary_genre.strip(),
            _track_month(track.metadata.generated_at),
        )
        groups[key].append(track)
    albums: list[AlbumSpec] = []
    for key in sorted(groups):
        ordered = sorted(groups[key], key=_track_sort_key)
        if len(ordered) < policy.min_tracks:
            continue
        albums.extend(_build_group_albums(key, ordered, policy))
    return albums


def validate_album(
    album: AlbumSpec,
    artwork_path: Path,
) -> list[ValidationIssue]:
    """Return all track-level blockers before a package can be published."""
    issues: list[ValidationIssue] = []
    seen_ids: set[str] = set()
    for track in album.tracks:
        if track.metadata.track_title.strip() in seen_ids:
            issues.append(
                ValidationIssue("duplicate_track", f"[{_track_identity(track)}] 곡 제목이 중복됩니다.")
            )
        seen_ids.add(track.metadata.track_title.strip())
        track_issues = validate_release(
            track.metadata,
            track.audio_path,
            artwork_path,
        )
        issues.extend(
            ValidationIssue(issue.code, f"[{_track_identity(track)}] {issue.message}")
            for issue in track_issues
        )
        issues.extend(_lyrics_issues(track))
    if not album.tracks:
        issues.append(ValidationIssue("album_empty", "앨범에 곡이 없습니다."))
    return issues


def generate_album_cover(
    album_title: str,
    artist_name: str,
    output_path: Path,
    size: int = 3000,
) -> Path:
    """Create a deterministic, local-only abstract RGB JPG cover."""
    if size < 1000:
        raise ValueError("앨범 커버 크기는 최소 1000픽셀이어야 합니다.")
    if output_path.suffix.casefold() not in {".jpg", ".jpeg"}:
        raise ValueError("앨범 커버 출력 파일은 JPG여야 합니다.")
    output_path.parent.mkdir(parents=True, exist_ok=True)
    seed_text = f"{artist_name}\0{album_title}".encode("utf-8")
    seed = int.from_bytes(hashlib.sha256(seed_text).digest()[:8], "big")
    rng = random.Random(seed)
    image = Image.new("RGB", (size, size), _palette_color(rng, 0.2)[:3])
    draw = ImageDraw.Draw(image, "RGBA")
    _draw_cover_layers(draw, rng, size)
    image.save(output_path, format="JPEG", quality=95, subsampling=0)
    return output_path


def build_album_package(
    album: AlbumSpec,
    artwork_path: Path,
    output_root: Path,
) -> Path:
    """Write one atomic DistroKid and YouTube preparation package."""
    issues = validate_album(album, artwork_path)
    if issues:
        raise ValueError("; ".join(issue.message for issue in issues))
    output_root.mkdir(parents=True, exist_ok=True)
    package_path = output_root / album.album_id
    if package_path.exists():
        raise FileExistsError(f"이미 존재하는 앨범 폴더입니다: {package_path}")
    _reject_duplicate_artwork(artwork_path, output_root)
    temporary_path = Path(tempfile.mkdtemp(prefix=f".{album.album_id}.", dir=output_root))
    try:
        _write_album_files(album, artwork_path, temporary_path)
        temporary_path.replace(package_path)
    except BaseException:
        shutil.rmtree(temporary_path, ignore_errors=True)
        raise
    return package_path


def build_ffmpeg_command(package_path: Path, album: AlbumSpec) -> list[str]:
    """Return the free local FFmpeg command for an album listening video."""
    package = _posix_path(package_path)
    concat_file = f"{package}/youtube/concat.txt"
    cover_file = f"{package}/artwork/cover.jpg"
    output_file = f"{package}/youtube/{album.album_id}.mp4"
    return [
        "ffmpeg",
        "-y",
        "-f",
        "concat",
        "-safe",
        "0",
        "-i",
        concat_file,
        "-loop",
        "1",
        "-i",
        cover_file,
        "-map",
        "1:v:0",
        "-map",
        "0:a:0",
        "-c:v",
        "libx264",
        "-tune",
        "stillimage",
        "-pix_fmt",
        "yuv420p",
        "-c:a",
        "aac",
        "-b:a",
        "192k",
        "-shortest",
        "-movflags",
        "+faststart",
        output_file,
    ]


def render_album_video(
    package_path: Path,
    album: AlbumSpec,
    runner: Any = subprocess.run,
) -> Path:
    """Render one listening video locally with FFmpeg."""
    output_path = package_path / "youtube" / f"{album.album_id}.mp4"
    runner(build_ffmpeg_command(package_path, album), check=True)
    return output_path


def load_album_manifest(path: Path) -> AlbumSpec:
    """Load an album manifest with per-track metadata and local file paths."""
    payload = _read_object(path)
    raw_tracks = payload.get("tracks")
    if not isinstance(raw_tracks, list) or not raw_tracks:
        raise ValueError("앨범 manifest의 tracks는 비어 있지 않은 목록이어야 합니다.")
    tracks = tuple(_load_manifest_track(path.parent, item, payload) for item in raw_tracks)
    album_id = _required_text(payload, "album_id")
    album_title = _required_text(payload, "album_title")
    artist_name = _required_text(payload, "artist_name")
    primary_genre = _required_text(payload, "primary_genre")
    month = _required_text(payload, "month")
    return AlbumSpec(album_id, album_title, artist_name, primary_genre, month, tracks)


def load_ingested_tracks(
    input_dir: Path,
    metadata_defaults: dict[str, Any],
) -> tuple[AlbumTrackInput, ...]:
    """Load the sidecars written by ``suno_ingest`` for album grouping."""
    if not input_dir.is_dir():
        raise ValueError(f"Suno 입력 폴더를 찾을 수 없습니다: {input_dir}")
    tracks = [
        _load_ingested_sidecar(sidecar, metadata_defaults)
        for sidecar in sorted(input_dir.glob("*.track.json"))
    ]
    return tuple(tracks)


def main(argv: list[str] | None = None) -> int:
    """Create one album package from a manifest and a local cover."""
    parser = argparse.ArgumentParser(description="Suno 앨범 패키지 생성")
    parser.add_argument("--manifest", required=True)
    parser.add_argument("--output", required=True)
    parser.add_argument("--cover")
    args = parser.parse_args(argv)
    album = load_album_manifest(Path(args.manifest))
    output_root = Path(args.output)
    generated_cover = output_root / f".{album.album_id}-cover.jpg"
    cover_path = Path(args.cover) if args.cover else generate_album_cover(
        album.album_title,
        album.artist_name,
        generated_cover,
    )
    try:
        package = build_album_package(album, cover_path, output_root)
    except (FileExistsError, OSError, ValueError) as error:
        parser.error(str(error))
    finally:
        if not args.cover:
            generated_cover.unlink(missing_ok=True)
    print(f"앨범 패키지를 만들었습니다: {package}")
    return 0


def _build_group_albums(
    key: tuple[str, str, str],
    tracks: list[AlbumTrackInput],
    policy: AlbumPolicy,
) -> list[AlbumSpec]:
    artist, genre, month = key
    chunks = [tracks[index:index + policy.max_tracks] for index in range(0, len(tracks), policy.max_tracks)]
    albums: list[AlbumSpec] = []
    for number, chunk in enumerate(chunks, start=1):
        if len(chunk) < policy.min_tracks:
            continue
        title = f"{policy.album_prefix} - {genre} - {month}"
        if len(chunks) > 1:
            title = f"{title} Vol. {number}"
        album_id = f"{_safe_slug(artist).casefold()}-{_safe_slug(genre).casefold()}-{month}-{number:02d}"
        albums.append(AlbumSpec(album_id, title, artist, genre, month, tuple(chunk)))
    return albums


def _track_sort_key(track: AlbumTrackInput) -> tuple[str, str]:
    return (track.metadata.generated_at, track.metadata.track_title.casefold())


def _track_month(value: str) -> str:
    try:
        return datetime.fromisoformat(value).strftime("%Y-%m")
    except ValueError:
        return "unknown-month"


def _track_identity(track: AlbumTrackInput) -> str:
    return track.track_id or track.metadata.track_title


def _lyrics_issues(track: AlbumTrackInput) -> list[ValidationIssue]:
    identity = _track_identity(track)
    if not track.lyrics_path.is_file():
        return [ValidationIssue("lyrics_missing", f"[{identity}] 가사 파일을 찾을 수 없습니다.")]
    if track.metadata.instrumental:
        return []
    try:
        has_lyrics = bool(track.lyrics_path.read_text(encoding="utf-8").strip())
    except OSError:
        return [ValidationIssue("lyrics_invalid", f"[{identity}] 가사 파일을 읽을 수 없습니다.")]
    if has_lyrics:
        return []
    return [ValidationIssue("lyrics_empty", f"[{identity}] 가사 파일이 비어 있습니다.")]


def _write_album_files(
    album: AlbumSpec,
    artwork_path: Path,
    package_path: Path,
) -> None:
    folders = _create_album_folders(package_path)
    shutil.copy2(artwork_path, folders["artwork"] / "cover.jpg")
    track_rows: list[dict[str, Any]] = []
    for number, track in enumerate(album.tracks, start=1):
        track_rows.append(_write_track(track, number, folders["tracks"]))
    _write_album_metadata(album, track_rows, folders["metadata"] / "album.json")
    _write_youtube_files(album, track_rows, package_path / "youtube")
    _write_checklist(album, track_rows, package_path / "distrokid" / "upload-checklist.md")


def _create_album_folders(package_path: Path) -> dict[str, Path]:
    names = ("tracks", "artwork", "metadata", "youtube", "distrokid")
    folders = {name: package_path / name for name in names}
    for folder in folders.values():
        folder.mkdir(parents=True, exist_ok=False)
    return folders


def _write_track(
    track: AlbumTrackInput,
    number: int,
    tracks_root: Path,
) -> dict[str, Any]:
    folder = tracks_root / f"{number:02d}-{_safe_slug(track.metadata.track_title)}"
    folder.mkdir(parents=True, exist_ok=False)
    audio_target = folder / f"audio{track.audio_path.suffix.lower()}"
    shutil.copy2(track.audio_path, audio_target)
    shutil.copy2(track.lyrics_path, folder / "lyrics.txt")
    metadata = asdict(track.metadata)
    metadata["target_stores"] = list(track.metadata.target_stores)
    metadata["distrokid_ai_credits"] = get_ai_credit_labels(track.metadata)
    package_root = tracks_root.parent
    metadata["files"] = {
        "audio": audio_target.relative_to(package_root).as_posix(),
        "lyrics": (folder / "lyrics.txt").relative_to(package_root).as_posix(),
    }
    _write_json(folder / "metadata.json", metadata)
    return {
        "track_number": number,
        "track_id": _track_identity(track),
        "title": track.metadata.track_title,
        "audio": audio_target.relative_to(package_root).as_posix(),
        "lyrics": (folder / "lyrics.txt").relative_to(package_root).as_posix(),
    }


def _write_album_metadata(
    album: AlbumSpec,
    track_rows: list[dict[str, Any]],
    path: Path,
) -> None:
    payload = {
        "album_id": album.album_id,
        "album_title": album.album_title,
        "artist_name": album.artist_name,
        "primary_genre": album.primary_genre,
        "month": album.month,
        "track_count": len(track_rows),
        "tracks": track_rows,
        "final_distrokid_submission": "manual_after_review",
    }
    _write_json(path, payload)


def _write_youtube_files(
    album: AlbumSpec,
    track_rows: list[dict[str, Any]],
    youtube_root: Path,
) -> None:
    tracklist = "\n".join(
        f"{row['track_number']}. {row['title']}" for row in track_rows
    )
    payload = {
        "title": f"{album.artist_name} - {album.album_title} (Full Album)",
        "description": f"AI-generated music created with Suno.\n\nTracklist:\n{tracklist}",
        "tags": [album.artist_name, album.primary_genre, "AI music"],
        "category_id": "10",
        "privacy_status": "private",
        "contains_synthetic_media": True,
        "notify_subscribers": False,
    }
    _write_json(youtube_root / "metadata.json", payload)
    concat_lines = [
        f"file '{_ffmpeg_quote('../' + row['audio'])}'" for row in track_rows
    ]
    (youtube_root / "concat.txt").write_text("\n".join(concat_lines) + "\n", encoding="utf-8")


def _write_checklist(
    album: AlbumSpec,
    track_rows: list[dict[str, Any]],
    path: Path,
) -> None:
    tracklist = "\n".join(
        f"- [ ] {row['track_number']:02d}. {row['title']}" for row in track_rows
    )
    content = f"""# DistroKid Album Upload Checklist

- [ ] Album: {album.album_title}
- [ ] Artist: {album.artist_name}
- [ ] Genre: {album.primary_genre}
- [ ] Track count: {len(track_rows)}
- [ ] Cover is one square RGB JPG, reviewed by a human
- [ ] Every track was played from start to finish
- [ ] Every track has paid Suno rights evidence
- [ ] AI credits and artist persona were checked for every track

## Tracklist

{tracklist}

Final DistroKid login and submission remain manual.
"""
    path.write_text(content, encoding="utf-8")


def _load_manifest_track(
    base_dir: Path,
    item: object,
    album_payload: dict[str, Any],
) -> AlbumTrackInput:
    if not isinstance(item, dict):
        raise ValueError("tracks 항목은 객체여야 합니다.")
    defaults = album_payload.get("metadata_defaults", {})
    overrides = item.get("metadata", {})
    if not isinstance(defaults, dict) or not isinstance(overrides, dict):
        raise ValueError("metadata_defaults와 metadata는 객체여야 합니다.")
    metadata_values = dict(defaults)
    metadata_values.update(overrides)
    metadata_values["track_title"] = item.get("title", metadata_values.get("track_title"))
    audio_path = _manifest_path(base_dir, item.get("audio_path"))
    lyrics_path = _manifest_path(base_dir, item.get("lyrics_path"))
    metadata = _metadata_from_values(metadata_values)
    track_id = str(item.get("track_id", "")).strip()
    return AlbumTrackInput(metadata, audio_path, lyrics_path, track_id)


def _load_ingested_sidecar(
    path: Path,
    metadata_defaults: dict[str, Any],
) -> AlbumTrackInput:
    payload = _read_object(path)
    values = dict(metadata_defaults)
    values["track_title"] = _required_text(payload, "title")
    values["primary_genre"] = _required_text(payload, "genre")
    values["generated_at"] = _required_text(payload, "created_at")
    values["downloaded_at"] = _required_text(payload, "downloaded_at")
    metadata = _metadata_from_values(values)
    track_id = _required_text(payload, "track_id")
    audio_path = _manifest_path(path.parent, payload.get("audio_path"))
    lyrics_path = _manifest_path(path.parent, payload.get("lyrics_path"))
    return AlbumTrackInput(metadata, audio_path, lyrics_path, track_id)


def _metadata_from_values(values: dict[str, Any]) -> ReleaseMetadata:
    data = dict(values)
    if isinstance(data.get("target_stores"), list):
        data["target_stores"] = tuple(data["target_stores"])
    try:
        return ReleaseMetadata(**data)
    except TypeError as error:
        raise ValueError(f"트랙 metadata 필드가 잘못되었습니다: {error}") from error


def _manifest_path(base_dir: Path, value: object) -> Path:
    if not isinstance(value, str) or not value.strip():
        raise ValueError("audio_path와 lyrics_path가 필요합니다.")
    path = Path(value)
    return path if path.is_absolute() else base_dir / path


def _read_object(path: Path) -> dict[str, Any]:
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        raise ValueError(f"앨범 manifest를 읽을 수 없습니다: {error}") from error
    if not isinstance(payload, dict):
        raise ValueError("앨범 manifest는 객체여야 합니다.")
    return payload


def _required_text(payload: dict[str, Any], name: str) -> str:
    value = payload.get(name)
    if not isinstance(value, str) or not value.strip():
        raise ValueError(f"앨범 manifest에 {name} 값이 필요합니다.")
    return value.strip()


def _draw_cover_layers(draw: ImageDraw.ImageDraw, rng: random.Random, size: int) -> None:
    for index in range(12):
        color = _palette_color(rng, 0.35 + index / 50)
        margin = rng.randint(-size // 4, size // 2)
        extent = rng.randint(size // 3, size)
        draw.ellipse((margin, margin, margin + extent, margin + extent), fill=color)
    for index in range(18):
        start = (rng.randint(0, size), rng.randint(0, size))
        end = (rng.randint(0, size), rng.randint(0, size))
        draw.line((start, end), fill=_palette_color(rng, 0.6), width=max(8, size // 180))


def _palette_color(rng: random.Random, alpha: float) -> tuple[int, int, int, int]:
    base = rng.choice(((20, 35, 80), (80, 25, 95), (10, 100, 110), (170, 70, 35)))
    return (*base, int(max(0.0, min(1.0, alpha)) * 255))


def _safe_slug(value: str) -> str:
    cleaned = re.sub(r"[^\w\s-]", "", value, flags=re.UNICODE).strip()
    cleaned = re.sub(r"[\s_-]+", "-", cleaned)
    return cleaned or "untitled"


def _ffmpeg_quote(value: str) -> str:
    return value.replace("'", "'\\''")


def _posix_path(path: Path) -> str:
    return path.as_posix()


def _reject_duplicate_artwork(artwork_path: Path, output_root: Path) -> None:
    artwork_hash = _file_sha256(artwork_path)
    for existing in output_root.rglob("cover.jpg"):
        if _file_sha256(existing) == artwork_hash:
            raise ValueError("duplicate_artwork: 이미 사용한 커버 이미지입니다.")


def _file_sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def _write_json(path: Path, payload: dict[str, Any]) -> None:
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":  # pragma: no cover
    raise SystemExit(main())
