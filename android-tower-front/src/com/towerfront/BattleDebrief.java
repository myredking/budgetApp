package com.towerfront;

import java.util.Locale;

/** Clear, truthful personal-record and medal targets for the result screen. */
public final class BattleDebrief {
    private BattleDebrief() { }
    public static String clock(long millis) {
        if(millis<0) return "--:--";
        return String.format(Locale.ROOT,"%02d:%02d.%d",millis/60000,millis/1000%60,millis/100%10);
    }
    public static String recordText(Engine e,BattleRecords records,long before) {
        if(e.skirmish || e.result()!=1) return e.rules.objectiveText();
        long best=records.bestMillis(e.level,e.difficulty);
        if(best<0) return "승리 기록을 저장하는 중입니다";
        String prefix=before<0?"첫 기록":Math.round(e.elapsed*1000)<before?"신기록":"최단 기록";
        return prefix+" · "+clock(best);
    }
    public static String nextTarget(Engine e,Profile p) {
        int stars=p.stars(e.level,e.difficulty);
        if(stars>=3) return "최고 별3개 · 최단 기록에 도전";
        if(e.rules.objective==BattleRules.SURVIVE) return "다음 별 · 종료 시 "+(stars<2?2:3)+"거점 유지";
        return "다음 별 · "+(stars<2?180:90)+"초 이내 승리";
    }
}
