using System;
using System.Linq;
using NUnit.Framework;
using UnityEngine;

public sealed class FieldPlayTests
{
    private static object Resolve(Vector3 velocity)
    {
        var type = AppDomain.CurrentDomain.GetAssemblies().Select(a => a.GetType("DeckBall.Showcase.FieldPlay")).FirstOrDefault(t => t != null);
        Assert.That(type, Is.Not.Null, "FieldPlay is missing");
        return type.GetMethod("Resolve").Invoke(null, new object[] { velocity });
    }

    private static int Bases(object result) => (int)result.GetType().GetField("Bases").GetValue(result);

    [Test]
    public void ShortGroundBallCanBeThrownOutButOverFenceIsHomeRun()
    {
        Assert.That(Bases(Resolve(new Vector3(0, -2, 18))), Is.EqualTo(0));
        var speed = 40f;
        var launch = new Vector3(0, Mathf.Sin(28 * Mathf.Deg2Rad), Mathf.Cos(28 * Mathf.Deg2Rad)) * speed;
        Assert.That(Bases(Resolve(launch)), Is.EqualTo(4));
    }

    [Test]
    public void FieldersHaveLegalIndexAndFiniteResolutionTime()
    {
        foreach (var velocity in new[] { new Vector3(0, 8, 20), new Vector3(12, 16, 30), new Vector3(-8, -3, 28) })
        {
            var result = Resolve(velocity);
            var type = result.GetType();
            Assert.That((int)type.GetField("Fielder").GetValue(result), Is.InRange(0, 7));
            Assert.That((float)type.GetField("Time").GetValue(result), Is.InRange(.1f, 20));
            Assert.That(Bases(result), Is.InRange(0, 4));
        }
    }
}
