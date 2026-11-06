using UnityEngine;

namespace DeckBall.Showcase
{
    public sealed class TrialHud : MonoBehaviour
    {
        public BattingTrialGame game;
        public Font font;
        public Texture2D batterPortrait, pitcherPortrait;
        public RosterPanel roster;
        public bool previewSafeArea;
        public Vector4 previewInsets = new Vector4(120, 0, 0, 56);
        private int level = 1, lastInputFrame = -1;
        private bool inningMode = true;
        private float pressedUntil, styleScale;
        private GUIStyle text, centered, heading;
        private TrialHudLayout layout;
        private readonly Color navy = new Color(.025f, .055f, .09f, .93f);
        private readonly Color accent = new Color(.18f, .79f, .88f, 1);

        private TrialHudLayout Layout()
        {
            var safe = Screen.safeArea;
            if (previewSafeArea)
                safe = new Rect(previewInsets.x, previewInsets.w, Screen.width - previewInsets.x - previewInsets.z,
                                Screen.height - previewInsets.y - previewInsets.w);
            return TrialHudLayout.Create(Screen.width, Screen.height, safe, Screen.dpi);
        }

        private void Update()
        {
            layout = Layout();
            if (layout.Portrait) return;
            if (roster != null && roster.Opened) return;
            if (Input.GetKeyDown(KeyCode.Escape)) game.TogglePause();
            if (!game.Started || game.Paused || game.Finished) return;
            if (Input.GetKeyDown(KeyCode.Space)) Activate(1);
            ReadPointer();
        }

        private void ReadPointer()
        {
            if (Input.touchCount > 0)
            {
                for (var index = 0; index < Input.touchCount; index++)
                {
                    var touch = Input.GetTouch(index);
                    if (touch.phase == TouchPhase.Began) Activate(layout.HitTest(touch.position, Screen.height));
                }
            }
            else if (Input.GetMouseButtonDown(0)) Activate(layout.HitTest(Input.mousePosition, Screen.height));
        }

        private void Activate(int action)
        {
            if (action == 0 || lastInputFrame == Time.frameCount) return;
            lastInputFrame = Time.frameCount;
            if (action == 1) { game.TrySwing(); pressedUntil = Time.unscaledTime + .1f; }
            if (action == 2) game.TogglePower();
            if (action == 3) game.TogglePause();
            if (action == 4) game.CycleSwingStyle();
        }

        private void PrepareStyles()
        {
            GUI.skin.font = font != null ? font : GUI.skin.font;
            if (text != null && Mathf.Abs(styleScale - layout.Scale) < .001f) return;
            styleScale = layout.Scale;
            text = new GUIStyle(GUI.skin.label) { fontSize = Mathf.RoundToInt(20 * layout.Scale), wordWrap = true, padding = new RectOffset() };
            text.normal.textColor = Color.white;
            centered = new GUIStyle(text) { alignment = TextAnchor.MiddleCenter };
            heading = new GUIStyle(centered) { fontSize = Mathf.RoundToInt(25 * layout.Scale), fontStyle = FontStyle.Bold };
            GUI.skin.button.fontSize = text.fontSize;
        }

        private void OnGUI()
        {
            if (game == null || game.Session == null) return;
            layout = Layout();
            PrepareStyles();
            if (roster != null && roster.Opened) { roster.Draw(layout.Safe,layout.Scale,font); return; }
            if (layout.Portrait) { PortraitMessage(); return; }
            DrawScore();
            DrawBatterCard();
            DrawPitcherCard();
            Panel(layout.Menu, navy);
            GUI.Label(layout.Menu, "Ⅱ  메뉴", centered);
            DrawControls();
            DrawFeedback();
            if (!game.Started || game.Paused || game.Finished) { DrawMenu(); DrawRosterButton(); }
        }

        private void DrawBatterCard()
        {
            var hitter = game.Roster?.Batter;
            DrawCard(layout.Card, batterPortrait, hitter != null ? hitter.Team + " · " + hitter.Year : "KIA  ·  5", hitter?.Name ?? "김도영", hitter?.Position ?? "우타 · 내야수", new Color(.68f, .04f, .10f),
                     "안타 " + (game.Inning != null ? game.Inning.Hits : game.Session.Hits) + "    정타 " + game.Session.Perfects);
        }

