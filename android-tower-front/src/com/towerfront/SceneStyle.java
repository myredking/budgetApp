package com.towerfront;
import java.util.Arrays;
public final class SceneStyle {
    public static final int INK=0xff24394b,PAPER=0xfff5efdf,BLUE=0xff2678bf,RED=0xffcd553d;
    private static final int[] COLORS={0xff817969,BLUE,RED,0xff8355ad,0xff278359,0xffcb685c,
        0xff218a91,0xff9a672c,0xffa28b0d,0xffb6438a,0xff5966b6,0xff56633a};
    private static final String[] TITLES={"지휘 본부","생산 기지","통신 기지","방어 요새"};
    private SceneStyle() { }
    public static int team(int owner) {
        return COLORS[Math.max(0,Math.min(11,owner))];
    }
    public static float buildingScale(Engine e) {
        if(e.towers.size()>12) return .62f;
        return e.towers.size()>8?.8f:1f;
    }
    public static int squadSize(int units) {return Math.max(0,Math.min(12,units));}
    public static String title(int kind) {
        return TITLES[kind];
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
