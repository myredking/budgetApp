package com.towerfront;
public final class GraphicsTest {
    private static void check(boolean ok,String text) {
        if(!ok) throw new AssertionError(text);
    }
    public static void main(String[] args) {
        Engine e=new Engine(8);
        String before=SaveCodec.encode(e);
        int[] order=SceneStyle.towerOrder(e);
        check(order.length==e.towers.size(),"all towers rendered");
        for(int i=1;i<order.length;i++) check(e.towers.get(order[i-1]).y<=e.towers.get(order[i]).y,"depth order");
        check(before.equals(SaveCodec.encode(e)),"render preparation cannot change gameplay");
        check(SceneStyle.squadSize(99)==12,"bounded unit sprites");
        check(SceneStyle.squadSize(0)==0 && SceneStyle.squadSize(1)==1,"unit sprite edges");
        check(SceneStyle.team(1)!=SceneStyle.team(2),"clear team colors");
        check(SceneStyle.title(0).equals("지휘 본부") && SceneStyle.title(3).equals("방어 요새"),"building identities");
        Engine hit=new Engine(1);Engine.Tower base=hit.towers.get(0);
        check(BattleControls.hit(hit,BattleControls.x(base),BattleControls.y(base)-54)==0,"roof is clickable");
        hit.towers.get(2).x=base.x;hit.towers.get(2).y=base.y-.03;
        check(BattleControls.hit(hit,BattleControls.x(base),BattleControls.y(base)-20)==0,"frontmost building receives overlap tap");
        System.out.println("Graphics depth, unit budget and state isolation checks passed");
    }
}
