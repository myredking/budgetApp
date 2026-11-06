using System;
using System.Collections.Generic;
using System.Linq;
using UnityEngine;

namespace DeckBall.Showcase
{
    public sealed class RosterBook
    {
        [Serializable] private sealed class SourceData { public int season; public PlayerRecord[] players; }
        [Serializable] private sealed class EditData { public int id, contact, power, control, skills; }
        [Serializable] private sealed class SaveData { public int version = 1, batter, pitcher; public EditData[] edits; }
        public PlayerCard[] Cards { get; private set; }
        public int BatterId { get; private set; }
        public int PitcherId { get; private set; }
        public PlayerCard Batter => Find(BatterId);
        public PlayerCard Pitcher => Find(PitcherId);

        public static RosterBook Load(string json)
        {
            var source = JsonUtility.FromJson<SourceData>(json);
            if (source == null || source.season != 2026 || source.players == null) throw new ArgumentException("2026 선수 데이터가 필요합니다.");
            var cards = source.players.Select(p => new PlayerCard(p)).ToArray();
            if (cards.Select(p => p.Id).Distinct().Count() != cards.Length) throw new ArgumentException("중복 선수 ID");
            return new RosterBook { Cards = cards, BatterId = cards.First(p => !p.Pitcher).Id, PitcherId = cards.First(p => p.Pitcher).Id };
        }

        public PlayerCard Find(int id) => Cards.First(p => p.Id == id);
        public void SelectBatter(int id)
        {
            if (Find(id).Pitcher) throw new ArgumentException("타자를 선택하세요.");
            BatterId = id;
        }
        public void SelectPitcher(int id)
        {
            if (!Find(id).Pitcher) throw new ArgumentException("투수를 선택하세요.");
            PitcherId = id;
        }

        public string Save()
        {
            var data = new SaveData { batter = BatterId, pitcher = PitcherId,
                edits = Cards.Select(p => new EditData { id = p.Id, contact = p.Contact, power = p.Power, control = p.Control, skills = p.Skills }).ToArray() };
            return JsonUtility.ToJson(data);
        }

        public bool Restore(string json)
        {
            try
            {
                var data = JsonUtility.FromJson<SaveData>(json);
                ValidateSave(data);
                foreach (var edit in data.edits)
                {
                    var card = Find(edit.id);
                    card.Edit(edit.contact, edit.power, edit.control);
                    card.SetSkills(edit.skills);
                }
                BatterId = data.batter; PitcherId = data.pitcher;
                return true;
            }
            catch (ArgumentException) { return false; }
            catch (InvalidOperationException) { return false; }
        }

        private void ValidateSave(SaveData data)
        {
            if (data == null || data.version != 1 || data.edits == null) throw new ArgumentException("저장 형식 오류");
            if (Find(data.batter).Pitcher || !Find(data.pitcher).Pitcher) throw new ArgumentException("선택 선수 역할 오류");
            var seen = new HashSet<int>();
            foreach (var edit in data.edits)
            {
                if (edit == null) throw new ArgumentException("빈 선수 데이터");
                if (!seen.Add(edit.id)) throw new ArgumentException("중복 선수");
                var current = Find(edit.id);
                var probe = new PlayerCard(new PlayerRecord { position = current.Position, contact = edit.contact, power = edit.power, control = edit.control });
                probe.SetSkills(edit.skills);
            }
        }
    }
}
