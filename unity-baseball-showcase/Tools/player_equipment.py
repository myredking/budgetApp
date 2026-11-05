"""Original review equipment meshes, authored in Blender rather than copied assets."""
from __future__ import annotations

import math
import bpy
from mathutils import Vector


def seam(name: str, points: list[tuple], radius: float, material: bpy.types.Material,
         parent: bpy.types.Object, closed: bool = False) -> bpy.types.Object:
    curve = bpy.data.curves.new(name, "CURVE")
    curve.dimensions = "3D"
    curve.bevel_depth, curve.bevel_resolution = radius, 2
    spline = curve.splines.new("POLY")
    spline.points.add(len(points) - 1)
    for point, coordinate in zip(spline.points, points):
        point.co = (*coordinate, 1)
    spline.use_cyclic_u = closed
    obj = bpy.data.objects.new(name, curve)
    bpy.context.collection.objects.link(obj)
    obj.data.materials.append(material)
    obj.parent = parent
    bpy.ops.object.select_all(action="DESELECT")
    obj.select_set(True)
    bpy.context.view_layer.objects.active = obj
    bpy.ops.object.convert(target="MESH")
    return bpy.context.object


def glove_outline(angle: float, fraction: float = 1) -> tuple[float, float, float]:
    scallop = 1 + .035 * math.cos(5 * angle)
    return (.092 * fraction * math.cos(angle) * scallop,
            .065 + .125 * fraction * math.sin(angle) * scallop,
            -.035 * (1 - fraction * fraction))


def glove_fingers(group: bpy.types.Object, leather: bpy.types.Material,
                  lace: bpy.types.Material) -> None:
    for index in range(5):
        thumb = index == 4
        x = -.093 if thumb else -.061 + index * .04
        y = .075 if thumb else .15 - abs(index - 1.5) * .007
        bpy.ops.mesh.primitive_uv_sphere_add(segments=20, ring_count=12)
        finger = bpy.context.object
        finger.name = "GloveThumb" if thumb else "GloveFinger"
        finger.parent = group
        finger.location = (x, y, -.013)
        finger.scale = (.028 if thumb else .023, .068, .019)
        finger.rotation_euler.z = -.65 if thumb else 0
        finger.data.materials.append(leather)
        for face in finger.data.polygons:
            face.use_smooth = True
        if not thumb:
            seam("FingerBinding", [(x, y + offset, .007) for offset in (-.04, 0, .05)],
                 .0012, lace, group)


def make_glove(rig: bpy.types.Object, leather: bpy.types.Material,
               lace: bpy.types.Material) -> bpy.types.Object:
    group = bpy.data.objects.new("FieldingGlove", None)
    bpy.context.collection.objects.link(group)
    vertices = [glove_outline(i * math.tau / 64, j / 12)
                for j in range(13) for i in range(64)]
    faces = [(j * 64 + i, j * 64 + (i + 1) % 64,
              (j + 1) * 64 + (i + 1) % 64, (j + 1) * 64 + i)
             for j in range(12) for i in range(64)]
    mesh = bpy.data.meshes.new("GlovePocket")
    mesh.from_pydata(vertices, [], faces)
    pocket = bpy.data.objects.new("GlovePocket", mesh)
    bpy.context.collection.objects.link(pocket)
    pocket.parent = group
    pocket.data.materials.append(leather)
    pocket.modifiers.new("LeatherThickness", "SOLIDIFY").thickness = .006
    for face in mesh.polygons:
        face.use_smooth = True
    seam("GloveBinding", [glove_outline(i * math.tau / 96) for i in range(96)],
         .003, leather, group, True)
    for index in range(40):
        angle = index * math.tau / 40
        seam("GloveLace", [glove_outline(angle - .017, .92),
                           glove_outline(angle + .017, 1.025)], .0014, lace, group)
    glove_fingers(group, leather, lace)
    wrist = rig.pose.bones["wrist.R"]
    world = rig.matrix_world @ wrist.matrix
    group.parent, group.parent_type, group.parent_bone = rig, "BONE", "wrist.R"
    bpy.context.view_layer.update()
    group.matrix_world = world
    return group


def make_ball(radius: float, white: bpy.types.Material, red: bpy.types.Material) -> bpy.types.Object:
    bpy.ops.mesh.primitive_uv_sphere_add(segments=32, ring_count=16, radius=radius)
    ball = bpy.context.object
    ball.name = "PitchBall"
    ball.data.materials.append(white)
    for face in ball.data.polygons:
        face.use_smooth = True
    for side in (-1, 1):
        points = []
        for index in range(96):
            angle = index * math.tau / 96
            position = Vector((.52 * side, math.cos(angle), math.sin(angle)))
            position.normalize()
            points.append(tuple(position * radius * 1.01))
        seam("BallStitching", points, radius * .018, red, ball, True)
    return ball
