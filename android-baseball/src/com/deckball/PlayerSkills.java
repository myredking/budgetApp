package com.deckball;

public final class PlayerSkills {
    public static final int CONTACT=1, POWER=2, CLUTCH=4, ACE=8, LEGEND=16;
    public static final int[] VALUES={CONTACT,POWER,CLUTCH,ACE,LEGEND};
    public static final String[] NAMES={"정교한 타격 · 컨택 +6","거포 본능 · 파워 +8","해결사 · 득점권 컨택 +10","정밀 제구 · 제구 +8","레전드 아우라 · 능력치 +3"};
    private PlayerSkills() {}
    public static int defaults(GameEngine.Player p) {
        int mask=p.legend ? LEGEND : 0;
        if(p.position.equals("P")) return mask | (p.control>=85 ? ACE : 0);
        if(p.power>=90) return mask | POWER;
        if(p.contact>=90) return mask | CONTACT;
        return mask;
    }
    public static void validate(GameEngine.Player p,int mask) {
        if(mask<0 || (mask & ~31)!=0 || Integer.bitCount(mask)>2) throw new IllegalArgumentException("특수 능력은 최대 2개");
        int allowed=p.position.equals("P") ? ACE | LEGEND : CONTACT | POWER | CLUTCH | LEGEND;
        if((mask & ~allowed)!=0) throw new IllegalArgumentException("포지션에 맞는 특수 능력을 선택하세요");
    }
    public static int bonus(GameEngine.Player p,int skill,int amount) { return (p.skills & skill)!=0 ? amount : 0; }
    public static int contact(GameEngine.Player p,boolean scoring) {
        int extra=bonus(p,CONTACT,6)+bonus(p,LEGEND,3);
        if(scoring) extra+=bonus(p,CLUTCH,10);
        return Math.min(110,p.contact+extra);
    }
    public static int power(GameEngine.Player p) { return Math.min(110,p.power+bonus(p,POWER,8)+bonus(p,LEGEND,3)); }
    public static int control(GameEngine.Player p) { return Math.min(110,p.control+bonus(p,ACE,8)+bonus(p,LEGEND,3)); }
    public static String describe(GameEngine.Player p) {
        StringBuilder text=new StringBuilder();
        for(int i=0;i<VALUES.length;i++) {
            if((p.skills & VALUES[i])==0) continue;
            if(text.length()>0) text.append(" / ");
            text.append(NAMES[i]);
        }
        return text.length()==0 ? "특수 능력 없음" : text.toString();
    }
}
