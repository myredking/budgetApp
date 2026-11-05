package com.bluehorizon;

public final class TradeRace {
    public static final int ENTRY_FEE=100;
    public static Contract offer(Engine e) {
        int target=World.region(e.port)*8+(e.port+4)%8,good=0;
        for(int product=1;product<6;product++) if(e.buyPrice(product)<e.buyPrice(good)) good=product;
        int days=WorldRoutes.between(e.port,target).days();
        return new Contract(target,good,5,e.day+days+3,400+days*12);
    }
    public static boolean start(Engine e) {
        if(!Rivals.mayAct(e,Rivals.owner(e.port))) return false;
        RivalState s=e.rivals;
        if(s.raceTarget>=0) return Adventure.fail(e,"이미 참가 중인 경주가 있습니다.");
        if(e.day-s.lastRaceDay<7) return Adventure.fail(e,"무역 경주는 참가 후 7일 간격으로 열립니다.");
        Contract c=offer(e);if(!Adventure.spend(e,ENTRY_FEE)) return false;
        s.raceRival=Rivals.owner(e.port);s.raceFrom=e.port;s.raceTarget=c.target;s.raceGood=c.good;
        s.raceAmount=c.amount;s.raceDeadline=c.deadline;s.raceReward=c.reward;s.lastRaceDay=e.day;
        e.note(Rivals.NAMES[s.raceRival]+"와의 납품 경주에 참가했다. −100 G.");return true;
    }
    public static boolean complete(Engine e) {
        if(e.actionBlocked()) return Adventure.fail(e,"항해와 사건을 먼저 마치세요.");
        RivalState s=e.rivals;if(s.raceTarget<0) return Adventure.fail(e,"참가 중인 경주가 없습니다.");
        if(e.day>s.raceDeadline) return lose(e,"상대 상단이 먼저 납품했다.");
        if(e.port!=s.raceTarget) return Adventure.fail(e,"경주 목적지에 직접 정박하세요.");
        if(e.inventory[s.raceGood]<s.raceAmount) return Adventure.fail(e,"경주 납품 화물이 부족합니다.");
        e.inventory[s.raceGood]-=s.raceAmount;e.gold+=s.raceReward;e.fame+=3;s.wins[s.raceRival]++;
        Rivals.relation(e,s.raceRival,8);Rivals.adjustPressure(e,World.region(e.port),-12);
        e.note("납품 경주 승리! +"+s.raceReward+" G, 명성 +3, 상단 관계 +8.");
        s.clearRace();e.advanceStory();return true;
    }
    public static boolean abandon(Engine e) {
        if(e.actionBlocked()) return Adventure.fail(e,"항해와 사건을 먼저 마치세요.");
        if(e.rivals.raceTarget<0) return Adventure.fail(e,"참가 중인 경주가 없습니다.");
        return lose(e,"경주를 포기했다.");
    }
    private static boolean lose(Engine e,String message) {
        RivalState s=e.rivals;Rivals.adjustPressure(e,World.region(s.raceFrom),5);Rivals.relation(e,s.raceRival,-3);
        s.clearRace();e.note(message+" 참가비는 돌려받지 않으며 화물은 유지된다. 경쟁 압력 +5, 관계 −3.");return true;
    }
}
