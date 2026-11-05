package com.towerfront;
import java.util.Arrays;
public final class SceneStyle {
    public static final int INK=0xff24394b,PAPER=0xfff5efdf,BLUE=0xff2678bf,RED=0xffcd553d;
    private SceneStyle() { }
    public static int team(int owner) {
        return owner==1?BLUE:owner==2?RED:0xff817969;
    }
    public static int squadSize(int units) {return Math.max(0,Math.min(12,units));}
    public static String title(int kind) {
        return new String[]{"지휘 본부","생산 기지","통신 기지","방어 요새"}[kind];
    }
    public static int[] towerOrder(Engine e) {
        Integer[] keys=new Integer[e.towers.size()];
        for(int i=0;i<keys.length;i++) keys[i]=i;
        Arrays.sort(keys,(a,b)->Double.compare(e.towers.get(a).y,e.towers.get(b).y));
        int[] order=new int[keys.length];
        for(int i=0;i<keys.length;i++) order[i]=keys[i];
        return order;
    }
}
