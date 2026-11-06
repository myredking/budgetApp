using System;
using UnityEngine;

namespace DeckBall.Showcase
{
    public static class TrialRules
    {
        public const float SwingLead = .10f;
        public static float FenceRadius(float angle) => Mathf.Lerp(125, 105, Mathf.Abs(angle) / 45);

        public static bool OverFence(Vector3 position)
        {
            var angle = Mathf.Atan2(position.x, position.z) * Mathf.Rad2Deg;
            return Mathf.Abs(angle) <= 45 && position.y > 2.7f
                && new Vector2(position.x, position.z).magnitude > FenceRadius(angle);
        }

        public static float Window(int level, bool power)
        {
            if (level < 0 || level > 2) throw new ArgumentOutOfRangeException(nameof(level));
            return new[] { .18f, .12f, .08f }[level] * (power ? .7f : 1);
        }

        public static float PlayerWindow(int level, bool power, int contact) => Window(level, power)
            * Mathf.Lerp(.8f, 1.2f, Mathf.Clamp(contact,1,110) / 110f);

        public static Vector3 AdjustPower(Vector3 velocity, int power) => velocity
            * Mathf.Lerp(.85f, 1.15f, Mathf.Clamp(power,1,110) / 110f);

        public static int Grade(float error, int level, bool power)
        {
            if (float.IsNaN(error) || float.IsInfinity(error)) throw new ArgumentOutOfRangeException(nameof(error));
            var ratio = Mathf.Abs(error) / Window(level, power);
            if (ratio <= .2f) return 3;
            if (ratio <= .55f) return 2;
            return ratio <= 1 ? 1 : 0;
        }

        public static float SwingClipTime(float sinceInput, float untilArrival, bool hit)
        {
            return Mathf.Clamp(hit ? 1.7f - untilArrival * 2 : 1.5f + sinceInput * 2, .75f, 2.6f);
        }

        public static Vector3 LaunchVelocity(int grade, bool power, int style, float timingRatio)
        {
            if (style < 0 || style > 3) throw new ArgumentOutOfRangeException(nameof(style));
            if (grade < 1 || grade > 3) throw new ArgumentOutOfRangeException(nameof(grade));
            var speed = new[] { 0f, 20f, 30f, 36f }[grade] + (power ? 4 : 0);
            var angle = new[] { 28f, 24f, 30f, -6f }[style] * Mathf.Deg2Rad;
            var side = Mathf.Clamp(timingRatio, -1, 1) * .35f;
            if (style == 1) side = .38f;
            if (style == 2) side = -.38f;
            return new Vector3(side, 0, 1).normalized * (speed * Mathf.Cos(angle))
                + Vector3.up * (speed * Mathf.Sin(angle));
        }
    }

    public sealed class BattingSession
    {
        public int Level { get; }
        private readonly int limit;
        public int Score { get; private set; }
        public int Pitches { get; private set; }
        public int Combo { get; private set; }
        public int Hits { get; private set; }
        public int Perfects { get; private set; }
        public int LastGrade { get; private set; }
        public bool LastPower { get; private set; }
        public bool Resolved { get; private set; }
        public bool Complete => Pitches >= limit;

        public BattingSession(int level) : this(level, 10) { }

        public BattingSession(int level, int pitchLimit)
        {
            TrialRules.Window(level, false);
            Level = level;
            if (pitchLimit < 1) throw new ArgumentOutOfRangeException(nameof(pitchLimit));
            limit = pitchLimit;
        }

        public void BeginPitch()
        {
            if (!Complete) Resolved = false;
        }

        public bool TryResolve(float error, bool power)
        {
            if (Complete || Resolved) return false;
            LastGrade = TrialRules.Grade(error, Level, power);
            LastPower = power;
            Combo = LastGrade == 0 ? 0 : Combo + 1;
            if (LastGrade > 0) Hits++;
            if (LastGrade == 3) Perfects++;
            var points = new[] { 0, 25, 60, 100 }[LastGrade];
            Score += Mathf.RoundToInt(points * (power ? 1.4f : 1) * (1 + Mathf.Min(4, Mathf.Max(0, Combo - 1)) * .1f));
            Pitches++;
            Resolved = true;
            return true;
        }
    }
}
