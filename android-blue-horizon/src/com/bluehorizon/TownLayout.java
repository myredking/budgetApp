package com.bluehorizon;

public final class TownLayout {
    public static final String[] NAMES={"교역소","조선소","주점","마을 광장"};
    private static final float[][] POINTS={{.20f,.57f},{.82f,.59f},{.54f,.41f},{.46f,.82f}};
    public static float[] point(int location) { return POINTS[location].clone(); }
    public static int hit(float x,float y) {
        for(int i=0;i<POINTS.length;i++) {
            if(Math.abs(x-POINTS[i][0])<.15f && Math.abs(y-POINTS[i][1])<.065f) return i;
        }
        return -1;
    }
    public static int[] cell(int index,int width,int height) {
        if(index<0 || index>7) throw new IllegalArgumentException("Unknown scene");
        return new int[]{index%2*width/2,index/2*height/4,(index%2+1)*width/2,(index/2+1)*height/4};
    }
    public static int[] townCell(int index,int width,int height) {
        int[] box=cell(index,width,height);
        if(index==0) box[2]=518*width/887;
        if(index==1) box[0]=518*width/887;
        return box;
    }
}
