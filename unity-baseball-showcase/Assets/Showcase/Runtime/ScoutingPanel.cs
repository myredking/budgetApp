using System;
using UnityEngine;

namespace DeckBall.Showcase
{
    public sealed class ScoutingPanel
    {
        private BattingTrialGame game;
        private Func<float,float,float,float,Rect> rect;
        private Action<string> message;

        public void Draw(BattingTrialGame controller, PlayerCard card, Func<float,float,float,float,Rect> layout, GUIStyle text, Action<string> status)
        {
            game = controller; rect = layout; message = status;
            var career = game.Career;
            GUI.Label(rect(420,160,830,70),"운영 " + career.Day + "일 · 예산 " + career.Funds + "만원\n" + ReportStatus(card),text);
            if (GUI.Button(rect(420,242,400,90),career.Enabled ? "자유 편성으로 전환" : "구단 운영 시작")) ToggleMode();
            GUI.enabled = career.Enabled;
            if (GUI.Button(rect(840,242,400,90),"다음 날")) { career.AdvanceDay(); Persist("하루가 지났습니다."); }
            DrawResearch(card);
            DrawRecruit(card);
            GUI.enabled = true;
            var report = career.Owns(card.Id) || career.DaysLeft(card.Id) == 0;
            GUI.Label(rect(420,566,830,68),report ? "보고서 · 원본 컨택 " + card.OriginalContact + " · 파워 " + card.OriginalPower + " · 제구 " + card.OriginalControl
                : "조사비 500만원 · 게임 내 2일 후 보고서 완료\n실제 계약금과 무관한 게임용 예산입니다.",text);
        }

        private string ReportStatus(PlayerCard card)
        {
            var career = game.Career;
            if (career.Owns(card.Id)) return "보유 선수 · 경기 선택 가능";
            var days = career.DaysLeft(card.Id);
            if (days < 0) return "미조사 선수";
            return days == 0 ? "보고서 완료 · 영입 가능" : "보고서 완료까지 " + days + "일";
        }

        private void ToggleMode()
        {
            game.Career.SetEnabled(!game.Career.Enabled);
            Persist(game.Career.Enabled ? "구단 운영 · 보유 선수로 경기합니다." : "자유 편성 · 모든 카드를 선택할 수 있습니다.");
        }

        private void DrawResearch(PlayerCard card)
        {
            var career = game.Career;
            GUI.enabled = career.Enabled && !career.Owns(card.Id) && career.DaysLeft(card.Id) < 0 && career.Funds >= 500;
            if (GUI.Button(rect(420,350,820,90),"스카우트 조사 · 500만원"))
                Persist(career.Scout(card.Id) ? "조사 시작 · 이틀 뒤 보고서가 완료됩니다." : "현재 조사할 수 없습니다.");
        }

        private void DrawRecruit(PlayerCard card)
        {
            var career = game.Career;
            var price = career.Price(card.Id);
            GUI.enabled = career.Enabled && !career.Owns(card.Id) && career.DaysLeft(card.Id) == 0 && career.Funds >= price;
            if (GUI.Button(rect(420,458,820,90),"영입 · " + price + "만원 (원본 능력 기준)"))
                Persist(career.Recruit(card.Id) ? card.Name + " 영입 완료!" : "현재 영입할 수 없습니다.");
        }

        private void Persist(string status)
        {
            game.SaveRoster();
            message(status);
        }
    }
}
