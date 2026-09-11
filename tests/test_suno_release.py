import io
import json
import struct
import wave
from dataclasses import asdict
from pathlib import Path

import pytest
from PIL import Image

import budget.suno_release as suno_release
from budget.suno_release import (
    ReleaseMetadata,
    build_release_package,
    get_ai_credit_labels,
    load_metadata,
    validate_release,
)


def make_artwork(path: Path, size: tuple[int, int] = (3000, 3000)) -> None:
    image = Image.new("RGB", size, color=(30, 80, 120))
    image.save(path, format="JPEG")


def make_audio(path: Path, frames: int = 44100) -> None:
    with wave.open(str(path), "wb") as audio:
        audio.setnchannels(1)
        audio.setsampwidth(2)
        audio.setframerate(44100)
        audio.writeframes(b"\0\0" * frames)


def make_aiff(path: Path, frames: int = 2, actual_frames: int = 2) -> None:
    comm_payload = struct.pack(">HIH", 1, frames, 16) + b"\0" * 10
    comm = struct.pack(">4sI", b"COMM", len(comm_payload)) + comm_payload
    sound_payload = struct.pack(">II", 0, 0) + b"\0\0" * actual_frames
    sound = struct.pack(">4sI", b"SSND", len(sound_payload)) + sound_payload
    body = b"AIFF" + comm + sound
    path.write_bytes(b"FORM" + struct.pack(">I", len(body)) + body)


def aiff_stream(payload: bytes) -> io.BytesIO:
    stream = io.BytesIO(b"\0" * 12 + payload)
    stream.seek(12)
    return stream


def paid_metadata(**overrides: object) -> ReleaseMetadata:
    values: dict[str, object] = {
        "artist_name": "Neon Harbor",
        "track_title": "Midnight Signal",
        "primary_genre": "Electronic",
        "songwriter_name": "Kim Jae-seop",
        "suno_plan": "Pro",
        "generated_at": "2026-09-08T10:00:00+09:00",
        "downloaded_at": "2026-09-08T10:05:00+09:00",
        "lyrics_by_ai": True,
        "music_by_ai": True,
        "audio_scope": "all",
        "rights_confirmed": True,
        "rights_evidence_path": str(Path(__file__)),
        "release_date": "2026-10-10",
        "artwork_reviewed": True,
        "audio_reviewed": True,
        "artist_persona_confirmed": True,
    }
    values.update(overrides)
    return ReleaseMetadata(**values)


def test_validate_release_accepts_paid_suno_release(tmp_path: Path) -> None:
    audio_path = tmp_path / "track.wav"
    artwork_path = tmp_path / "cover.jpg"
    make_audio(audio_path)
    make_artwork(artwork_path)

    issues = validate_release(paid_metadata(), audio_path, artwork_path)

    assert issues == []


def test_validate_release_blocks_free_plan_and_unconfirmed_rights(
    tmp_path: Path,
) -> None:
    audio_path = tmp_path / "track.wav"
    artwork_path = tmp_path / "cover.jpg"
    make_audio(audio_path)
    make_artwork(artwork_path)

    issues = validate_release(
        paid_metadata(suno_plan="Basic", rights_confirmed=False),
        audio_path,
        artwork_path,
    )

    codes = {issue.code for issue in issues}
    assert "suno_commercial_rights" in codes
    assert "rights_not_confirmed" in codes


def test_validate_release_checks_artwork_dimensions_and_color_mode(
    tmp_path: Path,
) -> None:
    audio_path = tmp_path / "track.wav"
    artwork_path = tmp_path / "cover.jpg"
    make_audio(audio_path)
    image = Image.new("CMYK", (800, 900), color=(0, 0, 0, 0))
    image.save(artwork_path, format="JPEG")

    issues = validate_release(paid_metadata(), audio_path, artwork_path)

    codes = {issue.code for issue in issues}
    assert "artwork_not_square" in codes
    assert "artwork_too_small" in codes
    assert "artwork_not_rgb" in codes


def test_validate_release_rejects_png_disguised_as_jpeg(
    tmp_path: Path,
) -> None:
    audio_path = tmp_path / "track.wav"
    artwork_path = tmp_path / "cover.jpg"
    make_audio(audio_path)
    Image.new("RGB", (3000, 3000), color=(30, 80, 120)).save(
        artwork_path,
        format="PNG",
    )

    codes = {
        issue.code
        for issue in validate_release(
            paid_metadata(), audio_path, artwork_path
        )
    }

    assert "artwork_format" in codes


def test_validate_release_rejects_truncated_jpeg(tmp_path: Path) -> None:
    audio_path = tmp_path / "track.wav"
    artwork_path = tmp_path / "cover.jpg"
    make_audio(audio_path)
    make_artwork(artwork_path)
    artwork_path.write_bytes(artwork_path.read_bytes()[:-2])

    codes = {
        issue.code
        for issue in validate_release(
            paid_metadata(), audio_path, artwork_path
        )
    }

    assert "artwork_invalid" in codes


def test_get_ai_credit_labels_for_full_suno_track() -> None:
    metadata = paid_metadata()

    assert get_ai_credit_labels(metadata) == [
        "The lyrics",
        "The music",
        "All of the audio",
    ]


def test_get_ai_credit_labels_for_part_audio() -> None:
    metadata = paid_metadata(audio_scope="part")

    assert get_ai_credit_labels(metadata) == [
        "The lyrics",
        "The music",
        "Part of the audio",
    ]


def test_get_ai_labels_partial_lyrics_audio() -> None:
    metadata = paid_metadata(
        audio_scope="part",
        lyrics_by_ai=True,
        music_by_ai=False,
    )

    assert get_ai_credit_labels(metadata) == [
        "The lyrics",
        "Part of the audio",
    ]


