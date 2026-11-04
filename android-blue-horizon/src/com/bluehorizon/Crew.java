package com.bluehorizon;

public final class Crew {
    public static final String[] NAMES={"서린","이안","미라","나디아","타보","아유"};
    public static final String[] ROLES={"선장 · 공동 해도의 제안자","교역 동료 · 첫 항해의 친구","항해사 · 별과 조류의 관측자","사서 · 알렉산드리아의 기록지기","선원 · 남쪽 바람의 길잡이","상인 · 계절풍 무역의 동반자"};
    public static final String[] BIO={
        "다른 항구의 지식이 모이면 누구나 더 멀리 갈 수 있다고 믿는다. 리스본에서 공동 해도를 만들기 위해 출항했다.",
        "장부와 사람의 마음을 함께 읽는 낙천적인 교역 동료. 오랜 친구 서린과 함께 첫 배에 올랐다.",
        "북쪽 항구들의 조류표를 비교해 항로의 빈칸을 찾아낸다. 오래된 기록을 새 눈으로 읽는 침착한 항해사.",
        "알렉산드리아에서 여러 언어의 관측 노트를 보관한다. 누군가의 기록이 모두의 지식이 되길 바란다.",
        "아프리카 남단의 거친 물결을 아는 선원. 기상보다 먼저 선원들의 지친 표정을 알아보는 든든한 동료.",
        "계절풍을 기다리는 상인들의 약속을 이어 온 무역상. 거래의 이익뿐 아니라 항구 사이의 신뢰를 소중히 여긴다."
    };
    public static final int[] JOINS={0,0,2,3,4,5};
    public static boolean available(int index,int chapter) { return chapter>=JOINS[index]; }
    public static int[] cell(int index,int width,int height) {
        if(index<0 || index>=6 || width<3 || height<2) throw new IllegalArgumentException("Invalid portrait atlas");
        return new int[]{index%3*width/3,index/3*height/2,(index%3+1)*width/3,(index/3+1)*height/2};
    }
    public static int guideRegion(int chapter) {
        int[] regions={0,0,1,2,3,5,6,0,-1};return regions[chapter];
    }
    public static int guideRegion(Engine game) {
        if(game.chapter==4 && game.hasVisited(28)) return 4;
        return guideRegion(game.chapter);
    }
    public static int guidePerson(int chapter) { return new int[]{1,1,2,3,4,5,5,0,0}[chapter]; }
    public static final String[] GUIDES={
        "첫 항해니까 작게 시작하자. 목재 다섯 개면 충분해!",
        "북쪽 두 항구의 조류표를 비교해 보자. 거래로 명성도 쌓고.",
        "별자리의 기록이 달라. 알렉산드리아 도서관에 답이 있을 거야.",
        "남쪽 하늘에는 다른 별이 떠요. 큰 배를 마련해 남단으로 가세요.",
        "바람에도 때가 있지. 캘리컷의 향신료를 말라카로 옮겨 보자.",
        "동쪽 해류의 기록은 차 상인들이 지키고 있어요. 항저우와 나가사키를 이어요.",
        "이제 대서양 양쪽의 기록을 맞출 차례예요. 긴 항해의 보급비를 챙기세요.",
        "마지막은 고향이야. 모두의 이름이 담긴 해도를 출판하자.",
        "우리의 해도가 완성됐어. 이제 다음 항해의 빈칸을 찾아볼까?"
    };
}
