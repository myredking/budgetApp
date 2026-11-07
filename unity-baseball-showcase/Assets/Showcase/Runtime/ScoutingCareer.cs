using System;
using System.Collections.Generic;
using System.Linq;
using UnityEngine;

namespace DeckBall.Showcase
{
    public sealed class ScoutingCareer
    {
        private readonly RosterBook book;
        private readonly HashSet<int> owned = new HashSet<int>();
        private readonly Dictionary<int,int> reports = new Dictionary<int,int>();
        private int match, claimed;
        public bool Enabled { get; private set; }
        public int Funds { get; private set; } = 25000;
        public int Day { get; private set; } = 1;

        public ScoutingCareer(RosterBook roster)
        {
            book = roster;
            owned.Add(book.BatterId); owned.Add(book.PitcherId);
        }

        public bool Owns(int id) => owned.Contains(id);
        public bool CanSelect(int id) => !Enabled || Owns(id);
        public int DaysLeft(int id) => reports.TryGetValue(id,out var ready) ? Math.Max(0,ready-Day) : -1;

        public void SetEnabled(bool enabled)
        {
            Enabled = enabled;
            if (!enabled) return;
            if (!Owns(book.BatterId)) book.SelectBatter(book.Cards.First(p => !p.Pitcher && Owns(p.Id)).Id);
            if (!Owns(book.PitcherId)) book.SelectPitcher(book.Cards.First(p => p.Pitcher && Owns(p.Id)).Id);
        }

        public int Price(int id)
        {
            var card = book.Cards.FirstOrDefault(p => p.Id == id);
            if (card == null) return -1;
            var rating = card.Pitcher ? card.OriginalControl : (card.OriginalContact+card.OriginalPower)/2;
            return 500+rating*rating;
        }

        public bool Scout(int id)
        {
            if (!Enabled || Price(id) < 0 || Owns(id) || reports.ContainsKey(id) || Funds < 500 || Day > 999998) return false;
            Funds -= 500;
            reports.Add(id,Day+2);
            return true;
        }

        public void AdvanceDay() { Day = Math.Min(1000000,Day+1); }

        public bool Recruit(int id)
        {
            var price = Price(id);
            if (!Enabled || price < 0 || Owns(id) || DaysLeft(id) != 0 || Funds < price) return false;
            Funds -= price;
            owned.Add(id);
            reports.Remove(id);
            return true;
        }

        public int StartMatch()
        {
            if (!Enabled || match >= 1000000000) return 0;
            return ++match;
        }

        public bool ClaimReward(int token, int runs)
        {
            if (!Enabled || token <= 0 || token != match || token <= claimed || runs < 0) return false;
            Funds = Math.Min(1000000000,Funds+800+Math.Min(21,runs)*200);
            claimed = token;
            AdvanceDay();
            return true;
        }

        public string Save()
        {
            var data = new CareerSave { enabled = Enabled, funds = Funds, day = Day, match = match, claimed = claimed,
                owned = owned.OrderBy(id => id).ToArray(), reports = reports.OrderBy(p => p.Key)
                    .Select(p => new ScoutReport { id = p.Key, ready = p.Value }).ToArray() };
            return JsonUtility.ToJson(data);
        }

        public bool Restore(string json)
        {
            try
            {
                var data = JsonUtility.FromJson<CareerSave>(json);
                CareerSave.Validate(data,book);
                Funds = data.funds; Day = data.day; match = data.match; claimed = data.claimed;
                owned.Clear(); reports.Clear();
                foreach (var id in data.owned) owned.Add(id);
                foreach (var report in data.reports) reports.Add(report.id,report.ready);
                SetEnabled(data.enabled);
                return true;
            }
            catch (ArgumentException) { return false; }
            catch (InvalidOperationException) { return false; }
        }
    }
}
