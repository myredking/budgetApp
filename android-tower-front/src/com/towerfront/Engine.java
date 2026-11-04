package com.towerfront;
import java.util.ArrayList;
import java.util.Iterator;
public final class Engine {
    public static final int NORMAL=0, FACTORY=1, RELAY=2;
    public static final class Tower {
        public double x,y,growth;
        public int owner,units,kind=0,rank=1;
        Tower(double px,double py,int team,int count) {x=px;y=py;owner=team;units=count;}
    }
    public static final class Fleet {
        public int from,to,owner,units;
        public double progress,duration;
        Fleet(int a,int b,int team,int count,double seconds) {
            from=a;to=b;owner=team;units=count;duration=seconds;
        }
    }
    public final ArrayList<Tower> towers=new ArrayList<>();
    public final ArrayList<Fleet> fleets=new ArrayList<>();
    private double aiTime;
    public final int level;
    public Engine(int stage) {
        level=stage;
        towers.add(new Tower(.22,.82,1,32));
        towers.add(new Tower(.78,.18,2,24+stage*2));
        towers.add(new Tower(.25,.56,0,8));
        towers.add(new Tower(.72,.46,0,10));
        towers.add(new Tower(.24,.22,0,12));
        towers.add(new Tower(.76,.76,0,8));
        if(stage>1) towers.add(new Tower(.48,.36,2,10+stage));
        if(stage>3) towers.add(new Tower(.48,.66,0,16));
        configureMap(stage);
    }
    private void configureMap(int stage) {
        int layout=(stage-1)%3;
        if(layout==1) for(Tower t:towers) t.x=1-t.x;
        if(layout==2) for(Tower t:towers) t.y=1-t.y;
        towers.get(3).kind=FACTORY;
        towers.get(4).kind=RELAY;
    }
    public boolean upgrade(int index) {
        if(index<0 || index>=towers.size() || result()!=0) return false;
        Tower t=towers.get(index);
        if(t.owner!=1 || t.rank>=3 || t.units<20*t.rank) return false;
        t.units-=20*t.rank;t.rank++;
        return true;
    }
    public boolean send(int from,int to) { return dispatch(from,to,1); }
    private boolean dispatch(int from,int to,int team) {
        if(from<0 || to<0 || from>=towers.size() || to>=towers.size()) return false;
        Tower a=towers.get(from),b=towers.get(to);
        if(from==to || a.owner!=team || a.units<2) return false;
        int count=a.units/2;
        a.units-=count;
        double duration=Math.hypot(a.x-b.x,a.y-b.y)*4;
        if(a.kind==RELAY) duration*=.6;
        fleets.add(new Fleet(from,to,team,count,Math.max(.4,duration)));
        return true;
    }
    public void update(double dt) {
        if(result()!=0) return;
        for(Tower t:towers) produce(t,dt);
        Iterator<Fleet> it=fleets.iterator();
        while(it.hasNext()) {
            Fleet f=it.next();f.progress+=dt/f.duration;
            if(f.progress>=1) {arrive(f);it.remove();}
        }
        aiTime+=dt;
        if(aiTime>=2.8) {aiTime=0;think();}
    }
    private void produce(Tower t,double dt) {
        if(t.owner==0) return;
        double bonus=(t.kind==FACTORY?1.7:1)*(1+(t.rank-1)*.5);
        t.growth+=dt*(t.owner==1?1.8:1.3+Math.min(level,10)*.08)*bonus;
        int gain=(int)t.growth;t.growth-=gain;
        t.units=Math.min(99,t.units+gain);
    }
    private void arrive(Fleet f) {
        Tower t=towers.get(f.to);
        if(t.owner==f.owner) {t.units=Math.min(99,t.units+f.units);return;}
        t.units-=f.units;
        if(t.units<0) {t.owner=f.owner;t.units=-t.units;t.growth=0;}
    }
    private void think() {
        for(int i=0;i<towers.size();i++) {
            Tower a=towers.get(i);
            if(a.owner!=2 || a.units<12) continue;
            int target=targetFor(a);
            if(target>=0) dispatch(i,target,2);
        }
    }
    private int targetFor(Tower a) {
        int best=-1;double score=Double.MAX_VALUE;
        for(int i=0;i<towers.size();i++) {
            Tower b=towers.get(i);
            if(b.owner==2) continue;
            double s=b.units+Math.hypot(a.x-b.x,a.y-b.y)*18;
            if(s<score) {score=s;best=i;}
        }
        return best;
    }
    public int result() {
        boolean blue=false,red=false;
        for(Tower t:towers) {blue|=t.owner==1;red|=t.owner==2;}
        for(Fleet f:fleets) {blue|=f.owner==1;red|=f.owner==2;}
        if(!blue) return 2;
        return red?0:1;
    }
}
