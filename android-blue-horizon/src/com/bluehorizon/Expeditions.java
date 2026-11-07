package com.bluehorizon;

public final class Expeditions {
    public static final String[] TREASURES={"북극성 나침반","청동 천구의","해풍의 수정","계절풍 관측판","청록 항로석","비취 별자리판","태양의 해도함"};
    public static int fragments(Engine e,int region) { return Long.bitCount(e.expedition.clues>>>(region*8) & 255L); }
    public static int site(int region) { return region*8+7; }
    public static boolean clue(Engine e) {
        if(e.actionBlocked()) return Adventure.fail(e,"항해와 전투를 먼저 마치세요.");
        long bit=1L<<e.port;
        if((e.expedition.clues & bit)!=0) return Adventure.fail(e,"이 항구의 단서는 이미 기록했습니다.");
        if(!Adventure.spend(e,40)) return false;
        e.expedition.clues|=bit;GameClock.advance(e,1);Rivals.advance(e);
        e.note(World.PORTS[e.port]+"의 오래된 기록에서 보물지도 조각을 찾았다. −40 G, 하루 경과.");return true;
    }
    public static boolean dig(Engine e) {
        if(e.actionBlocked()) return Adventure.fail(e,"항해와 전투를 먼저 마치세요.");
        int region=World.region(e.port),bit=1<<region;
        if((e.expedition.treasures & bit)!=0) return Adventure.fail(e,"이미 발견한 보물입니다.");
        if(fragments(e,region)<3) return Adventure.fail(e,"이 해역의 서로 다른 항구에서 지도 조각 3개가 필요합니다.");
        if(e.port!=site(region)) return Adventure.fail(e,World.PORTS[site(region)]+"의 탐사 지점으로 가세요.");
        if(!Adventure.spend(e,80)) return false;
        int reward=900+region*150;e.gold+=reward;e.fame+=3;GameClock.advance(e,1);e.town.morale=Math.min(100,e.town.morale+10);
        e.expedition.treasures|=bit;Rivals.advance(e);e.advanceStory();
        e.note(TREASURES[region]+" 발견! +"+reward+" G (탐사비 80 G 별도), 명성 +3, 사기 +10. 해역 강적의 항로를 알아냈다.");return true;
    }
}
