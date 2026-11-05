"""Sample hand-authored review motion in metres; not measured player mocap."""
import json
import math
from pathlib import Path


def validate_times(poses: list[dict]) -> list[float]:
    if not poses:
        raise ValueError("Nonempty poses are required")
    times = [pose["time"] for pose in poses]
    if not all(math.isfinite(t) for t in times):
        raise ValueError("Times must be finite")
    if any(b <= a for a, b in zip(times, times[1:])):
        raise ValueError("Pose times must be strictly increasing")
    return times


def track_values(poses: list[dict], track: str) -> list[list[float]]:
    values = [pose[track] for pose in poses]
    if any(len(value) != 3 for value in values):
        raise ValueError("Tracks require three coordinates")
    if any(not math.isfinite(v) for value in values for v in value):
        raise ValueError("Coordinates must be finite")
    return values


def sample_track(poses: list[dict], track: str, time: float) -> list[float]:
    if not math.isfinite(time):
        raise ValueError("A finite time is required")
    times = validate_times(poses)
    values = track_values(poses, track)
    if time <= times[0]:
        return list(values[0])
    for index in range(1, len(poses)):
        if time <= times[index]:
            ratio = (time - times[index - 1]) / (times[index] - times[index - 1])
            return [a + (b - a) * ratio for a, b in zip(values[index - 1], values[index])]
    return list(values[-1])


def load_profiles(path: Path) -> list[dict]:
    data = json.loads(path.read_text(encoding="utf-8"))
    return data["players"]
