package com.bluehorizon;

public final class TownWalk {
    public static float[] position(int place,float progress) {
        float t=Math.max(0,Math.min(1,progress));t=t*t*(3-2*t);
        float[] target=TownLayout.point(place);float u=1-t;
        return new float[]{u*u*.64f+2*u*t*.46f+t*t*target[0],u*u*.94f+2*u*t*.70f+t*t*(target[1]+.05f)};
    }
}
