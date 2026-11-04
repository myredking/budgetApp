from pathlib import Path
import importlib.util
import wave
import struct


def test_audio_assets(tmp_path: Path) -> None:
    script = Path(__file__).resolve().parents[1] / 'android-block-garden/tools/make_audio.py'
    assert script.exists(), 'audio generation missing (TDD red)'
    spec = importlib.util.spec_from_file_location('block_audio', script)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    module.generate(tmp_path)
    expected = {'bgm.wav', 'place.wav', 'clear.wav', 'win.wav', 'fail.wav', 'invalid.wav'}
    assert {p.name for p in tmp_path.glob('*.wav')} == expected
    for name in expected:
        check_wave(tmp_path / name)


def check_wave(path: Path) -> None:
    with wave.open(str(path), 'rb') as audio:
        assert audio.getnchannels() == 1
        assert audio.getsampwidth() == 2
        samples = struct.unpack('<' + 'h' * audio.getnframes(), audio.readframes(audio.getnframes()))
        assert max(samples) > 100
        assert max(abs(v) for v in samples) < 32000
        assert abs(samples[0]) < 100 and abs(samples[-1]) < 100
        if path.name == 'bgm.wav':
            assert audio.getnframes() / audio.getframerate() >= 8
