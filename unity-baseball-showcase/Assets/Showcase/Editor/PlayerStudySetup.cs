using System;
using System.IO;
using System.Linq;
using UnityEditor;
using UnityEditor.SceneManagement;
using UnityEngine;
using UnityEngine.Rendering;
using UnityEngine.Rendering.Universal;
using Object = UnityEngine.Object;

namespace DeckBall.Showcase.Editor
{
    public static class PlayerStudySetup
    {
        private const string Art = "Assets/Showcase/Art/";
        private const string Review = Art + "Review/";

        [MenuItem("DeckBall/Player Study/Build")]
        public static void Build()
        {
            if (!EditorSceneManager.SaveCurrentModifiedScenesIfUserWantsTo()) return;
            Directory.CreateDirectory(Review);
            AssetDatabase.Refresh();
            ConfigurePipeline();
            var scene = EditorSceneManager.NewScene(NewSceneSetup.EmptyScene, NewSceneMode.Single);
            AddPlayer("kim_doyoung", new Vector3(-.9f, 0, 0), .75f);
            AddPlayer("ryu_hyunjin", new Vector3(.9f, 0, 0), 1.1f);
            CreateStage();
            EditorSceneManager.SaveScene(scene, Review + "PlayerStudy.unity");
            AssetDatabase.SaveAssets();
            Debug.Log("Player motion study assembled. Likeness and reference-quality approval remain pending.");
        }

        private static void ConfigurePipeline()
        {
            var pipeline = AssetDatabase.LoadAssetAtPath<UniversalRenderPipelineAsset>(Review + "Pipeline.asset");
            if (pipeline == null)
            {
                var renderer = ScriptableObject.CreateInstance<UniversalRendererData>();
                AssetDatabase.CreateAsset(renderer, Review + "Renderer.asset");
                pipeline = UniversalRenderPipelineAsset.Create(renderer);
                AssetDatabase.CreateAsset(pipeline, Review + "Pipeline.asset");
            }
            GraphicsSettings.defaultRenderPipeline = pipeline;
            QualitySettings.renderPipeline = pipeline;
            PlayerSettings.runInBackground = true;
        }

        private static void AddPlayer(string id, Vector3 position, float time)
        {
            var path = Art + "Players/" + id + ".fbx";
            var importer = AssetImporter.GetAtPath(path) as ModelImporter;
            if (importer == null) throw new FileNotFoundException(path);
            importer.animationType = ModelImporterAnimationType.Legacy;
            importer.importAnimation = true;
            importer.SaveAndReimport();
            var prefab = AssetDatabase.LoadAssetAtPath<GameObject>(path);
            var player = (GameObject)PrefabUtility.InstantiatePrefab(prefab);
            player.name = id;
            player.transform.position = position;
            var clip = AssetDatabase.LoadAllAssetsAtPath(path).OfType<AnimationClip>()
                .First(c => !c.name.StartsWith("__preview__", StringComparison.Ordinal));
            var animation = player.GetComponent<Animation>() ?? player.AddComponent<Animation>();
            animation.AddClip(clip, "Study");
            animation.clip = clip;
            animation.playAutomatically = true;
            animation.wrapMode = WrapMode.Loop;
            animation.cullingType = AnimationCullingType.AlwaysAnimate;
            ReplaceMaterials(player);
            clip.SampleAnimation(player, time);
        }

        private static void ReplaceMaterials(GameObject player)
        {
            foreach (var renderer in player.GetComponentsInChildren<Renderer>())
            {
                var materials = renderer.sharedMaterials.Select(original =>
                {
                    var path = Review + player.name + "-" + original.name.Replace('/', '_') + ".mat";
                    var mat = AssetDatabase.LoadAssetAtPath<Material>(path);
                    if (mat == null)
                    {
                        mat = new Material(Shader.Find("Universal Render Pipeline/Lit"));
                        AssetDatabase.CreateAsset(mat, path);
                    }
                    mat.SetColor("_BaseColor", original.color);
                    mat.SetTexture("_BaseMap", original.mainTexture);
                    mat.SetFloat("_Smoothness", original.name.Contains("Headwear") ? .55f : .2f);
                    ApplyTexture(renderer.name, mat);
                    EditorUtility.SetDirty(mat);
                    return mat;
                }).ToArray();
                renderer.sharedMaterials = materials;
            }
        }

        private static void ApplyTexture(string mesh, Material material)
        {
            string name = null;
            if (mesh == "AthleteBody") name = "skin";
            else if (mesh.Contains("shoes06")) name = "shoes";
            else if (mesh.Contains("high-poly")) name = "eyes";
            if (name == null) return;
            var texture = AssetDatabase.LoadAssetAtPath<Texture2D>(Art + "Textures/" + name + ".png");
            if (texture == null) throw new FileNotFoundException("Missing texture: " + name);
            material.SetTexture("_BaseMap", texture);
            material.SetColor("_BaseColor", Color.white);
            material.SetFloat("_Smoothness", .2f);
        }

        private static void CreateStage()
        {
            var floor = GameObject.CreatePrimitive(PrimitiveType.Plane);
            floor.name = "ReviewFloor";
            var material = AssetDatabase.LoadAssetAtPath<Material>(Review + "Floor.mat");
            if (material == null)
            {
                material = new Material(Shader.Find("Universal Render Pipeline/Lit"));
                AssetDatabase.CreateAsset(material, Review + "Floor.mat");
            }
            material.SetColor("_BaseColor", new Color(.08f, .10f, .13f));
            floor.GetComponent<Renderer>().sharedMaterial = material;
            var sun = new GameObject("KeyLight").AddComponent<Light>();
            sun.type = LightType.Directional;
            sun.intensity = 1.2f;
            sun.transform.rotation = Quaternion.Euler(40, 150, 0);
            sun.shadows = LightShadows.Soft;
            RenderSettings.ambientMode = AmbientMode.Flat;
            RenderSettings.ambientLight = new Color(.45f, .48f, .55f);
            var camera = new GameObject("StudyCamera").AddComponent<Camera>();
            camera.tag = "MainCamera";
            camera.transform.position = new Vector3(2.4f, 1.9f, 5.5f);
            camera.transform.LookAt(new Vector3(0, 1.15f, 0));
            camera.orthographic = true;
            camera.orthographicSize = 1.7f;
            camera.clearFlags = CameraClearFlags.SolidColor;
            camera.backgroundColor = new Color(.035f, .05f, .08f);
        }

        [MenuItem("DeckBall/Player Study/Capture")]
        public static void Capture()
        {
            var camera = Camera.main;
            if (camera == null) throw new InvalidOperationException("Build the study first.");
            var target = new RenderTexture(1920, 1080, 24);
            var previous = RenderTexture.active;
            var previousTarget = camera.targetTexture;
            var texture = new Texture2D(1920, 1080, TextureFormat.RGB24, false);
            try
            {
                camera.targetTexture = target;
                camera.Render();
                RenderTexture.active = target;
                texture.ReadPixels(new Rect(0, 0, 1920, 1080), 0, 0);
                texture.Apply();
                Directory.CreateDirectory("Reports");
                File.WriteAllBytes("Reports/player-study-unity.png", texture.EncodeToPNG());
            }
            finally
            {
                camera.targetTexture = previousTarget;
                RenderTexture.active = previous;
                target.Release();
                Object.DestroyImmediate(target);
                Object.DestroyImmediate(texture);
            }
        }
    }
}
