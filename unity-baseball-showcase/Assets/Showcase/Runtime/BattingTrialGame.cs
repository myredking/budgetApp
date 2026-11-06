using System.Linq;
using UnityEngine;

namespace DeckBall.Showcase
{
    public sealed class BattingTrialGame : MonoBehaviour
    {
        public GameObject batter, pitcher;
        public Camera gameCamera;
        public AnimationClip[] swingClips;
        public BattingSession Session { get; private set; }
        public bool Started { get; private set; }
        public bool Paused { get; private set; }
        public bool Finished { get; private set; }
        public bool Power { get; private set; }
        public int SwingStyle { get; private set; }
        public int Best { get; private set; }
        public float PitchSpeed { get; private set; }
        public float FlightProgress => Mathf.Clamp01((clock - 2) / duration);
        public string Feedback { get; private set; } = "10구 안에 최고의 타격을 보여주세요";
        public float EstimatedDistance { get; private set; }
        private AnimationClip batClip, pitchClip;
        private Transform ball;
        private Renderer[] ballRenderers;
        private Vector3 release, velocity, hitVelocity;
        private float clock, arrival, duration = .55f, swingTime, resultDuration;
        private Vector3 cameraHome, lookHome;
        private bool impactPlayed;
        private const float Gravity = 9.81f;
        private string BestKey => "DeckBall.Trial.v1." + Session.Level;

        private void Awake()
        {
            Session = new BattingSession(1);
            batClip = batter.GetComponent<Animation>().clip;
            pitchClip = pitcher.GetComponent<Animation>().clip;
            batter.GetComponent<Animation>().enabled = false;
            pitcher.GetComponent<Animation>().enabled = false;
            ball = pitcher.GetComponentsInChildren<Transform>().First(t => t.name == "PitchBall");
            ballRenderers = ball.GetComponentsInChildren<Renderer>();
            cameraHome = gameCamera.transform.position;
            lookHome = new Vector3(0, 1.2f, 18.44f);
            Best = PlayerPrefs.GetInt(BestKey, 0);
            Application.targetFrameRate = 60;
            Screen.orientation = ScreenOrientation.LandscapeLeft;
            Screen.autorotateToLandscapeLeft = Screen.autorotateToLandscapeRight = true;
            Screen.autorotateToPortrait = Screen.autorotateToPortraitUpsideDown = false;
        }

        public void StartChallenge(int level)
        {
            Session = new BattingSession(level);
            Best = PlayerPrefs.GetInt(BestKey, 0);
            Started = true;
            Finished = Paused = Power = false;
            NewPitch();
        }

        public void TogglePower() { if (Started && !Finished && !Session.Resolved) Power = !Power; }
        public void TogglePause() { if (Started && !Finished) Paused = !Paused; }

        private void NewPitch()
        {
            Session.BeginPitch();
            clock = 0;
            impactPlayed = false;
            EstimatedDistance = 0;
            PitchSpeed = Random.Range(28f + Session.Level * 4, 31f + Session.Level * 4);
            pitchClip.SampleAnimation(pitcher, 2);
            release = ball.position;
            var target = new Vector3(Random.Range(-.08f, .08f), Random.Range(.84f, 1.02f), .2f);
            duration = Vector3.Distance(release, target) / PitchSpeed;
            arrival = 2 + duration;
            velocity = (target - release) / duration + Vector3.up * (.5f * Gravity * duration);
            foreach (var renderer in ballRenderers) renderer.enabled = true;
            Feedback = "공을 보고, 닿기 직전에 스윙";
            resultDuration = 1.5f;
            gameCamera.transform.position = cameraHome;
            gameCamera.transform.LookAt(lookHome);
            SamplePlayers();
        }

        public bool TrySwing()
        {
            if (!Started || Finished || Paused || clock < 2 || Session.Resolved) return false;
            var error = clock + TrialRules.SwingLead - arrival;
            if (!Session.TryResolve(error, Power)) return false;
            swingTime = clock;
            Feedback = new[] { error < 0 ? "너무 빨라요" : "늦었어요", "빗맞음", "좋은 타격!", "완벽한 타이밍!" }[Session.LastGrade];
            PrepareHit(error);
            RememberScore();
            return true;
        }

