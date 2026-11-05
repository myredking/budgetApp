using System;
using System.IO;
using UnityEditor;
using UnityEditor.SceneManagement;
using UnityEngine;
using UnityEngine.Rendering;
using UnityEngine.Rendering.Universal;

namespace DeckBall.Showcase.Editor
{
    public static class QualityShotSetup
    {
        private const string Bindings = "Assets/Showcase/ShowcaseAssets.asset";

        [MenuItem("DeckBall/Quality Shot/Create Asset Bindings")]
        public static void CreateBindings()
        {
            var assets = AssetDatabase.LoadAssetAtPath<ShowcaseAssets>(Bindings);
            if (assets == null)
            {
                assets = ScriptableObject.CreateInstance<ShowcaseAssets>();
                AssetDatabase.CreateAsset(assets, Bindings);
            }
            Selection.activeObject = assets;
            EditorGUIUtility.PingObject(assets);
        }

        [Serializable]
        private sealed class ReviewReport
        {
            public bool passed;
            public bool visual_review_passed;
            public bool motion_review_passed;
            public string[] issues;
        }

        [MenuItem("DeckBall/Quality Shot/Write Readiness Report")]
        public static void WriteReport()
        {
            var assets = AssetDatabase.LoadAssetAtPath<ShowcaseAssets>(Bindings);
            if (assets == null) throw new InvalidOperationException("Create asset bindings first.");
            var errors = assets.Problems();
            if (!(GraphicsSettings.currentRenderPipeline is UniversalRenderPipelineAsset)) errors.Add("URP is not configured.");
            var report = new ReviewReport
            {
                passed = errors.Count == 0,
                visual_review_passed = assets.visualReviewPassed,
                motion_review_passed = assets.motionReviewPassed,
                issues = errors.ToArray()
            };
            Directory.CreateDirectory("Reports");
            File.WriteAllText("Reports/editor-validation.json", JsonUtility.ToJson(report, true));
        }

        [MenuItem("DeckBall/Quality Shot/Build Visual Review Scene")]
        public static void BuildScene()
        {
            var assets = AssetDatabase.LoadAssetAtPath<ShowcaseAssets>(Bindings);
            if (assets == null) throw new InvalidOperationException("Create and assign art bindings first.");
            var problems = assets.Problems();
            if (!(GraphicsSettings.currentRenderPipeline is UniversalRenderPipelineAsset)) problems.Add("Configure the URP pipeline first.");
            if (problems.Count > 0) throw new InvalidOperationException(string.Join("\n", problems));
            if (!EditorSceneManager.SaveCurrentModifiedScenesIfUserWantsTo()) return;
            var scene = EditorSceneManager.NewScene(NewSceneSetup.EmptyScene, NewSceneMode.Single);
            Spawn(assets.stadium, "Stadium", Vector3.zero, 0);
            Spawn(assets.batter, "Batter", new Vector3(-1.05f, 0, 0), 90);
            Spawn(assets.pitcher, "Pitcher", new Vector3(0, 0, 18.44f), 180);
            CreateCamera(); CreateSun();
            Directory.CreateDirectory("Assets/Showcase/Scenes");
            EditorSceneManager.SaveScene(scene, "Assets/Showcase/Scenes/VisualReview.unity");
            Debug.Log("Visual review scene assembled. Animation and Android validation are still required.");
        }

        private static void Spawn(GameObject prefab, string name, Vector3 position, float yaw)
        {
            var instance = (GameObject)PrefabUtility.InstantiatePrefab(prefab);
            instance.name = name;
            instance.transform.SetPositionAndRotation(position, Quaternion.Euler(0, yaw, 0));
        }

        private static void CreateCamera()
        {
            var camera = new GameObject("BroadcastCamera").AddComponent<Camera>();
            camera.tag = "MainCamera";
            camera.transform.position = new Vector3(-.25f, 1.6f, -4.5f);
            camera.transform.LookAt(new Vector3(0, 1.3f, 18.44f));
            camera.fieldOfView = 48; camera.nearClipPlane = .08f; camera.farClipPlane = 250;
            camera.allowHDR = true; camera.allowMSAA = true;
        }

        private static void CreateSun()
        {
            var sun = new GameObject("AfternoonSun").AddComponent<Light>();
            sun.type = LightType.Directional; sun.intensity = 2.2f;
            sun.color = new Color(1, .95f, .88f); sun.shadows = LightShadows.Soft;
            sun.transform.rotation = Quaternion.Euler(38, -35, 0);
            RenderSettings.sun = sun;
            RenderSettings.ambientMode = AmbientMode.Trilight;
            RenderSettings.ambientSkyColor = new Color(.54f, .65f, .78f);
            RenderSettings.ambientEquatorColor = new Color(.38f, .42f, .46f);
            RenderSettings.ambientGroundColor = new Color(.19f, .17f, .13f);
        }
    }
}
