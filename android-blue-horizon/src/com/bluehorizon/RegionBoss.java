package com.bluehorizon;

public final class RegionBoss {
    public static final String[] NAMES={"안개바다의 검은 돛","청동 해협의 수호선","붉은 모래의 약탈함","계절풍의 쌍돛함","비취 군도의 맹수","달빛 해류의 검선","태양 해역의 철갑함"};
    public static final String[] INTENTS={"집중 포격 예고 · 방어 사격으로 피해를 줄이세요","접근 전술 예고 · 먼저 포격해 밀어내세요","적이 방어 중 · 포격 피해가 6 줄어듭니다"};
    public static int maxHP(int region) { return 80+region*15; }
    public static int intent(Engine e) { return (e.expedition.bossTurn+e.expedition.bossRegion)%3; }
    public static boolean begin(Engine e) {
        if(e.actionBlocked()) return Adventure.fail(e,"진행 중인 항해와 사건을 먼저 마치세요.");
        int region=World.region(e.port),bit=1<<region;
        if((e.expedition.treasures & bit)==0) return Adventure.fail(e,"이 해역의 보물을 발견하면 강적의 항로가 열립니다.");
        if((e.expedition.trophies & bit)!=0) return Adventure.fail(e,"이미 이긴 해역 강적입니다.");
        if(e.level<1+region/3 || e.hull<40) return Adventure.fail(e,"선체 40 이상, 선박 등급 "+(1+region/3)+" 이상이 필요합니다.");
        e.expedition.bossRegion=region;e.expedition.bossHP=maxHP(region);e.expedition.bossTurn=0;
        e.note(NAMES[region]+"과 마주쳤다. 적의 다음 행동을 보고 전술을 선택하자.");return true;
    }
    public static boolean choose(Engine e,int choice) {
        if(!e.expedition.active() || choice<0 || choice>2) return Adventure.fail(e,"진행 중인 강적 전투를 확인하세요.");
        if(e.town.moving() || e.adventure.pending>=0) return Adventure.fail(e,"다른 항해 사건을 먼저 마치세요.");
        if(choice==2) return retreat(e,"강적의 추격을 피해 후퇴했다. 선체 −4, 사기 −5, 하루 경과.");
        int region=e.expedition.bossRegion,next=intent(e),damage=attack(e,choice,next);
        e.expedition.bossHP=Math.max(0,e.expedition.bossHP-damage);e.expedition.bossTurn++;
        if(e.expedition.bossHP==0) return victory(e,region);
        return response(e,choice,next,damage);
    }
    public static boolean choose(Engine e,int choice,int expectedTurn) {
        if(e.expedition.bossTurn!=expectedTurn) return Adventure.fail(e,"다음 턴의 전술을 확인하세요.");
        return choose(e,choice);
    }
    private static boolean response(Engine e,int choice,int next,int damage) {
        int region=e.expedition.bossRegion;
        int hit=new int[]{10+region*2,8+region,3+region}[next];if(choice==1) hit=Math.max(1,hit/3);
        hit=Refits.damage(e,CrewAbilities.damage(e,hit));Adventure.damage(e,hit);
        if(e.hull==1 || e.expedition.bossTurn>=30) return retreat(e,"피해를 줄이기 위해 전장을 벗어났다. 항구에서 수리하고 다시 준비하자.");
        e.note("강적 전투 "+e.expedition.bossTurn+"턴 · 적 선체 −"+damage+", 우리 선체 −"+hit+".");return true;
    }
    private static int attack(Engine e,int choice,int next) {
        int base=choice==0?12+e.level*5:8+e.level*3;
        return Math.max(3,base+Refits.attack(e,choice)+TownRpg.attackBonus(e)+CrewConversations.support(e)-(next==2?6:0));
    }
    private static boolean victory(Engine e,int region) {
        int reward=1200+region*300;e.gold+=reward;e.fame+=5;e.adventure.battles++;
        e.expedition.trophies|=1<<region;e.expedition.clearBattle();e.advanceStory();
        e.note(NAMES[region]+" 격파! 해역 훈장, +"+reward+" G, 명성 +5.");return true;
    }
    private static boolean retreat(Engine e,String message) {
        Adventure.damage(e,4);e.town.morale=Math.max(0,e.town.morale-5);e.day++;e.expedition.clearBattle();
        Rivals.advance(e);e.note(message);return true;
    }
}
