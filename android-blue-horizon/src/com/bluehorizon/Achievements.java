package com.bluehorizon;

public final class Achievements {
    public static final String[] NAMES={"첫 교역","세 해역의 항해자","일곱 바다의 수집가","해적을 물리친 선장","바다의 구조자","믿음직한 운송인","세계의 모든 항구","공동 해도의 완성"};
    public static final String[] GOALS={"상품을 한 번 판매하기","서로 다른 3개 해역에 직접 정박","7개 해역 기념품 모두 수집","해상전 1회 승리","조난자 1회 직접 구조","납품 의뢰 3회 완료","56개 항구 모두 직접 방문","창작 이야기 8장 완료"};
    public static final int[] REWARDS={150,300,700,250,200,400,1500,1000};
    public static boolean ready(Engine e,int index) {
        switch(index) {
            case 0:return e.profit>0;
            case 1:return regions(e)>=3;
            case 2:return e.adventure.souvenirs==127;
            case 3:return e.adventure.battles>=1;
            case 4:return e.adventure.rescues>=1;
            case 5:return e.adventure.deliveries>=3;
            case 6:return e.visitedCount()==World.COUNT;
            case 7:return e.chapter==Story.REVEALS.length;
            default:return false;
        }
    }
    public static boolean claimed(Engine e,int index) { return (e.adventure.claims & (1<<index))!=0; }
    public static boolean claim(Engine e,int index) {
        if(e.adventure.pending>=0) return Adventure.fail(e,"항해 사건을 먼저 처리하세요.");
        if(index<0 || index>=8) return Adventure.fail(e,"달성 기록을 확인하세요.");
        if(!ready(e,index) || claimed(e,index)) return Adventure.fail(e,"아직 달성하지 않았거나 이미 받은 보상입니다.");
        e.adventure.claims|=1<<index;e.gold+=REWARDS[index];e.fame++;
        e.note("달성 기록: "+NAMES[index]+" · +"+REWARDS[index]+" G, 명성 +1.");e.advanceStory();return true;
    }
    public static int regions(Engine e) {
        int regions=0;
        for(int port=0;port<World.COUNT;port++) if(e.hasVisited(port)) regions|=1<<World.region(port);
        return Integer.bitCount(regions);
    }
}
