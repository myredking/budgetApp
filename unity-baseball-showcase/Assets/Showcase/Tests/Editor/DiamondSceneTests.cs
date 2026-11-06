using System;
using System.Linq;
using NUnit.Framework;
using UnityEditor.SceneManagement;
using UnityEngine;
using UnityEngine.SceneManagement;

public sealed class DiamondSceneTests
{
    private SceneSetup[] previous;

    [SetUp]
    public void ProtectUserScenes()
    {
        previous = null;
        for (var index = 0; index < SceneManager.sceneCount; index++)
            if (SceneManager.GetSceneAt(index).isDirty) Assert.Ignore("Save existing scenes first.");
        if (!System.IO.File.Exists("Assets/Showcase/Art/Players/kim_doyoung.fbx")) Assert.Ignore("Local art required.");
        previous = EditorSceneManager.GetSceneManagerSetup();
    }

    [TearDown]
    public void RestoreUserScenes()
    {
        if (previous != null && previous.Any(s => s.isLoaded && s.isActive && !string.IsNullOrEmpty(s.path)))
            EditorSceneManager.RestoreSceneManagerSetup(previous);
    }

    [Test]
    public void ChallengeRunsTenPitchesPausesAndRestarts()
    {
        var builder = AppDomain.CurrentDomain.GetAssemblies().Select(a => a.GetType("DeckBall.Showcase.Editor.DiamondStudySetup"))
            .First(t => t != null);
        builder.GetMethod("Build").Invoke(null, null);
        var game = GameObject.Find("BattingTrial").GetComponent("BattingTrialGame");
        var type = game.GetType();
        var advance = type.GetMethod("Advance");
        Assert.That(advance, Is.Not.Null);
        type.GetMethod("Awake", System.Reflection.BindingFlags.NonPublic | System.Reflection.BindingFlags.Instance).Invoke(game, null);
        type.GetMethod("StartChallenge").Invoke(game, new object[] { 0 });
        type.GetMethod("TogglePause").Invoke(game, null);
        advance.Invoke(game, new object[] { 20f });
        Assert.That(type.GetProperty("Finished").GetValue(game), Is.EqualTo(false));
        Assert.That(type.GetProperty("FlightProgress").GetValue(game), Is.EqualTo(0f));
        var pausedSession = type.GetProperty("Session").GetValue(game);
        Assert.That(pausedSession.GetType().GetProperty("Pitches").GetValue(pausedSession), Is.EqualTo(0));
        type.GetMethod("TogglePause").Invoke(game, null);
        for (var index = 0; index < 10; index++)
        {
            advance.Invoke(game, new object[] { 2f });
            Assert.That(type.GetMethod("TrySwing").Invoke(game, null), Is.EqualTo(true));
            Assert.That(type.GetMethod("TrySwing").Invoke(game, null), Is.EqualTo(false));
            advance.Invoke(game, new object[] { 10f });
        }
        Assert.That(type.GetProperty("Finished").GetValue(game), Is.EqualTo(true));
        type.GetMethod("StartChallenge").Invoke(game, new object[] { 2 });
        Assert.That(type.GetProperty("Finished").GetValue(game), Is.EqualTo(false));
        var session = type.GetProperty("Session").GetValue(game);
        Assert.That(session.GetType().GetProperty("Pitches").GetValue(session), Is.EqualTo(0));
        Assert.That(Camera.main.transform.position.z, Is.EqualTo(-3.8f).Within(.01f));
    }

    [Test]
    public void SuccessfulHitScoresTracksBallAndRestoresBest()
    {
        const string key = "DeckBall.Trial.v1.0";
        var existed = PlayerPrefs.HasKey(key);
        var saved = PlayerPrefs.GetInt(key);
        try
        {
            PlayerPrefs.SetInt(key, 0);
            var builder = AppDomain.CurrentDomain.GetAssemblies().Select(a => a.GetType("DeckBall.Showcase.Editor.DiamondStudySetup")).First(t => t != null);
            builder.GetMethod("Build").Invoke(null, null);
            var game = GameObject.Find("BattingTrial").GetComponent("BattingTrialGame");
            var type = game.GetType();
            var flags = System.Reflection.BindingFlags.NonPublic | System.Reflection.BindingFlags.Instance;
            type.GetMethod("Awake", flags).Invoke(game, null);
            var sound = game.GetComponent("TrialSound");
            sound.GetType().GetMethod("Awake", flags).Invoke(sound, null);
            type.GetMethod("StartChallenge").Invoke(game, new object[] { 0 });
            var arrival = (float)type.GetField("arrival", flags).GetValue(game);
            type.GetMethod("Advance").Invoke(game, new object[] { arrival - .1f });
            Assert.That(type.GetMethod("TrySwing").Invoke(game, null), Is.EqualTo(true));
            var session = type.GetProperty("Session").GetValue(game);
            Assert.That(session.GetType().GetProperty("Score").GetValue(session), Is.EqualTo(100));
            type.GetMethod("Advance").Invoke(game, new object[] { .2f });
            var ball = GameObject.Find("ryu_hyunjin").GetComponentsInChildren<Transform>().First(t => t.name == "PitchBall");
            Assert.That(ball.position.z, Is.GreaterThan(.2f));
            type.GetMethod("StartChallenge").Invoke(game, new object[] { 0 });
            Assert.That(type.GetProperty("Best").GetValue(game), Is.EqualTo(100));
        }
        finally
        {
            if (existed) PlayerPrefs.SetInt(key, saved);
            else PlayerPrefs.DeleteKey(key);
            PlayerPrefs.Save();
        }
    }

    [Test]
    public void DiamondPlacesPitcherAndCameraTowardHome()
    {
        var type = AppDomain.CurrentDomain.GetAssemblies().Select(a => a.GetType("DeckBall.Showcase.Editor.DiamondStudySetup"))
            .FirstOrDefault(t => t != null);
        Assert.That(type, Is.Not.Null, "Diamond builder is missing.");
        type.GetMethod("Build").Invoke(null, null);
        var home = GameObject.Find("HomePlate").transform.position;
        var rubber = GameObject.Find("PitchingRubber").transform.position;
        Assert.That(Vector2.Distance(new Vector2(home.x, home.z), new Vector2(rubber.x, rubber.z)), Is.EqualTo(18.44f).Within(.01f));
        var pitcher = GameObject.Find("ryu_hyunjin");
        var ball = pitcher.GetComponentsInChildren<Transform>().First(t => t.name == "PitchBall");
        var clip = pitcher.GetComponent<Animation>().clip;
        clip.SampleAnimation(pitcher, 2f);
        var before = Vector3.Distance(ball.position, home);
        clip.SampleAnimation(pitcher, 2.1f);
        Assert.That(Vector3.Distance(ball.position, home), Is.LessThan(before - 2));
        Assert.That(GameObject.Find("BattingTrial"), Is.Not.Null);
        Assert.That(GameObject.Find("GrandstandSeatsBlue"), Is.Not.Null);
        var zone = GameObject.Find("StrikeZone");
        Assert.That(zone, Is.Not.Null);
        Assert.That(zone.GetComponentsInChildren<LineRenderer>().Length, Is.GreaterThanOrEqualTo(3));
        var viewport = Camera.main.WorldToViewportPoint(pitcher.transform.position + Vector3.up);
        Assert.That(viewport.x, Is.InRange(.25f, .75f));
        Assert.That(viewport.z, Is.GreaterThan(0));
    }
}
