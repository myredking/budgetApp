"""Prepare Suno tracks for a human-reviewed DistroKid upload."""

import argparse
import hashlib
import json
import re
import shutil
import struct
import tempfile
import wave
from dataclasses import asdict, dataclass
from datetime import date, datetime
from pathlib import Path
from typing import Any

from mutagen import File as MutagenFile
from mutagen import MutagenError
from PIL import Image, UnidentifiedImageError


SUPPORTED_AUDIO_FORMATS = {".wav", ".flac", ".mp3", ".m4a", ".aiff", ".wma"}
PAID_SUNO_PLANS = {"pro", "premier"}
ARTWORK_FORMATS = {".jpg", ".jpeg"}
ARTWORK_MIN_SIZE = 1000
MAX_AUDIO_BYTES = 1_000_000_000
MAX_AUDIO_DURATION_SECONDS = 5 * 60 * 60
EXPECTED_AUDIO_TYPES = {
    ".wav": "WAVE",
    ".flac": "FLAC",
    ".mp3": "MP3",
    ".m4a": "MP4",
    ".aiff": "AIFF",
    ".wma": "ASF",
}


@dataclass(frozen=True)
class ReleaseMetadata:
    """Metadata required to prepare one release package."""

    artist_name: str
    track_title: str
    primary_genre: str
    songwriter_name: str
    suno_plan: str
    generated_at: str
    downloaded_at: str
    lyrics_by_ai: bool
    music_by_ai: bool
    audio_scope: str = "none"
    secondary_genre: str = ""
    language: str = "Korean"
    producer_name: str = ""
    explicit: bool = False
    is_cover: bool = False
    cover_license_confirmed: bool = False
    uses_sample: bool = False
    rights_confirmed: bool = False
    rights_evidence_path: str = ""
    artwork_reviewed: bool = False
    audio_reviewed: bool = False
    artist_persona: str = "human"
    artist_persona_confirmed: bool = False
    release_date: str = ""
    instrumental: bool = False
    target_stores: tuple[str, ...] = (
        "Spotify",
        "Apple Music",
        "YouTube Music",
        "TikTok",
    )


@dataclass(frozen=True)
class ValidationIssue:
    """A blocking issue that must be resolved before packaging."""

    code: str
    message: str


def get_ai_credit_labels(metadata: ReleaseMetadata) -> list[str]:
    """Return the DistroKid AI credit choices for the release."""
    labels: list[str] = []
    if metadata.lyrics_by_ai:
        labels.append("The lyrics")
    if metadata.music_by_ai:
        labels.append("The music")
    if metadata.audio_scope == "all" and metadata.music_by_ai:
        labels.append("All of the audio")
    if metadata.audio_scope == "part" and labels:
        labels.append("Part of the audio")
    return labels


def validate_release(
    metadata: ReleaseMetadata,
    audio_path: Path,
    artwork_path: Path,
) -> list[ValidationIssue]:
    """Return all issues that block a DistroKid-ready release package."""
    issues = _metadata_issues(metadata)
    issues.extend(_rights_issues(metadata))
    issues.extend(_audio_issues(audio_path))
    issues.extend(_artwork_issues(artwork_path))
    if not metadata.audio_reviewed:
        issues.append(
            ValidationIssue(
                "audio_not_reviewed",
                "음원을 처음부터 끝까지 직접 재생 확인해야 합니다.",
            )
        )
    if not metadata.artwork_reviewed:
        issues.append(
            ValidationIssue(
                "artwork_not_reviewed",
                "커버의 금지 요소와 사용 권리를 직접 확인해야 합니다.",
            )
        )
    return issues


def build_release_package(
    metadata: ReleaseMetadata,
    audio_path: Path,
    artwork_path: Path,
    lyrics_path: Path,
    output_root: Path,
) -> Path:
    """Copy assets and write metadata, rights, and upload checklist files."""
    issues = validate_release(metadata, audio_path, artwork_path)
    if issues:
        messages = "; ".join(issue.message for issue in issues)
        raise ValueError(messages)
    if not lyrics_path.is_file():
        raise ValueError("가사 파일을 찾을 수 없습니다.")
    lyrics = lyrics_path.read_text(encoding="utf-8")
    if not metadata.instrumental and not lyrics.strip():
        raise ValueError("가사 파일이 비어 있습니다. instrumental=true를 확인하세요.")

    output_root.mkdir(parents=True, exist_ok=True)
    package_path = output_root / _package_name(metadata)
    if package_path.exists():
        raise FileExistsError(f"이미 존재하는 발매 폴더입니다: {package_path}")
    _reject_duplicate_artwork(artwork_path, output_root)
    temporary_path = Path(
        tempfile.mkdtemp(prefix=f".{package_path.name}.", dir=output_root)
    )
    try:
        _write_package_files(
            metadata,
            audio_path,
            artwork_path,
            lyrics_path,
            temporary_path,
        )
        temporary_path.replace(package_path)
    except BaseException:
        shutil.rmtree(temporary_path, ignore_errors=True)
        raise
    return package_path


