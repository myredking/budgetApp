using System.IO;
using UnityEditor;
using UnityEngine;

namespace DeckBall.Showcase.Editor
{
    internal static class DiamondPreview
    {
        [MenuItem("DeckBall/Diamond Challenge/Start Preview")]
        public static void Start()
        {
            if (!EditorApplication.isPlaying) throw new System.InvalidOperationException("Enter Play mode first.");
            var game = Object.FindFirstObjectByType<BattingTrialGame>();
            game.StartChallenge(0);
            game.CycleSwingStyle();
            var report = "{\"width\":" + Screen.width + ",\"height\":" + Screen.height
                + ",\"physical_device\":false,\"input\":\"editor menu, not hardware touch\"}";
            File.WriteAllText("Reports/diamond-screen-check.json", report);
        }
    }
}
