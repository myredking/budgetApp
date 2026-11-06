using UnityEngine;

namespace DeckBall.Showcase.Editor
{
    internal static class DiamondStadium
    {
        public static void Build()
        {
            Stands();
            Fence();
            Structures();
        }

        private static void Stands()
        {
            var blue = new DiamondMesh();
            var red = new DiamondMesh();
            var concrete = new DiamondMesh();
            for (var row = 0; row < 22; row++)
            for (var seat = 0; seat < 480; seat++)
            {
                var angle = Mathf.Lerp(-112, 112, seat / 479f) * Mathf.Deg2Rad;
                var radius = 119 + row * .92f;
                var position = new Vector3(Mathf.Sin(angle) * radius, 1 + row * .48f, 20 + Mathf.Cos(angle) * radius);
                var rotation = Quaternion.Euler(0, angle * Mathf.Rad2Deg, 0);
                var mesh = seat / 60 % 2 == 0 ? blue : red;
                mesh.Box(position, new Vector3(.43f, .10f, .45f), rotation);
                mesh.Box(position + rotation * new Vector3(0, .27f, .18f), new Vector3(.43f, .48f, .08f), rotation);
                if (seat % 2 == 0)
                    concrete.Box(position + new Vector3(0, -.38f, 0), new Vector3(2.0f, .48f, .98f), rotation);
            }
            blue.Finish("GrandstandSeatsBlue", DiamondMaterials.Get("SeatBlue", new Color(.06f, .20f, .34f)));
            red.Finish("GrandstandSeatsRed", DiamondMaterials.Get("SeatRed", new Color(.45f, .055f, .065f)));
            concrete.Finish("GrandstandConcrete", DiamondMaterials.Get("Concrete", new Color(.49f, .49f, .45f)));
            foreach (var name in new[] { "GrandstandSeatsBlue", "GrandstandSeatsRed", "GrandstandConcrete" })
                GameObject.Find(name).GetComponent<Renderer>().shadowCastingMode = UnityEngine.Rendering.ShadowCastingMode.Off;
        }

        private static Vector3 Boundary(float angle)
        {
            var radius = TrialRules.FenceRadius(angle);
            var radians = angle * Mathf.Deg2Rad;
            return new Vector3(Mathf.Sin(radians) * radius, 0, Mathf.Cos(radians) * radius);
        }

        private static void Fence()
        {
            var wall = new DiamondMesh();
            var top = new DiamondMesh();
            for (var index = 0; index < 90; index++)
            {
                var a = Boundary(index - 45);
                var b = Boundary(index - 44);
                wall.Quad(a, a + Vector3.up * 2.6f, b + Vector3.up * 2.6f, b);
                wall.Quad(b, b + Vector3.up * 2.6f, a + Vector3.up * 2.6f, a);
                top.Box((a + b) * .5f + Vector3.up * 2.65f, new Vector3(.12f, .12f, Vector3.Distance(a, b)), Quaternion.LookRotation(b - a));
            }
            wall.Finish("OutfieldFence", DiamondMaterials.Get("FenceGreen", new Color(.025f, .20f, .15f)));
            top.Finish("YellowFenceRail", DiamondMaterials.Get("SafetyYellow", new Color(.95f, .65f, .06f)));
            var poles = new DiamondMesh();
            foreach (var angle in new[] { -45f, 45f })
                poles.Box(Boundary(angle) + Vector3.up * 6, new Vector3(.16f, 12, .16f), Quaternion.identity);
            poles.Finish("FoulPoles", DiamondMaterials.Get("PoleYellow", new Color(1, .75f, .08f)));
        }

        private static void Structures()
        {
            var steel = new DiamondMesh();
            var lamps = new DiamondMesh();
            foreach (var x in new[] { -83f, 83f })
            foreach (var z in new[] { 15f, 110f })
            {
                steel.Box(new Vector3(x, 15, z), new Vector3(.6f, 30, .6f), Quaternion.identity);
                steel.Box(new Vector3(x, 30, z), new Vector3(8, .4f, .5f), Quaternion.identity);
                for (var index = 0; index < 8; index++)
                    lamps.Box(new Vector3(x - 3 + index % 4 * 2, 30 + index / 4 * 1.1f, z), new Vector3(1.3f, .8f, .25f), Quaternion.identity);
            }
            steel.Box(new Vector3(0, 16, 145), new Vector3(28, 12, 1), Quaternion.identity);
            steel.Box(new Vector3(-9, 7, 145), new Vector3(1, 14, 1), Quaternion.identity);
            steel.Box(new Vector3(9, 7, 145), new Vector3(1, 14, 1), Quaternion.identity);
            steel.Finish("FloodlightTowersAndScoreboard", DiamondMaterials.Get("Steel", new Color(.19f, .23f, .26f)));
            lamps.Finish("FloodlightPanels", DiamondMaterials.Get("LampGlass", new Color(.9f, .92f, .82f)));
            var screen = new DiamondMesh();
            screen.Box(new Vector3(0, 16, 144.4f), new Vector3(25, 9.5f, .1f), Quaternion.identity);
            screen.Finish("StadiumVideoBoard", DiamondMaterials.Get("VideoBoard", new Color(.015f, .07f, .10f)));
        }
    }
}
