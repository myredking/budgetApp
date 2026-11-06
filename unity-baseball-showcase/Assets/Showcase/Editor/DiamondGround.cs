using UnityEngine;

namespace DeckBall.Showcase.Editor
{
    internal static class DiamondGround
    {
        public static void Build()
        {
            var grass = DiamondMaterials.Ground("Grass", new Color(.15f, .32f, .09f), true);
            var soil = DiamondMaterials.Ground("InfieldSoil", new Color(.45f, .26f, .13f), false);
            var ground = new DiamondMesh();
            ground.Quad(new Vector3(-150, 0, -40), new Vector3(-150, 0, 170), new Vector3(150, 0, 170), new Vector3(150, 0, -40));
            ground.Finish("FieldGrass", grass);
            var dirt = new DiamondMesh();
            dirt.Disc(new Vector3(0, .007f, 18.44f), 29, 0);
            dirt.Finish("InfieldDirt", soil);
            var inner = new DiamondMesh();
            inner.Quad(new Vector3(0, .017f, 5.6f), new Vector3(-13.8f, .017f, 19.4f),
                       new Vector3(0, .017f, 33.2f), new Vector3(13.8f, .017f, 19.4f));
            inner.Finish("InnerDiamondGrass", grass);
            var mound = new DiamondMesh();
            mound.Disc(new Vector3(0, .025f, 18.44f), 2.75f, .229f);
            mound.Finish("PitchersMound", soil);
            LinesAndBases();
        }

        private static void Line(DiamondMesh mesh, Vector3 a, Vector3 b, float width)
        {
            var delta = b - a;
            mesh.Box((a + b) * .5f, new Vector3(width, .012f, delta.magnitude), Quaternion.LookRotation(delta));
        }

        private static void LinesAndBases()
        {
            var white = DiamondMaterials.Get("Chalk", new Color(.94f, .92f, .84f));
            var markings = new DiamondMesh();
            Line(markings, new Vector3(0, .027f, 0), new Vector3(74.25f, .027f, 74.25f), .075f);
            Line(markings, new Vector3(0, .027f, 0), new Vector3(-74.25f, .027f, 74.25f), .075f);
            foreach (var side in new[] { -1, 1 })
            {
                var x = side * .825f;
                Line(markings, new Vector3(x - .457f, .03f, -.7f), new Vector3(x + .457f, .03f, -.7f), .045f);
                Line(markings, new Vector3(x - .457f, .03f, 1.13f), new Vector3(x + .457f, .03f, 1.13f), .045f);
                Line(markings, new Vector3(x - .457f, .03f, -.7f), new Vector3(x - .457f, .03f, 1.13f), .045f);
                Line(markings, new Vector3(x + .457f, .03f, -.7f), new Vector3(x + .457f, .03f, 1.13f), .045f);
            }
            markings.Finish("FoulLinesAndBattersBoxes", white);
            var bases = new DiamondMesh();
            foreach (var position in new[] { new Vector3(19.397f, .06f, 19.397f), new Vector3(0, .06f, 38.794f), new Vector3(-19.397f, .06f, 19.397f) })
                bases.Box(position, new Vector3(.4572f, .10f, .4572f), Quaternion.Euler(0, 45, 0));
            bases.Finish("Bases", white);
            var rubber = new GameObject("PitchingRubber");
            rubber.transform.position = new Vector3(0, .254f, 18.44f);
            var slab = new DiamondMesh();
            slab.Box(new Vector3(0, .267f, 18.516f), new Vector3(.61f, .025f, .152f), Quaternion.identity);
            slab.Finish("RubberSurface", white);
            HomePlate(white);
        }

        private static void HomePlate(Material white)
        {
            var plate = new DiamondMesh();
            var tip = new Vector3(0, .035f, 0);
            plate.Quad(tip, new Vector3(-.216f, .035f, .216f), new Vector3(-.216f, .035f, .432f), new Vector3(0, .035f, .432f));
            plate.Quad(tip, new Vector3(0, .035f, .432f), new Vector3(.216f, .035f, .432f), new Vector3(.216f, .035f, .216f));
            var obj = plate.Finish("HomePlate", white);
            obj.transform.position = Vector3.zero;
        }
    }
}
