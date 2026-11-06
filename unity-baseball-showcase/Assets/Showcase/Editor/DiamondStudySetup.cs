using System.IO;
using System.Linq;
using UnityEditor;
using UnityEditor.SceneManagement;
using UnityEngine;
using UnityEngine.Rendering;
using Object = UnityEngine.Object;

namespace DeckBall.Showcase.Editor
{
    public static class DiamondStudySetup
    {
        private const string Art = "Assets/Showcase/Art/";

        [MenuItem("DeckBall/Diamond Challenge/Build")]
        public static void Build()
        {
            if (!EditorSceneManager.SaveCurrentModifiedScenesIfUserWantsTo()) return;
            Directory.CreateDirectory(Art + "Diamond");
            Directory.CreateDirectory(Art + "Review");
            AssetDatabase.Refresh();
            PlayerStudySetup.ConfigurePipeline();
            var scene = EditorSceneManager.NewScene(NewSceneSetup.EmptyScene, NewSceneMode.Single);
            DiamondGround.Build();
            DiamondStrikeZone.Build();
            DiamondStadium.Build();
            Lighting();
            var batter = PlayerStudySetup.AddPlayer("kim_doyoung", new Vector3(1.0f, 0, 0), .75f);
            batter.transform.rotation = Quaternion.Euler(0, -90, 0);
            AlignBatter(batter);
            var pitcher = PlayerStudySetup.AddPlayer("ryu_hyunjin", new Vector3(0, .254f, 18.44f), 1.1f);
            pitcher.transform.rotation = Quaternion.Euler(0, 180, 0);
            var camera = CreateCamera();
            var controller = new GameObject("BattingTrial").AddComponent<BattingTrialGame>();
            controller.gameObject.AddComponent<TrialSound>();
            controller.batter = batter; controller.pitcher = pitcher; controller.gameCamera = camera;
            controller.swingClips = SwingClips(batter);
            var hud = controller.gameObject.AddComponent<TrialHud>();
            hud.game = controller;
            hud.font = AssetDatabase.LoadAssetAtPath<Font>(Art + "Fonts/NotoSansKR.ttf");
            hud.batterPortrait = Portrait(batter, .75f, "batter");
            hud.pitcherPortrait = Portrait(pitcher, 0, "pitcher");
            IncludeLicense();
            EditorSceneManager.SaveScene(scene, Art + "Diamond/DiamondChallenge.unity");
            EditorBuildSettings.scenes = new[] { new EditorBuildSettingsScene(Art + "Diamond/DiamondChallenge.unity", true) };
            PlayerSettings.defaultInterfaceOrientation = UIOrientation.LandscapeLeft;
            AssetDatabase.SaveAssets();
        }

        private static void AlignBatter(GameObject batter)
        {
            var clip = batter.GetComponent<Animation>().clip;
            clip.SampleAnimation(batter, 1.7f);
            var bat = batter.GetComponentsInChildren<Transform>().First(t => t.name == "Bat");
            var filter = bat.GetComponent<MeshFilter>();
            var center = bat.TransformPoint(filter.sharedMesh.bounds.center);
            var direction = (center - bat.position).normalized;
            var contact = bat.position + direction * .65f;
            var shift = new Vector3(0, .95f, .2f) - contact;
            shift.y = 0;
            batter.transform.position += shift;
            clip.SampleAnimation(batter, .75f);
        }

        private static AnimationClip[] SwingClips(GameObject batter)
        {
            var clips = new AnimationClip[4];
            clips[0] = batter.GetComponent<Animation>().clip;
            var suffixes = new[] { "push", "pull", "down" };
            for (var index = 0; index < suffixes.Length; index++)
            {
                var path = Art + "Players/kim_doyoung_" + suffixes[index] + ".fbx";
                var importer = AssetImporter.GetAtPath(path) as ModelImporter;
                if (importer == null) continue;
                importer.animationType = ModelImporterAnimationType.Legacy;
                importer.importAnimation = true;
                importer.SaveAndReimport();
                clips[index + 1] = AssetDatabase.LoadAllAssetsAtPath(path).OfType<AnimationClip>()
                    .First(c => !c.name.StartsWith("__preview__", System.StringComparison.Ordinal));
            }
            return clips;
        }

