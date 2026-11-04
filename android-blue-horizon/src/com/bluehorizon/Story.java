package com.bluehorizon;

public final class Story {
    public static final int REWARD=700;
    public static final String[] TITLES = {"01  리스본의 의뢰","02  북쪽의 조류표","03  별을 읽는 도서관","04  남쪽 바람의 경계","05  계절풍의 약속","06  차에 담긴 항로","07  대서양의 두 관측소","08  모두를 위한 해도","항해는 계속된다"};
    public static final String[] GOALS = {
        "리스본에서 목재 5개를 사서 세비야에서 한 번에 판매하세요.",
        "런던·암스테르담을 직접 방문하고 명성 10을 모으세요. 판매·투자로 명성을 얻습니다.",
        "알렉산드리아를 직접 방문해 현지 교역소에 1단계 이상 투자하세요.",
        "캐러벨 이상의 선박을 마련하고 케이프타운에 직접 정박하세요.",
        "캘리컷을 직접 방문한 뒤 향신료 5개를 말라카에서 한 번에 판매하세요.",
        "항저우를 직접 방문한 뒤 차 5개를 나가사키에서 한 번에 판매하세요.",
        "하바나·페르남부쿠를 직접 방문하고 명성 40을 모으세요.",
        "금화 12,000 G 이상을 모아 리스본으로 돌아오세요.",
        "세계 해도가 완성되었습니다. 56개 항구를 모두 방문하고 교역 동맹을 넓혀 보세요."
    };
    public static final String[] REVEALS = {
        "세비야의 조선공이 북쪽 조류표의 사본을 건넸다. 이안: 런던과 암스테르담의 항해 기록을 비교해 보자. [700 G]",
        "두 기록의 별자리가 서로 다르다. 항해사 미라는 알렉산드리아의 도서관에 옛 관측법이 남아 있다고 했다. [700 G]",
        "사서 나디아가 남반구의 관측 노트를 찾았다. 북쪽 별만으로는 남쪽 바다를 읽을 수 없다. 더 큰 배로 아프리카 남단에 가자. [700 G]",
        "케이프타운에서 선원 타보가 합류했다. 그는 계절풍을 기다리는 상인들의 약속을 들려주었다. 캘리컷과 말라카를 교역으로 잇자. [700 G]",
        "말라카의 상인 아유가 동쪽의 해류 기록을 나눴다. 항저우의 차 상인과 나가사키의 선원들이 마지막 사본을 보관하고 있다. [700 G]",
        "동쪽 기록에는 대서양으로 이어지는 해류의 흔적이 있었다. 이제 하바나와 페르남부쿠의 관측소를 찾아 서로의 기록을 맞추자. [700 G]",
        "대서양의 기록까지 모였다. 서린은 어느 항구도 다른 항구의 지식을 독점하지 않는 공동 해도를 만들기로 한다. 리스본에서 출판 비용을 마련하자. [700 G]",
        "리스본에서 세계 해도가 공개되었다. 각 항구의 이름 옆에는 기록을 나눈 사람들의 이름이 적혔다. 이안: 다음 항해의 빈칸은 우리가 함께 채우자. [700 G]"
    };
    public static boolean ready(Engine e) {
        switch(e.chapter) {
            case 1: return northernReady(e);
            case 2: return e.hasVisited(13) && e.influence[13]>=1;
            case 3: return e.hasVisited(21) && e.level>=2;
            case 6: return atlanticReady(e);
            case 7: return e.port==0 && e.gold>=12000;
            default: return false;
        }
    }
    private static boolean northernReady(Engine e) { return e.hasVisited(2) && e.hasVisited(3) && e.fame>=10; }
    private static boolean atlanticReady(Engine e) { return e.hasVisited(48) && e.hasVisited(53) && e.fame>=40; }
    private static boolean deliveryAt(Engine e,int good,int target,int product) { return e.port==target && good==product; }
    public static boolean deliveryReady(Engine e,int good) {
        switch(e.chapter) {
            case 0: return deliveryAt(e,good,1,0);
            case 4: return deliveryAt(e,good,32,3) && e.hasVisited(28);
            case 5: return deliveryAt(e,good,44,4) && e.hasVisited(42);
            default: return false;
        }
    }
    public static String introduction() {
        return "리스본의 젊은 선장 서린은 여러 항구의 관측 기록을 하나로 잇는 공동 해도를 만들기로 한다.\n\n친구 이안과 낡은 슬루프에 올라 북해에서 지중해, 희망봉과 인도양을 지나 동아시아와 아메리카까지 항해한다.\n\n첫 의뢰: 리스본에서 목재 5개를 사서 세비야에 판매하자.\n\n일곱 해역의 기록을 모으고 각지의 항해사들과 새 항로를 열어 보세요.";
    }
}
