using UnityEditor;
using UnityEngine;

namespace DeckBall.Showcase.Editor
{
    internal static class DiamondStrikeZone
    {
        public static void Build()
        {
            var root = new GameObject("StrikeZone");
            const string path = "Assets/Showcase/Art/Diamond/StrikeZone.mat";
            var material = AssetDatabase.LoadAssetAtPath<Material>(path);
            if (material == null)
            {
                material = new Material(Shader.Find("Universal Render Pipeline/Unlit"));
                AssetDatabase.CreateAsset(material, path);
            }
            material.SetColor("_BaseColor", new Color(.48f, .88f, 1));
            Line(root, material, new[] { new Vector3(-.216f,.65f,.216f), new Vector3(.216f,.65f,.216f),
                new Vector3(.216f,1.35f,.216f), new Vector3(-.216f,1.35f,.216f), new Vector3(-.216f,.65f,.216f) });
            for (var index = 1; index <= 2; index++)
            {
                var x = -.216f + .432f * index / 3;
                var y = .65f + .7f * index / 3;
                Line(root, material, new[] { new Vector3(x,.65f,.216f), new Vector3(x,1.35f,.216f) });
                Line(root, material, new[] { new Vector3(-.216f,y,.216f), new Vector3(.216f,y,.216f) });
            }
        }

        private static void Line(GameObject root, Material material, Vector3[] points)
        {
            var line = new GameObject("ZoneLine").AddComponent<LineRenderer>();
            line.transform.SetParent(root.transform);
            line.sharedMaterial = material;
            line.positionCount = points.Length;
            line.SetPositions(points);
            line.widthMultiplier = .008f;
            line.shadowCastingMode = UnityEngine.Rendering.ShadowCastingMode.Off;
            line.receiveShadows = false;
        }
    }
}