def load_metadata(path: Path) -> ReleaseMetadata:
    """Load release metadata from a JSON file."""
    payload: object = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(payload, dict):
        raise ValueError("메타데이터 JSON은 객체여야 합니다.")
    values: dict[str, Any] = dict(payload)
    _validate_metadata_payload(values)
    if "target_stores" in values:
        values["target_stores"] = tuple(values["target_stores"])
    try:
        return ReleaseMetadata(**values)
    except TypeError as error:
        raise ValueError(f"메타데이터 필드가 잘못되었습니다: {error}") from error


def _validate_metadata_payload(values: dict[str, Any]) -> None:
    _validate_string_fields(values)
    _validate_boolean_fields(values)
    _validate_target_stores(values)


def _validate_string_fields(values: dict[str, Any]) -> None:
    required_fields = (
        "artist_name",
        "track_title",
        "primary_genre",
        "songwriter_name",
        "suno_plan",
        "generated_at",
        "downloaded_at",
    )
    optional_fields = (
        "secondary_genre",
        "language",
        "producer_name",
        "audio_scope",
        "artist_persona",
        "release_date",
        "rights_evidence_path",
    )
    for field_name in required_fields:
        if not isinstance(values.get(field_name), str):
            raise ValueError(f"{field_name}은 문자열이어야 합니다.")
    for field_name in optional_fields:
        if field_name in values and not isinstance(values[field_name], str):
            raise ValueError(f"{field_name}은 문자열이어야 합니다.")


def _validate_boolean_fields(values: dict[str, Any]) -> None:
    required_fields = ("lyrics_by_ai", "music_by_ai")
    optional_fields = (
        "lyrics_by_ai",
        "music_by_ai",
        "explicit",
        "is_cover",
        "cover_license_confirmed",
        "uses_sample",
        "rights_confirmed",
        "artist_persona_confirmed",
        "artwork_reviewed",
        "audio_reviewed",
        "instrumental",
    )
    for field_name in required_fields:
        if not isinstance(values.get(field_name), bool):
            raise ValueError(f"{field_name}은 불리언이어야 합니다.")
    for field_name in optional_fields:
        if field_name in values and not isinstance(values[field_name], bool):
            raise ValueError(f"{field_name}은 불리언이어야 합니다.")


def _validate_target_stores(values: dict[str, Any]) -> None:
    if "target_stores" not in values:
        return
    stores = values["target_stores"]
    if not isinstance(stores, list) or not stores:
        raise ValueError("target_stores는 비어 있지 않은 문자열 목록이어야 합니다.")
    if not all(isinstance(store, str) and store.strip() for store in stores):
        raise ValueError("target_stores는 문자열 목록이어야 합니다.")


def main(argv: list[str] | None = None) -> int:
    """Create a release package from command-line paths and metadata."""
    parser = _build_parser()
    args = parser.parse_args(argv)
    try:
        metadata = load_metadata(Path(args.metadata))
        package_path = build_release_package(
            metadata,
            Path(args.audio),
            Path(args.artwork),
            Path(args.lyrics),
            Path(args.output),
        )
    except (FileExistsError, OSError, TypeError, ValueError) as error:
        parser.error(str(error))
    print(f"발매 패키지를 만들었습니다: {package_path}")
    return 0


def _metadata_issues(metadata: ReleaseMetadata) -> list[ValidationIssue]:
    issues = _required_metadata_issues(metadata)
    issues.extend(_timestamp_issues(metadata))
    issues.extend(_selection_issues(metadata))
    issues.extend(_target_store_issues(metadata))
    return issues


