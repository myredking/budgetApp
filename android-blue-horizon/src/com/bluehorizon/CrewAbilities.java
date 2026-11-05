package com.bluehorizon;

public final class CrewAbilities {
    public static final String[] EFFECTS={"새 항구 첫 방문 시 명성 +1 (항상 적용)","매입가 5% 감소","항해 보급비 10% 감소","난파선 잠수·섬 내륙 탐험 성공률 +10%p","일반 항해·해상전 선체 피해 −2 (최소 1)","판매가 +5%p (총 판매 비율 상한 94%)"};
    public static final String[] GOALS={"항구 6곳 직접 방문","판매 대금 누적 5,000 G","해역 3곳 직접 방문","해역 기념품 2개 수집","해상전 2회 승리","납품 의뢰 3회 완료"};
    public static final String[] RECORDS={
        "서린은 방문한 항구들을 해도 위에 표시했다. 낯설던 점들은 이제 자신을 기억하는 사람들의 마을이었다.",
        "이안의 장부 마지막 장에는 금화 대신 이름이 적혀 있었다. 좋은 거래는 다음 만남을 남기는 법이라고 그는 웃었다.",
        "미라는 서로 다른 바다의 조류표를 나란히 펼쳤다. 어긋나는 기록도 함께 읽으면 하나의 항로가 되었다.",
        "나디아는 기념품에 발견 장소와 날짜를 적었다. 작은 물건 하나에도 잊히지 않아야 할 항해의 이야기가 있었다.",
        "타보는 전투가 끝난 갑판에서 선원들의 이름을 하나씩 불렀다. 그에게 승리란 모두가 다시 대답하는 순간이었다.",
        "아유는 납품을 기다리던 사람들의 편지를 건넸다. 물건이 제때 도착하는 것만으로도 항구의 하루는 달라졌다."
    };
    public static boolean active(Engine e,int person) {
        if(person==0) return true;
        return Crew.available(person,e.chapter) && (e.crew.first==person || e.crew.second==person);
    }
    public static boolean assign(Engine e,int slot,int person) {
        if(e.actionBlocked()) return Adventure.fail(e,"항해와 사건을 마친 뒤 담당을 변경하세요.");
        if(!validSelection(slot,person)) return Adventure.fail(e,"담당과 동료를 확인하세요.");
        if(person>=1 && !Crew.available(person,e.chapter)) return Adventure.fail(e,"아직 합류하지 않은 동료입니다.");
        int other=slot==0?e.crew.second:e.crew.first;
        if(person>=0 && other==person) return Adventure.fail(e,"같은 동료를 두 자리에 배치할 수 없습니다.");
        if(slot==0) e.crew.first=person;else e.crew.second=person;
        e.note("전문 담당 "+(slot+1)+": "+(person<0?"비워 두기":Crew.NAMES[person]));return true;
    }
    private static boolean validSelection(int slot,int person) { return slot>=0 && slot<=1 && person!=0 && person>=-1 && person<=5; }
    public static int buyAt(Engine e,int port,int good) { return Math.max(1,World.price(port,good)*(active(e,1)?95:100)/100); }
    public static int salePercent(Engine e,int port) { return Math.min(94,85+e.influence[port]*2+(active(e,5)?5:0)); }
    public static int supply(Engine e,int base) { return Math.max(1,base*(active(e,2)?90:100)/100); }
    public static int damage(Engine e,int raw) { return Math.max(1,raw-(active(e,4)?2:0)); }
    public static int explorationChance(Engine e,int base) { return Math.min(95,base+(active(e,3)?10:0)); }
    public static boolean storyReady(Engine e,int person) {
        if(!Crew.available(person,e.chapter)) return false;
        switch(person) {
            case 0:return e.visitedCount()>=6;
            case 1:return e.profit>=5000;
            case 2:return Achievements.regions(e)>=3;
            case 3:return Integer.bitCount(e.adventure.souvenirs)>=2;
            case 4:return e.adventure.battles>=2;
            case 5:return e.adventure.deliveries>=3;
            default:return false;
        }
    }
    public static boolean storyClaimed(Engine e,int person) { return (e.crew.stories & (1<<person))!=0; }
    public static boolean claimStory(Engine e,int person) {
        if(e.actionBlocked()) return Adventure.fail(e,"항해와 사건을 먼저 마치세요.");
        if(person<0 || person>=6) return Adventure.fail(e,"동료를 확인하세요.");
        if(!storyReady(e,person) || storyClaimed(e,person)) return Adventure.fail(e,"조건을 달성하지 않았거나 이미 연 기록입니다.");
        e.crew.stories|=1<<person;e.gold+=250+person*100;e.fame+=2;
        e.note(Crew.NAMES[person]+"의 기록을 열었다. +"+(250+person*100)+" G, 명성 +2.");e.advanceStory();return true;
    }
}
