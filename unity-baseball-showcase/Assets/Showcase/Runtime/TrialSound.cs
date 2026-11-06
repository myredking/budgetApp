using UnityEngine;

namespace DeckBall.Showcase
{
    public sealed class TrialSound : MonoBehaviour
    {
        private AudioSource impact;
        private AudioClip crack;

        private void Awake()
        {
            crack = CreateImpact();
            impact = gameObject.AddComponent<AudioSource>();
            impact.playOnAwake = false;
            impact.spatialBlend = 0;
            impact.volume = .55f;
        }

        public void Hit() { impact.PlayOneShot(crack); }

        public static AudioClip CreateImpact()
        {
            const int rate = 44100;
            var samples = new float[rate / 5];
            var random = new System.Random(2026);
            for (var index = 0; index < samples.Length; index++)
            {
                var time = index / (float)rate;
                var attack = Mathf.Min(1, time * 2500);
                var envelope = attack * Mathf.Exp(-time * 55);
                var noise = (float)random.NextDouble() * 2 - 1;
                samples[index] = envelope * (.55f * noise + .4f * Mathf.Sin(time * 2 * Mathf.PI * 1250));
            }
            var clip = AudioClip.Create("Original bat impact", samples.Length, 1, rate, false);
            clip.SetData(samples, 0);
            return clip;
        }

        private void OnDestroy() { if (crack != null) Destroy(crack); }
    }
}