def _required_metadata_issues(
    metadata: ReleaseMetadata,
) -> list[ValidationIssue]:
    issues: list[ValidationIssue] = []
    required = {
        "artist_name": metadata.artist_name,
        "track_title": metadata.track_title,
        "primary_genre": metadata.primary_genre,
        "songwriter_name": metadata.songwriter_name,
        "generated_at": metadata.generated_at,
        "downloaded_at": metadata.downloaded_at,
    }
    for field_name, value in required.items():
        if not str(value).strip():
            issues.append(
                ValidationIssue("missing_metadata", f"{field_name} 값이 필요합니다.")
            )
    return issues


def _timestamp_issues(metadata: ReleaseMetadata) -> list[ValidationIssue]:
    generated = _parse_timestamp(metadata.generated_at)
    downloaded = _parse_timestamp(metadata.downloaded_at)
    issues = _invalid_timestamp_issues(generated, downloaded)
    issues.extend(_timestamp_timezone_issues(generated, downloaded))
    issues.extend(_timestamp_order_issues(generated, downloaded))
    issues.extend(_release_date_issues(metadata.release_date))
    return issues


def _timestamp_timezone_issues(
    generated: datetime | None,
    downloaded: datetime | None,
) -> list[ValidationIssue]:
    naive_timestamp = any(
        timestamp is not None and not _is_timezone_aware(timestamp)
        for timestamp in (generated, downloaded)
    )
    if naive_timestamp:
        return [
            ValidationIssue(
                "timestamp_timezone_required",
                "generated_at과 downloaded_at은 시간대를 포함해야 합니다.",
            )
        ]
    return []


def _invalid_timestamp_issues(
    generated: datetime | None,
    downloaded: datetime | None,
) -> list[ValidationIssue]:
    issues: list[ValidationIssue] = []
    if generated is None:
        issues.append(
            ValidationIssue(
                "invalid_generated_at",
                "generated_at 형식이 잘못되었습니다.",
            )
        )
    if downloaded is None:
        issues.append(
            ValidationIssue(
                "invalid_downloaded_at",
                "downloaded_at 형식이 잘못되었습니다.",
            )
        )
    return issues


def _timestamp_order_issues(
    generated: datetime | None,
    downloaded: datetime | None,
) -> list[ValidationIssue]:
    if not generated or not downloaded:
        return []
    issues: list[ValidationIssue] = []
    if _mixed_timezone(generated, downloaded):
        issues.append(
            ValidationIssue(
                "mixed_timestamp_timezone",
                "generated_at과 downloaded_at의 시간대 표기가 일치해야 합니다.",
            )
        )
        return issues
    if downloaded < generated:
        issues.append(
            ValidationIssue(
                "download_before_generation",
                "다운로드 시각이 생성 시각보다 빠릅니다.",
            )
        )
        return issues
    return []


def _release_date_issues(value: str) -> list[ValidationIssue]:
    if not value:
        return []
    if not re.fullmatch(r"\d{4}-\d{2}-\d{2}", value):
        return [
            ValidationIssue(
                "invalid_release_date",
                "release_date는 YYYY-MM-DD 형식이어야 합니다.",
            )
        ]
    try:
        date.fromisoformat(value)
    except ValueError:
        return [
            ValidationIssue(
                "invalid_release_date",
                "release_date는 YYYY-MM-DD 형식이어야 합니다.",
            )
        ]
    return []


def _mixed_timezone(first: datetime, second: datetime) -> bool:
    first_aware = _is_timezone_aware(first)
    second_aware = _is_timezone_aware(second)
    return first_aware != second_aware


def _is_timezone_aware(value: datetime) -> bool:
    return value.tzinfo is not None and value.utcoffset() is not None


def _parse_timestamp(value: str) -> datetime | None:
    try:
        return datetime.fromisoformat(value)
    except (TypeError, ValueError):
        return None


def _selection_issues(metadata: ReleaseMetadata) -> list[ValidationIssue]:
    issues: list[ValidationIssue] = []
    if metadata.audio_scope not in {"all", "part", "none"}:
        issues.append(
            ValidationIssue(
                "invalid_audio_scope",
                "audio_scope는 all, part, none 중 하나여야 합니다.",
            )
        )
    if metadata.artist_persona not in {"human", "ai"}:
        issues.append(
            ValidationIssue(
                "invalid_artist_persona",
                "artist_persona는 human 또는 ai여야 합니다.",
            )
        )
    issues.extend(_ai_scope_issues(metadata))
    return issues


