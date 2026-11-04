package com.deckball;

public final class BallparkScene {
    private BallparkScene() {}
    static float[] v(float x,float y,float z) { return Mesh3D.vector(x,y,z); }
    public static Mesh3D stadium() {
        Mesh3D m=new Mesh3D(); ground(m); seating(m); architecture(m); fielders(m);
        return m;
    }
    static void ground(Mesh3D m) {
        m.quad(v(-150,-.02f,-100),v(-150,-.02f,180),v(150,-.02f,180),v(150,-.02f,-100),0x42743a);
        disk(m,0,.005f,18,30,0xa58c6b);
        m.quad(v(0,.015f,5),v(-14,.015f,20),v(0,.015f,34),v(14,.015f,20),0x42743a);
        disk(m,0,.025f,18.44f,1.8f,0xb39c7b);
        m.ellipsoid(0,.03f,18.44f,1.6f,.14f,1.6f,0xb39c7b);
        m.box(0,.18f,18.44f,.61f,.025f,.15f,0xeae8de);
        stripe(m,0,0,83,83,.10f,0xeae8de);stripe(m,0,0,-83,83,.10f,0xeae8de);
        for(float[] b:new float[][]{{19.4f,19.4f},{0,38.8f},{-19.4f,19.4f}}) m.box(b[0],.06f,b[1],.4f,.10f,.4f,0xf1eee4);
        m.quad(v(-.216f,.022f,-.216f),v(-.216f,.022f,.216f),v(.216f,.022f,.216f),v(.216f,.022f,-.216f),0xeceade);
        m.triangle(v(-.216f,.022f,-.216f),v(.216f,.022f,-.216f),v(0,.022f,-.43f),0xeceade);
        for(int s:new int[]{-1,1}) {
            stripe(m,s*.65f,-.65f,s*1.55f,-.65f,.04f,0xe9e5d9);
            stripe(m,s*.65f,1.2f,s*1.55f,1.2f,.04f,0xe9e5d9);
            stripe(m,s*.65f,-.65f,s*.65f,1.2f,.04f,0xe9e5d9);
            stripe(m,s*1.55f,-.65f,s*1.55f,1.2f,.04f,0xe9e5d9);
        }
        disk(m,-1.1f,.028f,.15f,.75f,0x817057);
    }
    static void stripe(Mesh3D m,float x,float z,float xx,float zz,float width,int color) {
        float dx=xx-x,dz=zz-z,length=(float)Math.sqrt(dx*dx+dz*dz);
        float ox=-dz/length*width/2,oz=dx/length*width/2;
        m.quad(v(x+ox,.03f,z+oz),v(xx+ox,.03f,zz+oz),v(xx-ox,.03f,zz-oz),v(x-ox,.03f,z-oz),color);
    }
    static void disk(Mesh3D m,float x,float y,float z,float r,int color) {
        for(int i=0;i<64;i++) {
            float a=i*(float)Math.PI/32,b=(i+1)*(float)Math.PI/32;
            m.triangle(v(x,y,z),v(x+(float)Math.sin(a)*r,y,z+(float)Math.cos(a)*r),v(x+(float)Math.sin(b)*r,y,z+(float)Math.cos(b)*r),color);
        }
    }
    static float[] ring(float angle,float radius,float y) { return v((float)Math.cos(angle)*82*radius,y,40+(float)Math.sin(angle)*78*radius); }
    static void seating(Mesh3D m) {
        for(int row=0;row<14;row++) {
            float r=1+row*.023f,y=3+row*.8f;
            for(int col=0;col<100;col++) {
                float a=col*(float)Math.PI/50,b=(col+1)*(float)Math.PI/50;
                m.quad(ring(a,r,y),ring(b,r,y),ring(b,r+.023f,y),ring(a,r+.023f,y),row%3==0?0x6b7980:0x3d5666);
                m.quad(ring(a,r,y-.8f),ring(b,r,y-.8f),ring(b,r,y),ring(a,r,y),0x809098);
                if(col%7==0) continue;
                float[] seat=ring((a+b)/2,r+.008f,y+.22f);
                m.box(seat[0],seat[1],seat[2],.38f,.44f,.38f,crowdColor(row,col));
            }
        }
        for(int col=0;col<100;col++) {
            float a=col*(float)Math.PI/50,b=(col+1)*(float)Math.PI/50;
            m.quad(ring(a,.995f,0),ring(b,.995f,0),ring(b,.995f,3.4f),ring(a,.995f,3.4f),0x164c47);
        }
    }
    static int crowdColor(int row,int col) { return new int[]{0x8c99a5,0xb8b5a5,0x343c4b,0x9e7473,0xddd6c5,0x566e8a}[(row*13+col*7)%6]; }
    static void architecture(Mesh3D m) {
        m.box(0,13,120,20,11,1.2f,0x1d2b33);
        m.box(0,13,119.3f,17,8.5f,.12f,0x172c3c);
        for(int i=-7;i<=7;i++) m.box(i,12.6f,119.1f,.45f,.08f,.08f,0x7cbea6);
        for(int i=-3;i<=3;i++) m.box(i*2,15,119.1f,1,.8f,.08f,0xc6d1bc);
        for(int side:new int[]{-1,1}) {
            m.box(side*73,16,65,.6f,32,.6f,0x687e87);
            m.box(side*73,32,65,11,3,.7f,0x4c6370);
            for(int light=0;light<10;light++) m.box(side*73-4.5f+light,32,64.58f,.7f,1.7f,.15f,0xf2edd1);
            m.box(side*92,15,118,18,30,15,0x7b949d);
            for(int row=0;row<7;row++) for(int col=0;col<5;col++) m.box(side*92-7+col*3.4f,3+row*4,110.4f,1.8f,1.8f,.2f,0x4e6e7e);
        }
        m.box(-53,1.6f,18,16,3,5,0x465662);m.box(53,1.6f,18,16,3,5,0x465662);
    }
    static void fielders(Mesh3D m) {
        Mesh3D player=PlayerModel.player(false,-1,PlayerModel.RED);
        for(float[] point:new float[][]{{19,21},{-19,21},{8,31},{-9,31},{-33,64},{0,81},{35,64}}) {
            disk(m,point[0],.023f,point[1],.65f,0x365e33);m.merge(player,point[0],0,point[1],0);
        }
    }
    public static float[] ballPosition(float progress) {
        float t=Math.max(0,Math.min(1,progress));
        return v(.16f*(1-t),1.8f-.9f*t+.12f*(float)Math.sin(t*Math.PI),18.1f*(1-t)+.1f);
    }
    public static Mesh3D actors(float progress,float swing,int teamColor) {
        return actors(progress,swing,teamColor,PlayerModel.RED,0);
    }
    public static Mesh3D actors(float progress,float swing,int teamColor,int pitcherColor,float batted) {
        Mesh3D m=new Mesh3D();fillActors(m,progress,swing,teamColor,pitcherColor,batted);return m;
    }
    public static void fillActors(Mesh3D m,float progress,float swing,int teamColor,int pitcherColor,float batted) {
        m.reset();
        int uniform=teamColor==0?PlayerModel.NAVY:teamColor;
        float pose=progress<0?0:(float)Math.sin(Math.min(progress,.25f)/.25f*Math.PI);
        m.merge(PlayerModel.player(true,swing,uniform),-1.1f,0,.05f,-.55f+swing*.9f);
        m.merge(PlayerModel.player(false,pose,pitcherColor),0,.15f,18.44f,0);
        if(progress>=0) {
            float[] ball=ballPosition(Math.min(1,progress/.78f));
            m.ellipsoid(ball[0],ball[1],ball[2],.038f,.038f,.038f,0xf3eee0);
        }
        if(batted>0) {
            float y=1+12*(float)Math.sin(batted*Math.PI),z=batted*70;
            m.ellipsoid(-batted*13,y,z,.055f,.055f,.055f,0xf8f0da);
        }
    }
}
