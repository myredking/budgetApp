using UnityEditor;
using UnityEngine;

namespace DeckBall.Showcase.Editor
{
    internal static class DiamondMaterials
    {
        private const string Directory = "Assets/Showcase/Art/Diamond/";

        public static Material Get(string name, Color color)
        {
            var material = AssetDatabase.LoadAssetAtPath<Material>(Directory + name + ".mat");
            if (material == null)
            {
                material = new Material(Shader.Find("Universal Render Pipeline/Lit"));
                AssetDatabase.CreateAsset(material, Directory + name + ".mat");
            }
            material.SetColor("_BaseColor", color);
            material.SetFloat("_Smoothness", .15f);
            EditorUtility.SetDirty(material);
            return material;
        }

        public static Material Ground(string name, Color color, bool stripes)
        {
            var material = Get(name, Color.white);
            var path = Directory + name + "Texture.asset";
            var texture = AssetDatabase.LoadAssetAtPath<Texture2D>(path);
            if (texture == null)
            {
                texture = new Texture2D(256, 256, TextureFormat.RGB24, true);
                texture.wrapMode = TextureWrapMode.Repeat;
                texture.filterMode = FilterMode.Trilinear;
                texture.anisoLevel = 4;
                var pixels = new Color[256 * 256];
                for (var y = 0; y < 256; y++)
                for (var x = 0; x < 256; x++)
                {
                    var noise = .83f + Mathf.PerlinNoise(x * .24f, y * .24f) * .3f;
                    var stripe = stripes && y / 32 % 2 == 0 ? 1.14f : 1;
                    pixels[y * 256 + x] = color * noise * stripe;
                }
                texture.SetPixels(pixels); texture.Apply();
                AssetDatabase.CreateAsset(texture, path);
            }
            material.SetTexture("_BaseMap", texture);
            return material;
        }
    }
}