        private void DrawPitcherCard()
        {
            var thrower = game.Roster?.Pitcher;
            DrawCard(layout.PitcherCard, pitcherPortrait, thrower != null ? thrower.Team + " · " + thrower.Year : "HANWHA  ·  99", thrower?.Name ?? "류현진", "투수 · 공용 모델", new Color(.95f, .37f, .08f),
                     "연습구  " + Mathf.RoundToInt(game.PitchSpeed * 3.6f) + " km/h");
        }

        private void DrawRosterButton()
        {
            if (roster == null || game.Roster == null) return;
            GUI.enabled = !game.Paused;
            var s = layout.Scale;
            if (GUI.Button(new Rect(layout.Safe.x + 18*s,layout.Safe.yMax-102*s,330*s,84*s),"선수 선택 · 능력치 편집")) roster.Open();
            GUI.enabled = true;
        }

        private void Panel(Rect rect, Color color)
        {
            var old = GUI.color;
            GUI.color = color;
            GUI.DrawTexture(rect, Texture2D.whiteTexture);
            GUI.color = old;
        }

        private void DrawScore()
        {
            var r = layout.TopBar;
            Panel(r, navy);
            Panel(new Rect(r.x, r.y, 5 * layout.Scale, r.height), accent);
            if (game.Inning != null) { DrawInningScore(r); return; }
            var row = new Rect(r.x + 16 * layout.Scale, r.y + 8 * layout.Scale, r.width - 28 * layout.Scale, 36 * layout.Scale);
            GUI.Label(row, "DECKBALL 2026  |  10구 타격 챌린지", text);
            row.y += 34 * layout.Scale;
            GUI.Label(row, "점수  " + game.Session.Score.ToString("00000") + "     " + game.Session.Pitches + " / 10", text);
            row.y += 34 * layout.Scale;
            GUI.Label(row, "최고 " + game.Best + "       연속 성공  " + game.Session.Combo, text);
        }

        private void DrawInningScore(Rect r)
        {
            var s = layout.Scale;
            var row = new Rect(r.x + 16*s, r.y + 8*s, r.width - 28*s, 36*s);
            GUI.Label(row, "1회 초   " + (game.Roster?.Batter.Team ?? "KIA") + " vs " + (game.Roster?.Pitcher.Team ?? "HANWHA"), text);
            row.y += 34*s;
            GUI.Label(row, "득점 " + game.Inning.Runs + "   안타 " + game.Inning.Hits + "   " + game.Inning.Outs + " 아웃", text);
            row.y += 34*s;
            var bases = game.Inning.Bases;
            GUI.Label(row, "B " + game.Inning.Balls + "  S " + game.Inning.Strikes + "   주자 "
                + ((bases & 1) != 0 ? "1 " : "") + ((bases & 2) != 0 ? "2 " : "") + ((bases & 4) != 0 ? "3" : ""), text);
        }

        private void DrawCard(Rect r, Texture2D portrait, string club, string name, string role, Color color, string stats)
        {
            Panel(new Rect(r.x - 2, r.y - 2, r.width + 4, r.height + 4), new Color(.66f, .72f, .75f, .85f));
            Panel(r, navy);
            var s = layout.Scale;
            GUI.Label(new Rect(r.x + 10 * s, r.y + 3 * s, r.width - 16 * s, 31 * s), club, text);
            if (portrait != null) GUI.DrawTexture(new Rect(r.x + 4 * s, r.y + 34 * s, r.width - 8 * s, 154 * s), portrait, ScaleMode.ScaleAndCrop);
            GUI.Label(new Rect(r.x + 8*s,r.y + 158*s,r.width - 16*s,30*s),"공용 3D 모델",text);
            Panel(new Rect(r.x, r.y + 188 * s, r.width, 38 * s), color);
            GUI.Label(new Rect(r.x, r.y + 188 * s, r.width, 38 * s), name, heading);
            GUI.Label(new Rect(r.x + 8 * s, r.y + 231 * s, r.width - 12 * s, 33 * s), role, text);
            GUI.Label(new Rect(r.x + 8 * s, r.y + 270 * s, r.width - 12 * s, 33 * s), stats, text);
        }

