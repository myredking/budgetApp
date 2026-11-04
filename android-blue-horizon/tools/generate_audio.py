"""Compose two original synthesized nautical loops and six short game cues."""
from array import array
import math
from pathlib import Path
import sys
import wave

RATE = 22050
ASSETS = Path(__file__).resolve().parents[1] / "assets"


def timbre(phase: float, voice: str) -> float:
    if voice == "flute":
        return math.sin(phase) + .12 * math.sin(phase * 2) + .035 * math.sin(phase * 3)
    if voice == "strings":
        return .7 * math.sin(phase) + .19 * math.sin(phase * 2) + .09 * math.sin(phase * 3)
    return math.sin(phase) + .32 * math.sin(phase * 2) + .13 * math.sin(phase * 3)


def envelope(time: float, duration: float, voice: str) -> float:
    attack = min(1.0, time / .018)
    if voice == "pluck":
        return attack * math.exp(-5 * time / duration) * min(1.0, (duration - time) / .03)
    return attack * math.sin(math.pi * time / duration) ** .8


def note(buffer: array, start: float, duration: float, midi: int, gain: float, voice: str) -> None:
    offset = round(start * RATE)
    count = min(round(duration * RATE), len(buffer) - offset)
    frequency = 440 * 2 ** ((midi - 69) / 12)
    for index in range(count):
        time = index / RATE
        phase = math.tau * frequency * time + .06 * math.sin(math.tau * 5 * time)
        buffer[offset + index] += gain * envelope(time, duration, voice) * timbre(phase, voice)


def write(name: str, samples: array) -> None:
    peak = max(abs(value) for value in samples)
    scale = 23000 / max(.001, peak)
    fade = min(441, len(samples) // 4)
    pcm = array("h", (round(value * scale * min(1, index/fade, (len(samples)-1-index)/fade))
                      for index, value in enumerate(samples)))
    if sys.byteorder != "little":
        pcm.byteswap()
    with wave.open(str(ASSETS / f"{name}.wav"), "wb") as out:
        out.setnchannels(1)
        out.setsampwidth(2)
        out.setframerate(RATE)
        out.writeframes(pcm.tobytes())


def arrangement(name: str, bpm: int, chords: list[list[int]], melody: list[list[int]]) -> None:
    beat = 60 / bpm
    samples = array("f", [0]) * round(16 * 4 * beat * RATE)
    for bar in range(16):
        start = bar * 4 * beat
        chord = chords[bar % len(chords)]
        for pitch in chord:
            note(samples, start, beat * 3.9, pitch, .065, "strings")
        for tick in range(8):
            pitch = chord[tick % 3] + 12
            note(samples, start + tick * beat / 2, beat * .85, pitch, .13, "pluck")
        for tick, pitch in enumerate(melody[bar % len(melody)]):
            note(samples, start + tick * beat, beat * .9, pitch, .16, "flute")
        for tick in (0, 2):
            note(samples, start + tick * beat, beat * 1.7, chord[0] - 12, .16, "pluck")
    write(name, samples)


def cue(name: str, pitches: list[int], interval: float, duration: float) -> None:
    samples = array("f", [0]) * round((len(pitches) * interval + duration) * RATE)
    for index, pitch in enumerate(pitches):
        note(samples, index * interval, duration, pitch, .35, "pluck")
    write(name, samples)


def main() -> None:
    ASSETS.mkdir(exist_ok=True)
    arrangement("harbor", 100, [[50,54,57],[55,59,62],[47,50,54],[45,49,52]],
                [[74,78,76,74],[71,74,79,78],[76,74,71,69],[73,76,78,73],
                 [74,81,78,76],[79,78,74,71],[74,76,78,74],[73,69,71,73]])
    arrangement("sea", 96, [[50,53,57],[48,52,55],[46,50,53],[45,49,52]],
                [[69,74,77,76],[72,76,79,76],[74,77,81,77],[76,73,69,73],
                 [77,76,74,69],[72,79,76,72],[74,77,74,70],[73,76,73,69]])
    cue("click", [86], .02, .07)
    cue("trade", [79,86,91], .055, .18)
    cue("depart", [62,69,74,78], .11, .33)
    cue("chapter", [62,66,69,74,78,81], .095, .45)
    cue("error", [50,45], .08, .13)
    cue("upgrade", [57,62,66,69,74], .075, .32)
    print("Created 2 original music loops and 6 effects (mono PCM, 22050 Hz).")


if __name__ == "__main__":
    main()
