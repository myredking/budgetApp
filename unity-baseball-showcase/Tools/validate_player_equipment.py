"""Measure baked Blender attachment errors; this does not approve visual likeness."""
from __future__ import annotations

import argparse
import json
from pathlib import Path
import sys

import bpy
from mathutils import Vector

sys.path.insert(0, str(Path(__file__).resolve().parent))
from player_motion import load_profiles


def check_bat() -> dict:
    rig, bat = bpy.data.objects["AthleteBody.rig"], bpy.data.objects["Bat"]
    errors = []
    for frame in range(1, 98):
        bpy.context.scene.frame_set(frame)
        for side, distance in (("L", .045), ("R", .115)):
            palm = rig.matrix_world @ rig.pose.bones["wrist." + side].matrix @ Vector((0, .055, 0))
            handle = bat.matrix_world @ Vector((0, 0, distance))
            errors.append((palm - handle).length)
    return {"max_palm_handle_error_m": max(errors), "attachment_passed": max(errors) < .04}


def check_ball(profile: dict) -> dict:
    scene = bpy.context.scene
    rig, ball = bpy.data.objects["AthleteBody.rig"], bpy.data.objects["PitchBall"]
    errors = []
    release = round(profile["event_time"] * 30) + 1
    for frame in range(1, release + 1):
        scene.frame_set(frame)
        palm = rig.matrix_world @ rig.pose.bones["wrist.L"].matrix @ Vector((0, .065, 0))
        errors.append((palm - ball.matrix_world.translation).length)
    scene.frame_set(release)
    origin = ball.matrix_world.translation.copy()
    scene.frame_set(release + 3)
    flight = (ball.matrix_world.translation - origin).length
    return {"max_ball_hand_error_m": max(errors), "travel_in_100ms_m": flight,
            "release_passed": max(errors) < .005 and 2.8 < flight < 3.6}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--art", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args(sys.argv[sys.argv.index("--") + 1:])
    profiles = load_profiles(Path(__file__).resolve().parents[1] / "player-profiles.json")
    results = []
    for profile in profiles:
        bpy.ops.wm.open_mainfile(filepath=str(args.art / (profile["id"] + ".blend")))
        result = check_bat() if profile["role"] == "batter" else check_ball(profile)
        results.append({"id": profile["id"], "frames": 97, **result})
    args.output.write_text(json.dumps(results, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(results))
    if not results[0]["attachment_passed"] or not results[1]["release_passed"]:
        raise RuntimeError("Baked player attachment validation failed")


if __name__ == "__main__":
    main()