        private void DrawControls()
        {
            var active = game.Started && !game.Paused && !game.Finished && !game.Session.Resolved;
            var flash = Time.unscaledTime < pressedUntil;
            Panel(layout.Swing, active ? (flash ? Color.white : accent) : new Color(.2f, .24f, .28f, .8f));
            var previous = heading.normal.textColor;
            heading.normal.textColor = new Color(.025f, .07f, .10f);
            GUI.Label(layout.Swing, game.Session.Resolved ? "다음 공 준비" : "스윙", heading);
            heading.normal.textColor = previous;
            Panel(layout.Power, game.Power ? new Color(.86f, .24f, .09f, .95f) : navy);
            GUI.Label(layout.Power, game.Power ? "파워 ON" : "정확한 타격", centered);
            Panel(layout.Style, navy);
            GUI.Label(layout.Style, "타법: " + new[] { "기본", "밀어치기", "당겨치기", "내려치기" }[game.SwingStyle], centered);
        }

        private void DrawFeedback()
        {
            if (!game.Started) return;
            var r = layout.Feedback;
            Panel(r, new Color(.02f, .04f, .07f, .65f));
            GUI.Label(new Rect(r.x, r.y, r.width, r.height * .65f), game.Feedback, heading);
            var detail = game.EstimatedDistance > 0 ? "예상 비거리  " + Mathf.RoundToInt(game.EstimatedDistance) + "m" : "터치 순간 반응  ·  PC에서는 SPACE";
            GUI.Label(new Rect(r.x, r.y + r.height * .65f, r.width, r.height * .35f), detail, centered);
        }

        private void DrawMenu()
        {
            var p = layout.Panel;
            Panel(p, new Color(.02f, .04f, .065f, .98f));
            var s = layout.Scale;
            var title = MenuTitle();
            GUI.Label(new Rect(p.x + 20 * s, p.y + 22 * s, p.width - 40 * s, 55 * s), title, heading);
            GUI.Label(new Rect(p.x + 38 * s, p.y + 90 * s, p.width - 76 * s, 76 * s),
                MenuDescription(), centered);
            DrawMode(p, s);
            DrawDifficulty(p, s);
            var start = new Rect(p.x + 40 * s, p.y + 365 * s, p.width - 80 * s, 90 * s);
            if (GUI.Button(start, game.Paused ? "계속 플레이" : game.Finished ? "다시 도전" : "플레이 시작")) StartSelected();
        }

        private void DrawDifficulty(Rect p, float s)
        {
            var names = new[] { "입문", "보통", "도전" };
            GUI.enabled = !game.Paused;
            for (var index = 0; index < 3; index++)
            {
                var button = new Rect(p.x + (40 + index * 185) * s, p.y + 265 * s, 170 * s, 74 * s);
                GUI.backgroundColor = index == level ? accent : Color.gray;
                if (GUI.Button(button, names[index])) level = index;
            }
            GUI.backgroundColor = Color.white;
            GUI.enabled = true;
        }

        private void DrawMode(Rect p, float s)
        {
            GUI.enabled = !game.Paused;
            if (GUI.Button(new Rect(p.x + 40*s, p.y + 175*s, 265*s, 74*s), (inningMode ? "● " : "") + "한 이닝 승부")) inningMode = true;
            if (GUI.Button(new Rect(p.x + 315*s, p.y + 175*s, 265*s, 74*s), (!inningMode ? "● " : "") + "10구 챌린지")) inningMode = false;
            GUI.enabled = true;
        }

        private void StartSelected()
        {
            if (game.Paused) game.TogglePause();
            else if (inningMode) game.StartInning(level);
            else game.StartChallenge(level);
        }

        private string MenuDescription()
        {
            if (game.Finished && game.Inning != null) return "3아웃 종료 · 득점 " + game.Inning.Runs + " · 안타 " + game.Inning.Hits;
            if (game.Finished) return "점수 " + game.Session.Score + " · 정타 " + game.Session.Perfects;
            return "좋은 공은 스윙, 볼은 기다리세요. 안타와 장타로 주자를 홈에 불러들이세요.";
        }

        private string MenuTitle()
        {
            if (game.Finished) return "플레이 완료!";
            return game.Paused ? "잠시 쉬어가기" : "DECKBALL 2026";
        }

        private void PortraitMessage()
        {
            Panel(new Rect(0, 0, Screen.width, Screen.height), navy);
            GUI.Label(new Rect(20, Screen.height * .35f, Screen.width - 40, Screen.height * .3f), "화면을 가로로 돌려주세요", heading);
        }
    }
}
