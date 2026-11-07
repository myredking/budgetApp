package com.towerfront;

public final class ControlsTest {
    private static void check(boolean ok,String message) {
        if(!ok) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        Engine e=new Engine(1);
        BattleControls c=new BattleControls(e);
        c.press(0);check(c.release(0)==0 && c.selected==0,"tap selects ally");
        c.press(2);check(c.release(2)==1,"second tap dispatches");
        check(c.selected==-1 && e.fleets.size()==1,"selection reset");
        c.press(1);check(c.release(0)==0,"enemy cannot command");
        c.press(0);c.cancel();check(c.selected==-1,"cancel clears selection");
        Engine full=new Engine(1);BattleControls d=new BattleControls(full);
        d.all=true;d.press(0);check(d.release(2)==1,"drag dispatches");
        check(full.towers.get(0).units==0 && full.fleets.get(0).units==32,"all troops sent");
        check(BattleControls.hit(full,BattleControls.x(full.towers.get(0)),
            BattleControls.y(full.towers.get(0)))==0,"tower hit location");
        check(BattleControls.hit(full,-100,-100)==-1,"empty hit safe");
        Engine over=new Engine(1);for(Engine.Tower t:over.towers) t.owner=1;
        check(!over.sendAll(0,2),"full send blocked after victory");
        Engine one=new Engine(1);one.towers.get(0).units=1;
        check(one.sendAll(0,2),"full dispatch may send final unit");
        touchTolerance();
        previewChecks();
        toggleChecks();
        System.out.println("Touch and full dispatch checks passed");
    }
    private static void toggleChecks() {
        Engine e=new Engine(1);BattleControls c=new BattleControls(e);
        c.press(0);c.release(0);check(c.selected==0,"first tap keeps selection");
        String before=SaveCodec.encode(e);
        c.press(0);check(c.selected==0,"pressing selected tower still allows dragging");
        c.release(0);check(c.selected==-1,"second tap on same tower deselects");
        check(before.equals(SaveCodec.encode(e)),"deselect does not change troops or dispatch");
        c.press(0);c.release(0);check(c.selected==0,"third tap can select again");
        c.press(0);check(c.release(2)==1,"dragging selected tower to target still dispatches");
        check(e.fleets.size()==1,"repeat press does not duplicate dispatch");
        c.press(0);c.cancel();c.press(0);c.release(0);
        check(c.selected==0,"cancel clears repeat-tap state");
    }
    private static void touchTolerance() {
        Engine e=new Engine(1);Engine.Tower t=e.towers.get(0);
        float x=BattleControls.x(t),y=BattleControls.y(t);
        check(BattleControls.hitTouch(e,x+60,y,70)==0,"near miss selects small tower");
        check(BattleControls.hitTouch(e,x,y,70)==BattleControls.hit(e,x,y),"direct hit wins");
        check(BattleControls.hitTouch(e,-100,-100,70)==-1,"distant ground stays empty");
        check(BattleControls.hitTouch(e,x+60,y,20)==-1,"radius follows physical zoom");
        check(BattleControls.releaseTarget(0,2,3,8)==0,"finger jitter keeps pressed tower");
        check(BattleControls.releaseTarget(0,2,20,8)==2,"intentional drag reaches target");
    }
    private static void previewChecks() {
        Engine e=new Engine(1);BattleControls c=new BattleControls(e);
        check(c.previewUnits(2)==0,"no selection has no preview");
        c.press(0);e.towers.get(0).units=31;
        check(c.previewUnits(2)==15,"half preview rounds down");
        check(c.previewUnits(0)==0 && c.previewUnits(-1)==0,"self and empty ground cancel");
        check(c.previewUnits(999)==0,"invalid target safe");
        check(e.fleets.isEmpty() && e.towers.get(0).units==31,"preview does not send");
        c.all=true;check(c.previewUnits(2)==31,"full preview sends all");
        e.towers.get(2).owner=1;
        check(c.previewUnits(2)==31,"allied support uses same amount");
        e.towers.get(2).owner=0;
        e.towers.get(0).units=1;c.all=false;
        check(c.previewUnits(2)==0,"half cannot send final troop");
        c.all=true;check(c.previewUnits(2)==1,"full can send final troop");
        check(c.release(2)==1 && e.fleets.get(0).units==1,"actual dispatch matches preview");
        c.press(0);e.towers.get(0).owner=2;
        check(c.previewUnits(2)==0,"captured source cannot preview");
        Engine ended=new Engine(1);BattleControls done=new BattleControls(ended);done.press(0);
        for(Engine.Tower tower:ended.towers) tower.owner=1;
        check(done.previewUnits(2)==0,"completed battle cannot preview dispatch");
    }
}
