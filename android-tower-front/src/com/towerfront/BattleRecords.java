package com.towerfront;

import java.util.Arrays;

/** Best mission times use a separate versioned save; old star saves stay intact. */
public final class BattleRecords {
    private static final long MAX_MILLIS=1_000_000_000_000L;
    private final long[] best=new long[3*Campaign.COUNT];
    public BattleRecords() {Arrays.fill(best,-1);}
    public long bestMillis(int stage,int mode) {
        if(stage<1 || stage>Campaign.COUNT || mode<0 || mode>2) return -1;
        return best[mode*Campaign.COUNT+stage-1];
    }
    public void recordBattle(Engine e) {
        if(e==null || e.skirmish || e.result()!=1) return;
        if(!Double.isFinite(e.elapsed) || e.elapsed<0 || e.elapsed>1e9) return;
        int at=e.difficulty*Campaign.COUNT+e.level-1;long millis=Math.round(e.elapsed*1000);
        if(best[at]<0 || millis<best[at]) best[at]=millis;
    }
    public String encode() {
        StringBuilder out=new StringBuilder("TR1:");
        for(int i=0;i<best.length;i++) {if(i>0) out.append(',');out.append(best[i]);}
        return out.toString();
    }
    public static BattleRecords decode(String text) {
        BattleRecords result=new BattleRecords();
        if(text==null || text.length()>5000 || !text.startsWith("TR1:")) return result;
        String[] values=text.substring(4).split(",",-1);
        if(values.length!=result.best.length) return result;
        try {
            for(int i=0;i<values.length;i++) {
                long value=Long.parseLong(values[i]);
                if(value< -1 || value>MAX_MILLIS) return new BattleRecords();
                result.best[i]=value;
            }
        } catch(NumberFormatException ex) {return new BattleRecords();}
        return result;
    }
}