def test_get_ai_labels_music_only_combinations() -> None:
    partial = paid_metadata(
        audio_scope="part",
        lyrics_by_ai=False,
        music_by_ai=True,
    )
    full = paid_metadata(
        audio_scope="all",
        lyrics_by_ai=False,
        music_by_ai=True,
    )

    assert get_ai_credit_labels(partial) == ["The music", "Part of the audio"]
    assert get_ai_credit_labels(full) == ["The music", "All of the audio"]


def test_get_ai_credit_labels_for_partial_ai_track() -> None:
    metadata = paid_metadata(
        audio_scope="none",
        lyrics_by_ai=True,
        music_by_ai=True,
    )

    assert get_ai_credit_labels(metadata) == ["The lyrics", "The music"]


def test_get_ai_credit_labels_for_human_track() -> None:
    metadata = paid_metadata(
        audio_scope="none",
        lyrics_by_ai=False,
        music_by_ai=False,
    )

    assert get_ai_credit_labels(metadata) == []


def test_load_metadata_preserves_default_target_stores(tmp_path: Path) -> None:
    metadata_path = tmp_path / "release.json"
    values = {
        "artist_name": "Neon Harbor",
        "track_title": "Midnight Signal",
        "primary_genre": "Electronic",
        "songwriter_name": "Kim Jae-seop",
        "suno_plan": "Pro",
        "generated_at": "2026-09-08T10:00:00+09:00",
        "downloaded_at": "2026-09-08T10:05:00+09:00",
        "lyrics_by_ai": True,
        "music_by_ai": True,
    }
    metadata_path.write_text(json.dumps(values), encoding="utf-8")

    metadata = load_metadata(metadata_path)

    assert metadata.target_stores == (
        "Spotify",
        "Apple Music",
        "YouTube Music",
        "TikTok",
    )


def test_load_metadata_reads_explicit_target_stores(tmp_path: Path) -> None:
    metadata_path = tmp_path / "release.json"
    values = {
        "artist_name": "Neon Harbor",
        "track_title": "Midnight Signal",
        "primary_genre": "Electronic",
        "songwriter_name": "Kim Jae-seop",
        "suno_plan": "Pro",
        "generated_at": "2026-09-08T10:00:00+09:00",
        "downloaded_at": "2026-09-08T10:05:00+09:00",
        "lyrics_by_ai": True,
        "music_by_ai": True,
        "target_stores": ["Spotify"],
    }
    metadata_path.write_text(json.dumps(values), encoding="utf-8")

    metadata = load_metadata(metadata_path)

    assert metadata.target_stores == ("Spotify",)


def test_load_metadata_rejects_invalid_schema(tmp_path: Path) -> None:
    metadata_path = tmp_path / "release.json"
    values = asdict(paid_metadata())
    values["artist_name"] = 123
    metadata_path.write_text(json.dumps(values), encoding="utf-8")

    with pytest.raises(ValueError, match="artist_name"):
        load_metadata(metadata_path)

    values["artist_name"] = "Neon Harbor"
    values["target_stores"] = "Spotify"
    metadata_path.write_text(json.dumps(values), encoding="utf-8")

    with pytest.raises(ValueError, match="target_stores"):
        load_metadata(metadata_path)

    values["target_stores"] = []
    metadata_path.write_text(json.dumps(values), encoding="utf-8")
    with pytest.raises(ValueError, match="비어 있지 않은"):
        load_metadata(metadata_path)

    values["target_stores"] = [""]
    metadata_path.write_text(json.dumps(values), encoding="utf-8")
    with pytest.raises(ValueError, match="문자열 목록"):
        load_metadata(metadata_path)

    values["target_stores"] = ["Spotify"]
    values["secondary_genre"] = 42
    metadata_path.write_text(json.dumps(values), encoding="utf-8")
    with pytest.raises(ValueError, match="secondary_genre"):
        load_metadata(metadata_path)

    values["secondary_genre"] = ""
    values["explicit"] = 1
    metadata_path.write_text(json.dumps(values), encoding="utf-8")
    with pytest.raises(ValueError, match="explicit"):
        load_metadata(metadata_path)

    values["explicit"] = False
    values.pop("lyrics_by_ai")
    metadata_path.write_text(json.dumps(values), encoding="utf-8")
    with pytest.raises(ValueError, match="lyrics_by_ai"):
        load_metadata(metadata_path)

    values["lyrics_by_ai"] = True
    values["unexpected"] = True
    metadata_path.write_text(json.dumps(values), encoding="utf-8")
    with pytest.raises(ValueError, match="메타데이터 필드"):
        load_metadata(metadata_path)


def test_load_metadata_rejects_non_object_json(tmp_path: Path) -> None:
    metadata_path = tmp_path / "release.json"
    metadata_path.write_text("[]", encoding="utf-8")

    with pytest.raises(ValueError, match="객체"):
        load_metadata(metadata_path)


def test_validate_release_reports_invalid_metadata_and_rights(
    tmp_path: Path,
) -> None:
    audio_path = tmp_path / "track.wav"
    artwork_path = tmp_path / "cover.jpg"
    make_audio(audio_path)
    make_artwork(artwork_path)
    metadata = paid_metadata(
        generated_at="not-a-date",
        downloaded_at="2026-09-08T09:00:00+09:00",
        release_date="2026/10/10",
        audio_scope="bad",
        artist_persona="robot",
        is_cover=True,
        uses_sample=True,
        cover_license_confirmed=False,
        rights_confirmed=False,
        artwork_reviewed=False,
    )

    codes = {
        issue.code
        for issue in validate_release(metadata, audio_path, artwork_path)
    }

    assert {
        "invalid_generated_at",
        "invalid_release_date",
        "invalid_audio_scope",
        "invalid_artist_persona",
        "rights_not_confirmed",
        "cover_license_missing",
        "sample_rights_review",
        "artwork_not_reviewed",
    } <= codes


