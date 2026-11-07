package com.towerfront;
public final class SkillDisplay {
    private static final String[] LABELS={"증원","보호","가속"};
    private SkillDisplay() { }
    public static int seconds(BattleRules rules,int type) {return (int)Math.ceil(rules.cooldowns[type]);}
    public static float remaining(BattleRules rules,int type) {
        return (float)Math.max(0,Math.min(1,rules.cooldowns[type]/BattleRules.cooldown(type)));
    }
    public static String label(int type) {return LABELS[type];}
}
