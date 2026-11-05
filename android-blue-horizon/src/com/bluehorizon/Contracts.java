package com.bluehorizon;

public final class Contracts {
    public static Contract offer(Engine e) {
        int target=World.region(e.port)*8+(e.port+3)%8;
        int good=0;for(int i=1;i<6;i++) if(e.buyPrice(i)<e.buyPrice(good)) good=i;
        int days=e.travelDays(target);
        return new Contract(target,good,e.level==1?5:10,e.day+days*3+15,350+days*24);
    }
    public static boolean accept(Engine e) {
        if(e.actionBlocked()) return Adventure.fail(e,"항해와 사건을 먼저 마치세요.");
        if(e.adventure.target>=0) return Adventure.fail(e,"진행 중인 의뢰가 있습니다.");
        Contract c=offer(e);AdventureState a=e.adventure;
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
