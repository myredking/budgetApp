"""Blender background entry point for two rigged player motion studies."""
from __future__ import annotations

import argparse
import json
import math
from pathlib import Path
import sys

import bpy
from mathutils import Quaternion, Vector

sys.path.insert(0, str(Path(__file__).resolve().parent))
from player_motion import load_profiles, sample_track


def material(name: str, color: list[float], roughness: float = 0.65) -> bpy.types.Material:
    mat = bpy.data.materials.new(name)
    mat.use_nodes = True
    node = mat.node_tree.nodes.get("Principled BSDF")
    node.inputs["Base Color"].default_value = color
    node.inputs["Roughness"].default_value = roughness
    mat.diffuse_color = color
    return mat


def target(name: str, location: Vector) -> bpy.types.Object:
    obj = bpy.data.objects.new(name, None)
    bpy.context.collection.objects.link(obj)
    obj.location = location
    obj.empty_display_size = 0.04
    return obj


def limb_target(rig: bpy.types.Object, bone: str, name: str) -> bpy.types.Object:
    pose = rig.pose.bones[bone]
    obj = target(name, pose.tail)
    constraint = pose.constraints.new("IK")
    constraint.target = obj
    constraint.chain_count = 4
    constraint.use_stretch = False
    for ancestor in [pose, pose.parent, pose.parent.parent, pose.parent.parent.parent]:
        ancestor.ik_stretch = 0
    if "foot" in name:
        foot = rig.pose.bones["foot." + bone[-1]]
        obj.rotation_euler = foot.bone.matrix_local.to_euler()
        keep_level = foot.constraints.new("COPY_ROTATION")
        keep_level.target = obj
        keep_level.target_space = "WORLD"
        keep_level.owner_space = "WORLD"
    return obj


def make_bat() -> bpy.types.Object:
    rings = [(0, .018), (.018, .019), (.035, .012), (.31, .013),
             (.46, .021), (.64, .031), (.82, .032), (.85, .025)]
    vertices = [(radius * math.cos(i * math.tau / 32),
                 radius * math.sin(i * math.tau / 32), z * .88)
                for z, radius in rings for i in range(32)]
    faces = [(j * 32 + i, j * 32 + (i + 1) % 32,
              (j + 1) * 32 + (i + 1) % 32, (j + 1) * 32 + i)
             for j in range(len(rings) - 1) for i in range(32)]
    faces.extend([tuple(reversed(range(32))), tuple(range(224, 256))])
    mesh = bpy.data.meshes.new("TurnedBat")
    mesh.from_pydata(vertices, [], faces)
    bat = bpy.data.objects.new("Bat", mesh)
    bpy.context.collection.objects.link(bat)
    bat.data.materials.append(material("Maple", [.48, .26, .11, 1], .35))
    for face in mesh.polygons:
        face.use_smooth = True
    return bat


def style(profile: dict, rig: bpy.types.Object) -> None:
    cloth = bpy.data.objects["AthleteBody.male_casualsuit06"]
    cloth.data.materials[0] = material(profile["id"] + " Jersey", profile["color"])
    helmet = material(profile["id"] + " Headwear", [.25, .008, .016, 1], .28)
    if profile["role"] == "pitcher":
        helmet = material("Pitcher Cap", [.012, .014, .018, 1], .7)
        for vertex in bpy.data.objects["BattingHelmet"].data.vertices:
            vertex.co.z = 1.635 + (vertex.co.z - 1.635) * .8
    for name in ("BattingHelmet", "HelmetVisor"):
        bpy.data.objects[name].data.materials.clear()
        bpy.data.objects[name].data.materials.append(helmet)
    for obj in bpy.data.objects:
        if obj.type == "MESH":
            for face in obj.data.polygons:
                face.use_smooth = True
    for bone in rig.pose.bones:
        if bone.name.startswith("finger"):
            bone.rotation_mode = "XYZ"
            bone.rotation_euler.x = .65


def pitching_equipment(rig: bpy.types.Object) -> None:
    wrist = rig.pose.bones["wrist.R"]
    bpy.ops.mesh.primitive_uv_sphere_add(segments=32, ring_count=16, location=wrist.tail)
    glove = bpy.context.object
    glove.name = "GloveBlockout"
    glove.scale = (.075, .052, .105)
    glove.data.materials.append(material("Glove Leather", [.16, .065, .025, 1], .8))
    bpy.context.view_layer.update()
    world = glove.matrix_world.copy()
    glove.parent, glove.parent_type, glove.parent_bone = rig, "BONE", "wrist.R"
    bpy.context.view_layer.update()
    glove.matrix_world = world


