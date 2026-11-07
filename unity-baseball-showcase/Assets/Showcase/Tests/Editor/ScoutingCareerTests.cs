using System;
using System.IO;
using System.Linq;
using NUnit.Framework;

public sealed class ScoutingCareerTests
{
    private Type type;
    private object career, book;
    private static Type Find(string name) => AppDomain.CurrentDomain.GetAssemblies().Select(a => a.GetType("DeckBall.Showcase." + name)).FirstOrDefault(t => t != null);
    private object Call(string name, params object[] args) => type.GetMethod(name).Invoke(career,args);
    private int Value(string name) => (int)type.GetProperty(name).GetValue(career);

    [SetUp]
    public void CreateCareer()
    {
        type = Find("ScoutingCareer");
        Assert.That(type,Is.Not.Null,"ScoutingCareer is missing");
        var json = File.ReadAllText(Path.GetFullPath("../android-baseball/data/roster-2026.json"));
        book = Find("RosterBook").GetMethod("Load").Invoke(null,new object[] { json });
        career = Activator.CreateInstance(type,new[] { book });
        Call("SetEnabled",true);
    }

    [Test]
    public void ResearchChargesOnceAndRecruitmentWaitsTwoDays()
    {
        var funds = Value("Funds");
        Assert.That(Call("Scout",1),Is.EqualTo(true));
        Assert.That(Value("Funds"),Is.EqualTo(funds-500));
        Assert.That(Call("Scout",1),Is.EqualTo(false));
        Assert.That(Value("Funds"),Is.EqualTo(funds-500));
        Assert.That(Call("Recruit",1),Is.EqualTo(false));
        Call("AdvanceDay"); Call("AdvanceDay");
        var price = (int)Call("Price",1);
        Assert.That(Call("Recruit",1),Is.EqualTo(true));
        Assert.That(Value("Funds"),Is.EqualTo(funds-500-price));
        Assert.That(Call("Owns",1),Is.EqualTo(true));
        Assert.That(Call("Recruit",1),Is.EqualTo(false));
    }

    [Test]
    public void EditingRatingsDoesNotDiscountRecruitmentAndInvalidIdDoesNotCharge()
    {
        var price = Call("Price",1);
        var card = book.GetType().GetMethod("Find").Invoke(book,new object[] { 1 });
        card.GetType().GetMethod("Edit").Invoke(card,new object[] { 1,1,1 });
        Assert.That(Call("Price",1),Is.EqualTo(price));
        var funds = Value("Funds");
        Assert.That(Call("Scout",999999),Is.EqualTo(false));
        Assert.That(Value("Funds"),Is.EqualTo(funds));
    }

    [Test]
    public void CompletedMatchRewardCannotBeClaimedTwiceOrForAnotherMatch()
    {
        var match = Call("StartMatch");
        var funds = Value("Funds");
        Assert.That(Call("ClaimReward",match,2),Is.EqualTo(true));
        Assert.That(Value("Funds"),Is.GreaterThan(funds));
        var rewarded = Value("Funds");
        Assert.That(Call("ClaimReward",match,2),Is.EqualTo(false));
        Call("StartMatch");
        Assert.That(Call("ClaimReward",match,2),Is.EqualTo(false));
        Assert.That(Value("Funds"),Is.EqualTo(rewarded));
    }

    [Test]
    public void FreshCareerRestoresPendingResearchAndInvalidSaveIsAtomic()
    {
        Call("Scout",1); Call("AdvanceDay");
        var funds = Value("Funds");
        var save = Call("Save");
        career = Activator.CreateInstance(type,new[] { book });
        Assert.That(Call("Restore",save),Is.EqualTo(true));
        Assert.That(Value("Funds"),Is.EqualTo(funds));
        Assert.That(Value("Day"),Is.EqualTo(2));
        Call("AdvanceDay");
        Assert.That(Call("Recruit",1),Is.EqualTo(true));
        var recruited = Value("Funds");
        Assert.That(Call("Restore","{\"version\":1,\"funds\":-1}"),Is.EqualTo(false));
        Assert.That(Value("Funds"),Is.EqualTo(recruited));
        Assert.That(Call("Owns",1),Is.EqualTo(true));
    }
}
