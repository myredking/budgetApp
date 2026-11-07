package com.bluehorizon;
/** Visual snapshot only: never advances turns or modifies the save. */
public final class BattleFrame {
    public final int action,ourBefore,ourAfter,ourMax,enemyBefore,enemyAfter,enemyMax,ourDamage,enemyDamage;
    public BattleFrame(int choice,int before,int after,int maximum,int foeBefore,int foeAfter,int foeMax) {
        action=choice;ourMax=Math.max(1,maximum);enemyMax=Math.max(1,foeMax);
        ourBefore=clamp(before,ourMax);ourAfter=clamp(after,ourMax);
        enemyBefore=clamp(foeBefore,enemyMax);enemyAfter=clamp(foeAfter,enemyMax);
        ourDamage=Math.max(0,ourBefore-ourAfter);enemyDamage=Math.max(0,enemyBefore-enemyAfter);
    }
    private static int clamp(int value,int max) { return Math.max(0,Math.min(max,value)); }
    public boolean firing() { return action==0; }
    public boolean guarding() { return action==1; }
    public float progress(long elapsed) { return Math.max(0,Math.min(1,elapsed/1300f)); }
    private float mix(int before,int after,int max,float progress) {
        float t=Math.max(0,Math.min(1,progress));return (before+(after-before)*t)/max;
    }
    public float ourFraction(float progress) { return mix(ourBefore,ourAfter,ourMax,progress); }
    public float enemyFraction(float progress) { return mix(enemyBefore,enemyAfter,enemyMax,progress); }
}