        private static void Lighting()
        {
            var sun = new GameObject("AfternoonSun").AddComponent<Light>();
            sun.type = LightType.Directional;
            sun.intensity = 1.5f;
            sun.color = new Color(1, .94f, .84f);
            sun.transform.rotation = Quaternion.Euler(42, -30, 0);
            sun.shadows = LightShadows.Soft;
            RenderSettings.sun = sun;
            RenderSettings.ambientMode = AmbientMode.Trilight;
            RenderSettings.ambientSkyColor = new Color(.55f, .68f, .82f);
            RenderSettings.ambientEquatorColor = new Color(.38f, .43f, .46f);
            RenderSettings.ambientGroundColor = new Color(.18f, .20f, .15f);
            var sky = AssetDatabase.LoadAssetAtPath<Material>(Art + "Diamond/Sky.mat");
            if (sky == null)
            {
                sky = new Material(Shader.Find("Skybox/Procedural"));
                AssetDatabase.CreateAsset(sky, Art + "Diamond/Sky.mat");
            }
            sky.SetColor("_SkyTint", new Color(.43f, .57f, .72f));
            RenderSettings.skybox = sky;
            RenderSettings.fog = true;
            RenderSettings.fogMode = FogMode.Linear;
            RenderSettings.fogStartDistance = 130;
            RenderSettings.fogEndDistance = 280;
            RenderSettings.fogColor = new Color(.65f, .73f, .78f);
        }

        private static Camera CreateCamera()
        {
            var camera = new GameObject("BattingCamera").AddComponent<Camera>();
            camera.tag = "MainCamera";
            camera.gameObject.AddComponent<AudioListener>();
            camera.transform.position = new Vector3(.1f, 1.65f, -3.8f);
            camera.transform.LookAt(new Vector3(0, 1.2f, 18.44f));
            camera.fieldOfView = 48;
            camera.nearClipPlane = .08f;
            camera.farClipPlane = 350;
            camera.allowHDR = true;
            camera.allowMSAA = true;
            return camera;
        }

        private static Texture2D Portrait(GameObject player, float time, string name)
        {
            var clone = Object.Instantiate(player);
            clone.transform.SetPositionAndRotation(new Vector3(5000, 0, 0), Quaternion.identity);
            clone.GetComponent<Animation>().clip.SampleAnimation(clone, time);
            var camera = new GameObject("PortraitCamera").AddComponent<Camera>();
            camera.transform.position = new Vector3(5000, 1.35f, 3);
            camera.transform.LookAt(new Vector3(5000, 1.35f, 0));
            camera.orthographic = true;
            camera.orthographicSize = .8f;
            camera.clearFlags = CameraClearFlags.SolidColor;
            camera.backgroundColor = new Color(.09f, .13f, .18f);
            camera.farClipPlane = 12;
            var target = new RenderTexture(256, 320, 24);
            camera.targetTexture = target;
            camera.Render();
            var previous = RenderTexture.active;
            RenderTexture.active = target;
            var texture = new Texture2D(256, 320, TextureFormat.RGB24, false);
            texture.ReadPixels(new Rect(0, 0, 256, 320), 0, 0); texture.Apply();
            var path = Art + "Diamond/" + name + "Portrait.png";
            File.WriteAllBytes(path, texture.EncodeToPNG());
            RenderTexture.active = previous;
            camera.targetTexture = null;
            target.Release(); Object.DestroyImmediate(target); Object.DestroyImmediate(texture);
            Object.DestroyImmediate(camera.gameObject); Object.DestroyImmediate(clone);
            AssetDatabase.ImportAsset(path, ImportAssetOptions.ForceUpdate);
            return AssetDatabase.LoadAssetAtPath<Texture2D>(path);
        }

        private static void IncludeLicense()
        {
            const string directory = "Assets/StreamingAssets/ThirdParty";
            Directory.CreateDirectory(directory);
            File.Copy(Art + "Fonts/OFL.txt", directory + "/NotoSansKR-OFL.txt", true);
        }
    }
}
