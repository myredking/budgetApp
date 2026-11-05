using System;
using System.Linq;
using NUnit.Framework;
using UnityEditor;
using UnityEditor.SceneManagement;
using UnityEngine;
using UnityEngine.SceneManagement;

public sealed class PlayerStudyTests
{
    private SceneSetup[] previousScenes;

    [SetUp]
    public void PreserveExistingScenes()
    {
        previousScenes = null;
        for (var index = 0; index < SceneManager.sceneCount; index++)
            if (SceneManager.GetSceneAt(index).isDirty)
                Assert.Ignore("Save existing scenes before running the local art integration test.");
        foreach (var id in new[] { "kim_doyoung", "ryu_hyunjin" })
            if (!System.IO.File.Exists("Assets/Showcase/Art/Players/" + id + ".fbx"))
                Assert.Ignore("This integration test requires the local art files documented in README.");
        previousScenes = EditorSceneManager.GetSceneManagerSetup();
    }

    [TearDown]
    public void RestoreExistingScenes()
    {
        if (previousScenes != null && previousScenes.Any(s => s.isLoaded && s.isActive && !string.IsNullOrEmpty(s.path)))
            EditorSceneManager.RestoreSceneManagerSetup(previousScenes);
    }

    [Test]
    public void ImportedPlayersHaveSkinningAndMovingAnimation()
    {
        var type = AppDomain.CurrentDomain.GetAssemblies()
            .Select(a => a.GetType("DeckBall.Showcase.Editor.PlayerStudySetup"))
            .FirstOrDefault(t => t != null);
        Assert.That(type, Is.Not.Null, "The player study scene builder is missing.");
        type.GetMethod("Build").Invoke(null, null);
        var floor = GameObject.Find("ReviewFloor").GetComponent<Renderer>();
        Assert.That(AssetDatabase.GetAssetPath(floor.sharedMaterial), Is.Not.Empty);
        foreach (var name in new[] { "kim_doyoung", "ryu_hyunjin" })
        {
            var player = GameObject.Find(name);
            Assert.That(player, Is.Not.Null);
            Assert.That(player.GetComponentsInChildren<SkinnedMeshRenderer>().Length, Is.GreaterThan(0));
            var skin = player.GetComponentsInChildren<Renderer>().First(r => r.name == "AthleteBody");
            Assert.That(skin.sharedMaterial.GetTexture("_BaseMap"), Is.Not.Null, "Skin texture must survive import.");
            var animation = player.GetComponent<Animation>();
            Assert.That(animation, Is.Not.Null);
            Assert.That(animation.clip.length, Is.GreaterThan(3));
            var bone = player.GetComponentsInChildren<Transform>().First(t => t.name == "wrist.L");
            animation.clip.SampleAnimation(player, .7f);
            var before = bone.position;
            animation.clip.SampleAnimation(player, 2f);
            Assert.That(Vector3.Distance(before, bone.position), Is.GreaterThan(.1f));
        }
    }
}
