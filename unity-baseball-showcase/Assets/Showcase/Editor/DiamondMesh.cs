using System.Collections.Generic;
using UnityEditor;
using UnityEngine;
using UnityEngine.Rendering;

namespace DeckBall.Showcase.Editor
{
    internal sealed class DiamondMesh
    {
        private readonly List<Vector3> vertices = new List<Vector3>();
        private readonly List<int> triangles = new List<int>();
        private readonly List<Vector2> uv = new List<Vector2>();

        public void Quad(Vector3 a, Vector3 b, Vector3 c, Vector3 d)
        {
            var start = vertices.Count;
            vertices.AddRange(new[] { a, b, c, d });
            foreach (var point in new[] { a, b, c, d }) uv.Add(new Vector2(point.x * .05f, point.z * .05f));
            triangles.AddRange(new[] { start, start + 1, start + 2, start, start + 2, start + 3 });
        }

        public void Box(Vector3 center, Vector3 size, Quaternion rotation)
        {
            var half = size * .5f;
            var p = new Vector3[8];
            for (var index = 0; index < 8; index++)
                p[index] = center + rotation * new Vector3((index & 1) == 0 ? -half.x : half.x,
                    (index & 2) == 0 ? -half.y : half.y, (index & 4) == 0 ? -half.z : half.z);
            Quad(p[0], p[4], p[6], p[2]); Quad(p[1], p[3], p[7], p[5]);
            Quad(p[0], p[1], p[5], p[4]); Quad(p[2], p[6], p[7], p[3]);
            Quad(p[0], p[2], p[3], p[1]); Quad(p[4], p[5], p[7], p[6]);
        }

        public GameObject Finish(string name, Material material)
        {
            const string directory = "Assets/Showcase/Art/Diamond/";
            var mesh = AssetDatabase.LoadAssetAtPath<Mesh>(directory + name + ".asset");
            if (mesh == null) { mesh = new Mesh(); AssetDatabase.CreateAsset(mesh, directory + name + ".asset"); }
            mesh.Clear();
            mesh.indexFormat = IndexFormat.UInt32;
            mesh.SetVertices(vertices); mesh.SetTriangles(triangles, 0); mesh.SetUVs(0, uv);
            mesh.RecalculateNormals(); mesh.RecalculateBounds();
            EditorUtility.SetDirty(mesh);
            var obj = new GameObject(name);
            obj.AddComponent<MeshFilter>().sharedMesh = mesh;
            obj.AddComponent<MeshRenderer>().sharedMaterial = material;
            return obj;
        }

        public void Disc(Vector3 center, float radius, float height)
        {
            for (var segment = 0; segment < 96; segment++)
            {
                var a = segment * Mathf.PI * 2 / 96;
                var b = (segment + 1) * Mathf.PI * 2 / 96;
                var edgeA = center + new Vector3(Mathf.Sin(a), 0, Mathf.Cos(a)) * radius;
                var edgeB = center + new Vector3(Mathf.Sin(b), 0, Mathf.Cos(b)) * radius;
                Quad(center + Vector3.up * height, edgeA, edgeB, center + Vector3.up * height);
            }
        }
    }
}
