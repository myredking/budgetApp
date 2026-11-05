package com.bluehorizon;

public final class Contracts {
    public static Contract offer(Engine e) {
        return offers(e)[0];
    }
    public static final String[] TYPES={"가벼운 운송","기한 우선 운송","대량 교역"};
    public static Contract[] offers(Engine e) {
        Contract[] result=new Contract[3];
        for(int choice=0;choice<3;choice++) result[choice]=create(e,choice);
        return result;
    }
    private static Contract create(Engine e,int choice) {
        int shift=1+(e.adventure.deliveries%5+choice*2)%7;
        int target=World.region(e.port)*8+(e.port%8+shift)%8;
        int good=(e.port+e.adventure.deliveries+choice)%6;
        int amount=new int[]{3,5,Math.min(20,e.capacity()/2)}[choice];
        int days=e.travelDays(target),slack=choice==1?Math.max(4,days/2):days*2+12;
        int reward=e.buyPrice(good)*amount+e.travelCost(target,1)+new int[]{180,330,420}[choice]+days*8;
        return new Contract(target,good,amount,e.day+days+slack,reward);
    }
    public static boolean accept(Engine e) {
        return accept(e,0);
    }
    public static boolean accept(Engine e,int choice) {
        if(e.actionBlocked()) return Adventure.fail(e,"항해와 사건을 먼저 마치세요.");
        if(e.adventure.target>=0) return Adventure.fail(e,"진행 중인 의뢰가 있습니다.");
        if(choice<0 || choice>2) return Adventure.fail(e,"의뢰를 선택하세요.");
        Contract c=offers(e)[choice];AdventureState a=e.adventure;
        a.origin=e.port;a.target=c.target;a.good=c.good;a.amount=c.amount;a.deadline=c.deadline;a.reward=c.reward;
        e.note(World.PORTS[a.target]+"에 "+Engine.GOODS[a.good]+" "+a.amount+"개 납품 의뢰를 수락했다.");return true;
    }
    public static boolean deliver(Engine e) {
        if(e.actionBlocked()) return Adventure.fail(e,"항해와 사건을 먼저 마치세요.");
        AdventureState a=e.adventure;
        if(a.target<0) return Adventure.fail(e,"납품할 의뢰가 없습니다.");
        if(e.day>a.deadline) return Adventure.fail(e,"납품 기한이 지났습니다. 의뢰를 취소하고 다시 받을 수 있습니다.");
        if(e.port!=a.target) return Adventure.fail(e,"납품 항구에 직접 정박하세요.");
        if(e.inventory[a.good]<a.amount) return Adventure.fail(e,"납품할 화물이 부족합니다.");
        e.inventory[a.good]-=a.amount;e.gold+=a.reward;e.fame+=3;a.deliveries++;
        e.note("납품 완료! 보상 +"+a.reward+" G, 명성 +3.");a.clearContract();e.advanceStory();return true;
    }
    public static boolean cancel(Engine e) {
        if(e.actionBlocked()) return Adventure.fail(e,"항해와 사건을 먼저 마치세요.");
        if(e.adventure.target<0) return Adventure.fail(e,"진행 중인 의뢰가 없습니다.");
        e.adventure.clearContract();e.note("납품 의뢰를 취소했다. 위약금은 없다.");return true;
    }
}
