package com.bluehorizon;

public final class CrewConversations {
    public static final String[][] TOPICS={
        {"이안은 첫 무역 장부 옆에 작은 별을 그려 넣었다. ‘이익 말고도 우리가 기억할 것을 적어 두고 싶어.’","이안은 발견한 보물을 바라본다. ‘우리 둘의 성공으로만 남겨도 될까?’"},
        {"미라는 서로 다른 별자리 기록을 펼친다. ‘어느 쪽이 틀렸다고 단정하기보다, 함께 관측하고 싶어요.’","미라는 잊힌 항로를 가리킨다. ‘완벽한 계산을 기다릴까요, 작은 관측부터 시작할까요?’"},
        {"나디아는 오래된 기록의 빈 서명란을 만진다. ‘이 지식을 남긴 사람을 찾을 수 있을까요?’","나디아는 보물의 해독문을 내민다. ‘안전을 위해 보관할까요, 모두 읽도록 공개할까요?’"},
        {"타보는 지친 선원에게 자신의 물을 나눈다. ‘가장 느린 동료도 끝까지 함께 가는 배가 좋아.’","타보는 강적의 흔적을 살핀다. ‘이길 힘과 돌아올 길, 둘 다 준비했나?’"},
        {"아유는 항구 상인들의 약속을 읽는다. ‘값을 깎는 기술보다 약속을 지킨 이름이 오래 남아요.’","아유는 새 항로의 이익을 계산한다. ‘우리의 발견이 이곳 사람들에게도 도움이 되었으면 해요.’"}
    };
    public static boolean unlocked(Engine e,int person,int stage) {
        if(person<1 || person>5 || stage<0 || stage>1) return false;
        if(!Crew.available(person,e.chapter)) return false;
        return stage==0 || (e.chapter>=Crew.JOINS[person]+1 && e.expedition.treasures!=0);
    }
    public static boolean chosen(Engine e,int person,int stage) { return (e.expedition.talks & (1<<((person-1)*2+stage)))!=0; }
    public static int support(Engine e) {
        int total=0;for(int trust:e.expedition.trust) total+=trust;return Math.min(3,total/4);
    }
    public static boolean choose(Engine e,int person,int stage,int choice) {
        if(e.actionBlocked()) return Adventure.fail(e,"항해와 전투를 먼저 마치세요.");
        if(!unlocked(e,person,stage) || choice<0 || choice>1) return Adventure.fail(e,"아직 나눌 수 없는 대화입니다.");
        if(chosen(e,person,stage)) return Adventure.fail(e,"이 대화에서 내린 선택은 이미 기록했습니다.");
        int bit=1<<((person-1)*2+stage);e.expedition.talks|=bit;
        if(choice==0) { e.expedition.trust[person-1]+=2;e.town.morale=Math.min(100,e.town.morale+10); }
        else { e.expedition.practical|=bit;e.expedition.trust[person-1]++;e.fame++; }
        e.note(Crew.NAMES[person]+"와 "+(choice==0?"마음을 나눴다. 신뢰 +2, 사기 +10.":"실행 계획을 세웠다. 신뢰 +1, 명성 +1."));e.advanceStory();return true;
    }
}
