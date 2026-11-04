package com.bluehorizon;

/** Greedy map label placement, separated from Android for bounds/collision tests. */
public final class MapLabels {
    public final float[][] boxes=new float[8][];
    private final int region;
    public MapLabels(MapProjection map) {
        region=map.region;
        int[] ports=World.portsIn(map.region);
        for(int i=0;i<8;i++) {
            float[] point=map.point(World.LON[ports[i]],World.LAT[ports[i]]);
            boxes[i]=place(point,World.PORTS[ports[i]].length()*11+12,i);
        }
    }
    private float[] place(float[] point,float width,int count) {
        float best=Float.POSITIVE_INFINITY;float[] selected=null;
        for(int y=24;y<=240;y+=20) {
            for(int x=16;x+width<=344;x+=16) {
                float[] candidate={x,y,x+width,y+18};
                float dx=x+width/2-point[0],dy=y+9-point[1];
                float score=dx*dx+dy*dy;
                if(score<best && available(candidate,count)) { best=score;selected=candidate; }
            }
        }
        return selected;
    }
    private boolean available(float[] box,int count) {
        for(int i=0;i<count;i++) if(overlap(box,boxes[i])) return false;
        return true;
    }
    public int hit(float x,float y) {
        for(int i=0;i<boxes.length;i++) {
            float[] b=boxes[i];
            if(x>=b[0] && x<=b[2] && y>=b[1] && y<=b[3]) return region*8+i;
        }
        return -1;
    }
    public static boolean overlap(float[] a,float[] b) {
        return a[0]<b[2]+2 && a[2]+2>b[0] && a[1]<b[3]+2 && a[3]+2>b[1];
    }
}
