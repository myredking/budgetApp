from pathlib import Path
import importlib.util

import pytest


ROOT = Path(__file__).resolve().parents[1] / "unity-baseball-showcase"


def motion_module() -> object:
    path = ROOT / "Tools/player_motion.py"
    spec = importlib.util.spec_from_file_location("player_motion", path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def test_interpolation_keeps_planted_foot_and_release_timing() -> None:
    module = motion_module()
    frames = [{"time": 0, "foot": [0, 0, 0.06]},
              {"time": 0.75, "foot": [0, 0, 0.06]},
              {"time": 1, "foot": [0, -0.2, 0.1]}]
    assert module.sample_track(frames, "foot", 0.5) == [0, 0, 0.06]
    assert module.sample_track(frames, "foot", 1) == [0, -0.2, 0.1]
    assert module.sample_track(frames, "foot", 99) == [0, -0.2, 0.1]


def test_reversed_or_duplicate_timestamps_are_rejected() -> None:
    module = motion_module()
    for times in ([0, 0], [1, 0]):
        with pytest.raises(ValueError, match="increasing"):
            module.sample_track([{"time": t, "hand": [0, 0, 1]} for t in times], "hand", 0)


def test_official_profile_handedness_drives_distinct_motion() -> None:
    module = motion_module()
    kim, ryu = module.load_profiles(ROOT / "player-profiles.json")
    assert (kim["height_cm"], kim["weight_kg"], kim["bat_hand"]) == (183, 85, "R")
    assert (ryu["height_cm"], ryu["weight_kg"], ryu["throw_hand"]) == (190, 113, "L")
    release = module.sample_track(ryu["poses"], "left_hand", ryu["event_time"])
    glove = module.sample_track(ryu["poses"], "right_hand", ryu["event_time"])
    assert release[1] < glove[1] - 0.2, "Left throwing hand must lead toward the plate"


def test_nan_coordinates_are_rejected() -> None:
    module = motion_module()
    with pytest.raises(ValueError):
        module.sample_track([{"time": 0, "hand": [float("nan"), 0, 0]}], "hand", 0)


def test_mismatched_coordinate_sizes_do_not_silently_truncate() -> None:
    module = motion_module()
    with pytest.raises(ValueError):
        module.sample_track([{"time": 0, "hand": [0, 0, 0]},
                             {"time": 1, "hand": [1, 1]}], "hand", 0.5)
