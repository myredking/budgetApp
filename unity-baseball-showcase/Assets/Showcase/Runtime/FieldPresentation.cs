using UnityEngine;

namespace DeckBall.Showcase
{
    public sealed class FieldPresentation : MonoBehaviour
    {
        public Transform[] fielders;
        public Transform[] runners;
        public Transform pitcher;
        public static readonly Vector3[] Bases = { Vector3.zero, new Vector3(19.397f,0,19.397f),
            new Vector3(0,0,38.794f), new Vector3(-19.397f,0,19.397f), Vector3.zero };

        public void ResetPlayers(InningSession inning)
        {
            for (var index = 0; index < fielders.Length; index++)
            {
                fielders[index].gameObject.SetActive(inning != null);
                fielders[index].position = FieldPlay.Positions[index];
                fielders[index].LookAt(Vector3.zero);
            }
            pitcher.position = FieldPlay.Positions[7];
            pitcher.rotation = Quaternion.Euler(0, 180, 0);
            for (var index = 0; index < runners.Length; index++)
            {
                var visible = inning != null && index > 0 && (inning.Bases & (1 << (index - 1))) != 0;
                runners[index].gameObject.SetActive(visible);
                runners[index].position = Bases[index];
            }
        }

        public void Play(FieldResult result, float elapsed)
        {
            var fielder = result.Fielder == 7 ? pitcher : fielders[result.Fielder];
            var target = result.Position;
            target.y = 0;
            fielder.position = Vector3.MoveTowards(FieldPlay.Positions[result.Fielder], target, Mathf.Max(0, elapsed - .3f) * 6.2f);
            fielder.LookAt(new Vector3(0, 0, .2f));
            runners[0].gameObject.SetActive(true);
            var progress = Mathf.Min(elapsed * 7.5f / 27.43f, Mathf.Max(1, result.Bases));
            MoveRunner(runners[0], progress);
            for (var index = 1; index < runners.Length; index++)
                if (runners[index].gameObject.activeSelf) MoveRunner(runners[index], Mathf.Min(index + progress, 4));
        }

        private static void MoveRunner(Transform runner, float progress)
        {
            var leg = Mathf.Clamp(Mathf.FloorToInt(progress), 0, 3);
            runner.position = Vector3.Lerp(Bases[leg], Bases[leg + 1], progress - leg);
            runner.LookAt(Bases[leg + 1] + new Vector3(.01f, 0, .01f));
        }
    }
}
