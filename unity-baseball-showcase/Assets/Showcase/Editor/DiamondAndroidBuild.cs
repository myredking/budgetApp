using System;
using System.IO;
using System.Linq;
using UnityEditor;
using UnityEditor.Build;
using UnityEditor.Build.Reporting;
using UnityEngine;
using UnityEngine.Rendering;

namespace DeckBall.Showcase.Editor
{
    public static class DiamondAndroidBuild
    {
        [Serializable] private sealed class Evidence
        {
            public string result, output;
            public long apkBytes, reportedBuildBytes;
            public int errors;
        }

        public static void Build()
        {
            Configure();
            var output = Output();
            Directory.CreateDirectory(Path.GetDirectoryName(output));
            var scenes = EditorBuildSettings.scenes.Where(s => s.enabled).Select(s => s.path).ToArray();
            if (scenes.Length == 0) throw new InvalidOperationException("Build the diamond scene first.");
            var report = BuildPipeline.BuildPlayer(new BuildPlayerOptions { scenes = scenes, target = BuildTarget.Android,
                locationPathName = output, options = BuildOptions.None });
            var summary = report.summary;
            var evidence = new Evidence { result = summary.result.ToString(), output = output,
                apkBytes = summary.result == BuildResult.Succeeded ? new FileInfo(output).Length : 0,
                reportedBuildBytes = (long)summary.totalSize, errors = (int)summary.totalErrors };
            File.WriteAllText("Reports/android-build.json", JsonUtility.ToJson(evidence,true));
            if (summary.result != BuildResult.Succeeded) throw new InvalidOperationException("Android build failed; see build log.");
        }

        private static void Configure()
        {
            PlayerSettings.companyName = "DeckBall";
            PlayerSettings.productName = "DeckBall 2026";
            PlayerSettings.bundleVersion = "0.5.0";
            PlayerSettings.SetApplicationIdentifier(NamedBuildTarget.Android,"com.deckball.unity2026");
            PlayerSettings.SetScriptingBackend(NamedBuildTarget.Android,ScriptingImplementation.IL2CPP);
            PlayerSettings.Android.targetArchitectures = AndroidArchitecture.ARM64;
            PlayerSettings.Android.applicationEntry = AndroidApplicationEntry.Activity;
            PlayerSettings.Android.minSdkVersion = AndroidSdkVersions.AndroidApiLevel26;
            PlayerSettings.Android.targetSdkVersion = AndroidSdkVersions.AndroidApiLevel36;
            PlayerSettings.SetUseDefaultGraphicsAPIs(BuildTarget.Android,false);
            PlayerSettings.SetGraphicsAPIs(BuildTarget.Android,new[] { GraphicsDeviceType.OpenGLES3 });
            PlayerSettings.defaultInterfaceOrientation = UIOrientation.LandscapeLeft;
            EditorUserBuildSettings.buildAppBundle = false;
        }

        private static string Output()
        {
            var arguments = Environment.GetCommandLineArgs();
            var index = Array.IndexOf(arguments,"-buildOutput");
            return Path.GetFullPath(index >= 0 && index + 1 < arguments.Length ? arguments[index+1] : "Builds/DeckBallUnity2026.apk");
        }
    }
}
