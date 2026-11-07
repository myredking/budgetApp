using System;
using UnityEngine;

namespace DeckBall.Showcase
{
    [Serializable]
    public sealed class PlayerRecord
    {
        public int id, year, contact, power, control;
        public string name, team, position, record, source;
        public bool legend;
    }

    public sealed class PlayerCard
    {
        public int Id { get; }
        public string Name { get; }
        public string Team { get; }
        public string Position { get; }
        public string Record { get; }
        public string Source { get; }
        public int Year { get; }
        public bool Legend { get; }
        public bool Pitcher => Position == "P";
        public int OriginalContact { get; }
        public int OriginalPower { get; }
        public int OriginalControl { get; }
        public int Contact { get; private set; }
        public int Power { get; private set; }
        public int Control { get; private set; }
        public int Skills { get; private set; }
        public int EffectivePower => Math.Min(110, Power + Bonus(2, 8) + Bonus(16, 3));
        public int EffectiveControl => Math.Min(110, Control + Bonus(8, 8) + Bonus(16, 3));

        public PlayerCard(PlayerRecord record)
        {
            Id = record.id; Name = record.name; Team = record.team; Position = record.position;
            Record = record.record; Source = record.source; Year = record.year; Legend = record.legend;
            OriginalContact = record.contact;
            OriginalPower = record.power; OriginalControl = record.control;
            Edit(record.contact, record.power, record.control);
        }

        public void Edit(int contact, int power, int control)
        {
            if (contact < 1 || contact > 99) throw new ArgumentOutOfRangeException(nameof(contact));
            if (power < 1 || power > 99) throw new ArgumentOutOfRangeException(nameof(power));
            if (control < 1 || control > 99) throw new ArgumentOutOfRangeException(nameof(control));
            Contact = contact; Power = power; Control = control;
        }

        public void SetSkills(int mask)
        {
            var allowed = Pitcher ? 24 : 23;
            if (mask < 0 || (mask & ~allowed) != 0) throw new ArgumentOutOfRangeException(nameof(mask));
            var count = 0;
            for (var bit = 1; bit <= 16; bit <<= 1) if ((mask & bit) != 0) count++;
            if (count > 2) throw new ArgumentException("특수 능력은 최대 2개입니다.");
            Skills = mask;
        }

        private int Bonus(int mask, int amount) => (Skills & mask) != 0 ? amount : 0;
        public int EffectiveContact(bool scoring) => Math.Min(110, Contact + Bonus(1, 6) + Bonus(16, 3) + (scoring ? Bonus(4, 10) : 0));
    }
}