def test_validate_release_reports_download_before_generation(
    tmp_path: Path,
) -> None:
    audio_path = tmp_path / "track.wav"
    artwork_path = tmp_path / "cover.jpg"
    make_audio(audio_path)
    make_artwork(artwork_path)
    metadata = paid_metadata(
        generated_at="2026-09-08T10:00:00+09:00",
        downloaded_at="2026-09-08T09:00:00+09:00",
    )

    codes = {
        issue.code
        for issue in validate_release(metadata, audio_path, artwork_path)
    }

    assert "download_before_generation" in codes


def test_validate_release_allows_omitted_release_date(tmp_path: Path) -> None:
    audio_path = tmp_path / "track.wav"
    artwork_path = tmp_path / "cover.jpg"
    make_audio(audio_path)
    make_artwork(artwork_path)

    issues = validate_release(
        paid_metadata(release_date=""), audio_path, artwork_path
    )

    assert issues == []


def test_validate_release_rejects_impossible_release_date(
    tmp_path: Path,
) -> None:
    audio_path = tmp_path / "track.wav"
    artwork_path = tmp_path / "cover.jpg"
    make_audio(audio_path)
    make_artwork(artwork_path)

    issues = validate_release(
        paid_metadata(release_date="2026-13-40"),
        audio_path,
        artwork_path,
    )

    assert "invalid_release_date" in {issue.code for issue in issues}


def test_validate_release_reports_mixed_timestamp_timezone(
    tmp_path: Path,
) -> None:
    audio_path = tmp_path / "track.wav"
    artwork_path = tmp_path / "cover.jpg"
    make_audio(audio_path)
    make_artwork(artwork_path)
    metadata = paid_metadata(downloaded_at="2026-09-08T09:00:00")

    codes = {
        issue.code
        for issue in validate_release(metadata, audio_path, artwork_path)
    }

    assert "timestamp_timezone_required" in codes


def test_validate_release_reports_missing_download_timestamp(
    tmp_path: Path,
) -> None:
    audio_path = tmp_path / "track.wav"
    artwork_path = tmp_path / "cover.jpg"
    make_audio(audio_path)
    make_artwork(artwork_path)
    metadata = paid_metadata(downloaded_at="")

    codes = {
        issue.code
        for issue in validate_release(metadata, audio_path, artwork_path)
    }

    assert "missing_metadata" in codes
    assert "invalid_downloaded_at" in codes


def test_validate_release_reports_empty_audio(tmp_path: Path) -> None:
    artwork_path = tmp_path / "cover.jpg"
    make_artwork(artwork_path)
    empty_audio = tmp_path / "empty.wav"
    empty_audio.write_bytes(b"")
    empty_codes = {
        issue.code
        for issue in validate_release(
            paid_metadata(), empty_audio, artwork_path
        )
    }

    assert "audio_empty" in empty_codes


def test_validate_release_reports_invalid_audio(tmp_path: Path) -> None:
    artwork_path = tmp_path / "cover.jpg"
    make_artwork(artwork_path)
    invalid_audio = tmp_path / "invalid.wav"
    invalid_audio.write_bytes(b"not audio")
    invalid_codes = {
        issue.code
        for issue in validate_release(
            paid_metadata(), invalid_audio, artwork_path
        )
    }

    assert "audio_invalid" in invalid_codes


def test_validate_release_reports_zero_duration_audio(tmp_path: Path) -> None:
    artwork_path = tmp_path / "cover.jpg"
    make_artwork(artwork_path)
    audio_path = tmp_path / "track.wav"
    make_audio(audio_path, frames=0)
    codes = {
        issue.code
        for issue in validate_release(
            paid_metadata(), audio_path, artwork_path
        )
    }

    assert "audio_invalid" in codes
    assert not suno_release._audio_content_is_complete(audio_path)


