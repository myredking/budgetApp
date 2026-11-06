"""Sample hand-authored review motion in metres; not measured player mocap."""
import json
import math
import copy
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


def swing_variant(base: dict, style: int) -> dict:
    if style not in (1, 2, 3):
        raise ValueError("Style must be push, pull or down")
    result = copy.deepcopy(base)
    result["id"] += ("_push", "_pull", "_down")[style - 1]
    for pose in result["poses"]:
        weight = max(0, 1 - abs(pose["time"] - 1.7) / .55)
        pose["bat_direction"][0] += (-.22, .22, 0)[style - 1] * weight
        if style == 3:
            for hand in ("left_hand", "right_hand"):
                pose[hand][2] -= .025 * weight
            pose["bat_direction"][2] -= .08 * weight
    return result


def finite_vector(value: list[float]) -> None:
    if len(value) != 3 or not all(math.isfinite(v) for v in value):
        raise ValueError("A finite three-coordinate vector is required")


def grip_layout(center: list[float], direction: list[float]) -> dict[str, list[float]]:
    finite_vector(center)
    finite_vector(direction)
    length = math.sqrt(sum(v * v for v in direction))
    if length < 1e-8:
        raise ValueError("Bat direction cannot be zero")
    axis = [v / length for v in direction]
    return {
        "left_hand": [v - .035 * a for v, a in zip(center, axis)],
        "right_hand": [v + .035 * a for v, a in zip(center, axis)],
        "bat_origin": [v - .08 * a for v, a in zip(center, axis)],
        "axis": axis,
    }


def ball_flight(origin: list[float], velocity: list[float], elapsed: float) -> list[float]:
    finite_vector(origin)
    finite_vector(velocity)
    if not math.isfinite(elapsed) or elapsed < 0:
        raise ValueError("Elapsed flight time must be finite and nonnegative")
    position = [v + speed * elapsed for v, speed in zip(origin, velocity)]
    position[2] -= 4.905 * elapsed * elapsed
    return position
