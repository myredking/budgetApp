using UnityEngine;
using Object = UnityEngine.Object;

namespace DeckBall.Showcase.Editor
{
    internal static class DiamondFielders
    {
        public static FieldPresentation Build(GameObject batter, GameObject pitcher)
        {
            var root = new GameObject("PracticeFielders").AddComponent<FieldPresentation>();
            root.fielders = new Transform[7];
            root.runners = new Transform[4];
            root.pitcher = pitcher.transform;
            for (var index = 0; index < root.fielders.Length; index++)
                root.fielders[index] = Clone(pitcher, root.transform, "PracticeFielder" + index);
            for (var index = 0; index < root.runners.Length; index++)
                root.runners[index] = Clone(batter, root.transform, "PracticeRunner" + index);
            root.ResetPlayers(null);
            return root;
        }

        private static Transform Clone(GameObject source, Transform parent, string name)
        {
            var clone = Object.Instantiate(source, parent);
            clone.name = name;
            var animation = clone.GetComponent<Animation>();
            animation.clip.SampleAnimation(clone, 0);
            animation.enabled = false;
            foreach (var child in clone.GetComponentsInChildren<Transform>())
                if (child.name == "PitchBall" || child.name == "Bat" || child.name == "JerseyNumber") child.gameObject.SetActive(false);
            return clone.transform;
        }
    }
}
