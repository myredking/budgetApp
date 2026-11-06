from pathlib import Path
import importlib.util
import math

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


def test_two_hands_remain_on_rotating_bat_handle() -> None:
    module = motion_module()
    for direction in ([0, 0, 1], [1, 0, 0], [.3, -.6, .2]):
        grip = module.grip_layout([.2, -.3, 1.2], direction)
        assert math.dist(grip["left_hand"], grip["right_hand"]) == pytest.approx(.07)
        length = math.sqrt(sum(v * v for v in direction))
        axis = [v / length for v in direction]
        for side, distance in (("left_hand", .045), ("right_hand", .115)):
            expected = [v + a * distance for v, a in zip(grip["bat_origin"], axis)]
            assert grip[side] == pytest.approx(expected)


def test_zero_bat_direction_is_rejected() -> None:
    with pytest.raises(ValueError):
        motion_module().grip_layout([0, 0, 1], [0, 0, 0])


def test_ball_flight_starts_at_release_and_uses_elapsed_time() -> None:
    module = motion_module()
    assert module.ball_flight([0, 0, 1.5], [0, -32, 0], 0) == [0, 0, 1.5]
    assert module.ball_flight([0, 0, 1.5], [0, -32, 0], .1) == pytest.approx([0, -3.2, 1.45095])
    with pytest.raises(ValueError):
        module.ball_flight([0, 0, 1.5], [0, -32, 0], -.1)


def test_swing_variants_have_distinct_contact_poses_without_mutating_base() -> None:
    module = motion_module()
    base = module.load_profiles(ROOT / "player-profiles.json")[0]
    import copy
    original = copy.deepcopy(base)
    push = module.swing_variant(base, 1)
    pull = module.swing_variant(base, 2)
    down = module.swing_variant(base, 3)
    assert base == original
    push_axis = module.sample_track(push["poses"], "bat_direction", 1.7)
    pull_axis = module.sample_track(pull["poses"], "bat_direction", 1.7)
    assert push_axis[0] < pull_axis[0]
    assert module.sample_track(down["poses"], "left_hand", 1.7)[2] < module.sample_track(base["poses"], "left_hand", 1.7)[2]
    with pytest.raises(ValueError):
        module.swing_variant(base, 0)
