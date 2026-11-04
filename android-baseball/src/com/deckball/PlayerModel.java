package com.deckball;

public final class PlayerModel {
    static final int SKIN=0xbc8d70, PANTS=0xdedfd8, NAVY=0x17283c, RED=0x9c273d;
    private static final java.util.Map<Long,Mesh3D> POSES=new java.util.LinkedHashMap<Long,Mesh3D>(32,.75f,true) {
        @Override protected boolean removeEldestEntry(java.util.Map.Entry<Long,Mesh3D> entry) { return size()>32; }
    };
    private PlayerModel() {}
    public static synchronized Mesh3D player(boolean batter,float pose,int uniform) {
        int frame=Math.round(Math.max(0,Math.min(1,pose))*16);
        long key=((long)uniform<<6)|(batter?32:0)|frame;
        Mesh3D mesh=POSES.get(key);
        if(mesh==null) { mesh=build(batter,frame/16f,uniform);POSES.put(key,mesh); }
        return mesh;
    }
    private static Mesh3D build(boolean batter,float pose,int uniform) {
        Mesh3D mesh=new Mesh3D();
        body(mesh,uniform);legs(mesh,batter,pose);head(mesh,uniform,batter);
        if(batter) battingArms(mesh,pose,uniform);else pitchingArms(mesh,pose,uniform);
        return mesh;
    }
    static void body(Mesh3D m,int uniform) {
        m.ellipsoid(0,1.19f,0,.235f,.32f,.145f,uniform);
        m.tube(v(0,.92f,0),v(0,1.4f,0),.17f,.22f,uniform);
        m.ellipsoid(0,.89f,0,.19f,.14f,.13f,PANTS);
        m.box(0,.97f,-.01f,.37f,.045f,.26f,0x1a202a);
        m.tube(v(0,1.43f,0),v(0,1.57f,0),.07f,.07f,SKIN);
        m.box(0,1.22f,-.146f,.012f,.35f,.008f,0xb5c6ce);
        for(int i=0;i<4;i++) m.ellipsoid(0,1.1f+i*.075f,-.155f,.013f,.013f,.008f,0xe3e5e0);
        m.box(-.105f,1.34f,-.146f,.10f,.018f,.014f,0xe4dcc4);
    }
    static void legs(Mesh3D m,boolean bat,float pose) {
        float spread=bat?.24f:.13f;
        for(int side:new int[]{-1,1}) {
            float ankle=side*spread,knee=side*(spread*.8f),lift=(!bat && side==1)?Math.max(0,pose)*.28f:0;
            m.tube(v(side*.10f,.87f,0),v(knee,.49f+lift,-.06f),.105f,.09f,PANTS);
            m.ellipsoid(knee,.49f+lift,-.06f,.092f,.10f,.09f,PANTS);
            m.tube(v(knee,.49f+lift,-.06f),v(ankle,.10f+lift,0),.085f,.055f,PANTS);
            m.ellipsoid(ankle,.065f+lift,-.055f,.085f,.055f,.16f,0x19222c);
            m.box(ankle,.025f+lift,-.065f,.16f,.018f,.27f,0x444c51);
        }
    }
    static void head(Mesh3D m,int uniform,boolean bat) {
        m.ellipsoid(0,1.67f,-.012f,.102f,.135f,.096f,SKIN);
        m.ellipsoid(0,1.765f,-.005f,.115f,.07f,.108f,uniform);
        m.box(0,1.745f,-.115f,.20f,.016f,.15f,uniform);
        m.ellipsoid(-.103f,1.66f,-.003f,.022f,.04f,.022f,SKIN);
        m.ellipsoid(.103f,1.66f,-.003f,.022f,.04f,.022f,SKIN);
        m.ellipsoid(0,1.66f,-.11f,.019f,.032f,.027f,SKIN);
        for(int side:new int[]{-1,1}) m.ellipsoid(side*.039f,1.695f,-.097f,.014f,.006f,.007f,0x27282a);
        if(bat) m.ellipsoid(.105f,1.675f,.012f,.022f,.085f,.07f,uniform);
    }
    static void limb(Mesh3D m,float[] shoulder,float[] elbow,float[] hand,int uniform) {
        m.ellipsoid(shoulder[0],shoulder[1],shoulder[2],.11f,.11f,.11f,uniform);
        m.tube(shoulder,elbow,.095f,.075f,uniform);
        m.ellipsoid(elbow[0],elbow[1],elbow[2],.07f,.07f,.07f,SKIN);
        m.tube(elbow,hand,.065f,.042f,SKIN);
        m.ellipsoid(hand[0],hand[1],hand[2],.05f,.065f,.05f,0xe8e7df);
    }
    static void battingArms(Mesh3D m,float pose,int uniform) {
        float a=Math.max(0,pose),x=.26f-a*.62f,z=-.12f-a*.32f;
        float[] hand=v(x,1.38f-a*.20f,z);
        limb(m,v(-.20f,1.39f,0),v(-.18f,1.18f,-.21f),hand,uniform);
        limb(m,v(.20f,1.39f,0),v(.36f,1.25f,-.02f),v(x+.04f,hand[1]+.06f,z),uniform);
        float[] tip=v(x+.30f-a*1.0f,2.16f-a*.9f,z+.25f-a*.35f);
        m.tube(hand,tip,.024f,.052f,0xb29164);
        m.ellipsoid(tip[0],tip[1],tip[2],.051f,.045f,.051f,0xc3a276);
    }
    static void pitchingArms(Mesh3D m,float pose,int uniform) {
        float a=Math.max(0,pose),handY=1.13f+a*.75f;
        limb(m,v(-.21f,1.39f,0),v(-.34f,1.19f,-.04f),v(-.08f,1.16f,-.26f),uniform);
        limb(m,v(.21f,1.39f,0),v(.38f,1.45f+a*.25f,.1f),v(.21f,handY,.24f-a*.55f),uniform);
        m.ellipsoid(-.08f,1.16f,-.29f,.115f,.13f,.08f,0x9d633d);
        for(int i=0;i<4;i++) m.tube(v(-.14f+i*.037f,1.14f,-.36f),v(-.14f+i*.037f,1.27f,-.31f),.012f,.012f,0x5e3829);
    }
    static float[] v(float x,float y,float z) { return Mesh3D.vector(x,y,z); }
}
