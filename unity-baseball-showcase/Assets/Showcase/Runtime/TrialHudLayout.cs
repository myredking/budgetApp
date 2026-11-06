using System;
using UnityEngine;

namespace DeckBall.Showcase
{
    public sealed class TrialHudLayout
    {
        public Rect Safe, Swing, Power, Style, Menu, Card, PitcherCard, TopBar, Feedback, Panel;
        public float Scale;
        public bool Portrait;

        public static TrialHudLayout Create(int width, int height, Rect safe, float dpi)
        {
            if (width <= 0 || height <= 0) throw new ArgumentOutOfRangeException(nameof(width));
            safe = ClampSafe(width, height, safe);
            var area = new Rect(safe.x, height - safe.yMax, safe.width, safe.height);
            var scale = Mathf.Min(area.width / 1280, area.height / 720);
            var pad = 18 * scale;
            var touch = 48 * Mathf.Clamp(dpi > 0 ? dpi / 160 : 2.6f, 1, 4);
            var swingHeight = Mathf.Max(100 * scale, touch);
            var powerHeight = Mathf.Max(84 * scale, touch);
            var swing = new Rect(area.xMax - pad - 210 * scale, area.yMax - pad - swingHeight, 210 * scale, swingHeight);
            return new TrialHudLayout
            {
                Safe = area, Scale = scale, Portrait = width < height, Swing = swing,
                Power = new Rect(swing.x - 174 * scale - pad, area.yMax - pad - powerHeight, 174 * scale, powerHeight),
                Style = new Rect(swing.x - 348 * scale - 2 * pad, area.yMax - pad - powerHeight, 174 * scale, powerHeight),
                Menu = new Rect(area.xMax - pad - 166 * scale, area.y + pad, 166 * scale, Mathf.Max(74 * scale, touch)),
                TopBar = new Rect(area.x + pad, area.y + pad, 345 * scale, 115 * scale),
                Card = new Rect(area.x + pad, area.y + area.height * .27f, 184 * scale, 310 * scale),
                PitcherCard = new Rect(area.xMax - pad - 184 * scale, area.y + area.height * .27f, 184 * scale, 310 * scale),
                Feedback = new Rect(area.center.x - 235 * scale, area.y + 112 * scale, 470 * scale, 94 * scale),
                Panel = new Rect(area.center.x - 310 * scale, area.center.y - 210 * scale, 620 * scale, 420 * scale)
            };
        }

        private static Rect ClampSafe(int width, int height, Rect safe)
        {
            var x0 = Mathf.Clamp(safe.xMin, 0, width);
            var y0 = Mathf.Clamp(safe.yMin, 0, height);
            var x1 = Mathf.Clamp(safe.xMax, 0, width);
            var y1 = Mathf.Clamp(safe.yMax, 0, height);
            return x1 <= x0 || y1 <= y0 ? new Rect(0, 0, width, height) : Rect.MinMaxRect(x0, y0, x1, y1);
        }

        public int HitTest(Vector2 screenPoint, int height)
        {
            var point = new Vector2(screenPoint.x, height - screenPoint.y);
            if (!Safe.Contains(point)) return 0;
            if (Swing.Contains(point)) return 1;
            if (Power.Contains(point)) return 2;
            if (Style.Contains(point)) return 4;
            return Menu.Contains(point) ? 3 : 0;
        }
    }
}