def _ai_scope_issues(metadata: ReleaseMetadata) -> list[ValidationIssue]:
    has_ai_parts = metadata.lyrics_by_ai or metadata.music_by_ai
    if metadata.audio_scope == "none" and has_ai_parts:
        return [
            ValidationIssue(
                "ai_scope_mismatch",
                "AI 생성 항목이 있으면 audio_scope를 all 또는 part로 설정해야 합니다.",
            )
        ]
    if metadata.audio_scope == "all" and not metadata.music_by_ai:
        return [
            ValidationIssue(
                "ai_scope_mismatch",
                "audio_scope=all에는 AI로 생성된 음악 플래그가 필요합니다.",
            )
        ]
    if metadata.audio_scope == "all" and not metadata.artist_persona_confirmed:
        return [
            ValidationIssue(
                "artist_persona_not_confirmed",
                "All of the audio 선택 시 아티스트 페르소나를 직접 확인해야 합니다.",
            )
        ]
    if metadata.audio_scope == "part" and not has_ai_parts:
        return [
            ValidationIssue(
                "ai_scope_mismatch",
                "AI 플래그가 없으면 audio_scope를 none으로 설정해야 합니다.",
            )
        ]
    return []


def _target_store_issues(metadata: ReleaseMetadata) -> list[ValidationIssue]:
    if not metadata.target_stores:
        return [
            ValidationIssue(
                "target_stores_empty",
                "하나 이상의 배포 대상 스토어를 선택해야 합니다.",
            )
        ]
    if any(
        not isinstance(store, str) or not store.strip()
        for store in metadata.target_stores
    ):
        return [
            ValidationIssue(
                "target_store_invalid",
                "배포 대상 스토어 이름은 비어 있을 수 없습니다.",
            )
        ]
    return []


def _rights_issues(metadata: ReleaseMetadata) -> list[ValidationIssue]:
    issues: list[ValidationIssue] = []
    if metadata.suno_plan.casefold() not in PAID_SUNO_PLANS:
        issues.append(
            ValidationIssue(
                "suno_commercial_rights",
                "Suno Pro 또는 Premier 구독 기록이 필요합니다.",
            )
        )
    if not metadata.rights_confirmed:
        issues.append(
            ValidationIssue(
                "rights_not_confirmed",
                "배포 권리와 원본 자료를 직접 확인해야 합니다.",
            )
        )
    if not metadata.rights_evidence_path.strip():
        issues.append(
            ValidationIssue(
                "rights_evidence_missing",
                "Suno 구독·권리 확인 증빙의 경로 또는 기록을 남겨야 합니다.",
            )
        )
    elif not Path(metadata.rights_evidence_path).is_file():
        issues.append(
            ValidationIssue(
                "rights_evidence_invalid",
                "Suno 구독·권리 확인 증빙 파일을 찾을 수 없습니다.",
            )
        )
    if metadata.is_cover and not metadata.cover_license_confirmed:
        issues.append(
            ValidationIssue("cover_license_missing", "커버곡 라이선스 확인이 필요합니다.")
        )
    if metadata.uses_sample:
        issues.append(
            ValidationIssue(
                "sample_rights_review",
                "샘플 사용 곡은 별도 권리 검토가 필요합니다.",
            )
        )
    return issues


def _audio_issues(path: Path) -> list[ValidationIssue]:
    if not path.is_file():
        return [ValidationIssue("audio_missing", "음원 파일을 찾을 수 없습니다.")]
    if path.suffix.casefold() not in SUPPORTED_AUDIO_FORMATS:
        return [
            ValidationIssue("audio_format", "DistroKid 허용 음원 형식이 아닙니다.")
        ]
    if path.stat().st_size == 0:
        return [ValidationIssue("audio_empty", "음원 파일이 비어 있습니다.")]
    try:
        audio = MutagenFile(path)
    except (OSError, MutagenError):
        return [ValidationIssue("audio_invalid", "음원 파일 형식을 읽을 수 없습니다.")]
    if audio is None or not _has_audio_duration(audio):
        return [ValidationIssue("audio_invalid", "음원 재생 시간 정보를 읽을 수 없습니다.")]
    issues = _audio_limit_issues(path, audio)
    if not _audio_type_matches(path, audio):
        issues.append(
            ValidationIssue("audio_type_mismatch", "음원 확장자와 실제 형식이 다릅니다.")
        )
    if not _audio_content_is_complete(path):
        issues.append(
            ValidationIssue("audio_incomplete", "음원 데이터가 손상되었거나 잘렸습니다.")
        )
    return issues


