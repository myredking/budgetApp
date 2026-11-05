"""Offline checks for bundled original audio and cast artwork."""
from array import array
import hashlib
from pathlib import Path
import sys
import wave
import zipfile

from PIL import Image
import pytest

PROJECT = Path(__file__).resolve().parents[1]
MUSIC = ["harbor", "sea"]
EFFECTS = ["click", "trade", "depart", "chapter", "error", "upgrade", "cannon", "discovery"]


@pytest.mark.parametrize("name", MUSIC + EFFECTS)
def test_pcm_asset_is_audible_and_not_clipped(name: str) -> None:
    with wave.open(str(PROJECT / "assets" / f"{name}.wav"), "rb") as source:
        assert source.getnchannels() == 1
        assert source.getsampwidth() == 2
        assert source.getframerate() == 22050
        duration = source.getnframes() / source.getframerate()
        data = array("h", source.readframes(source.getnframes()))
    if sys.byteorder != "little":
        data.byteswap()
    assert max(abs(value) for value in data) < 32000
    assert sum(value * value for value in data) / len(data) > 200 ** 2
    assert data[0] == data[-1] == 0
    if name in MUSIC:
        assert 32 <= duration <= 60
    else:
        assert .05 <= duration <= 2


def test_music_tracks_are_distinct() -> None:
    hashes = [hashlib.sha256((PROJECT / "assets" / f"{name}.wav").read_bytes()).digest() for name in MUSIC]
    assert hashes[0] != hashes[1]


def test_rpg_art_is_packaged() -> None:
    names = ["town-scenes-v1.png", "facility-scenes-v1.png", "sailing-ships-v1.png"]
    with zipfile.ZipFile(PROJECT / "BlueHorizon.apk") as package:
        for name in names:
            assert package.getinfo(f"assets/{name}").file_size > 10000
    with Image.open(PROJECT / "assets/sailing-ships-v1.png") as image:
        assert image.mode == "RGBA"
        assert image.getchannel("A").getextrema()[0] == 0


def test_portrait_atlas_has_six_equal_square_regions() -> None:
    with Image.open(PROJECT / "assets/crew-portraits-v1.png") as image:
        assert image.width // 3 == image.height // 2
        assert image.width % 3 == image.height % 2 == 0


def test_apk_media_is_seekable() -> None:
    with zipfile.ZipFile(PROJECT / "BlueHorizon.apk") as package:
        for name in MUSIC + EFFECTS:
            info = package.getinfo(f"assets/{name}.wav")
            assert info.compress_type == zipfile.ZIP_STORED
        assert package.getinfo("assets/crew-portraits-v1.png").file_size > 10000
