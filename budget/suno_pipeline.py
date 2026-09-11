"""Local, review-first orchestration for Suno album candidates."""

from __future__ import annotations

import argparse
import json
import subprocess
from dataclasses import dataclass
from pathlib import Path
from typing import Any

from .suno_album import (
    AlbumPolicy,
    AlbumSpec,
    build_album_package,
    generate_album_cover,
    group_tracks_into_albums,
    load_ingested_tracks,
    render_album_video,
    validate_album,
)
from .youtube_upload import YouTubeUploadResult, YouTubeUploader, load_youtube_video


@dataclass(frozen=True)
class AlbumCandidate:
    """An album candidate and its human-review status."""

    album: AlbumSpec
    cover_path: Path
    issues: tuple[dict[str, str], ...]


def prepare_album_candidates(
    input_dir: Path,
    output_root: Path,
    metadata_defaults: dict[str, Any],
    policy: AlbumPolicy,
) -> list[AlbumCandidate]:
    """Scan ingested sidecars, generate covers, and write review plans."""
    tracks = load_ingested_tracks(input_dir, metadata_defaults)
    albums = group_tracks_into_albums(tracks, policy)
    covers_dir = output_root / "covers"
    candidates: list[AlbumCandidate] = []
    for album in albums:
        cover_path = _ensure_cover(album, covers_dir)
        issues = tuple(
            {"code": issue.code, "message": issue.message}
            for issue in validate_album(album, cover_path)
        )
        candidate = AlbumCandidate(album, cover_path, issues)
        _write_plan(output_root / "plans" / f"{album.album_id}.json", candidate)
        candidates.append(candidate)
    return candidates


def build_ready_albums(
    candidates: list[AlbumCandidate],
    output_root: Path,
) -> list[Path]:
    """Build only candidates with no blocking policy issues."""
    packages: list[Path] = []
    for candidate in candidates:
        if candidate.issues:
            continue
        package_path = output_root / candidate.album.album_id
        if not package_path.exists():
            package_path = build_album_package(
                candidate.album,
                candidate.cover_path,
                output_root,
            )
        packages.append(package_path)
    return packages


def render_ready_videos(
    candidates: list[AlbumCandidate],
    package_paths: list[Path],
    runner: Any = subprocess.run,
) -> list[Path]:
    """Render local listening videos for successfully packaged albums."""
    package_by_id = {path.name: path for path in package_paths}
    videos: list[Path] = []
    for candidate in candidates:
        package_path = package_by_id.get(candidate.album.album_id)
        if candidate.issues or package_path is None:
            continue
        videos.append(render_album_video(package_path, candidate.album, runner))
    return videos


def upload_ready_videos(
    candidates: list[AlbumCandidate],
    package_paths: list[Path],
    uploader: YouTubeUploader,
    dry_run: bool = False,
) -> list[YouTubeUploadResult]:
    """Upload rendered videos using the official API and local manifest state."""
    package_by_id = {path.name: path for path in package_paths}
    results: list[YouTubeUploadResult] = []
    for candidate in candidates:
        package_path = package_by_id.get(candidate.album.album_id)
        if candidate.issues or package_path is None:
            continue
        metadata_path = package_path / "youtube" / "metadata.json"
        video_path = package_path / "youtube" / f"{candidate.album.album_id}.mp4"
        results.append(
            uploader.upload(video_path, load_youtube_video(metadata_path), dry_run=dry_run)
        )
    return results


def main(argv: list[str] | None = None) -> int:
    """Run the local candidate scan and optional package build."""
    parser = argparse.ArgumentParser(description="Suno 앨범 자동화 후보 생성")
    parser.add_argument("--input", required=True, help="suno_ingest 출력 폴더")
    parser.add_argument("--output", required=True, help="앨범 출력 폴더")
    parser.add_argument("--metadata-defaults", required=True)
    parser.add_argument("--min-tracks", type=int, default=4)
    parser.add_argument("--max-tracks", type=int, default=12)
    parser.add_argument("--build", action="store_true", help="검토 완료 후보만 패키징")
    parser.add_argument("--render-video", action="store_true", help="로컬 FFmpeg 영상 생성")
    parser.add_argument("--upload-youtube", action="store_true", help="YouTube에 비공개 업로드")
    parser.add_argument("--dry-run", action="store_true", help="YouTube 전송 없이 점검")
    parser.add_argument("--youtube-state", default=".state/youtube-uploads.json")
    parser.add_argument("--youtube-client-secrets", default="secrets/youtube-client.json")
    parser.add_argument("--youtube-token", default=".state/youtube-token.json")
    args = parser.parse_args(argv)
    try:
        defaults = _load_defaults(Path(args.metadata_defaults))
        policy = AlbumPolicy(args.min_tracks, args.max_tracks)
        candidates = prepare_album_candidates(
            Path(args.input),
            Path(args.output),
            defaults,
            policy,
        )
        needs_build = args.build or args.render_video or args.upload_youtube
        packages = build_ready_albums(candidates, Path(args.output)) if needs_build else []
        videos = render_ready_videos(candidates, packages) if args.render_video or args.upload_youtube else []
        uploads = []
        if args.upload_youtube:
            uploader = YouTubeUploader(
                state_path=Path(args.youtube_state),
                client_secrets_path=Path(args.youtube_client_secrets),
                token_path=Path(args.youtube_token),
            )
            uploads = upload_ready_videos(candidates, packages, uploader, args.dry_run)
    except (OSError, RuntimeError, ValueError) as error:
        parser.error(str(error))
    _print_summary(candidates, packages, videos, uploads)
    return 0


def _ensure_cover(album: AlbumSpec, covers_dir: Path) -> Path:
    path = covers_dir / f"{album.album_id}.jpg"
    if not path.is_file():
        generate_album_cover(album.album_title, album.artist_name, path)
    return path


def _write_plan(path: Path, candidate: AlbumCandidate) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    payload = {
        "album_id": candidate.album.album_id,
        "album_title": candidate.album.album_title,
        "artist_name": candidate.album.artist_name,
        "primary_genre": candidate.album.primary_genre,
        "month": candidate.album.month,
        "track_count": len(candidate.album.tracks),
        "cover_path": candidate.cover_path.as_posix(),
        "track_ids": [track.track_id for track in candidate.album.tracks],
        "issues": list(candidate.issues),
    }
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def _load_defaults(path: Path) -> dict[str, Any]:
    payload = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(payload, dict):
        raise ValueError("metadata defaults는 객체여야 합니다.")
    return payload


def _print_summary(
    candidates: list[AlbumCandidate],
    packages: list[Path],
    videos: list[Path],
    uploads: list[YouTubeUploadResult],
) -> None:
    for candidate in candidates:
        status = "READY" if not candidate.issues else f"REVIEW ({len(candidate.issues)})"
        print(f"{candidate.album.album_id}: {status}; cover={candidate.cover_path}")
    for package in packages:
        print(f"패키징 완료: {package}")
    for video in videos:
        print(f"영상 생성 완료: {video}")
    for upload in uploads:
        message = "전송 점검 완료" if upload.dry_run else f"YouTube 업로드 완료: {upload.url}"
        print(message)


if __name__ == "__main__":  # pragma: no cover
    raise SystemExit(main())
