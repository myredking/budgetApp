package com.bluehorizon;

/** Read-only next action: safety, pending rewards, contracts, then story. */
public final class CaptainPlan {
    public final String title,body;
    public final int page,target,good,amount,achievement;
    public final boolean selling,deliver,event,continueStory;
    private CaptainPlan(String name,String detail,int screen,int port,int item,int count,int kind) {
        title=name;body=detail;page=screen;target=kind==4?-1:port;good=item;amount=count;
        selling=kind==1;deliver=kind==2;event=kind==3;continueStory=kind==5;achievement=kind==4?port:-1;
    }
    private static CaptainPlan page(String title,String body,int page) {
        return new CaptainPlan(title,body,page,-1,-1,0,0);
    }
    private static CaptainPlan route(int port) {
        return new CaptainPlan(World.PORTS[port]+"로 항해 준비","항로·보급비·선체 상태를 확인하고 출항하세요.",0,port,-1,0,0);
    }
    private static CaptainPlan order(int good,int amount,boolean sale) {
        return new CaptainPlan(Engine.GOODS[good]+" "+amount+"개 "+(sale?"판매하기":"준비하기"),
            "주문서를 열어 수량과 총액을 확인하세요.",1,-1,good,amount,sale?1:0);
    }
    public static CaptainPlan next(Engine e) {
        if(e.actionBlocked()) return new CaptainPlan("항해 사건 이어가기","먼저 선장의 결정을 마무리하세요.",0,-1,-1,0,3);
        CaptainPlan contract=delivery(e);if(contract!=null) return contract;
        for(int i=0;i<8;i++) {
            if(Achievements.ready(e,i) && !Achievements.claimed(e,i))
                return new CaptainPlan("달성 보상 받기 · "+Achievements.NAMES[i],"+"+Achievements.REWARDS[i]+" G · 명성 +1",5,i,-1,0,4);
        }
        if(Story.ready(e)) return new CaptainPlan("이야기 이어가기 · 새 기록 확인","다음 장의 조건을 달성했습니다. 동료의 기록을 확인하세요.",4,-1,-1,0,5);
        if(e.hull<25) return page("출항 전 선체 수리","선체 25 이상이 필요합니다. 부족한 비용은 부두 일거리로 마련하세요.",2);
        if(active(e)) return contractTrip(e);
        return story(e);
    }
    private static boolean active(Engine e) { return e.adventure.target>=0 && e.day<=e.adventure.deadline; }
    private static CaptainPlan delivery(Engine e) {
        AdventureState a=e.adventure;
        if(!active(e) || e.port!=a.target || e.inventory[a.good]<a.amount) return null;
        return new CaptainPlan("의뢰 납품 · +"+a.reward+" G",Engine.GOODS[a.good]+" "+a.amount+"개를 전달합니다.",5,-1,-1,0,2);
    }
    private static CaptainPlan contractTrip(Engine e) {
        AdventureState a=e.adventure;int missing=a.amount-e.inventory[a.good];
        if(missing>0) return order(a.good,missing,false);
        return route(a.target);
    }
    private static CaptainPlan trade(Engine e,int good,int source,int target) {
        if(e.inventory[good]>=5) return e.port==target?order(good,5,true):route(target);
        if(e.port!=source) return route(source);
        return order(good,5-e.inventory[good],false);
    }
    private static CaptainPlan story(Engine e) {
        switch(e.chapter) {
            case 0:return trade(e,0,0,1);
            case 1:return northern(e);
            case 2:return library(e);
            case 3:return cape(e);
            case 4:return easternTrade(e,3,28,32);
            case 5:return easternTrade(e,4,42,44);
            case 6:return atlantic(e);
            case 7:return publication(e);
            default:return exploration(e);
        }
    }
    private static CaptainPlan library(Engine e) {
        return e.port==13?page("도서관의 항구에 투자","조선소에서 교역 동맹에 1단계 투자하세요.",2):route(13);
    }
    private static CaptainPlan cape(Engine e) {
        return e.level<2?page("캐러벨 마련하기","교역·납품으로 2400 G를 모아 조선소에서 배를 바꾸세요.",2):route(21);
    }
    private static CaptainPlan easternTrade(Engine e,int good,int source,int target) {
        return !e.hasVisited(source)?route(source):trade(e,good,source,target);
    }
    private static CaptainPlan publication(Engine e) {
        return e.gold<12000?page("공동 해도 출판 기금","목표 12,000 G · 의뢰와 교역으로 기금을 마련하세요.",5):route(0);
    }
    private static CaptainPlan northern(Engine e) {
        if(!e.hasVisited(2)) return route(2);
        if(!e.hasVisited(3)) return route(3);
        return page("북쪽 항구에서 명성 쌓기","명성 "+e.fame+" / 10 · 납품·판매·현지 소문으로 올리세요.",5);
    }
    private static CaptainPlan atlantic(Engine e) {
        if(!e.hasVisited(48)) return route(48);
        if(!e.hasVisited(53)) return route(53);
        return page("대서양 항해자의 명성","명성 "+e.fame+" / 40 · 항구 의뢰와 교역을 이어가세요.",5);
    }
    private static CaptainPlan exploration(Engine e) {
        for(int port=0;port<World.COUNT;port++) if(!e.hasVisited(port)) return route(port);
        return page("일곱 바다의 선장","모든 항구를 방문했습니다. 동료 기록과 교역 동맹을 완성해 보세요.",3);
    }
}
