package com.bluehorizon;

public final class EventCatalog {
    public static final String[] TITLES={"갑작스러운 폭풍","낯선 해적선","파도 위의 난파선","조난 신호","항구의 축제","밀봉 상자의 제안","돌고래와 순풍","지도 밖의 작은 섬"};
    private static final String[] STORIES={
        "하늘이 어두워지고 바람이 돛을 거칠게 잡아당긴다. 선원들이 선장의 결정을 기다린다.",
        "무장한 배가 항로를 막았다. 싸울지, 교섭할지, 안개를 이용할지 결정해야 한다.",
        "기울어진 배의 일부가 수면 위로 드러났다. 아직 건질 만한 물건이 남아 있을지도 모른다.",
        "멀리서 작은 구조 깃발이 흔들린다. 보급과 시간을 나누면 누군가의 항해를 구할 수 있다.",
        "도착한 항구에서 축제가 열렸다. 주민들이 먼바다의 손님을 반갑게 맞는다.",
        "상인이 밀봉된 상자를 은밀히 내민다. 값진 물건일 수도, 거의 빈 상자일 수도 있다.",
        "돌고래 떼가 배 옆을 지나고 바람이 부드럽게 돛을 채운다. 잠시 여유가 생겼다.",
        "해도에 자세히 기록되지 않은 작은 섬이 보인다. 해변 너머에는 오래된 건물의 흔적이 있다."
    };
    private static final String[][] OPTIONS={
        {"돛 줄이기 · 선체 −4, 하루 경과","정면 돌파 · 성공 65%: 선체 −1/실패 −16·화물 1개","해안에서 대기 · 40 G, 이틀 경과"},
        {"해상전 시작 · 턴제 전술 선택","통행료 교섭 · 80 G + 해역당 20 G","안개 속 회피 · 선체 −6"},
        {"갑판 수색 · +120 G, 선체 −3, 이틀","잠수 탐색 · 60%: 기념품·250 G·명성 +4·선체 −5 / 실패 선체 −12","위치만 기록 · 명성 +1"},
        {"보급을 나눠 구조 · 50 G, 명성 +6","항구까지 예인 · 이틀, 선체 −4, 명성 +4","지원 신호 전달 · 명성 +1"},
        {"화물 1개 기증 · +100 G, 명성 +5","항해담 나누기 · 하루, 명성 +2","환영 선물 받기 · +60 G"},
        {"상자 구입 · 120 G, 60%로 300 G/그 외 20 G","정식 서류 확인 · 이틀, +80 G, 명성 +2","제안 거절 · 명성 +1"},
        {"해류 관측 · 하루, 명성 +3","순풍으로 보급 절약 · +90 G","갑판 정비 · 선체 +8, 명성 +1"},
        {"해변 조사 · 하루, 기념품·100 G, 명성 +2","내륙 탐험 · 사흘, 성공 65%/실패 선체 −7","섬 위치만 기록 · 명성 +1"}
    };
    public static String title(Engine e) { return e.adventure.battleHP>0?"해적선과의 해상전":TITLES[e.adventure.pending]; }
    public static String body(Engine e) {
        String status="\n\n우리 선체 "+e.hull+"/"+e.maxHull()+" · "+e.gold+" G · 화물 "+e.cargo()+"개";
        if(e.adventure.battleHP>0) return "해적 선체 "+e.adventure.battleHP+" · "+e.adventure.battleTurn+"턴 진행\n\n포격은 공격력이 높고, 회피 사격은 받는 피해를 줄입니다. 도주는 언제든 가능합니다."+status+"\n\n"+e.message;
        return STORIES[e.adventure.pending]+status;
    }
    public static String option(Engine e,int choice) {
        if(e.adventure.battleHP>0) return new String[]{"포격 · 높은 공격력","회피 사격 · 반격 피해 감소","긴급 도주 · 화물 1개 또는 최대 40 G, 선체 −3, 하루"}[choice];
        if(e.adventure.pending==1 && choice==1) return "통행료 교섭 · "+(80+World.region(e.port)*20)+" G";
        String text=OPTIONS[e.adventure.pending][choice];
        if(e.adventure.pending==2 && choice==1) return text.replace("60%",CrewAbilities.explorationChance(e,60)+"%");
        if(e.adventure.pending==7 && choice==1) return text.replace("65%",CrewAbilities.explorationChance(e,65)+"%");
        return text;
    }
}
