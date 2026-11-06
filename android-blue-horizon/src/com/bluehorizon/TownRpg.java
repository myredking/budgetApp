package com.bluehorizon;

public final class TownRpg {
    public static final String[] FLAVORS={
        "벽돌 창고 사이로 돛줄 소리와 갈매기 울음이 들려온다.",
        "하얀 벽과 붉은 지붕 사이로 짭짤한 바닷바람이 지나간다.",
        "햇살 아래 천막이 펄럭이고, 상인들이 다채로운 직물을 펼친다.",
        "향신료 냄새가 가득한 골목에서 저울추 부딪치는 소리가 난다.",
        "야자수 그늘 아래 작은 배들이 화물을 나르고 있다.",
        "기와지붕 너머로 종소리가 번지고, 차와 도자기 상자가 부두에 쌓인다.",
        "색색의 벽과 꽃으로 장식된 골목에 먼바다 선원들이 모여든다."
    };
    public static int style(int port) { return port<2?1:World.region(port); }
    public static int experience(Engine e) {
        long value=(e.visitedCount()-1)*10L+e.adventure.resolved*8L+e.adventure.battles*12L
            +e.adventure.deliveries*16L+e.chapter*20L+Long.bitCount(e.town.rumors)*6L
            +Long.bitCount(e.expedition.clues)*8L+Integer.bitCount(e.expedition.treasures)*30L+Integer.bitCount(e.expedition.talks)*12L;
        return (int)Math.min(100000000,value);
    }
    public static int rank(Engine e) { return Math.min(20,1+experience(e)/80); }
    public static int rankBonus(Engine e) { return Math.min(3,rank(e)/5); }
    public static int supplyCost(Engine e,int cost) { return e.town.morale<25?(cost*105+99)/100:cost; }
    public static int attackBonus(Engine e) {
        if(e.town.morale>=85) return 2+rankBonus(e);
        return (e.town.morale<25?-1:0)+rankBonus(e);
    }
    public static boolean rest(Engine e) {
        if(e.actionBlocked()) return Adventure.fail(e,"항해와 사건을 마친 뒤 쉬세요.");
        if(e.town.morale==100) return Adventure.fail(e,"일행의 사기가 충분합니다.");
        if(!Adventure.spend(e,60)) return false;
        e.town.morale=Math.min(100,e.town.morale+25);e.day++;Rivals.advance(e);
        e.note("주점에서 식사하고 쉬었다. −60 G, 하루 경과, 사기 +25(최대 100).");e.advanceStory();return true;
    }
    private static TradeHint localHint(Engine e) {
        int good=0;for(int i=1;i<6;i++) if(e.buyPrice(i)<e.buyPrice(good)) good=i;
        return TradeHint.best(e,good,5);
    }
    public static int rumorTarget(Engine e) { TradeHint hint=localHint(e);return hint.net>0?hint.port:-1; }
    public static String rumor(Engine e) {
        TradeHint hint=localHint(e);
        if(hint.net<=0) return "이 항구에서는 작은 거래부터 시작해 보세요. 보급비를 남기고 조선소에서 배를 키우는 게 좋아요.";
        return "선원들 사이에선 "+World.PORTS[hint.port]+" 항구 이야기가 들려요. 이곳의 저가 상품 5개를 사서 옮기면 현시세로 약 "+hint.net+" G 차익이래요. 수리·귀환비는 별도이고 도착 시 시세는 달라질 수 있어요.";
    }
    public static boolean hearRumor(Engine e) {
        if(e.actionBlocked()) return Adventure.fail(e,"항해와 사건을 먼저 마치세요.");
        String text=rumor(e);long bit=1L<<e.port;
        if((e.town.rumors & bit)==0) {
            e.town.rumors|=bit;e.fame++;e.note("현지 소문을 처음 들었다. 명성 +1.");e.advanceStory();
        }
        e.message=text;return true;
    }
}
