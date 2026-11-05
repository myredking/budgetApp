package com.towerfront;

/** Font-independent emblems shared by flags, tower badges and moving squads. */
public final class FactionMark {
    private static final float[][] POINTS={
        {-1,0,1,0},
        {0,-1,.25f,-.3f,1,-.3f,.4f,.15f,.6f,1,0,.5f,-.6f,1,-.4f,.15f,-1,-.3f,-.25f,-.3f},
        {0,-1,1,1,-1,1},
        {-1,-1,1,-1,1,1,-1,1},
        {0,-1,1,0,0,1,-1,0},
        {-.3f,-1,.3f,-1,.3f,-.3f,1,-.3f,1,.3f,.3f,.3f,.3f,1,-.3f,1,-.3f,.3f,-1,.3f,-1,-.3f,-.3f,-.3f},
        {-1,-.6f,-.6f,-1,0,-.4f,.6f,-1,1,-.6f,.4f,0,1,.6f,.6f,1,0,.4f,-.6f,1,-1,.6f,-.4f,0},
        {-.5f,-1,.5f,-1,1,0,.5f,1,-.5f,1,-1,0},
        {-1,-1,1,-1,-1,1,1,1},
        {-1,-1,0,0,1,-1,1,0,0,1,-1,0},
        {-1,-1,-.3f,-1,-.3f,.3f,.3f,.3f,.3f,-1,1,-1,1,1,-1,1},
        {-1,-1,-.4f,-1,-.4f,-.3f,.4f,-.3f,.4f,-1,1,-1,1,1,.4f,1,.4f,.3f,-.4f,.3f,-.4f,1,-1,1}
    };
    private FactionMark() { }
    private static int team(int owner) {return Math.max(0,Math.min(11,owner));}
    public static int vertices(int owner) {return POINTS[team(owner)].length/2;}
    public static float x(int owner,int vertex) {return POINTS[team(owner)][vertex*2];}
    public static float y(int owner,int vertex) {return POINTS[team(owner)][vertex*2+1];}
}
