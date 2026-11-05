package com.towerfront;

public final class BattleControls {
    private final Engine engine;
    public int selected=-1;
    public boolean all;
    private boolean pressed;
    public BattleControls(Engine battle) {engine=battle;}
    public static float x(Engine.Tower t) {return 32+(float)t.x*336;}
    public static float y(Engine.Tower t) {return 176+(float)t.y*470;}
    public static int hit(Engine e,float x,float y) {
        int[] order=SceneStyle.towerOrder(e);
        for(int i=order.length-1;i>=0;i--) {
            Engine.Tower t=e.towers.get(order[i]);
            if(insideBuilding(t,x,y,SceneStyle.buildingScale(e))) return order[i];
        }
        return -1;
    }
    private static boolean insideBuilding(Engine.Tower t,float px,float py,float scale) {
        return px>=x(t)-45*scale && px<=x(t)+45*scale && py>=y(t)-68*scale && py<=y(t)+47*Math.max(.8f,scale);
    }
    public static int hitTouch(Engine e,float px,float py,float radius) {
        int direct=hit(e,px,py);
        if(direct>=0) return direct;
        int nearest=-1;double distance=radius*radius;
        for(int i=0;i<e.towers.size();i++) {
            Engine.Tower t=e.towers.get(i);
            double dx=px-x(t),dy=py-y(t),candidate=dx*dx+dy*dy;
            if(candidate<distance) {distance=candidate;nearest=i;}
        }
        return nearest;
    }
    public void validate() {
        if(selected<0) return;
        if(engine.towers.get(selected).owner!=1) selected=-1;
    }
    public static int releaseTarget(int start,int end,double movement,float slop) {
        return movement<=slop?start:end;
    }
    public void press(int index) {
        validate();pressed=index>=0;
        if(!pressed || index>=engine.towers.size()) return;
        if(selected<0 && engine.towers.get(index).owner==1) selected=index;
    }
    public int release(int target) {
        validate();
        if(!pressed) return 0;
        pressed=false;
        if(selected<0 || target<0 || target==selected) return 0;
        boolean sent=all?engine.sendAll(selected,target):engine.send(selected,target);
        selected=-1;return sent?1:-1;
    }
    public void cancel() {selected=-1;pressed=false;}
}
