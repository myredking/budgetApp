package com.bluehorizon;

public final class Rivals {
    public static final String[] NAMES={"아스트라 상회","청람 교역단","사프란 연맹"};
    public static final String[] LEADERS={"레온","린","사미르"};
    public static final String[] REGIONS={"북해·서유럽 / 지중해 / 아메리카","동남아시아 / 동아시아","아프리카 / 인도양"};
    private static final int[] OWNERS={0,0,2,2,1,1,0};
    public static int owner(int port) { return OWNERS[World.region(port)]; }
    public static boolean allied(Engine e,int rival) { return (e.rivals.allies & (1<<rival))!=0; }
    public static void initialise(Engine e) {
        e.rivals.lastDay=e.day;
        for(int region=0;region<7;region++) {
            int investment=0;for(int port:World.portsIn(region)) investment+=e.influence[port];
            e.rivals.pressure[region]=Math.max(0,35-investment*3);
        }
    }
    public static void advance(Engine e) {
        int periods=(e.day-e.rivals.lastDay)/7;if(periods<=0) return;
        e.rivals.lastDay+=periods*7;int steps=Math.min(8,periods);
        for(int region=0;region<7;region++) {
            int change=allied(e,OWNERS[region])?-steps:steps;
            e.rivals.pressure[region]=Math.max(0,Math.min(80,e.rivals.pressure[region]+change));
        }
    }
    public static int buyFee(Engine e,int port) {
        return !allied(e,owner(port)) && e.rivals.pressure[World.region(port)]>=60?3:0;
    }
    public static int saleBonus(Engine e,int port) {
        return (allied(e,owner(port))?2:0)+(e.rivals.pressure[World.region(port)]==0?2:0);
    }
    public static int buyPrice(Engine e,int port,int base) { return base*(100+buyFee(e,port))/100; }
    public static void onSale(Engine e,int amount) {
        adjustPressure(e,World.region(e.port),-Math.max(1,amount/5));
        if(!allied(e,owner(e.port))) relation(e,owner(e.port),-1);
    }
    public static void onInvest(Engine e) {
        adjustPressure(e,World.region(e.port),-5);
        if(!allied(e,owner(e.port))) relation(e,owner(e.port),-2);
    }
    public static void adjustPressure(Engine e,int region,int delta) {
        e.rivals.pressure[region]=Math.max(0,Math.min(80,e.rivals.pressure[region]+delta));
    }
    public static void relation(Engine e,int rival,int delta) { e.rivals.relations[rival]=Math.max(-50,Math.min(100,e.rivals.relations[rival]+delta)); }
    public static boolean negotiate(Engine e,int rival,int choice) {
        if(!mayAct(e,rival)) return false;
        if(choice<0 || choice>2) return Adventure.fail(e,"협상 방식을 확인하세요.");
        if(e.day-e.rivals.lastNegotiation[rival]<7) return Adventure.fail(e,"같은 상단과의 서신 교류는 7일 간격입니다.");
        boolean success;
        if(choice==0) success=gift(e,rival);
        else if(choice==1) success=letter(e,rival);
        else success=challenge(e,rival);
        if(!success) return false;
        e.day++;e.rivals.lastNegotiation[rival]=e.day;advance(e);e.advanceStory();return true;
    }
    private static boolean gift(Engine e,int rival) {
        if(e.rivals.relations[rival]>=100) return Adventure.fail(e,"이미 충분한 신뢰를 얻었습니다.");
        if(!Adventure.spend(e,150)) return false;
        relation(e,rival,12);e.note(NAMES[rival]+"에 교역 선물을 보냈다. −150 G, 관계 +12, 하루 경과.");return true;
    }
    private static boolean letter(Engine e,int rival) {
        if(e.adventure.deliveries<=e.rivals.letters[rival]) return Adventure.fail(e,"새로 완료한 납품 의뢰 실적이 필요합니다.");
        if(e.rivals.relations[rival]>=100) return Adventure.fail(e,"이미 충분한 신뢰를 얻었습니다.");
        e.rivals.letters[rival]++;relation(e,rival,10);
        e.note(NAMES[rival]+"와 납품 실적을 공유했다. 관계 +10, 하루 경과.");return true;
    }
    private static boolean challenge(Engine e,int rival) {
        if(allied(e,rival) || owner(e.port)!=rival || e.fame<20) return Adventure.fail(e,"비동맹 기반 해역에서 명성 20 이상이 필요합니다.");
        if(e.rivals.pressure[World.region(e.port)]==0) return Adventure.fail(e,"이미 이 해역의 경쟁 압력을 낮췄습니다.");
        adjustPressure(e,World.region(e.port),-8);relation(e,rival,-8);
        e.note("현지 거래처에 공개 경쟁을 제안했다. 경쟁 압력 −8, 관계 −8, 하루 경과.");return true;
    }
    public static boolean formAlliance(Engine e,int rival) {
        if(!mayAct(e,rival)) return false;
        if(allied(e,rival)) return Adventure.fail(e,"이미 동맹인 상단입니다.");
        if(e.rivals.relations[rival]<40 || e.fame<15) return Adventure.fail(e,"관계 40과 명성 15가 필요합니다.");
        if(!Adventure.spend(e,500)) return false;
        e.rivals.allies|=1<<rival;relation(e,rival,10);e.day++;advance(e);
        e.note(NAMES[rival]+"와 교역 동맹을 맺었다. −500 G, 관계 +10, 하루 경과.");return true;
    }
    public static boolean endAlliance(Engine e,int rival) {
        if(!mayAct(e,rival)) return false;
        if(!allied(e,rival)) return Adventure.fail(e,"동맹 상태가 아닙니다.");
        e.rivals.allies&=~(1<<rival);relation(e,rival,-20);e.day++;advance(e);
        e.note(NAMES[rival]+"와의 동맹을 종료했다. 관계 −20, 하루 경과.");return true;
    }
    public static boolean mayAct(Engine e,int rival) {
        if(e.actionBlocked()) return Adventure.fail(e,"항해와 사건을 먼저 마치세요.");
        if(rival<0 || rival>=3) return Adventure.fail(e,"상단을 확인하세요.");
        return true;
    }
    public static String dialogue(Engine e,int rival) {
        if(allied(e,rival)) return "함께 지킨 약속이 다음 항로를 엽니다. 우리 거래처를 이용하세요.";
        if(e.rivals.relations[rival]<0) return "거래처를 두고 경쟁할 수는 있죠. 그래도 서신의 문은 열어 두겠습니다.";
        return "항로에는 경쟁자도, 동반자도 필요합니다. 당신은 어떤 거래를 제안하겠습니까?";
    }
}
