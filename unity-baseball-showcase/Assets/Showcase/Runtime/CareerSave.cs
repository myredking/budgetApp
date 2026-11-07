using System;
using System.Collections.Generic;
using System.Linq;

namespace DeckBall.Showcase
{
    [Serializable] internal sealed class ScoutReport { public int id, ready; }

    [Serializable] internal sealed class CareerSave
    {
        public int version = 1, day, funds, match, claimed;
        public bool enabled;
        public int[] owned;
        public ScoutReport[] reports;

        public static void Validate(CareerSave data, RosterBook book)
        {
            if (data == null || data.version != 1) throw new ArgumentException("저장 형식 오류");
            ValidateBounds(data);
            var known = new HashSet<int>(book.Cards.Select(p => p.Id));
            var owners = ValidateOwned(data.owned,book,known);
            ValidateReports(data.reports,known,owners);
        }

        private static void ValidateBounds(CareerSave data)
        {
            if (data.day < 1 || data.day > 1000000 || data.funds < 0 || data.funds > 1000000000) throw new ArgumentException("날짜/예산 오류");
            if (data.match < 0 || data.match > 1000000000 || data.claimed < 0 || data.claimed > data.match) throw new ArgumentException("경기 기록 오류");
        }

        private static HashSet<int> ValidateOwned(int[] ids, RosterBook book, HashSet<int> known)
        {
            if (ids == null || ids.Length > known.Count) throw new ArgumentException("보유 선수 오류");
            var result = new HashSet<int>(ids);
            if (result.Count != ids.Length || result.Any(id => !known.Contains(id))) throw new ArgumentException("선수 ID 오류");
            if (!book.Cards.Any(p => result.Contains(p.Id) && p.Pitcher)) throw new ArgumentException("투수가 필요합니다.");
            if (!book.Cards.Any(p => result.Contains(p.Id) && !p.Pitcher)) throw new ArgumentException("타자가 필요합니다.");
            return result;
        }

        private static void ValidateReports(ScoutReport[] reports, HashSet<int> known, HashSet<int> owned)
        {
            if (reports == null || reports.Length > known.Count) throw new ArgumentException("보고서 오류");
            var seen = new HashSet<int>();
            foreach (var report in reports)
            {
                if (report == null) throw new ArgumentException("빈 보고서");
                if (!known.Contains(report.id) || owned.Contains(report.id) || !seen.Add(report.id)) throw new ArgumentException("보고서 선수 오류");
                if (report.ready < 1 || report.ready > 1000002) throw new ArgumentException("조사 날짜 오류");
            }
        }
    }
}
