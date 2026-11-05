package com.towerfront;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public final class Skirmish {
    public static final int MAPS=5;
    private static final String[] NAMES={"격자 전선","엇갈린 전선","환형 전선","대각 전선","산개 전선"};
    private Skirmish() { }
    public static String name(int map) {return NAMES[Math.max(0,Math.min(MAPS-1,map))];}
    public static int towerCount(int map,int enemies) {
        if(enemies<=4) return 12;
        return map==2?16:20;
    }
    public static Engine create(int map,int enemies,int difficulty,long seed) {
        Engine e=new Engine(1,difficulty);e.skirmish=true;
        populate(e,map,enemies,seed);e.rules=BattleRules.start(e);return e;
    }
    public static void populate(Engine e,int map,int enemies,long seed) {
        e.mapId=Math.max(0,Math.min(MAPS-1,map));e.enemyCount=Math.max(1,Math.min(10,enemies));e.seed=seed;
        e.towers.clear();e.fleets.clear();e.drainImpacts();
        Random random=new Random(seed);
        ArrayList<double[]> points=points(e.mapId,e.enemyCount,random);
        Collections.shuffle(points,random);spaceSpawns(points,e.enemyCount+1);
        for(int i=0;i<points.size();i++) {
            int owner=i<=e.enemyCount?i+1:0;
            int units=owner==1?40:owner>1?20+e.difficulty*5:8+random.nextInt(9);
            double[] xy=points.get(i);
            Engine.Tower t=new Engine.Tower(xy[0],xy[1],owner,units);
            if(owner==0) t.kind=1+random.nextInt(3);
            e.towers.add(t);
        }
    }
    private static ArrayList<double[]> points(int map,int enemies,Random random) {
        int count=towerCount(map,enemies);ArrayList<double[]> result=new ArrayList<>();
        for(int i=0;i<count;i++) result.add(point(map,count,i,random));
        return result;
    }
    private static double[] point(int map,int count,int i,Random random) {
        if(map==2) return ring(count,i);
        int cols=count==12?3:4,rows=count/cols;
        double x=.12+(i%cols)*.76/(cols-1),y=.12+(i/cols)*.76/(rows-1);
        if(map==1) x=Math.max(.1,Math.min(.9,x+((i/cols)%2==0?.025:-.025)));
        if(map==3) {double previous=x;x=y;y=previous;}
        if(map==4) {x+=(random.nextDouble()-.5)*.03;y+=(random.nextDouble()-.5)*.03;}
        return new double[]{x,y};
    }
    private static double[] ring(int count,int index) {
        int outer=count==12?8:10;
        double radius=index<outer?.39:.20;
        int local=index<outer?index:index-outer,slots=index<outer?outer:count-outer;
        double angle=2*Math.PI*local/slots-Math.PI/2;
        return new double[]{.5+radius*Math.cos(angle),.5+radius*Math.sin(angle)};
    }
    private static void spaceSpawns(ArrayList<double[]> points,int count) {
        for(int index=0;index<count;index++) {
            int best=index;double bestDistance=-1;
            for(int candidate=index;candidate<points.size();candidate++) {
                double distance=spawnDistance(points,candidate,index);
                if(distance>bestDistance) {bestDistance=distance;best=candidate;}
            }
            Collections.swap(points,index,best);
        }
    }
    private static double spawnDistance(ArrayList<double[]> points,int candidate,int placed) {
        double[] p=points.get(candidate);
        if(placed==0) return 2-Math.hypot(p[0]-.12,p[1]-.88);
        double nearest=2;
        for(int i=0;i<placed;i++) {
            double[] other=points.get(i);nearest=Math.min(nearest,Math.hypot(p[0]-other[0],p[1]-other[1]));
        }
        return nearest;
    }
}
