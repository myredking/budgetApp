using System;

namespace DeckBall.Showcase
{
    public sealed class InningSession
    {
        public int Balls { get; private set; }
        public int Strikes { get; private set; }
        public int Outs { get; private set; }
        public int Runs { get; private set; }
        public int Hits { get; private set; }
        public int Bases { get; private set; }
        public int Batters { get; private set; }
        public bool Complete => Outs >= 3;
        public string Result { get; private set; } = "1회 초";

        public void Pitch(bool inZone, bool swung, bool foul)
        {
            if (Complete) return;
            if (foul)
            {
                Strikes = Math.Min(2, Strikes + 1);
                Result = "파울";
                return;
            }
            if (inZone || swung) Strike();
            else Ball();
        }

        private void Strike()
        {
            Strikes++;
            Result = "스트라이크";
            if (Strikes < 3) return;
            Out();
            Result = "삼진!";
        }

        private void Ball()
        {
            Balls++;
            Result = "볼";
            if (Balls < 4) return;
            var forced = 1;
            while ((Bases & forced) != 0 && forced < 8) forced <<= 1;
            if (forced == 8) Runs++;
            else Bases |= forced;
            EndAtBat();
            Result = "볼넷";
        }

        public void Hit(int count)
        {
            if (count < 1 || count > 4) throw new ArgumentOutOfRangeException(nameof(count));
            if (Complete) return;
            var advanced = (Bases << count) | (1 << (count - 1));
            for (var bit = 3; bit <= 6; bit++)
                if ((advanced & (1 << bit)) != 0) Runs++;
            Bases = advanced & 7;
            Hits++;
            EndAtBat();
            Result = new[] { "", "안타!", "2루타!", "3루타!", "홈런!" }[count];
        }

        public void Out()
        {
            if (Complete) return;
            Outs++;
            EndAtBat();
            Result = Complete ? "3아웃 · 공수 교대" : "아웃!";
        }

        private void EndAtBat()
        {
            Balls = Strikes = 0;
            Batters++;
        }
    }
}
