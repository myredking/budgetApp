package com.bluehorizon;

/** Fund the next reachable action, including its immediate travel and cargo. */
public final class FundingPlan {
    public static int required(Engine e) { return e.port==0?12000:12000+e.travelCost(0,1)+200; }
    public static int page(Engine e) {
        if(e.hull<25) return 2;
        int region=World.region(e.port),bit=1<<region;
        if((e.expedition.treasures & bit)==0) return e.gold>=expeditionCost(e)?9:commissionPage(e);
        if((e.expedition.trophies & bit)==0) return bossPage(e,region);
        return commissionPage(e);
    }
    private static int bossPage(Engine e,int region) {
        if(e.level>=1+region/3) return e.hull>=40?9:(e.gold>=e.repairCost()?2:commissionPage(e));
        return e.gold>=e.upgradeCost()?2:commissionPage(e);
    }
    private static int commissionPage(Engine e) { return e.gold>=commissionCost(e)?5:2; }
    public static int expeditionCost(Engine e) {
        int region=World.region(e.port);
        if(Expeditions.fragments(e,region)>=3) return (e.port==Expeditions.site(region)?0:e.travelCost(Expeditions.site(region),1))+80;
        if((e.expedition.clues & (1L<<e.port))==0) return 40;
        int cost=Integer.MAX_VALUE;
        for(int port:World.portsIn(region)) {
            if((e.expedition.clues & (1L<<port))==0) cost=Math.min(cost,e.travelCost(port,1)+40);
        }
        return cost;
    }
    private static int commissionCost(Engine e) {
        int cost=Integer.MAX_VALUE;
        for(Contract c:Contracts.offers(e)) {
            int cargo=Math.max(0,c.amount-e.inventory[c.good])*e.buyPrice(c.good);
            cost=Math.min(cost,cargo+e.travelCost(c.target,1));
        }
        return cost;
    }
    public static int recoveryCost(Engine e) {
        int region=World.region(e.port),bit=1<<region,commission=commissionCost(e);
        if((e.expedition.treasures & bit)==0) return Math.min(expeditionCost(e),commission);
        if((e.expedition.trophies & bit)==0) {
            if(e.level<1+region/3) return Math.min(e.upgradeCost(),commission);
            if(e.hull<40) return Math.min(e.repairCost(),commission);
            return 0;
        }
        return commission;
    }
    public static String description(Engine e) {
        int target=page(e),region=World.region(e.port);
        if(target==2 && e.gold<recoveryCost(e)) return "다음 활동 준비비 "+recoveryCost(e)+" G가 필요합니다. 조선소에서 하루 일하면 120 G를 받습니다.";
        if(target==2) return "조선소에서 선체를 수리하거나 필요한 배 등급을 마련한 뒤 강적에 도전하세요.";
        if(target==9) return "현지 보물 "+(900+region*150)+" G · 강적 승리 "+(1200+region*300)+" G. 조사·탐사비와 도전 조건은 별도입니다.";
        return "현지 납품과 교역으로 자금을 마련하세요. 의뢰 준비비 최소 "+commissionCost(e)+" G · 화물 공간과 수리·사건 비용도 확인하세요.";
    }
}
