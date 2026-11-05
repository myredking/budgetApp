package com.bluehorizon;

public final class SeaBattle {
    public static boolean choose(Engine e,int choice) {
        if(choice==2) return retreat(e);
        AdventureState a=e.adventure;a.battleTurn++;
        int attack=choice==0?10+e.level*4+a.draw(5):6+e.level*3+a.draw(4);
        a.battleHP=Math.max(0,a.battleHP-attack);
        if(a.battleHP==0) return victory(e);
        int damage=4+World.region(e.port)+a.draw(4);if(choice==1) damage=Math.max(1,damage-4);
        damage=CrewAbilities.damage(e,damage);
        Adventure.damage(e,damage);
        if(e.hull==1 || a.battleTurn>=12) return defeat(e);
        e.note("전술 "+a.battleTurn+"턴 · 해적 선체 −"+attack+", 우리 선체 −"+damage+".");return true;
    }
    private static boolean victory(Engine e) {
        int reward=180+World.region(e.port)*35;e.gold+=reward;e.fame+=4;e.adventure.battles++;
        return Adventure.finish(e,"해상전 승리! 전리품 +"+reward+" G, 명성 +4.");
    }
    private static boolean defeat(Engine e) {
        int loss=Math.min(e.gold,120);e.gold-=loss;Adventure.takeCargo(e);
        return Adventure.finish(e,"충돌을 피해 가까스로 벗어났다. −"+loss+" G, 보유 화물 1개 유실. 항구에서 수리하자.");
    }
    private static boolean retreat(Engine e) {
        boolean cargo=Adventure.takeCargo(e);if(!cargo) e.gold-=Math.min(40,e.gold);
        e.day++;Adventure.damage(e,3);
        return Adventure.finish(e,"짐을 줄이고 전장을 벗어났다. "+(cargo?"화물 1개 유실":"최대 40 G 소모")+", 선체 −3, 하루 경과.");
    }
}
