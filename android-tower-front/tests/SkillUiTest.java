package com.towerfront;
public final class SkillUiTest {
    private static void check(boolean ok,String text) {if(!ok) throw new AssertionError(text);}
    public static void main(String[] args) {
        Engine e=new Engine(1);e.rules.energy=100;
        check(e.useSkill(0,0),"cast succeeds");
        String before=SaveCodec.encode(e);
        check(SkillDisplay.seconds(e.rules,0)==18,"cooldown starts at full seconds");
        check(SkillDisplay.remaining(e.rules,0)==1,"ring starts full");
        check(before.equals(SaveCodec.encode(e)),"display cannot alter battle");
        e.update(3);
        check(SkillDisplay.seconds(e.rules,0)==15,"numeric cooldown counts down");
        check(SkillDisplay.remaining(e.rules,0)<1,"ring drains over time");
        e.rules.cooldowns[0]=.1;check(SkillDisplay.seconds(e.rules,0)==1,"last fraction remains visible");
        e.rules.cooldowns[0]=0;check(SkillDisplay.remaining(e.rules,0)==0,"ready ring clears");
        BattleEffects fx=new BattleEffects();fx.cast(0,100,200);fx.cast(1,120,220);fx.cast(2,140,240);
        check(fx.casts.size()==3 && fx.casts.get(1).type==1,"each skill has distinct visual type");
        check(fx.needsFrame(true,false,1),"skill effect can finish on result screen");
        fx.update(2);check(fx.casts.isEmpty(),"skill effects expire");
        for(int i=0;i<30;i++) fx.cast(i%3,100,200);
        check(fx.casts.size()<=6,"rapid skill visuals remain bounded");
        fx.clear();check(fx.casts.isEmpty(),"clear removes skill effects");
    }
}