def face_and_number(profile: dict, rig: bpy.types.Object) -> None:
    body = bpy.data.objects["AthleteBody"]
    meshes = [body.data.vertices]
    if body.data.shape_keys:
        meshes = [key.data for key in body.data.shape_keys.key_blocks]
    width = 1.16 if profile["role"] == "pitcher" else .96
    for points in meshes:
        for vertex in points:
            blend = max(0, 1 - abs(vertex.co.z - 1.425) / .065)
            vertex.co.x *= 1 + (width - 1) * blend
    bpy.ops.object.text_add(location=(.055, -.15, 1.16), rotation=(math.pi / 2, 0, 0))
    label = bpy.context.object
    label.name = "JerseyNumber"
    label.data.body = str(profile["number"])
    label.data.size = .075
    label.data.extrude = .0005
    color = [.012, .016, .022, 1] if profile["role"] == "pitcher" else [.9, .88, .83, 1]
    label.data.materials.append(material("Number", color))
    bpy.ops.object.convert(target="MESH")
    world = label.matrix_world.copy()
    label.parent, label.parent_type, label.parent_bone = rig, "BONE", "spine01"
    bpy.context.view_layer.update()
    label.matrix_world = world


def animate(profile: dict, rig: bpy.types.Object, controls: dict) -> None:
    root = rig.pose.bones["root"]
    spine = rig.pose.bones["spine03"]
    spine.rotation_mode = "QUATERNION"
    rest = spine.bone.matrix_local.to_quaternion()
    for frame in range(1, 98):
        time = (frame - 1) / 30
        for name, obj in controls.items():
            obj.location = sample_track(profile["poses"], name, time)
            obj.keyframe_insert("location", frame=frame)
        shift = Vector(sample_track(profile["poses"], "root", time))
        root.location = root.bone.matrix_local.to_3x3().inverted() @ shift
        root.keyframe_insert("location", frame=frame)
        angles = sample_track(profile["poses"], "torso", time)
        delta = Quaternion(Vector((0, 0, 1)), math.radians(angles[2]))
        delta = delta @ Quaternion(Vector((1, 0, 0)), math.radians(angles[0]))
        spine.rotation_quaternion = rest.inverted() @ delta @ rest
        spine.keyframe_insert("rotation_quaternion", frame=frame)


def animate_bat(profile: dict, bat: bpy.types.Object) -> None:
    bat.rotation_mode = "QUATERNION"
    for frame in range(1, 98):
        time = (frame - 1) / 30
        left = Vector(sample_track(profile["poses"], "left_hand", time))
        right = Vector(sample_track(profile["poses"], "right_hand", time))
        direction = Vector(sample_track(profile["poses"], "bat_direction", time)).normalized()
        bat.location = (left + right) / 2 - direction * .07
        bat.rotation_quaternion = direction.to_track_quat("Z", "Y")
        bat.keyframe_insert("location", frame=frame)
        bat.keyframe_insert("rotation_quaternion", frame=frame)


def bake_and_export(profile: dict, rig: bpy.types.Object, output: Path) -> None:
    bpy.ops.object.select_all(action="DESELECT")
    rig.select_set(True)
    bpy.context.view_layer.objects.active = rig
    bpy.ops.nla.bake(frame_start=1, frame_end=97, only_selected=False,
                     visual_keying=True, clear_constraints=True, use_current_action=True,
                     bake_types={"POSE"})
    rig.animation_data.action.name = profile["id"] + "_motion_study"
    for name in ("left_hand", "right_hand", "left_foot", "right_foot"):
        bpy.data.objects.remove(bpy.data.objects[name], do_unlink=True)
    scale = profile["height_cm"] / 160
    assembly = target("PlayerRoot", Vector((0, 0, 0)))
    for obj in list(bpy.data.objects):
        if obj.parent is None and obj != assembly:
            obj.parent = assembly
    assembly.scale = (scale * profile["width_scale"], scale, scale)
    scene = bpy.context.scene
    scene.frame_start, scene.frame_end, scene.render.fps = 1, 97, 30
    scene.frame_set(round(profile["preview_time"] * 30) + 1)
    bpy.ops.file.pack_all()
    output.mkdir(parents=True, exist_ok=True)
    bpy.ops.wm.save_as_mainfile(filepath=str(output / (profile["id"] + ".blend")))
    bpy.ops.export_scene.fbx(filepath=str(output / (profile["id"] + ".fbx")),
        object_types={"ARMATURE", "MESH", "EMPTY"}, add_leaf_bones=False,
        bake_anim=True, bake_anim_use_all_actions=False, bake_anim_use_nla_strips=False,
        path_mode="COPY", embed_textures=True, use_mesh_modifiers=True)


def build(profile: dict, source: Path, output: Path) -> None:
    bpy.ops.wm.open_mainfile(filepath=str(source))
    rig = bpy.data.objects["AthleteBody.rig"]
    style(profile, rig)
    face_and_number(profile, rig)
    if profile["role"] == "pitcher":
        pitching_equipment(rig)
    controls = {}
    for side, letter in (("left", "L"), ("right", "R")):
        controls[side + "_hand"] = limb_target(rig, "lowerarm02." + letter, side + "_hand")
        controls[side + "_foot"] = limb_target(rig, "lowerleg02." + letter, side + "_foot")
    animate(profile, rig, controls)
    if profile["role"] == "batter":
        animate_bat(profile, make_bat())
    bake_and_export(profile, rig, output)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args(sys.argv[sys.argv.index("--") + 1:])
    profiles = load_profiles(Path(__file__).resolve().parents[1] / "player-profiles.json")
    for profile in profiles:
        build(profile, args.source, args.output)


if __name__ == "__main__":
    main()
