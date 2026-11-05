package com.deckball;

public final class SceneTest {
    private SceneTest() {}
    static void check(boolean ok,String message) { EngineTest.check(ok,message); }
    public static void run() {
        Mesh3D stadium=BallparkScene.stadium();
        check(stadium.size()>10000 && stadium.size()<500000,"Bounded real3D stadium mesh");
        validate(stadium);
        Mesh3D ready=BallparkScene.actors(-1,0,0),pitch=BallparkScene.actors(.7f,0,0);
        check(ready.size()>1000,"Player model geometry"); validate(ready); validate(pitch);
        check(!java.util.Arrays.equals(ready.array(),pitch.array()),"Animated 3D poses");
        Mesh3D stance=PlayerModel.player(true,0,PlayerModel.NAVY),swung=PlayerModel.player(true,1,PlayerModel.NAVY);
        check(!java.util.Arrays.equals(stance.array(),swung.array()),"Batter skeleton changes with same ball state");
        Mesh3D set=PlayerModel.player(false,0,PlayerModel.RED),release=PlayerModel.player(false,1,PlayerModel.RED);
        check(!java.util.Arrays.equals(set.array(),release.array()),"Pitcher skeleton changes with same ball state");
        reusableActors();batCrossesPlate();
        float[] start=BallparkScene.ballPosition(0),end=BallparkScene.ballPosition(1);
        check(start[2]>end[2] && end[2]<1,"Ball moves from mound toward home");
        float[] camera=Camera3D.matrix(16f/9f,0);
        for(float value:camera) check(Float.isFinite(value),"Finite camera matrix");
        float[] plate=Camera3D.project(camera,new float[]{0,0,0});
        check(plate[3]>0,"Home plate in front of camera");
        check(Math.abs(plate[0]/plate[3])<1,"Home plate inside viewport");
    }
    static void batCrossesPlate() {
        float[] hand=PlayerModel.batHand(1),tip=PlayerModel.batTip(1);
        double yaw=.35;
        double handX=-1.1+hand[0]*Math.cos(yaw)+hand[2]*Math.sin(yaw);
        double tipX=-1.1+tip[0]*Math.cos(yaw)+tip[2]*Math.sin(yaw);
        check(handX<0 && tipX>0,"Swing reaches across the home plate instead of moving away");
        double fraction=-handX/(tipX-handX);
        double z=.05-(hand[0]+fraction*(tip[0]-hand[0]))*Math.sin(yaw)+(hand[2]+fraction*(tip[2]-hand[2]))*Math.cos(yaw);
        check(Math.abs(z)<.5,"Bat crosses home plate depth");
    }
    static void reusableActors() {
        Mesh3D reused=new Mesh3D();
        BallparkScene.fillActors(reused,.3f,0,0,PlayerModel.RED,0);
        float[] expected=BallparkScene.actors(.3f,0,0).array();
        java.nio.FloatBuffer buffer=java.nio.FloatBuffer.allocate(expected.length);
        reused.copyTo(buffer); buffer.flip(); float[] actual=new float[expected.length];buffer.get(actual);
        check(java.util.Arrays.equals(actual,expected),"Reusable actor mesh equals fresh mesh");
        int first=reused.size();
        BallparkScene.fillActors(reused,.3f,0,0,PlayerModel.RED,0);
        check(reused.size()==first,"Actor rebuild resets vertices instead of appending");
        check(PlayerModel.player(true,.5f,0x17283c)==PlayerModel.player(true,.5f,0x17283c),"Immutable pose cache reuse");
    }
    static void validate(Mesh3D mesh) {
        float[] vertices=mesh.array();
        check(vertices.length%27==0,"Triangle topology");
        for(int i=0;i<vertices.length;i+=9) {
            for(int j=0;j<9;j++) check(Float.isFinite(vertices[i+j]),"Finite mesh data");
            double n=Math.sqrt(vertices[i+3]*vertices[i+3]+vertices[i+4]*vertices[i+4]+vertices[i+5]*vertices[i+5]);
            check(n>.95 && n<1.05,"Unit normal");
        }
    }
}
