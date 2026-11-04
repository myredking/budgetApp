package com.towerfront;
import java.util.ArrayList;
import java.util.Iterator;

/** Transient visuals: never persisted and never used to calculate battle outcomes. */
public final class BattleEffects {
    public static final class Burst {
        public final Engine.Impact impact;
        public final float x,y;
        public double age;
        Burst(Engine.Impact event,float px,float py) {impact=event;x=px;y=py;}
    }
    public static final class Confetti {
        public float x,y,vx,vy,angle;
        public double age;
        public final int palette;
        Confetti(int index) {
            x=20+(index*73)%360;y=-20-(index*47)%250;
            vx=(index%9-4)*12;vy=100+(index*17)%100;angle=index*37;palette=index%4;
        }
    }
    public final ArrayList<Burst> bursts=new ArrayList<>();
    public final ArrayList<Confetti> confetti=new ArrayList<>();
    private boolean celebrated;
    public void impact(Engine.Impact event,float x,float y) {
        if(bursts.size()>=32) bursts.remove(0);
        bursts.add(new Burst(event,x,y));
    }
    public void celebrate() {
        if(celebrated) return;
        celebrated=true;
        for(int i=0;i<72;i++) confetti.add(new Confetti(i));
    }
    public void update(double dt) {
        if(!Double.isFinite(dt) || dt<=0) return;
        Iterator<Burst> it=bursts.iterator();
        while(it.hasNext()) {
            Burst burst=it.next();burst.age+=dt;
            if(burst.age>=1.1) it.remove();
        }
        updateConfetti(dt);
    }
    private void updateConfetti(double dt) {
        Iterator<Confetti> it=confetti.iterator();
        while(it.hasNext()) {
            Confetti p=it.next();p.age+=dt;p.x+=p.vx*dt;
            p.y+=p.vy*dt;p.vy+=45*dt;p.angle+=110*dt;
            if(p.age>=3.2) it.remove();
        }
    }
    public float towerScale(int index) {
        double age=1;
        for(Burst b:bursts) {
            if(b.impact.target==index && b.impact.kind==Engine.CAPTURE) age=Math.min(age,b.age);
        }
        if(age>=.6) return 1;
        return 1+(float)(Math.sin(age/.6*Math.PI)*.2);
    }
    public void clear() {bursts.clear();confetti.clear();}
}
