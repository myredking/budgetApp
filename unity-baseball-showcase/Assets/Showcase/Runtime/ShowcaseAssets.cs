using System.Collections.Generic;
using UnityEngine;

namespace DeckBall.Showcase
{
    [CreateAssetMenu(menuName = "DeckBall/Quality Shot Assets")]
    public sealed class ShowcaseAssets : ScriptableObject
    {
        public GameObject batter;
        public GameObject pitcher;
        public GameObject stadium;
        public GameObject bat;
        public GameObject ball;
        public AnimationClip batterIdle;
        public AnimationClip swing;
        public AnimationClip pitcherIdle;
        public AnimationClip pitch;
        public bool visualReviewPassed;
        public bool motionReviewPassed;

        public List<string> Problems(bool requireVisualReview = false)
        {
            var errors = new List<string>();
            CheckPlayer(batter, "Batter", errors);
            CheckPlayer(pitcher, "Pitcher", errors);
            if (stadium == null) errors.Add("Stadium prefab is missing.");
            if (bat == null || ball == null) errors.Add("Bat / Ball prefab is missing.");
            if (batterIdle == null || swing == null) errors.Add("Batter animation clips are missing.");
            if (pitcherIdle == null || pitch == null) errors.Add("Pitcher animation clips are missing.");
            if (requireVisualReview && !visualReviewPassed) errors.Add("Reference-level visual review is pending.");
            return errors;
        }

        private static void CheckPlayer(GameObject prefab, string label, List<string> errors)
        {
            if (prefab == null) { errors.Add(label + " prefab is missing."); return; }
            var skins = prefab.GetComponentsInChildren<SkinnedMeshRenderer>(true);
            if (skins.Length == 0) errors.Add(label + " has no skinned human mesh.");
            if (prefab.GetComponentInChildren<Animator>(true) == null)
                errors.Add(label + " has no Animator.");
        }
    }
}
