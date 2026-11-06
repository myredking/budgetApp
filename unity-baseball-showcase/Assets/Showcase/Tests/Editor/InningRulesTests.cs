using System;
using System.Linq;
using NUnit.Framework;

public sealed class InningRulesTests
{
    private object inning;
    private Type type;

    [SetUp]
    public void CreateInning()
    {
        type = AppDomain.CurrentDomain.GetAssemblies().Select(a => a.GetType("DeckBall.Showcase.InningSession")).FirstOrDefault(t => t != null);
        Assert.That(type, Is.Not.Null, "InningSession is missing");
        inning = Activator.CreateInstance(type);
    }

    private int Value(string name) => (int)type.GetProperty(name).GetValue(inning);
    private void Pitch(bool strike, bool swing, bool foul) => type.GetMethod("Pitch").Invoke(inning, new object[] { strike, swing, foul });
    private void Hit(int bases) => type.GetMethod("Hit").Invoke(inning, new object[] { bases });

    [Test]
    public void TwoStrikeFoulsDoNotStrikeOutButThirdMissDoes()
    {
        Pitch(true, true, false); Pitch(true, false, false);
        Pitch(true, true, true); Pitch(true, true, true);
        Assert.That(Value("Strikes"), Is.EqualTo(2));
        Assert.That(Value("Outs"), Is.EqualTo(0));
        Pitch(false, true, false);
        Assert.That(Value("Outs"), Is.EqualTo(1));
        Assert.That(Value("Strikes"), Is.EqualTo(0));
    }

    [Test]
    public void WalkOnlyMovesForcedRunnersAndLoadedWalkScoresOne()
    {
        Hit(2);
        for (var count = 0; count < 4; count++) Pitch(false, false, false);
        Assert.That(Value("Bases"), Is.EqualTo(3));
        for (var count = 0; count < 8; count++) Pitch(false, false, false);
        Assert.That(Value("Bases"), Is.EqualTo(7));
        Assert.That(Value("Runs"), Is.EqualTo(1));
    }

    [Test]
    public void HomeRunScoresAllRunnersAndClearsBases()
    {
        Hit(1); Hit(1); Hit(1); Hit(4);
        Assert.That(Value("Runs"), Is.EqualTo(4));
        Assert.That(Value("Bases"), Is.EqualTo(0));
        Assert.That(Value("Hits"), Is.EqualTo(4));
    }

    [Test]
    public void ThirdOutEndsInningAndRejectsFurtherActions()
    {
        for (var count = 0; count < 3; count++) type.GetMethod("Out").Invoke(inning, null);
        Hit(4); Pitch(false, false, false);
        Assert.That(Value("Outs"), Is.EqualTo(3));
        Assert.That(Value("Runs"), Is.EqualTo(0));
        Assert.That((bool)type.GetProperty("Complete").GetValue(inning), Is.True);
    }
}
