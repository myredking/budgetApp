package com.bluehorizon;

public final class Adventure {
    public static boolean sail(Engine e,int target,int mode) {
        if(!e.sail(target,mode)) return false;
        AdventureState a=e.adventure;a.voyages++;
        if(a.voyages<2) return true;
        if(mode==1 && a.voyages%2!=0) return true;
        int kind=a.draw(8);if(kind==a.lastKind) kind=(kind+3)%8;
        a.pending=kind;a.lastKind=kind;a.roll=a.draw(100);
        e.note(EventCatalog.TITLES[kind]+" · 선장의 선택을 기다립니다.");
        return true;
    }
    public static boolean fail(Engine e,String message) { e.message=message;return false; }
    public static boolean spend(Engine e,int amount) {
        if(e.gold<amount) return fail(e,"금화가 부족합니다. 다른 선택을 할 수 있습니다.");
        e.gold-=amount;return true;
    }
    public static void damage(Engine e,int amount) { e.hull=Math.max(1,e.hull-amount); }
    public static boolean takeCargo(Engine e) {
        for(int good=0;good<6;good++) {
            if(e.inventory[good]>0) { e.inventory[good]--;return true; }
        }
        return false;
    }
    public static boolean finish(Engine e,String text) {
        AdventureState a=e.adventure;a.pending=-1;a.roll=0;a.battleHP=0;a.battleTurn=0;a.resolved++;
        e.note(text);e.advanceStory();e.message=text;return true;
    }
    public static void souvenir(Engine e) { e.adventure.souvenirs|=1<<World.region(e.port); }
}