def test_validate_release_blocks_oversized_audio(
    tmp_path: Path,
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    artwork_path = tmp_path / "cover.jpg"
    audio_path = tmp_path / "track.wav"
    make_artwork(artwork_path)
    make_audio(audio_path)
    monkeypatch.setattr(suno_release, "MAX_AUDIO_BYTES", 100)

    codes = {
        issue.code
        for issue in validate_release(
            paid_metadata(), audio_path, artwork_path
        )
    }

    assert "audio_too_large" in codes


def test_validate_release_blocks_five_hour_audio(
    tmp_path: Path,
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    artwork_path = tmp_path / "cover.jpg"
    audio_path = tmp_path / "track.wav"
    make_artwork(artwork_path)
    make_audio(audio_path)
    monkeypatch.setattr(suno_release, "MAX_AUDIO_DURATION_SECONDS", 0.5)

    codes = {
        issue.code
        for issue in validate_release(
            paid_metadata(), audio_path, artwork_path
        )
    }

    assert "audio_too_long" in codes


@pytest.mark.parametrize(
    ("suffix", "content"),
    [
        (".flac", b"fLaC\x80\x00\x00\x22" + b"0" * 10),
        (".m4a", struct.pack(">I4s", 32, b"mdat") + b"data"),
        (".mp3", b"\xff\xfb\x90\x64" + b"0" * 20),
        (".wma", b"\x30\x26\xb2\x75" + b"0" * 20),
    ],
)
def test_audio_content_check_rejects_truncated_non_wav_formats(
    tmp_path: Path,
    suffix: str,
    content: bytes,
) -> None:
    audio_path = tmp_path / f"track{suffix}"
    audio_path.write_bytes(content)

    assert not suno_release._audio_content_is_complete(audio_path)


def test_audio_content_check_accepts_complete_aiff(tmp_path: Path) -> None:
    audio_path = tmp_path / "track.aiff"
    make_aiff(audio_path)

    assert suno_release._audio_content_is_complete(audio_path)


def test_audio_content_check_rejects_truncated_aiff(tmp_path: Path) -> None:
    audio_path = tmp_path / "track.aiff"
    make_aiff(audio_path, frames=4, actual_frames=2)

    assert not suno_release._audio_content_is_complete(audio_path)


def test_audio_content_check_rejects_unknown_suffix(tmp_path: Path) -> None:
    audio_path = tmp_path / "track.ogg"
    audio_path.write_bytes(b"audio")

    assert not suno_release._audio_content_is_complete(audio_path)


def test_aiff_content_check_rejects_invalid_headers(tmp_path: Path) -> None:
    short_header = tmp_path / "short.aiff"
    short_header.write_bytes(b"FORM\x00\x00\x00\x04NOPE")
    wrong_form = tmp_path / "wrong-form.aiff"
    wrong_form.write_bytes(b"NOPE\x00\x00\x00\x04AIFF")
    too_large = tmp_path / "too-large.aiff"
    too_large.write_bytes(b"FORM\x00\x00\x00\x64AIFF")
    too_small = tmp_path / "too-small.aiff"
    too_small.write_bytes(b"FORM\x00\x00\x00\x00AIFF")
    missing = tmp_path / "missing.aiff"

    assert not suno_release._aiff_content_is_complete(short_header)
    assert not suno_release._aiff_content_is_complete(wrong_form)
    assert not suno_release._aiff_content_is_complete(too_large)
    assert not suno_release._aiff_content_is_complete(too_small)
    assert not suno_release._aiff_content_is_complete(missing)


def test_aiff_chunk_parser_rejects_malformed_chunks() -> None:
    too_small_comm = io.BytesIO()
    short_chunk = aiff_stream(b"COMM")
    too_large = aiff_stream(b"JUNK" + struct.pack(">I", 10))
    short_comm = aiff_stream(b"COMM" + struct.pack(">I", 8) + b"1234")
    invalid_comm = aiff_stream(b"COMM" + struct.pack(">I", 8) + b"\0" * 8)
    short_ssnd = aiff_stream(b"SSND" + struct.pack(">I", 4) + b"\0" * 4)
    short_ssnd_payload = aiff_stream(
        b"SSND" + struct.pack(">I", 8) + b"\0" * 4
    )
    invalid_ssnd_offset = aiff_stream(
        b"SSND"
        + struct.pack(">I", 8)
        + struct.pack(">II", 1, 0)
    )
    odd_unknown = aiff_stream(b"JUNK" + struct.pack(">I", 3) + b"123X")

    assert suno_release._aiff_comm_info(too_small_comm, 4) is None
    assert not suno_release._aiff_chunks_are_complete(short_chunk, 20)
    assert not suno_release._aiff_chunks_are_complete(too_large, 20)
    assert not suno_release._aiff_chunks_are_complete(short_comm, 28)
    assert not suno_release._aiff_chunks_are_complete(invalid_comm, 28)
    assert not suno_release._aiff_chunks_are_complete(short_ssnd, 24)
    assert not suno_release._aiff_chunks_are_complete(short_ssnd_payload, 28)
    assert not suno_release._aiff_chunks_are_complete(invalid_ssnd_offset, 28)
    assert not suno_release._aiff_chunks_are_complete(odd_unknown, 24)


def test_aiff_chunk_parser_rejects_missing_odd_padding() -> None:
    missing_padding = aiff_stream(b"JUNK" + struct.pack(">I", 3) + b"123")

    assert not suno_release._aiff_chunks_are_complete(missing_padding, 24)


def test_aiff_audio_requirements_reject_empty_audio() -> None:
    assert not suno_release._aiff_audio_meets_requirements(12, 12, 1, 0)


def test_audio_content_check_accepts_complete_mp3(tmp_path: Path) -> None:
    audio_path = tmp_path / "track.mp3"
    header = bytes.fromhex("ff fb 90 64")
    frame_length = suno_release._mp3_frame_length(header)
    assert frame_length is not None
    frame = header + b"0" * (frame_length - len(header))
    audio_path.write_bytes(frame + b"TAG" + b"0" * 125)

    assert suno_release._audio_content_is_complete(audio_path)


def test_audio_content_check_accepts_mp3_with_id3_tag(tmp_path: Path) -> None:
    audio_path = tmp_path / "track.mp3"
    header = bytes.fromhex("ff fb 90 64")
    frame_length = suno_release._mp3_frame_length(header)
    assert frame_length is not None
    frame = header + b"0" * (frame_length - len(header))
    id3_header = b"ID3\x04\x00\x00\x00\x00\x00\x04"
    audio_path.write_bytes(id3_header + b"meta" + frame)

    assert suno_release._audio_content_is_complete(audio_path)


def test_mp3_frame_length_rejects_invalid_headers() -> None:
    assert suno_release._mp3_frame_length(b"bad") is None
    assert suno_release._mp3_frame_length(bytes.fromhex("ff eb 90 64")) is None
    assert suno_release._mp3_frame_length(bytes.fromhex("ff fb 00 64")) is None
    assert suno_release._mp3_frame_length(bytes.fromhex("ff fb 9c 64")) is None


def test_mp3_frame_length_supports_mpeg_versions() -> None:
    version_two = bytes.fromhex("ff f3 50 64")
    version_two_five = bytes.fromhex("ff e3 50 64")

    assert suno_release._mp3_frame_length(version_two) is not None
    assert suno_release._mp3_frame_length(version_two_five) is not None


def test_flac_content_check_accepts_metadata_and_frame(tmp_path: Path) -> None:
    audio_path = tmp_path / "track.flac"
    streaminfo = b"0" * 34
    metadata = b"fLaC" + b"\x80\x00\x00\x22" + streaminfo
    audio_path.write_bytes(metadata + b"\xff\xf8\x00")

    assert suno_release._audio_content_is_complete(audio_path)


def test_flac_content_check_reads_multiple_metadata_blocks(
    tmp_path: Path,
) -> None:
    audio_path = tmp_path / "track.flac"
    streaminfo = b"0" * 34
    metadata = (
        b"fLaC"
        + b"\x00\x00\x00\x22"
        + streaminfo
        + b"\x81\x00\x00\x00"
    )
    audio_path.write_bytes(metadata + b"\xff\xf8\x00")

    assert suno_release._audio_content_is_complete(audio_path)


def test_flac_content_check_rejects_bad_metadata(tmp_path: Path) -> None:
    audio_path = tmp_path / "track.flac"
    audio_path.write_bytes(b"no flac")

    assert not suno_release._audio_content_is_complete(audio_path)


def test_flac_content_check_rejects_short_blocks_and_missing_streaminfo(
    tmp_path: Path,
) -> None:
    short_header = tmp_path / "short-header.flac"
    short_header.write_bytes(b"fLaC\x80")
    short_data = tmp_path / "short-data.flac"
    short_data.write_bytes(b"fLaC\x80\x00\x00\x22" + b"0" * 10)
    missing_streaminfo = tmp_path / "missing-streaminfo.flac"
    missing_streaminfo.write_bytes(b"fLaC\x81\x00\x00\x00\xff\xf8")
    missing_path = tmp_path / "missing.flac"

    assert not suno_release._audio_content_is_complete(short_header)
    assert not suno_release._audio_content_is_complete(short_data)
    assert not suno_release._audio_content_is_complete(missing_streaminfo)
    assert not suno_release._flac_content_is_complete(missing_path)


def test_mp4_content_check_accepts_complete_boxes(tmp_path: Path) -> None:
    audio_path = tmp_path / "track.m4a"
    file_type = struct.pack(">I4s", 8, b"ftyp")
    media = struct.pack(">I4s", 8, b"mdat")
    audio_path.write_bytes(file_type + media)

    assert suno_release._audio_content_is_complete(audio_path)


def test_mp4_content_check_supports_extended_and_zero_size_boxes(
    tmp_path: Path,
) -> None:
    audio_path = tmp_path / "track.m4a"
    extended = struct.pack(">I4sQ", 1, b"free", 16)
    file_type = struct.pack(">I4s", 8, b"ftyp")
    audio_path.write_bytes(extended + file_type)

    assert suno_release._audio_content_is_complete(audio_path)


def test_mp4_content_check_rejects_invalid_box_sizes(tmp_path: Path) -> None:
    audio_path = tmp_path / "track.m4a"
    audio_path.write_bytes(struct.pack(">I4s", 4, b"ftyp"))

    assert not suno_release._audio_content_is_complete(audio_path)


def test_mp4_content_check_rejects_short_headers_and_extended_sizes(
    tmp_path: Path,
) -> None:
    short_header = tmp_path / "short-header.m4a"
    short_header.write_bytes(b"ftyp")
    short_extended = tmp_path / "short-extended.m4a"
    short_extended.write_bytes(struct.pack(">I4s", 1, b"free"))
    zero_size = tmp_path / "zero-size.m4a"
    zero_size.write_bytes(struct.pack(">I4s", 0, b"ftyp"))

    assert not suno_release._audio_content_is_complete(short_header)
    assert not suno_release._audio_content_is_complete(short_extended)
    assert suno_release._audio_content_is_complete(zero_size)


def test_asf_content_check_accepts_header_and_data_objects(
    tmp_path: Path,
) -> None:
    audio_path = tmp_path / "track.wma"
    header_guid = bytes.fromhex(
        "30 26 b2 75 8e 66 cf 11 a6 d9 00 aa 00 62 ce 6c"
    )
    data_guid = bytes.fromhex(
        "36 26 b2 75 8e 66 cf 11 a6 d9 00 aa 00 62 ce 6c"
    )
    header = header_guid + struct.pack("<Q", 24)
    data = data_guid + struct.pack("<Q", 24)
    audio_path.write_bytes(header + data)

    assert suno_release._audio_content_is_complete(audio_path)


def test_asf_content_check_rejects_short_and_incomplete_objects(
    tmp_path: Path,
) -> None:
    short_object = tmp_path / "short.wma"
    short_object.write_bytes(b"0" * 10)
    header_guid = bytes.fromhex(
        "30 26 b2 75 8e 66 cf 11 a6 d9 00 aa 00 62 ce 6c"
    )
    header_only = tmp_path / "header-only.wma"
    header_only.write_bytes(header_guid + struct.pack("<Q", 24))

    assert not suno_release._audio_content_is_complete(short_object)
    assert not suno_release._audio_content_is_complete(header_only)


def test_validate_release_rejects_mislabeled_audio(tmp_path: Path) -> None:
    artwork_path = tmp_path / "cover.jpg"
    make_artwork(artwork_path)
    audio_path = tmp_path / "track.mp3"
    make_audio(audio_path)

    codes = {
        issue.code
        for issue in validate_release(
            paid_metadata(), audio_path, artwork_path
        )
    }

    assert "audio_type_mismatch" in codes
    assert not suno_release._audio_content_is_complete(audio_path)


def test_validate_release_rejects_truncated_wav(tmp_path: Path) -> None:
    artwork_path = tmp_path / "cover.jpg"
    make_artwork(artwork_path)
    audio_path = tmp_path / "track.wav"
    make_audio(audio_path)
    audio_path.write_bytes(audio_path.read_bytes()[:-100])

    codes = {
        issue.code
        for issue in validate_release(
            paid_metadata(), audio_path, artwork_path
        )
    }

    assert "audio_incomplete" in codes
    assert not suno_release._audio_content_is_complete(
        tmp_path / "missing.wav"
    )


def test_validate_release_reports_unsupported_audio(tmp_path: Path) -> None:
    artwork_path = tmp_path / "cover.jpg"
    make_artwork(artwork_path)
    audio_path = tmp_path / "track.ogg"
    audio_path.write_bytes(b"audio")
    codes = {
        issue.code
        for issue in validate_release(
            paid_metadata(), audio_path, artwork_path
        )
    }

    assert "audio_format" in codes


def test_validate_release_reports_missing_audio(tmp_path: Path) -> None:
    artwork_path = tmp_path / "cover.jpg"
    make_artwork(artwork_path)
    audio_path = tmp_path / "missing.wav"
    codes = {
        issue.code
        for issue in validate_release(
            paid_metadata(), audio_path, artwork_path
        )
    }

    assert "audio_missing" in codes


def test_validate_release_reports_missing_and_invalid_artwork(
    tmp_path: Path,
) -> None:
    audio_path = tmp_path / "track.wav"
    make_audio(audio_path)
    missing_path = tmp_path / "missing.jpg"
    png_path = tmp_path / "cover.png"
    png_path.write_bytes(b"png")
    invalid_path = tmp_path / "invalid.jpg"
    invalid_path.write_bytes(b"not image")

    missing_codes = {
        issue.code
        for issue in validate_release(
            paid_metadata(), audio_path, missing_path
        )
    }
    png_codes = {
        issue.code
        for issue in validate_release(paid_metadata(), audio_path, png_path)
    }
    invalid_codes = {
        issue.code
        for issue in validate_release(
            paid_metadata(), audio_path, invalid_path
        )
    }

    assert "artwork_missing" in missing_codes
    assert "artwork_format" in png_codes
    assert "artwork_invalid" in invalid_codes


@pytest.mark.parametrize(
    ("suffix", "header"),
    [
        (".flac", b"fLaC"),
        (".mp3", b"ID3"),
        (".m4a", b"\0\0\0\0ftyp"),
        (".aiff", b"FORM\0\0\0\0AIFF"),
        (".wma", b"\x30\x26\xb2\x75"),
    ],
)
def test_validate_release_rejects_truncated_audio_formats(
    tmp_path: Path,
    suffix: str,
    header: bytes,
) -> None:
    audio_path = tmp_path / f"track{suffix}"
    artwork_path = tmp_path / "cover.jpg"
    audio_path.write_bytes(header + b"0" * 100)
    make_artwork(artwork_path)
    metadata = paid_metadata(
        audio_scope="none",
        lyrics_by_ai=False,
        music_by_ai=False,
    )

    codes = {
        issue.code
        for issue in validate_release(metadata, audio_path, artwork_path)
    }

    assert "audio_invalid" in codes


def test_validate_release_reports_wav_without_frames(tmp_path: Path) -> None:
    audio_path = tmp_path / "track.wav"
    artwork_path = tmp_path / "cover.jpg"
    make_audio(audio_path, frames=0)
    make_artwork(artwork_path)

    codes = {
        issue.code
        for issue in validate_release(
            paid_metadata(), audio_path, artwork_path
        )
    }

    assert "audio_invalid" in codes


def test_validate_release_requires_consistent_ai_scope(tmp_path: Path) -> None:
    audio_path = tmp_path / "track.wav"
    artwork_path = tmp_path / "cover.jpg"
    make_audio(audio_path)
    make_artwork(artwork_path)
    metadata = paid_metadata(
        audio_scope="all",
        lyrics_by_ai=False,
        music_by_ai=False,
    )

    codes = {
        issue.code
        for issue in validate_release(metadata, audio_path, artwork_path)
    }

    assert "ai_scope_mismatch" in codes

    metadata = paid_metadata(
        audio_scope="none",
        lyrics_by_ai=True,
        music_by_ai=False,
    )
    codes = {
        issue.code
        for issue in validate_release(metadata, audio_path, artwork_path)
    }
    assert "ai_scope_mismatch" in codes

    metadata = paid_metadata(
        audio_scope="part",
        lyrics_by_ai=False,
        music_by_ai=False,
    )
    codes = {
        issue.code
        for issue in validate_release(metadata, audio_path, artwork_path)
    }
    assert "ai_scope_mismatch" in codes


def test_validate_release_requires_artist_persona_confirmation_for_all_audio(
    tmp_path: Path,
) -> None:
    audio_path = tmp_path / "track.wav"
    artwork_path = tmp_path / "cover.jpg"
    make_audio(audio_path)
    make_artwork(artwork_path)

    codes = {
        issue.code
        for issue in validate_release(
            paid_metadata(artist_persona_confirmed=False),
            audio_path,
            artwork_path,
        )
    }

    assert "artist_persona_not_confirmed" in codes


def test_validate_release_requires_rights_evidence_reference(
    tmp_path: Path,
) -> None:
    audio_path = tmp_path / "track.wav"
    artwork_path = tmp_path / "cover.jpg"
    make_audio(audio_path)
    make_artwork(artwork_path)

    codes = {
        issue.code
        for issue in validate_release(
            paid_metadata(rights_evidence_path=""),
            audio_path,
            artwork_path,
        )
    }

    assert "rights_evidence_missing" in codes


def test_validate_release_requires_audio_playback_review(
    tmp_path: Path,
) -> None:
    audio_path = tmp_path / "track.wav"
    artwork_path = tmp_path / "cover.jpg"
    make_audio(audio_path)
    make_artwork(artwork_path)

    codes = {
        issue.code
        for issue in validate_release(
            paid_metadata(audio_reviewed=False),
            audio_path,
            artwork_path,
        )
    }

    assert "audio_not_reviewed" in codes


def test_validate_release_rejects_missing_rights_evidence_file(
    tmp_path: Path,
) -> None:
    audio_path = tmp_path / "track.wav"
    artwork_path = tmp_path / "cover.jpg"
    make_audio(audio_path)
    make_artwork(artwork_path)

    codes = {
        issue.code
        for issue in validate_release(
            paid_metadata(
                rights_evidence_path=str(tmp_path / "missing-proof.png")
            ),
            audio_path,
            artwork_path,
        )
    }

    assert "rights_evidence_invalid" in codes


def test_validate_release_requires_target_store(tmp_path: Path) -> None:
    audio_path = tmp_path / "track.wav"
    artwork_path = tmp_path / "cover.jpg"
    make_audio(audio_path)
    make_artwork(artwork_path)

    empty_codes = {
        issue.code
        for issue in validate_release(
            paid_metadata(target_stores=()), audio_path, artwork_path
        )
    }
    blank_codes = {
        issue.code
        for issue in validate_release(
            paid_metadata(target_stores=("",)), audio_path, artwork_path
        )
    }

    assert "target_stores_empty" in empty_codes
    assert "target_store_invalid" in blank_codes


def test_build_release_package_creates_distrokid_ready_files(
    tmp_path: Path,
) -> None:
    audio_path = tmp_path / "raw-track.wav"
    artwork_path = tmp_path / "raw-cover.jpg"
    lyrics_path = tmp_path / "raw-lyrics.txt"
    output_root = tmp_path / "releases"
    make_audio(audio_path)
    make_artwork(artwork_path)
    lyrics_path.write_text("[Verse 1]\nMidnight signal", encoding="utf-8")

    package_path = build_release_package(
        paid_metadata(),
        audio_path,
        artwork_path,
        lyrics_path,
        output_root,
    )

    manifest_path = package_path / "metadata" / "release.json"
    checklist_path = package_path / "distrokid" / "upload-checklist.md"
    suno_record_path = package_path / "rights" / "suno-record.json"
    assert (package_path / "audio" / "master.wav").read_bytes().startswith(
        b"RIFF"
    )
    assert (package_path / "artwork" / "cover.jpg").exists()
    assert (package_path / "lyrics" / "lyrics.txt").exists()
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    assert manifest["track_title"] == "Midnight Signal"
    assert "All of the audio" in checklist_path.read_text(encoding="utf-8")
    suno_record = json.loads(suno_record_path.read_text(encoding="utf-8"))
    assert suno_record["plan"] == "Pro"


def test_build_release_package_rejects_duplicate_artwork(
    tmp_path: Path,
) -> None:
    audio_path = tmp_path / "track.wav"
    artwork_path = tmp_path / "cover.jpg"
    lyrics_path = tmp_path / "lyrics.txt"
    output_root = tmp_path / "releases"
    make_audio(audio_path)
    make_artwork(artwork_path)
    lyrics_path.write_text("lyrics", encoding="utf-8")
    build_release_package(
        paid_metadata(), audio_path, artwork_path, lyrics_path, output_root
    )
    other_folder = output_root / "other-release" / "artwork"
    other_folder.mkdir(parents=True)
    (other_folder / "cover.jpg").write_bytes(b"different artwork")

    with pytest.raises(FileExistsError):
        build_release_package(
            paid_metadata(), audio_path, artwork_path, lyrics_path, output_root
        )

    with pytest.raises(ValueError, match="duplicate_artwork"):
        build_release_package(
            paid_metadata(
                track_title="Second Signal",
                release_date="2026-10-11",
            ),
            audio_path,
            artwork_path,
            lyrics_path,
            output_root,
        )


def test_build_release_package_checks_all_existing_artwork(
    tmp_path: Path,
) -> None:
    audio_path = tmp_path / "track.wav"
    artwork_path = tmp_path / "cover.jpg"
    new_artwork_path = tmp_path / "new-cover.jpg"
    lyrics_path = tmp_path / "lyrics.txt"
    output_root = tmp_path / "releases"
    make_audio(audio_path)
    make_artwork(artwork_path)
    Image.new("RGB", (3000, 3000), color=(200, 20, 20)).save(
        new_artwork_path,
        format="JPEG",
    )
    lyrics_path.write_text("lyrics", encoding="utf-8")
    first = output_root / "first" / "artwork"
    second = output_root / "second" / "artwork"
    first.mkdir(parents=True)
    second.mkdir(parents=True)
    (first / "cover.jpg").write_bytes(b"first")
    (second / "cover.jpg").write_bytes(b"second")
    empty_root = tmp_path / "empty-releases"
    empty_root.mkdir()
    suno_release._reject_duplicate_artwork(new_artwork_path, empty_root)

    package_path = build_release_package(
        paid_metadata(track_title="Third Signal", release_date="2026-10-12"),
        audio_path,
        new_artwork_path,
        lyrics_path,
        output_root,
    )

    assert package_path.exists()


def test_build_release_package_cleans_up_after_write_failure(
    tmp_path: Path,
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    audio_path = tmp_path / "track.wav"
    artwork_path = tmp_path / "cover.jpg"
    lyrics_path = tmp_path / "lyrics.txt"
    output_root = tmp_path / "releases"
    make_audio(audio_path)
    make_artwork(artwork_path)
    lyrics_path.write_text("lyrics", encoding="utf-8")

    def fail_write(*args: object, **kwargs: object) -> None:
        raise RuntimeError("simulated write failure")

    monkeypatch.setattr(suno_release, "_write_package_files", fail_write)

    with pytest.raises(RuntimeError, match="simulated write failure"):
        build_release_package(
            paid_metadata(), audio_path, artwork_path, lyrics_path, output_root
        )

    assert list(output_root.iterdir()) == []


def test_build_release_package_cleans_up_after_interrupt(
    tmp_path: Path,
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    audio_path = tmp_path / "track.wav"
    artwork_path = tmp_path / "cover.jpg"
    lyrics_path = tmp_path / "lyrics.txt"
    output_root = tmp_path / "releases"
    make_audio(audio_path)
    make_artwork(artwork_path)
    lyrics_path.write_text("lyrics", encoding="utf-8")

    def interrupt_write(*args: object, **kwargs: object) -> None:
        raise KeyboardInterrupt()

    monkeypatch.setattr(suno_release, "_write_package_files", interrupt_write)

    with pytest.raises(KeyboardInterrupt):
        build_release_package(
            paid_metadata(), audio_path, artwork_path, lyrics_path, output_root
        )

    assert list(output_root.iterdir()) == []


def test_build_release_package_requires_lyrics_and_valid_metadata(
    tmp_path: Path,
) -> None:
    audio_path = tmp_path / "track.wav"
    artwork_path = tmp_path / "cover.jpg"
    make_audio(audio_path)
    make_artwork(artwork_path)
    output_root = tmp_path / "releases"

    with pytest.raises(ValueError, match="가사 파일"):
        build_release_package(
            paid_metadata(),
            audio_path,
            artwork_path,
            tmp_path / "missing.txt",
            output_root,
        )

    with pytest.raises(ValueError, match="배포 권리"):
        build_release_package(
            paid_metadata(rights_confirmed=False),
            audio_path,
            artwork_path,
            tmp_path / "missing.txt",
            output_root,
        )


def test_build_release_package_rejects_blank_lyrics_unless_instrumental(
    tmp_path: Path,
) -> None:
    audio_path = tmp_path / "track.wav"
    artwork_path = tmp_path / "cover.jpg"
    lyrics_path = tmp_path / "lyrics.txt"
    output_root = tmp_path / "releases"
    make_audio(audio_path)
    make_artwork(artwork_path)
    lyrics_path.write_text("  \n", encoding="utf-8")

    with pytest.raises(ValueError, match="가사 파일이 비어"):
        build_release_package(
            paid_metadata(), audio_path, artwork_path, lyrics_path, output_root
        )

    package_path = build_release_package(
        paid_metadata(instrumental=True),
        audio_path,
        artwork_path,
        lyrics_path,
        output_root,
    )
    assert (package_path / "lyrics" / "lyrics.txt").exists()


def test_safe_slug_and_package_name_have_fallbacks() -> None:
    metadata = paid_metadata(track_title="!!!", release_date="")

    assert suno_release._safe_slug("!!!") == "untitled"
    assert suno_release._package_name(metadata).endswith("_untitled")


def test_main_creates_package_from_json(tmp_path: Path) -> None:
    audio_path = tmp_path / "track.wav"
    artwork_path = tmp_path / "cover.jpg"
    lyrics_path = tmp_path / "lyrics.txt"
    metadata_path = tmp_path / "metadata.json"
    output_root = tmp_path / "releases"
    make_audio(audio_path)
    make_artwork(artwork_path)
    lyrics_path.write_text("lyrics", encoding="utf-8")
    metadata_path.write_text(
        json.dumps({
            "artist_name": "Neon Harbor",
            "track_title": "Midnight Signal",
            "primary_genre": "Electronic",
            "songwriter_name": "Kim Jae-seop",
            "suno_plan": "Pro",
            "generated_at": "2026-09-08T10:00:00+09:00",
            "downloaded_at": "2026-09-08T10:05:00+09:00",
            "lyrics_by_ai": True,
            "music_by_ai": True,
            "audio_scope": "all",
            "rights_confirmed": True,
            "rights_evidence_path": str(Path(__file__)),
            "artwork_reviewed": True,
            "audio_reviewed": True,
            "artist_persona_confirmed": True,
            "release_date": "2026-10-10",
        }),
        encoding="utf-8",
    )

    result = suno_release.main([
        "--metadata", str(metadata_path),
        "--audio", str(audio_path),
        "--artwork", str(artwork_path),
        "--lyrics", str(lyrics_path),
        "--output", str(output_root),
    ])

    assert result == 0
    assert (output_root / "2026-10-10_Midnight-Signal").exists()


def test_main_reports_invalid_input() -> None:
    with pytest.raises(SystemExit):
        suno_release.main(["--metadata", "missing.json"])


def test_main_reports_package_error(tmp_path: Path) -> None:
    with pytest.raises(SystemExit):
        suno_release.main(
            [
                "--metadata",
                str(tmp_path / "missing.json"),
                "--audio",
                str(tmp_path / "track.wav"),
                "--artwork",
                str(tmp_path / "cover.jpg"),
                "--lyrics",
                str(tmp_path / "lyrics.txt"),
                "--output",
                str(tmp_path / "releases"),
            ]
        )
