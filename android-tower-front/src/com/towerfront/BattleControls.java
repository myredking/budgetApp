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
            if(insideBuilding(t,x,y)) return order[i];
        }
        return -1;
    }
    private static boolean insideBuilding(Engine.Tower t,float px,float py) {
        return px>=x(t)-45 && px<=x(t)+45 && py>=y(t)-68 && py<=y(t)+47;
    }
    public void validate() {
        if(selected<0) return;
        if(engine.towers.get(selected).owner!=1) selected=-1;
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
