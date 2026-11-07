using System;
using UnityEngine;

namespace DeckBall.Showcase
{
    public sealed class RosterPanel : MonoBehaviour
    {
        public BattingTrialGame game;
        public bool Opened { get; private set; }
        private int page, selected;
        private bool scoutView;
        private readonly ScoutingPanel scouting = new ScoutingPanel();
        private float scale;
        private GUIStyle text, title;
        private string status = "능력치는 게임용 환산 수치입니다. 선수 외형은 공용 모델을 사용합니다.";
        private Rect R(float x, float y, float w, float h) => new Rect(x*scale,y*scale,w*scale,h*scale);

        public void Open() { Opened = true; selected = game.Roster.BatterId; }

        public void Draw(Rect safe, float uiScale, Font font)
        {
            scale = uiScale;
            if (text == null || text.fontSize != Mathf.RoundToInt(19*scale)) PrepareStyles(font);
            var color = GUI.color;
            GUI.color = new Color(.02f,.04f,.065f,1);
            GUI.DrawTexture(safe,Texture2D.whiteTexture);
            GUI.color = color;
            GUI.BeginGroup(new Rect(safe.center.x - 640*scale, safe.center.y - 350*scale, 1280*scale, 700*scale));
            if (GUI.Button(R(20,10,170,90), "저장 · 닫기")) { game.SaveRoster(); Opened = false; }
            DrawList();
            DrawCard(game.Roster.Find(selected));
            GUI.Label(R(420,642,830,52), status, text);
            GUI.EndGroup();
        }

        private void PrepareStyles(Font font)
        {
            text = new GUIStyle(GUI.skin.label) { font = font, fontSize = Mathf.RoundToInt(19*scale), wordWrap = true };
            title = new GUIStyle(text) { fontSize = Mathf.RoundToInt(26*scale), fontStyle = FontStyle.Bold };
        }

        private void DrawList()
        {
            if (GUI.Button(R(205,10,195,90),scoutView ? "능력치 보기" : "스카우트 보기")) scoutView = !scoutView;
            var cards = game.Roster.Cards;
            for (var row = 0; row < 5; row++)
            {
                var index = page*5 + row;
                if (index >= cards.Length) break;
                var card = cards[index];
                if (GUI.Button(R(20,104+row*96,380,90), card.Name + "  " + card.Team + "  " + card.Year)) selected = card.Id;
            }
            GUI.enabled = page > 0;
            if (GUI.Button(R(20,604,180,90), "이전")) page--;
            GUI.enabled = (page+1)*5 < cards.Length;
            if (GUI.Button(R(220,604,180,90), "다음")) page++;
            GUI.enabled = true;
        }

        private void DrawCard(PlayerCard card)
        {
            GUI.Label(R(420,14,600,65), card.Name + " · " + card.Team + " " + card.Year, title);
            if (GUI.Button(R(1040,10,210,90), card.Pitcher ? "투수로 선택" : "타자로 선택")) Select(card);
            GUI.Label(R(420,92,830,58), card.Record + "\n원본 기록 보존 · 공용 3D 모델", text);
            if (scoutView) { scouting.Draw(game,card,R,text,value => status = value); return; }
            var contact = Stat("컨택", card.Contact, 150);
            var power = Stat("파워", card.Power, 246);
            var control = Stat("제구", card.Control, 342);
            card.Edit(contact, power, control);
            DrawSkills(card);
        }

        private int Stat(string name, int value, int y)
        {
            GUI.Label(R(420,y+20,200,50), name + "  " + value, title);
            var changed = Mathf.RoundToInt(GUI.HorizontalSlider(R(650,y+32,350,40), value, 1, 99));
            if (GUI.Button(R(1030,y,96,90), "−")) changed--;
            if (GUI.Button(R(1145,y,96,90), "+")) changed++;
            return Mathf.Clamp(changed,1,99);
        }

        private void DrawSkills(PlayerCard card)
        {
            var labels = new[] { "정교 +6", "거포 +8", "해결사 +10", "제구 +8", "아우라 +3" };
            var allowed = card.Pitcher ? 24 : 23;
            for (var index = 0; index < labels.Length; index++)
            {
                var mask = 1 << index;
                GUI.enabled = (allowed & mask) != 0;
                var label = ((card.Skills & mask) != 0 ? "● " : "") + labels[index];
                if (GUI.Button(R(420+(index%3)*280,442+(index/3)*98,265,90),label)) ToggleSkill(card,mask);
            }
            GUI.enabled = true;
        }

        private void ToggleSkill(PlayerCard card, int mask)
        {
            try { card.SetSkills(card.Skills ^ mask); status = "특수 능력 적용 · 최대 2개"; }
            catch (ArgumentException error) { status = error.Message; }
        }

        private void Select(PlayerCard card)
        {
            if (game.Career != null && !game.Career.CanSelect(card.Id)) { status = "보유하지 않은 선수입니다. 조사·영입 후 선택하세요."; return; }
            if (card.Pitcher) game.Roster.SelectPitcher(card.Id);
            else game.Roster.SelectBatter(card.Id);
            game.SaveRoster();
            status = card.Name + " 선택 완료 · 다음 경기부터 적용됩니다.";
        }
    }
}