def _has_audio_duration(audio: Any) -> bool:
    info = getattr(audio, "info", None)
    duration = getattr(info, "length", 0)
    return isinstance(duration, (int, float)) and duration > 0


def _audio_limit_issues(path: Path, audio: Any) -> list[ValidationIssue]:
    issues: list[ValidationIssue] = []
    if path.stat().st_size > MAX_AUDIO_BYTES:
        issues.append(
            ValidationIssue("audio_too_large", "음원 파일은 1GB 이하여야 합니다.")
        )
    duration = float(getattr(getattr(audio, "info", None), "length", 0))
    if duration >= MAX_AUDIO_DURATION_SECONDS:
        issues.append(
            ValidationIssue("audio_too_long", "트랙 길이는 5시간 미만이어야 합니다.")
        )
    return issues


def _audio_type_matches(path: Path, audio: Any) -> bool:
    expected_type = EXPECTED_AUDIO_TYPES.get(path.suffix.casefold())
    return type(audio).__name__ == expected_type


def _audio_content_is_complete(path: Path) -> bool:
    suffix = path.suffix.casefold()
    if suffix == ".wav":
        return _wav_content_is_complete(path)
    if suffix == ".aiff":
        return _aiff_content_is_complete(path)
    if suffix == ".flac":
        return _flac_content_is_complete(path)
    if suffix == ".m4a":
        return _mp4_content_is_complete(path)
    if suffix == ".mp3":
        return _mp3_content_is_complete(path)
    if suffix == ".wma":
        return _asf_content_is_complete(path)
    return False


def _wav_content_is_complete(path: Path) -> bool:
    try:
        with wave.open(str(path), "rb") as wav_file:
            frame_size = wav_file.getnchannels() * wav_file.getsampwidth()
            expected_size = wav_file.getnframes() * frame_size
            actual_size = len(wav_file.readframes(wav_file.getnframes()))
            return expected_size > 0 and actual_size == expected_size
    except (EOFError, OSError, wave.Error):
        return False


def _aiff_content_is_complete(path: Path) -> bool:
    try:
        file_size = path.stat().st_size
        with path.open("rb") as audio:
            header = audio.read(12)
            if len(header) != 12 or header[:4] != b"FORM":
                return False
            if header[8:12] not in {b"AIFF", b"AIFC"}:
                return False
            form_end = 8 + struct.unpack(">I", header[4:8])[0]
            if form_end > file_size or form_end < 12:
                return False
            return _aiff_chunks_are_complete(audio, form_end)
    except (OSError, struct.error):
        return False


def _aiff_chunks_are_complete(
    audio: Any,
    form_end: int,
) -> bool:
    position = 12
    expected_audio_bytes: int | None = None
    actual_audio_bytes = 0
    while position < form_end:
        chunk_header = audio.read(8)
        if len(chunk_header) != 8:
            return False
        chunk_id = chunk_header[:4]
        chunk_size = struct.unpack(">I", chunk_header[4:])[0]
        if not _aiff_chunk_fits(position, chunk_size, form_end):
            return False
        chunk_info = _aiff_chunk_info(audio, chunk_id, chunk_size)
        if chunk_info is None:
            return False
        chunk_type, chunk_audio_bytes = chunk_info
        if chunk_type == "comm":
            expected_audio_bytes = chunk_audio_bytes
        if chunk_type == "ssnd":
            actual_audio_bytes = chunk_audio_bytes
        if chunk_size % 2 and len(audio.read(1)) != 1:
            return False
        position += 8 + chunk_size + (chunk_size % 2)
    return _aiff_audio_meets_requirements(
        position,
        form_end,
        expected_audio_bytes,
        actual_audio_bytes,
    )


def _aiff_chunk_fits(position: int, chunk_size: int, form_end: int) -> bool:
    return position + 8 + chunk_size <= form_end


def _aiff_chunk_info(
    audio: Any,
    chunk_id: bytes,
    chunk_size: int,
) -> tuple[str, int] | None:
    if chunk_id == b"COMM":
        return _aiff_comm_info(audio, chunk_size)
    if chunk_id == b"SSND":
        return _aiff_ssnd_info(audio, chunk_size)
    audio.seek(chunk_size, 1)
    return "other", 0


