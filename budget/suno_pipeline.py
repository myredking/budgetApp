"""Local, review-first orchestration for Suno album candidates."""

from __future__ import annotations

import argparse
import json
import shutil
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
from .youtube_upload import (
    YouTubeUploadResult,
    YouTubeUploader,
    load_youtube_video,
    validate_youtube_token_file,
)


@dataclass(frozen=True)
class AlbumCandidate:
    """An album candidate and its human-review status."""

    album: AlbumSpec
    cover_path: Path
    issues: tuple[dict[str, str], ...]


@dataclass(frozen=True)
class PreflightIssue:
    """One environment problem that should be fixed before automation runs."""

    code: str
    message: str


def run_preflight(
    input_dir: Path,
    output_root: Path,
    defaults_path: Path,
    *,
    needs_render: bool = False,
    needs_upload: bool = False,
    dry_run: bool = False,
    require_youtube_token: bool = False,
    client_secrets_path: Path = Path("secrets/youtube-client.json"),
    token_path: Path = Path(".state/youtube-token.json"),
) -> list[PreflightIssue]:
    """Check local prerequisites without creating folders or calling APIs."""
    issues: list[PreflightIssue] = []
    if not input_dir.is_dir():
        issues.append(PreflightIssue("input_missing", f"입력 폴더가 없습니다: {input_dir}"))
    if output_root.exists() and not output_root.is_dir():
        issues.append(PreflightIssue("output_invalid", f"출력 경로가 폴더가 아닙니다: {output_root}"))
    issues.extend(_check_defaults_file(defaults_path))
    if needs_render and shutil.which("ffmpeg") is None:
        issues.append(PreflightIssue("ffmpeg_missing", "영상 생성에 필요한 FFmpeg를 PATH에서 찾을 수 없습니다."))
    if needs_upload and not dry_run:
        issues.extend(
            _check_youtube_auth(
                client_secrets_path,
                token_path,
                require_token=require_youtube_token,
            )
        )
    return issues


def _check_youtube_auth(
    client_secrets_path: Path,
    token_path: Path,
    *,
    require_token: bool,
) -> list[PreflightIssue]:
    if require_token and not token_path.is_file():
        return [
            PreflightIssue(
                "youtube_token_missing",
                "비대화형 업로드에는 YouTube token JSON이 필요합니다.",
            )
        ]
    if require_token:
        try:
            validate_youtube_token_file(token_path)
        except (OSError, ValueError) as error:
            return [PreflightIssue("youtube_token_invalid", str(error))]
        return []
    if _youtube_credentials_available(client_secrets_path, token_path):
        return []
    return [
        PreflightIssue(
            "youtube_credentials_missing",
            "YouTube OAuth client JSON 또는 기존 token JSON이 필요합니다.",
        )
    ]


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
        if package_path.exists():
            if not _existing_package_matches(package_path, candidate.album):
                raise ValueError(
                    "기존 앨범 패키지가 현재 후보의 트랙 구성과 다릅니다: "
                    f"{package_path}"
                )
        else:
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
    parser.add_argument("--preflight", action="store_true", help="실행 전 사전 점검")
    parser.add_argument("--preflight-only", action="store_true", help="사전 점검만 실행하고 종료")
    parser.add_argument("--non-interactive", action="store_true", help="브라우저 인증 없이 실행")
    parser.add_argument("--youtube-state", default=".state/youtube-uploads.json")
    parser.add_argument("--youtube-client-secrets", default="secrets/youtube-client.json")
    parser.add_argument("--youtube-token", default=".state/youtube-token.json")
    args = parser.parse_args(argv)
    try:
        _preflight_args(args)
        if _is_preflight_only(args):
            print("사전 점검 통과: 로컬 파일과 실행 조건이 준비되었습니다.")
            return 0
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
                allow_interactive_oauth=not args.non_interactive,
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


def _check_defaults_file(path: Path) -> list[PreflightIssue]:
    if not path.is_file():
        return [
            PreflightIssue("defaults_missing", f"메타데이터 설정 파일이 없습니다: {path}")
        ]
    try:
        payload = _load_defaults(path)
    except (OSError, ValueError) as error:
        return [PreflightIssue("defaults_invalid", str(error))]
    required = {
        "artist_name",
        "songwriter_name",
        "suno_plan",
        "lyrics_by_ai",
        "music_by_ai",
    }
    missing = sorted(name for name in required if name not in payload)
    if missing:
        return [
            PreflightIssue(
                "defaults_incomplete",
                f"설정 파일에 필수값이 없습니다: {', '.join(missing)}",
            )
        ]
    return []


def _preflight_args(args: argparse.Namespace) -> None:
    issues = run_preflight(
        Path(args.input),
        Path(args.output),
        Path(args.metadata_defaults),
        needs_render=args.render_video or args.upload_youtube,
        needs_upload=args.upload_youtube,
        dry_run=args.dry_run,
        require_youtube_token=args.non_interactive,
        client_secrets_path=Path(args.youtube_client_secrets),
        token_path=Path(args.youtube_token),
    )
    if issues:
        raise ValueError(_format_preflight_issues(issues))


