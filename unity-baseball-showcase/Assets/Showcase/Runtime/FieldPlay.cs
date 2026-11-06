using UnityEngine;

namespace DeckBall.Showcase
{
    public sealed class FieldResult
    {
        public int Bases, Fielder;
        public float Time, CollectTime;
        public Vector3 Position;
        public bool FlyOut;
    }

    public static class FieldPlay
    {
        public static readonly Vector3[] Positions = {
            new Vector3(19,0,19), new Vector3(11,0,30), new Vector3(-11,0,30),
            new Vector3(-19,0,19), new Vector3(-37,0,72), new Vector3(0,0,90), new Vector3(37,0,72), new Vector3(0,.254f,18.44f) };

        public static Vector3 Flight(Vector3 velocity, float time) => new Vector3(0,.95f,.2f)
            + velocity * time - Vector3.up * (4.905f * time * time);

        public static FieldResult Resolve(Vector3 velocity)
        {
            var ground = (velocity.y + Mathf.Sqrt(velocity.y * velocity.y + 18.639f)) / 9.81f;
            for (var time = .1f; time < ground; time += .04f)
            {
                var position = Flight(velocity, time);
                if (TrialRules.OverFence(position)) return new FieldResult { Bases = 4, Time = time, Position = position };
                var index = Nearest(position);
                var reach = TravelTime(index, position);
                if (time > reach && position.y < 2.5f && position.y > .2f)
                    return new FieldResult { Fielder = index, Time = time, CollectTime = time, Position = position, FlyOut = true };
            }
            return Ground(velocity, ground);
        }

        public static int Nearest(Vector3 point)
        {
            var best = 0;
            for (var index = 1; index < Positions.Length; index++)
                if (Horizontal(point - Positions[index]) < Horizontal(point - Positions[best])) best = index;
            return best;
        }

        private static float Horizontal(Vector3 delta) => new Vector2(delta.x, delta.z).magnitude;
        private static float TravelTime(int index, Vector3 point) => .3f + Mathf.Max(0, Horizontal(point - Positions[index]) - 1.2f) / 6.2f;

        private static FieldResult Ground(Vector3 velocity, float time)
        {
            var position = Flight(velocity, time);
            position.y = 0;
            position += new Vector3(velocity.x, 0, velocity.z) * .35f;
            var index = Nearest(position);
            var collect = Mathf.Max(time, TravelTime(index, position));
            var returning = collect + .6f + Horizontal(position - Positions[0]) / 25;
            var bases = returning < 27.43f / 7.5f ? 0 : Mathf.Clamp(Mathf.FloorToInt(returning / (27.43f / 7.5f)), 1, 3);
            return new FieldResult { Bases = bases, Fielder = index, Position = position, Time = returning, CollectTime = collect };
        }
    }
}