def _aiff_comm_info(audio: Any, chunk_size: int) -> tuple[str, int] | None:
    if chunk_size < 8:
        return None
    payload = audio.read(8)
    if len(payload) < 8:
        return None
    channels, frames, sample_bits = struct.unpack(">HIH", payload)
    if not _aiff_stream_is_valid(channels, frames, sample_bits):
        return None
    expected = frames * channels * ((sample_bits + 7) // 8)
    audio.seek(chunk_size - len(payload), 1)
    return "comm", expected


def _aiff_stream_is_valid(
    channels: int,
    frames: int,
    sample_bits: int,
) -> bool:
    return bool(channels and frames and sample_bits)


def _aiff_ssnd_info(audio: Any, chunk_size: int) -> tuple[str, int] | None:
    if chunk_size < 8:
        return None
    payload = audio.read(8)
    if len(payload) < 8:
        return None
    offset = struct.unpack(">I", payload[:4])[0]
    if chunk_size < 8 + offset:
        return None
    audio.seek(chunk_size - len(payload), 1)
    return "ssnd", chunk_size - 8 - offset


def _aiff_audio_meets_requirements(
    position: int,
    form_end: int,
    expected_audio_bytes: int | None,
    actual_audio_bytes: int,
) -> bool:
    return (
        position == form_end
        and expected_audio_bytes is not None
        and actual_audio_bytes > 0
        and actual_audio_bytes >= expected_audio_bytes
    )


def _flac_content_is_complete(path: Path) -> bool:
    try:
        with path.open("rb") as audio:
            if audio.read(4) != b"fLaC":
                return False
            saw_streaminfo = False
            while True:
                block_header = audio.read(4)
                if len(block_header) != 4:
                    return False
                block_size = int.from_bytes(block_header[1:], "big")
                block_data = audio.read(block_size)
                if len(block_data) != block_size:
                    return False
                if block_header[0] & 0x7F == 0:
                    saw_streaminfo = block_size >= 34
                if block_header[0] & 0x80:
                    break
            return saw_streaminfo and _has_flac_frame(audio.read())
    except OSError:
        return False


def _has_flac_frame(data: bytes) -> bool:
    return any(
        data[index] == 0xFF and (data[index + 1] & 0xFC) == 0xF8
        for index in range(max(0, len(data) - 1))
    )


def _mp4_content_is_complete(path: Path) -> bool:
    file_size = path.stat().st_size
    offset = 0
    saw_file_type = False
    with path.open("rb") as audio:
        while offset < file_size:
            header = audio.read(8)
            if len(header) != 8:
                return False
            box_size, box_type = struct.unpack(">I4s", header)
            header_size = 8
            if box_size == 1:
                extended_size = audio.read(8)
                if len(extended_size) != 8:
                    return False
                box_size = struct.unpack(">Q", extended_size)[0]
                header_size = 16
            elif box_size == 0:
                box_size = file_size - offset
            if box_size < header_size or offset + box_size > file_size:
                return False
            saw_file_type = saw_file_type or box_type == b"ftyp"
            audio.seek(box_size - header_size, 1)
            offset += box_size
    return saw_file_type


def _mp3_content_is_complete(path: Path) -> bool:
    data = path.read_bytes()
    offset = _mp3_audio_offset(data)
    frames = 0
    while offset + 4 <= len(data):
        if _is_mp3_trailer(data[offset:]):
            break
        frame_length = _mp3_frame_length(data[offset:offset + 4])
        if frame_length is None or offset + frame_length > len(data):
            return False
        offset += frame_length
        frames += 1
    return frames > 0


def _mp3_audio_offset(data: bytes) -> int:
    if not data.startswith(b"ID3") or len(data) < 10:
        return 0
    tag_size = sum(
        (byte & 0x7F) << (7 * (3 - index))
        for index, byte in enumerate(data[6:10])
    )
    return 10 + tag_size


def _is_mp3_trailer(data: bytes) -> bool:
    return data.startswith(b"TAG") or data.startswith(b"APETAGEX")


def _mp3_frame_length(header_bytes: bytes) -> int | None:
    if len(header_bytes) != 4:
        return None
    header = int.from_bytes(header_bytes, "big")
    version = (header >> 19) & 0x03
    layer = (header >> 17) & 0x03
    bitrate_index = (header >> 12) & 0x0F
    sample_index = (header >> 10) & 0x03
    if header >> 21 != 0x7FF or version == 1 or layer != 1:
        return None
    if bitrate_index in {0, 15} or sample_index == 3:
        return None
    if version == 3:
        bitrates = (
            0, 32, 40, 48, 56, 64, 80, 96,
            112, 128, 160, 192, 224, 256, 320,
        )
        sample_rates = (44100, 48000, 32000)
        coefficient = 144000
    else:
        bitrates = (
            0, 8, 16, 24, 32, 40, 48, 56,
            64, 80, 96, 112, 128, 144, 160,
        )
        sample_rates = (
            (22050, 24000, 16000)
            if version == 2
            else (11025, 12000, 8000)
        )
        coefficient = 72000
    padding = (header >> 9) & 0x01
    return (
        coefficient * bitrates[bitrate_index] * 1000
        // sample_rates[sample_index]
        + padding
    )


def _asf_content_is_complete(path: Path) -> bool:
    header_guid = bytes.fromhex(
        "30 26 b2 75 8e 66 cf 11 a6 d9 00 aa 00 62 ce 6c"
    )
    data_guid = bytes.fromhex(
        "36 26 b2 75 8e 66 cf 11 a6 d9 00 aa 00 62 ce 6c"
    )
    file_size = path.stat().st_size
    offset = 0
    saw_header = False
    saw_data = False
    with path.open("rb") as audio:
        while offset < file_size:
            object_header = audio.read(24)
            if len(object_header) != 24:
                return False
            object_size = struct.unpack("<Q", object_header[16:])[0]
            if object_size < 24 or offset + object_size > file_size:
                return False
            saw_header = saw_header or object_header[:16] == header_guid
            saw_data = saw_data or object_header[:16] == data_guid
            audio.seek(object_size - 24, 1)
            offset += object_size
    return saw_header and saw_data


def _artwork_issues(path: Path) -> list[ValidationIssue]:
    if not path.is_file():
        return [ValidationIssue("artwork_missing", "커버 이미지 파일을 찾을 수 없습니다.")]
    if path.suffix.casefold() not in ARTWORK_FORMATS:
        return [
            ValidationIssue("artwork_format", "커버 이미지는 JPG 형식이어야 합니다.")
        ]
    try:
        with Image.open(path) as image:
            image_format = image.format
            image.verify()
        if image_format != "JPEG":
            return [
                ValidationIssue("artwork_format", "커버 이미지는 JPG 형식이어야 합니다.")
            ]
        with Image.open(path) as image:
            image.load()
            return _image_issues(image)
    except (OSError, UnidentifiedImageError):
        return [ValidationIssue("artwork_invalid", "커버 이미지를 읽을 수 없습니다.")]


def _image_issues(image: Image.Image) -> list[ValidationIssue]:
    width, height = image.size
    issues: list[ValidationIssue] = []
    if width != height:
        issues.append(
            ValidationIssue("artwork_not_square", "커버 이미지는 정사각형이어야 합니다.")
        )
    if min(width, height) < ARTWORK_MIN_SIZE:
        issues.append(
            ValidationIssue(
                "artwork_too_small",
                "커버 이미지는 최소 1000x1000이어야 합니다.",
            )
        )
    if image.mode != "RGB":
        issues.append(
            ValidationIssue("artwork_not_rgb", "커버 이미지 색상 모드는 RGB여야 합니다.")
        )
    return issues


def _package_name(metadata: ReleaseMetadata) -> str:
    release_date = metadata.release_date or date.today().isoformat()
    return f"{release_date}_{_safe_slug(metadata.track_title)}"


def _safe_slug(value: str) -> str:
    cleaned = re.sub(r"[^\w\s-]", "", value, flags=re.UNICODE).strip()
    cleaned = re.sub(r"[\s_-]+", "-", cleaned)
    return cleaned or "untitled"


def _create_package_folders(package_path: Path) -> dict[str, Path]:
    names = ("audio", "artwork", "lyrics", "metadata", "rights", "distrokid")
    folders = {name: package_path / name for name in names}
    for folder in folders.values():
        folder.mkdir(parents=True, exist_ok=False)
    return folders


def _copy_asset(source: Path, target: Path) -> None:
    shutil.copy2(source, target)


def _write_package_files(
    metadata: ReleaseMetadata,
    audio_path: Path,
    artwork_path: Path,
    lyrics_path: Path,
    package_path: Path,
) -> None:
    folders = _create_package_folders(package_path)
    audio_target = folders["audio"] / f"master{audio_path.suffix.lower()}"
    artwork_target = folders["artwork"] / "cover.jpg"
    _copy_asset(audio_path, audio_target)
    _copy_asset(artwork_path, artwork_target)
    _copy_asset(lyrics_path, folders["lyrics"] / "lyrics.txt")
    _write_package_metadata(
        metadata,
        package_path,
        audio_target,
        artwork_target,
    )
    _write_rights_record(metadata, folders["rights"] / "suno-record.json")
    _write_checklist(
        metadata,
        folders["distrokid"] / "upload-checklist.md",
        audio_target.suffix,
    )


def _reject_duplicate_artwork(artwork_path: Path, output_root: Path) -> None:
    artwork_hash = _file_sha256(artwork_path)
    for existing_artwork in output_root.rglob("cover.jpg"):
        if _file_sha256(existing_artwork) == artwork_hash:
            raise ValueError("duplicate_artwork: 이미 사용한 커버 이미지입니다.")


def _file_sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as asset:
        for chunk in iter(lambda: asset.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def _write_package_metadata(
    metadata: ReleaseMetadata,
    package_path: Path,
    audio_path: Path,
    artwork_path: Path,
) -> None:
    payload = asdict(metadata)
    payload["distrokid_ai_credits"] = get_ai_credit_labels(metadata)
    payload["files"] = {
        "audio": audio_path.relative_to(package_path).as_posix(),
        "artwork": artwork_path.relative_to(package_path).as_posix(),
        "lyrics": "lyrics/lyrics.txt",
    }
    _write_json(package_path / "metadata" / "release.json", payload)


def _write_rights_record(metadata: ReleaseMetadata, path: Path) -> None:
    payload = {
        "platform": "Suno",
        "plan": metadata.suno_plan,
        "generated_at": metadata.generated_at,
        "downloaded_at": metadata.downloaded_at,
        "commercial_use_reviewed": metadata.rights_confirmed,
        "evidence_reference": metadata.rights_evidence_path,
        "audio_playback_reviewed": metadata.audio_reviewed,
    }
    _write_json(path, payload)


def _write_json(path: Path, payload: dict[str, Any]) -> None:
    path.write_text(
        json.dumps(payload, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )


def _write_checklist(
    metadata: ReleaseMetadata,
    path: Path,
    audio_suffix: str,
) -> None:
    ai_credits = ", ".join(get_ai_credit_labels(metadata))
    stores = ", ".join(metadata.target_stores)
    cover_license = (
        "Confirmed" if metadata.cover_license_confirmed else "Not applicable"
    )
    persona_confirmed = "Yes" if metadata.artist_persona_confirmed else "No"
    audio_reviewed = "Yes" if metadata.audio_reviewed else "No"
    checklist = f"""# DistroKid Upload Checklist

## Release

- [ ] Artist: {metadata.artist_name}
- [ ] Track title: {metadata.track_title}
- [ ] Primary genre: {metadata.primary_genre}
- [ ] Secondary genre: {metadata.secondary_genre or '(선택)'}
- [ ] Language: {metadata.language}
- [ ] Release date: {metadata.release_date or '(DistroKid에서 선택)'}
- [ ] Stores: {stores}

## Credits and declarations

- [ ] Songwriter real name: {metadata.songwriter_name}
- [ ] Producer: {metadata.producer_name or '(선택)'}
- [ ] Explicit lyrics: {'Yes' if metadata.explicit else 'No'}
- [ ] AI credits: {ai_credits or 'None'}
- [ ] Artist persona: {metadata.artist_persona}
- [ ] Artist persona selection confirmed: {persona_confirmed}
- [ ] Audio playback reviewed from start to finish: {audio_reviewed}
- [ ] Cover song license: {cover_license}
- [ ] Rights evidence reference: {metadata.rights_evidence_path}
- [ ] Final rights and originality review completed

## Files

- Audio: `audio/master{audio_suffix}`
- Artwork: `artwork/cover.jpg`
- Lyrics: `lyrics/lyrics.txt`

Final submission remains manual: review every field in
DistroKid before submitting.
"""
    path.write_text(checklist, encoding="utf-8")


def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Suno 발매 패키지 생성")
    parser.add_argument("--metadata", required=True, help="release.json 경로")
    parser.add_argument("--audio", required=True, help="음원 파일 경로")
    parser.add_argument("--artwork", required=True, help="커버 JPG 경로")
    parser.add_argument("--lyrics", required=True, help="가사 TXT 경로")
    parser.add_argument("--output", required=True, help="발매 패키지 저장 폴더")
    return parser


if __name__ == "__main__":  # pragma: no cover
    raise SystemExit(main())
