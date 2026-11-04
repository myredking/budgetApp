"""Generate original offline puzzle music and short effects as PCM WAV files."""
from pathlib import Path
import math
import struct
import wave

RATE = 22050


def note(buffer: list[float], start: float, duration: float, pitch: int, volume: float) -> None:
    frequency = 440 * 2 ** ((pitch - 69) / 12)
    length = int(duration * RATE)
    offset = int(start * RATE)
    for i in range(min(length, len(buffer) - offset)):
        t = i / RATE
        envelope = min(1.0, t / 0.008) * max(0.0, 1 - i / length) ** 2
        tone = math.sin(2 * math.pi * frequency * t)
        tone += 0.18 * math.sin(4 * math.pi * frequency * t)
        buffer[offset + i] += tone * envelope * volume


def write(path: Path, buffer: list[float]) -> None:
    pcm = [max(-31000, min(31000, round(v * 24000))) for v in buffer]
    with wave.open(str(path), 'wb') as output:
        output.setnchannels(1)
        output.setsampwidth(2)
        output.setframerate(RATE)
        output.writeframes(struct.pack('<' + 'h' * len(pcm), *pcm))


def music() -> list[float]:
    beat = 0.4
    data = [0.0] * int(beat * 32 * RATE)
    melody = [72, 76, 79, 76, 74, 77, 81, 77, 71, 74, 79, 74, 69, 72, 76, 72]
    bass = [48, 53, 55, 57]
    for i in range(32):
        note(data, i * beat, 0.35, melody[i % 16], 0.24)
    for i in range(8):
        note(data, i * beat * 4, 1.4, bass[(i // 2) % 4], 0.22)
    return data


def generate(directory: Path) -> None:
    directory.mkdir(parents=True, exist_ok=True)
    write(directory / 'bgm.wav', music())
    effects = {
        'place': [60, 67], 'clear': [72, 79, 84, 91],
        'win': [72, 76, 79, 84, 88], 'fail': [55, 52, 48],
        'invalid': [42],
    }
    for name, pitches in effects.items():
        duration = 0.09 if name == 'place' else 0.12
        data = [0.0] * int((len(pitches) * duration + 0.16) * RATE)
        for i, pitch in enumerate(pitches):
            note(data, i * duration, duration + 0.12, pitch, 0.5)
        write(directory / (name + '.wav'), data)


if __name__ == '__main__':
    generate(Path(__file__).resolve().parents[1] / 'assets')
