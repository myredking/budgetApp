package com.towerfront;

/** Contextual teaching reads battle state without changing simulation or saves. */
public final class BattleCoach {
    public static final String GENERAL="아군 선택 → 출병 / 전술 스킬";
    private BattleCoach() { }
    public static String tip(Engine e,int selected) {
        if(e.skirmish || e.level>3) return GENERAL;
        int target=selected;
        if(target<0 || target>=e.towers.size()) target=-1;
        else if(e.towers.get(target).owner!=1) target=-1;
        if(e.level==1) return first(e,target);
        return e.level==2?upgrade(e,target):skill(e,target);
    }
    private static String first(Engine e,int selected) {
        if(e.captures>0) return "점령한 타워에서도 출병할 수 있어요";
        if(e.dispatched>0) return "병력이 도착해 수비병을 이기면 점령";
        return selected<0?"① 파란 타워를 터치하세요":"② 목표 타워로 끌어 출병하세요";
    }
    private static String upgrade(Engine e,int selected) {
        for(Engine.Tower t:e.towers) if(t.owner==1 && t.rank>1) return "강화 완료 · 생산이 빨라집니다";
        if(selected<0) return "아군 선택 → 병력 20명으로 강화";
        if(e.towers.get(selected).units<20) return "병력 20명을 모으면 강화할 수 있어요";
        return "아래 강화 버튼으로 생산을 높이세요";
    }
    private static String skill(Engine e,int selected) {
        for(double remaining:e.rules.cooldowns) if(remaining>0) return "스킬 사용 완료 · 에너지는 자동 회복";
        return selected<0?"아군을 선택하고 스킬을 사용해보세요":"아래 전술 스킬을 눌러보세요";
    }
    public static String defeatHint(Engine e) {
        if(e.dispatched==0) return "아군 선택 후 목표로 끌어 출병하세요";
        switch(e.rules.objective) {
            case BattleRules.HOLD: return "보급을 점령한 뒤 병력을 지원하세요";
            case BattleRules.SURVIVE: return "거점을 지키며 생존 시간을 채우세요";
            case BattleRules.CONTROL: return "세 거점에 방어 병력을 남겨두세요";
            default: return "가까운 거점부터 병력을 모아 공략하세요";
        }
    }
}
