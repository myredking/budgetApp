package com.bluehorizon;

public final class Refits {
    public static final String[] NAMES={"돛과 삭구","대포","보강 선체","화물 창고"};
    public static final String[] EFFECTS={"단계마다 항해 일수 −5% (올림, 최소 이틀)","포격 피해 +4 / 회피 사격 피해 +2","최대 선체 +15 / 일반 항해·전투 피해 −1","적재량 +8"};
    private static final int[] BASE={400,650,550,450};
    public static int slots(Engine e) { return e.level*2+1; }
    public static int cost(Engine e,int part) { return BASE[part]*(e.refits.levels[part]+1); }
    public static int refund(Engine e,int part) { return BASE[part]*e.refits.levels[part]/2; }
    public static int days(Engine e,int base) { return base==0?0:Math.max(2,(base*(100-5*e.refits.levels[0])+99)/100); }
    public static int damage(Engine e,int base) { return Math.max(1,base-e.refits.levels[2]); }
    public static int attack(Engine e,int choice) { return e.refits.levels[1]*(choice==0?4:2); }
    public static boolean install(Engine e,int part) {
        if(!mayChange(e,part)) return false;
        if(e.refits.levels[part]>=3) return Adventure.fail(e,"이미 최고 단계의 부품입니다.");
        if(e.refits.used()>=slots(e)) return Adventure.fail(e,"개조 슬롯이 부족합니다. 다른 부품을 해제하거나 더 큰 배를 마련하세요.");
        int price=cost(e,part);if(e.gold<price) return Adventure.fail(e,"개조 비용이 부족합니다.");
        int maximum=e.maxHull();e.gold-=price;e.refits.levels[part]++;e.hull+=e.maxHull()-maximum;
        e.note(NAMES[part]+" "+e.refits.levels[part]+"단계 장착 · −"+price+" G.");return true;
    }
    public static boolean remove(Engine e,int part) {
        if(!mayChange(e,part)) return false;
        if(e.refits.levels[part]==0) return Adventure.fail(e,"해제할 부품이 없습니다.");
        if(part==3 && e.cargo()>e.capacity()-8) return Adventure.fail(e,"화물을 먼저 줄여야 창고를 해제할 수 있습니다.");
        int money=refund(e,part);e.refits.levels[part]--;e.gold+=money;e.hull=Math.min(e.hull,e.maxHull());
        e.note(NAMES[part]+" 한 단계 해제 · 환급 +"+money+" G.");return true;
    }
    private static boolean mayChange(Engine e,int part) {
        if(e.adventure.pending>=0) return Adventure.fail(e,"항해 사건을 마친 뒤 개조하세요.");
        if(part<0 || part>=4) return Adventure.fail(e,"부품을 확인하세요.");
        return true;
    }
}
