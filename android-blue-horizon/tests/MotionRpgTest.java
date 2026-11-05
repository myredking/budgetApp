package com.bluehorizon;

public final class MotionRpgTest {
    private static int checks;
    private static void check(boolean value,String text) { checks++;if(!value) throw new AssertionError(text); }
    private static void walking() {
        for(int place=0;place<4;place++) {
            float[] start=TownWalk.position(place,0),end=TownWalk.position(place,1),target=TownLayout.point(place);
            check(start[0]==.64f && start[1]==.94f,"captain starts beside square label");
            check(Math.abs(end[0]-target[0])<.001 && Math.abs(end[1]-(target[1]+.05))<.001,"captain reaches facility path");
            for(int step=0;step<=100;step++) {
                float[] p=TownWalk.position(place,step/100f);
                check(p[0]>=0 && p[0]<=1 && p[1]>=0 && p[1]<=1,"walk stays inside town");
            }
        }
    }
    private static void growth() {
        Engine e=new Engine();e.town.morale=100;
        check(TownRpg.rankBonus(e)==0 && TownRpg.attackBonus(e)==2,"opening combat unchanged");
        e.adventure.resolved=40;
        check(TownRpg.rank(e)==5 && TownRpg.rankBonus(e)==1,"exploration experience gives real combat growth");
        check(TownRpg.attackBonus(e)==3,"rank and morale bonuses combine");
        e.adventure.resolved=1000;
        check(TownRpg.rankBonus(e)==3,"growth capped to preserve combat choices");
        check(Engine.decode(e.encode())!=null,"growth uses existing saved progress");
    }
    private static void headingAndBounds() {
        ShipMotion motion=new ShipMotion(0,1);
        for(int step=0;step<=100;step++) check(Float.isFinite(motion.heading(step/100.0)),"finite route heading");
        for(int angle=0;angle<360;angle+=5) {
            float size=ShipMotion.fitSize(360,500,angle);
            double a=Math.toRadians(angle),w=size*(Math.abs(Math.cos(a))+2*Math.abs(Math.sin(a)));
            check(w<=312.1,"rotated ship leaves room for sway");
        }
    }
    public static void main(String[] args) {
        walking();growth();headingAndBounds();System.out.println("PASS: "+checks+" RPG and motion checks");
    }
}
