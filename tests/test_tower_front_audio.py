"""Validate the packaged original music loop without an Android device."""
from array import array
from pathlib import Path
import math
import wave


def test_music_loop() -> None:
    path = Path(__file__).resolve().parents[1] / 'android-tower-front/res/raw/island_theme.wav'
    with wave.open(str(path), 'rb') as sound:
        assert sound.getnchannels() == 1
        assert sound.getsampwidth() == 2
        assert sound.getframerate() == 22050
        assert 40 < sound.getnframes() / sound.getframerate() < 45
        samples = array('h', sound.readframes(sound.getnframes()))
    peak = max(abs(sample) for sample in samples) / 32768
    rms = math.sqrt(sum(sample * sample for sample in samples) / len(samples)) / 32768
    assert .1 < peak < .9
    assert .02 < rms < .4
    assert abs(samples[0] - samples[-1]) / 32768 < .01