def _is_preflight_only(args: argparse.Namespace) -> bool:
    return args.preflight_only


def _existing_package_matches(package_path: Path, album: AlbumSpec) -> bool:
    if not package_path.is_dir():
        return False
    payload = _load_package_metadata(package_path)
    if payload is None:
        return False
    rows = _package_track_rows(payload)
    if rows is None:
        return False
    expected_ids = [
        track.track_id or track.metadata.track_title for track in album.tracks
    ]
    if not _package_metadata_matches(payload, rows, album.album_id, expected_ids):
        return False
    return _package_files_are_complete(package_path, rows)


def _load_package_metadata(package_path: Path) -> dict[str, Any] | None:
    metadata_path = package_path / "metadata" / "album.json"
    try:
        payload = json.loads(metadata_path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        return None
    return payload if isinstance(payload, dict) else None


def _package_track_rows(payload: dict[str, Any]) -> list[dict[str, Any]] | None:
    rows = payload.get("tracks")
    if not isinstance(rows, list) or not rows:
        return None
    if not all(
        isinstance(row, dict) and isinstance(row.get("track_id"), str)
        for row in rows
    ):
        return None
    return rows


def _package_metadata_matches(
    payload: dict[str, Any],
    rows: list[dict[str, Any]],
    album_id: str,
    expected_ids: list[str],
) -> bool:
    track_ids = [row["track_id"] for row in rows]
    track_count = payload.get("track_count")
    return (
        payload.get("album_id") == album_id
        and type(track_count) is int
        and track_count == len(expected_ids)
        and track_ids == expected_ids
    )


def _package_files_are_complete(
    package_path: Path,
    rows: list[dict[str, Any]],
) -> bool:
    required_files = (
        package_path / "artwork" / "cover.jpg",
        package_path / "metadata" / "album.json",
        package_path / "youtube" / "metadata.json",
        package_path / "youtube" / "concat.txt",
        package_path / "distrokid" / "upload-checklist.md",
    )
    if not all(_file_has_content(path) for path in required_files):
        return False
    if not _youtube_files_are_valid(package_path, rows):
        return False
    return all(_track_files_are_complete(package_path, row) for row in rows)


def _track_files_are_complete(
    package_path: Path,
    row: dict[str, Any],
) -> bool:
    audio_value = row.get("audio")
    lyrics_value = row.get("lyrics")
    if not isinstance(audio_value, str) or not isinstance(lyrics_value, str):
        return False
    audio_path = _package_relative_path(package_path, audio_value)
    lyrics_path = _package_relative_path(package_path, lyrics_value)
    if audio_path is None or lyrics_path is None:
        return False
    metadata_path = audio_path.parent / "metadata.json"
    return (
        _file_has_content(audio_path)
        and lyrics_path.is_file()
        and _json_object_file(metadata_path)
    )


def _youtube_files_are_valid(
    package_path: Path,
    rows: list[dict[str, Any]],
) -> bool:
    metadata_path = package_path / "youtube" / "metadata.json"
    try:
        load_youtube_video(metadata_path)
        payload = json.loads(metadata_path.read_text(encoding="utf-8"))
    except (OSError, ValueError, json.JSONDecodeError):
        return False
    if not isinstance(payload, dict):
        return False
    if payload.get("privacy_status") != "private":
        return False
    if payload.get("contains_synthetic_media") is not True:
        return False
    if payload.get("notify_subscribers") is not False:
        return False
    return _concat_file_matches(package_path, rows)


def _concat_file_matches(
    package_path: Path,
    rows: list[dict[str, Any]],
) -> bool:
    concat_path = package_path / "youtube" / "concat.txt"
    try:
        actual_lines = [
            line.strip()
            for line in concat_path.read_text(encoding="utf-8").splitlines()
            if line.strip()
        ]
    except OSError:
        return False
    expected_lines: list[str] = []
    for row in rows:
        audio_value = row.get("audio")
        if not isinstance(audio_value, str):
            return False
        if _package_relative_path(package_path, audio_value) is None:
            return False
        expected_lines.append(f"file '../{audio_value}'")
    return actual_lines == expected_lines


def _file_has_content(path: Path) -> bool:
    try:
        return path.is_file() and path.stat().st_size > 0
    except OSError:
        return False


def _package_relative_path(package_path: Path, value: str) -> Path | None:
    relative_path = Path(value)
    if relative_path.is_absolute():
        return None
    try:
        package_root = package_path.resolve()
        resolved_path = (package_root / relative_path).resolve()
        resolved_path.relative_to(package_root)
    except (OSError, ValueError):
        return None
    return resolved_path


def _json_object_file(path: Path) -> bool:
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        return False
    return isinstance(payload, dict)


def _youtube_credentials_available(client_secrets_path: Path, token_path: Path) -> bool:
    return client_secrets_path.is_file() or token_path.is_file()


def _format_preflight_issues(issues: list[PreflightIssue]) -> str:
    return "사전 점검 실패: " + "; ".join(
        f"{issue.code} - {issue.message}" for issue in issues
    )


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
