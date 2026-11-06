using System;
using System.Linq;
using NUnit.Framework;
using UnityEngine;

public sealed class BattingTrialTests
{
    private static Type Find(string name)
    {
        var type = AppDomain.CurrentDomain.GetAssemblies().Select(a => a.GetType("DeckBall.Showcase." + name))
            .FirstOrDefault(t => t != null);
        Assert.That(type, Is.Not.Null, name + " is missing");
        return type;
    }

    [Test]
    public void TimingDifficultyAndPowerHaveClearTradeoffs()
    {
        var method = Find("TrialRules").GetMethod("Grade");
        Assert.That(method.Invoke(null, new object[] { 0f, 1, false }), Is.EqualTo(3));
        Assert.That(method.Invoke(null, new object[] { .1f, 0, false }), Is.Not.EqualTo(0));
        Assert.That(method.Invoke(null, new object[] { .1f, 2, false }), Is.EqualTo(0));
        Assert.That(method.Invoke(null, new object[] { .1f, 1, true }), Is.EqualTo(0));
    }

    [Test]
    public void OnePitchCannotScoreTwiceAndTenPitchesEndChallenge()
    {
        var type = Find("BattingSession");
        var session = Activator.CreateInstance(type, 1);
        var resolve = type.GetMethod("TryResolve");
        Assert.That(resolve.Invoke(session, new object[] { 0f, false }), Is.True);
        var score = type.GetProperty("Score").GetValue(session);
        Assert.That(resolve.Invoke(session, new object[] { 0f, true }), Is.False);
        Assert.That(type.GetProperty("Score").GetValue(session), Is.EqualTo(score));
        for (var index = 1; index < 10; index++)
        {
            type.GetMethod("BeginPitch").Invoke(session, null);
            resolve.Invoke(session, new object[] { 0f, false });
        }
        Assert.That(type.GetProperty("Complete").GetValue(session), Is.True);
        type.GetMethod("BeginPitch").Invoke(session, null);
        Assert.That(resolve.Invoke(session, new object[] { 0f, false }), Is.False);
    }

    [TestCase(2340, 1080, 120, 56, 2220, 1024)]
    [TestCase(2340, 1080, 0, 56, 2220, 1024)]
    [TestCase(1920, 1080, 0, 0, 1920, 1080)]
    public void LandscapeButtonsStayInsideSafeAreaAndDoNotOverlap(int width, int height,
        int x, int y, int safeWidth, int safeHeight)
    {
        var type = Find("TrialHudLayout");
        var layout = type.GetMethod("Create").Invoke(null,
            new object[] { width, height, new Rect(x, y, safeWidth, safeHeight), 411f });
        var safeGui = new Rect(x, height - y - safeHeight, safeWidth, safeHeight);
        var swing = (Rect)type.GetField("Swing").GetValue(layout);
        var power = (Rect)type.GetField("Power").GetValue(layout);
        foreach (var name in new[] { "Swing", "Power", "Style", "Menu", "Card", "TopBar" })
        {
            var rect = (Rect)type.GetField(name).GetValue(layout);
            Assert.That(safeGui.Contains(rect.min) && safeGui.Contains(rect.max - Vector2.one), Is.True, name);
        }
        Assert.That(swing.Overlaps(power), Is.False);
        Assert.That(swing.height, Is.GreaterThanOrEqualTo(48 * 411f / 160));
    }

    [Test]
    public void TouchUsesBottomLeftScreenCoordinatesAndIgnoresCutout()
    {
        var type = Find("TrialHudLayout");
        var layout = type.GetMethod("Create").Invoke(null,
            new object[] { 2340, 1080, new Rect(120, 56, 2220, 1024), 411f });
        var button = (Rect)type.GetField("Swing").GetValue(layout);
        var point = new Vector2(button.center.x, 1080 - button.center.y);
        Assert.That(type.GetMethod("HitTest").Invoke(layout, new object[] { point, 1080 }), Is.EqualTo(1));
        Assert.That(type.GetMethod("HitTest").Invoke(layout, new object[] { new Vector2(20, 500), 1080 }), Is.EqualTo(0));
    }

    [Test]
    public void SwingReachesContactWithinOneTenthSecond()
    {
        var method = Find("TrialRules").GetMethod("SwingClipTime");
        Assert.That(method, Is.Not.Null);
        var start = (float)method.Invoke(null, new object[] { 0f, .1f, false });
        var after = (float)method.Invoke(null, new object[] { .1f, 0f, false });
        Assert.That(start, Is.LessThan(1.7f));
        Assert.That(after, Is.GreaterThanOrEqualTo(1.7f));
    }

    [Test]
    public void TouchSwingStylesChangeDirectionAndGroundBallAngle()
    {
        var method = Find("TrialRules").GetMethod("LaunchVelocity");
        Assert.That(method, Is.Not.Null);
        var push = (Vector3)method.Invoke(null, new object[] { 3, false, 1, 0f });
        var pull = (Vector3)method.Invoke(null, new object[] { 3, false, 2, 0f });
        var down = (Vector3)method.Invoke(null, new object[] { 3, false, 3, 0f });
        Assert.That(push.x, Is.GreaterThan(0));
        Assert.That(pull.x, Is.LessThan(0));
        Assert.That(down.y, Is.LessThan(0));
    }

    [Test]
    public void HomeRunUsesCurvedFenceAndExcludesFoulAndLowBalls()
    {
        var method = Find("TrialRules").GetMethod("OverFence");
        Assert.That(method, Is.Not.Null);
        Assert.That(method.Invoke(null, new object[] { new Vector3(0, 3, 126) }), Is.EqualTo(true));
        Assert.That(method.Invoke(null, new object[] { new Vector3(42, 3.2f, 109) }), Is.EqualTo(true));
        Assert.That(method.Invoke(null, new object[] { new Vector3(42, 2, 109) }), Is.EqualTo(false));
        Assert.That(method.Invoke(null, new object[] { new Vector3(130, 5, 30) }), Is.EqualTo(false));
    }

    [Test]
    public void EditedContactWidensWindowAndEditedPowerChangesExitSpeed()
    {
        var rules = Find("TrialRules");
        var window = rules.GetMethod("PlayerWindow");
        var power = rules.GetMethod("AdjustPower");
        Assert.That(window, Is.Not.Null); Assert.That(power, Is.Not.Null);
        var low = (float)window.Invoke(null, new object[] { 1, false, 20 });
        var high = (float)window.Invoke(null, new object[] { 1, false, 99 });
        Assert.That(high, Is.GreaterThan(low));
        var weak = (Vector3)power.Invoke(null, new object[] { new Vector3(0,15,30), 20 });
        var strong = (Vector3)power.Invoke(null, new object[] { new Vector3(0,15,30), 99 });
        Assert.That(strong.magnitude, Is.GreaterThan(weak.magnitude));
    }

    [Test]
    public void ImpactSoundHasFiniteAudibleSamplesAndQuietEnds()
    {
        var method = Find("TrialSound").GetMethod("CreateImpact");
        var clip = (AudioClip)method.Invoke(null, null);
        var samples = new float[clip.samples];
        clip.GetData(samples, 0);
        Assert.That(samples.All(v => !float.IsNaN(v) && Mathf.Abs(v) <= 1), Is.True);
        Assert.That(samples.Sum(v => v * v) / samples.Length, Is.GreaterThan(.001f));
        Assert.That(Mathf.Abs(samples[0]), Is.LessThan(.02f));
        Assert.That(Mathf.Abs(samples[samples.Length - 1]), Is.LessThan(.02f));
        UnityEngine.Object.DestroyImmediate(clip);
    }
}