        private void PrepareHit(float error)
        {
            if (Session.LastGrade == 0) return;
            hitVelocity = TrialRules.LaunchVelocity(Session.LastGrade, Session.LastPower, SwingStyle,
                error / TrialRules.Window(Session.Level, Session.LastPower));
            var flight = (hitVelocity.y + Mathf.Sqrt(hitVelocity.y * hitVelocity.y + 2 * Gravity)) / Gravity;
            EstimatedDistance = new Vector2(hitVelocity.x, hitVelocity.z).magnitude * flight;
            resultDuration = Mathf.Clamp(flight + .3f, 1.5f, 4.8f);
        }

        private void RememberScore()
        {
            if (Session.Score <= Best) return;
            Best = Session.Score;
            PlayerPrefs.SetInt(BestKey, Best);
        }

        private void Update()
        {
            Advance(Time.deltaTime);
        }

        public void Advance(float seconds)
        {
            if (!Started || Finished || Paused) return;
            clock += seconds;
            SamplePlayers();
            MoveBall();
            if (!Session.Resolved && clock > arrival + .25f)
            {
                Session.TryResolve(1, false);
                swingTime = clock;
                Feedback = "스윙하지 못했어요";
            }
            if (!Session.Resolved || clock < arrival + resultDuration) return;
            if (!Session.Complete) { NewPitch(); return; }
            Finished = true;
            PlayerPrefs.Save();
        }

        private void SamplePlayers()
        {
            pitchClip.SampleAnimation(pitcher, Mathf.Min(clock, 2.65f));
            var time = .75f;
            if (Session.Resolved) time = TrialRules.SwingClipTime(clock - swingTime, arrival - clock, Session.LastGrade > 0);
            var clip = swingClips != null && swingClips.Length > SwingStyle ? swingClips[SwingStyle] : batClip;
            (clip != null ? clip : batClip).SampleAnimation(batter, time);
        }

        public void CycleSwingStyle()
        {
            if (!Started || Paused || Finished || Session.Resolved) return;
            SwingStyle = (SwingStyle + 1) % 4;
        }

        private void MoveBall()
        {
            if (clock < 2) return;
            var elapsed = clock - 2;
            var position = release + velocity * elapsed - Vector3.up * (.5f * Gravity * elapsed * elapsed);
            if (Session.Resolved && Session.LastGrade > 0 && clock >= arrival)
            {
                PlayImpact();
                elapsed = clock - arrival;
                position = new Vector3(0, .95f, .2f) + hitVelocity * elapsed - Vector3.up * (.5f * Gravity * elapsed * elapsed);
                if (SwingStyle == 3) position = GroundBall(elapsed);
                if (TrialRules.OverFence(position)) Feedback = "홈런!";
                var follow = new Vector3(position.x * .18f, 5 + elapsed * 3, -8 - elapsed * 2);
                gameCamera.transform.position = Vector3.Lerp(gameCamera.transform.position, follow, Time.deltaTime * 2);
                gameCamera.transform.LookAt(Vector3.Lerp(new Vector3(0, 1, 20), position, .65f));
            }
            else
            {
                gameCamera.transform.position = Vector3.Lerp(gameCamera.transform.position, cameraHome, Time.deltaTime * 6);
                gameCamera.transform.LookAt(lookHome);
            }
            ball.position = position;
            SetBallVisibility(position);
        }

        private void PlayImpact()
        {
            if (impactPlayed) return;
            GetComponent<TrialSound>()?.Hit();
            impactPlayed = true;
        }

        private void SetBallVisibility(Vector3 position)
        {
            var visible = position.y > .03f && (Session.LastGrade > 0 || clock < arrival + .15f);
            foreach (var renderer in ballRenderers) renderer.enabled = visible;
        }

        private Vector3 GroundBall(float elapsed)
        {
            var impact = (hitVelocity.y + Mathf.Sqrt(hitVelocity.y * hitVelocity.y + 2 * Gravity * .95f)) / Gravity;
            if (elapsed <= impact) return new Vector3(0, .95f, .2f) + hitVelocity * elapsed - Vector3.up * (.5f * Gravity * elapsed * elapsed);
            var first = new Vector3(0, .04f, .2f) + new Vector3(hitVelocity.x, 0, hitVelocity.z) * impact;
            var rolling = elapsed - impact;
            var distance = .55f * (1 - Mathf.Exp(-1.5f * rolling)) / 1.5f;
            return first + new Vector3(hitVelocity.x, 0, hitVelocity.z) * distance
                + Vector3.up * (.17f * Mathf.Abs(Mathf.Sin(rolling * 15)) * Mathf.Exp(-rolling * 4));
        }

        private void OnApplicationPause(bool paused) { if (paused) { Paused = Started && !Finished; PlayerPrefs.Save(); } }
    }
}
