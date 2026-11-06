using System;
using System.IO;
using System.Linq;
using NUnit.Framework;

public sealed class RosterBookTests
{
    private Type type;
    private object book;
    private string source;

    [SetUp]
    public void LoadBook()
    {
        type = AppDomain.CurrentDomain.GetAssemblies().Select(a => a.GetType("DeckBall.Showcase.RosterBook")).FirstOrDefault(t => t != null);
        Assert.That(type, Is.Not.Null, "RosterBook is missing");
        source = File.ReadAllText(Path.GetFullPath("../android-baseball/data/roster-2026.json"));
        book = type.GetMethod("Load").Invoke(null, new object[] { source });
    }

    private object Card(int id) => type.GetMethod("Find").Invoke(book, new object[] { id });
    private static int Stat(object card, string name) => (int)card.GetType().GetProperty(name).GetValue(card);

    [Test]
    public void SnapshotContainsCurrentAndHistoricalCardsAndEditingKeepsSourceRecord()
    {
        var cards = (Array)type.GetProperty("Cards").GetValue(book);
        Assert.That(cards.Length, Is.EqualTo(33));
        var card = Card(0);
        var record = card.GetType().GetProperty("Record").GetValue(card);
        card.GetType().GetMethod("Edit").Invoke(card, new object[] { 50, 80, 40 });
        Assert.That(Stat(card, "Contact"), Is.EqualTo(50));
        Assert.That(Stat(card, "OriginalContact"), Is.EqualTo(92));
        Assert.That(card.GetType().GetProperty("Record").GetValue(card), Is.EqualTo(record));
        Assert.That(Card(32).GetType().GetProperty("Legend").GetValue(Card(32)), Is.EqualTo(true));
    }

    [Test]
    public void InvalidEditIsAtomicAndSkillsRespectRoleAndTwoSlotLimit()
    {
        var card = Card(0);
        Assert.Throws<System.Reflection.TargetInvocationException>(() => card.GetType().GetMethod("Edit").Invoke(card, new object[] { 50, 100, 40 }));
        Assert.That(Stat(card, "Contact"), Is.EqualTo(92));
        Assert.Throws<System.Reflection.TargetInvocationException>(() => card.GetType().GetMethod("SetSkills").Invoke(card, new object[] { 8 }));
        Assert.Throws<System.Reflection.TargetInvocationException>(() => card.GetType().GetMethod("SetSkills").Invoke(card, new object[] { 7 }));
        card.GetType().GetMethod("SetSkills").Invoke(card, new object[] { 3 });
        Assert.That(Stat(card, "EffectivePower"), Is.EqualTo(70));
    }

    [Test]
    public void SaveRestorePreservesEditsAndSelectedPlayers()
    {
        var card = Card(0);
        card.GetType().GetMethod("Edit").Invoke(card, new object[] { 77, 88, 40 });
        card.GetType().GetMethod("SetSkills").Invoke(card, new object[] { 3 });
        type.GetMethod("SelectBatter").Invoke(book, new object[] { 7 });
        type.GetMethod("SelectPitcher").Invoke(book, new object[] { 29 });
        var save = type.GetMethod("Save").Invoke(book, null);
        book = type.GetMethod("Load").Invoke(null, new object[] { source });
        var restored = type.GetMethod("Restore").Invoke(book, new[] { save });
        Assert.That(restored, Is.EqualTo(true));
        Assert.That(Stat(Card(0), "Power"), Is.EqualTo(88));
        Assert.That(Stat(Card(0), "Skills"), Is.EqualTo(3));
        Assert.That((int)type.GetProperty("BatterId").GetValue(book), Is.EqualTo(7));
        Assert.That((int)type.GetProperty("PitcherId").GetValue(book), Is.EqualTo(29));
        Assert.That(type.GetMethod("Restore").Invoke(book, new object[] { "{bad" }), Is.EqualTo(false));
        Assert.That(Stat(Card(0), "Power"), Is.EqualTo(88));
    }

    [Test]
    public void InvalidLaterEditAndWrongRolesDoNotPartiallyMutateBook()
    {
        const string invalid = "{\"version\":1,\"batter\":7,\"pitcher\":29,\"edits\":[{\"id\":0,\"contact\":77,\"power\":88,\"control\":40,\"skills\":3},{\"id\":1,\"contact\":91,\"power\":100,\"control\":40,\"skills\":0}]}";
        Assert.That(type.GetMethod("Restore").Invoke(book,new object[] { invalid }),Is.EqualTo(false));
        Assert.That(Stat(Card(0),"Contact"),Is.EqualTo(92));
        Assert.That(Stat(Card(0),"Power"),Is.EqualTo(62));
        var wrongRole = invalid.Replace("\"batter\":7,\"pitcher\":29", "\"batter\":29,\"pitcher\":7").Replace("\"power\":100","\"power\":66");
        Assert.That(type.GetMethod("Restore").Invoke(book,new object[] { wrongRole }),Is.EqualTo(false));
        Assert.That(Stat(Card(0),"Contact"),Is.EqualTo(92));
        Assert.That((int)type.GetProperty("BatterId").GetValue(book),Is.EqualTo(0));
    }
}
