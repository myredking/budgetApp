package com.bluehorizon;

/** Immutable receipt; confirmation rejects changes rather than silently repricing. */
public final class TradeOrder {
    public final int good,quantity,unit,total,goldAfter,cargoAfter,maximum,bonusGold,reserved;
    public final boolean selling;
    public final int nextSaleUnit;
    public final String error;
    private final String snapshot;
    private boolean used;
    private TradeOrder(Engine e,int item,boolean sale,int amount,int limit,String problem,int protectedCount) {
        good=item;selling=sale;quantity=amount;maximum=limit;error=problem;reserved=protectedCount;
        unit=item<0?0:(sale?e.sellPrice(item):e.buyPrice(item));total=unit*amount;
        snapshot=e.encode();
        Engine projected=project(e);
        nextSaleUnit=forecast(e,projected);
        goldAfter=projected.gold;cargoAfter=projected.cargo();
        bonusGold=valid()?goldAfter-(e.gold+(sale?total:-total)):0;
    }
    public boolean valid() { return error.isEmpty(); }
    private int forecast(Engine source,Engine projected) {
        if(!valid() || !selling || projected==source || !MarketPressure.applies(source.port,good)) return -1;
        return projected.sellPriceAt(source.port,good);
    }
    private Engine project(Engine source) {
        Engine copy=Engine.decode(snapshot);
        if(copy==null) return source;
        if(!valid()) return copy;
        if(selling) copy.sell(good,quantity);else copy.buy(good,quantity);
        return copy;
    }
    public static int number(String input) {
        if(input==null) return -1;
        String clean=input.trim();
        if(!clean.matches("[0-9]+|[0-9]{1,3}(,[0-9]{3})+")) return -1;
        try { return Integer.parseInt(clean.replace(",","")); }
        catch(NumberFormatException error) { return -1; }
    }
    public static TradeOrder quote(Engine e,int good,boolean sale,int mode,int value,int reserve) {
        return quote(e,good,sale,mode,value,reserve,false);
    }
    public static TradeOrder quote(Engine e,int good,boolean sale,int mode,int value,int reserve,boolean protect) {
        if(good<0 || good>=6) return new TradeOrder(e,-1,sale,0,0,"상품을 선택하세요.",0);
        int held=sale && protect?MissionCargo.held(e,good):0;
        int maximum=Math.max(0,maximum(e,good,sale,reserve)-held);
        String error=problem(e,sale,mode,value,reserve);
        int amount=amount(e,good,sale,mode,value,maximum);
        if(error.isEmpty()) error=amountProblem(sale,amount,maximum);
        if(!error.isEmpty() && held>0) error+="\n목표용 "+held+"개는 보관 중입니다. 원하면 보호 체크를 해제하세요.";
        return new TradeOrder(e,good,sale,Math.max(0,Math.min(amount,e.capacity())),maximum,error,held);
    }
    private static int maximum(Engine e,int good,boolean sale,int reserve) {
        if(sale) return e.inventory[good];
        int funds=(int)Math.max(0,(long)e.gold-Math.max(0,reserve));
        return Math.min(e.capacity()-e.cargo(),funds/e.buyPrice(good));
    }
    private static int amount(Engine e,int good,boolean sale,int mode,int value,int maximum) {
        if(mode==2) return maximum;
        if(mode==1 && !sale) return Math.min(maximum,Math.max(0,value)/e.buyPrice(good));
        return value;
    }
    private static String problem(Engine e,boolean sale,int mode,int value,int reserve) {
        if(e.actionBlocked()) return "항해와 사건을 마친 뒤 거래하세요.";
        if(mode<0 || mode>2) return "거래 방식을 선택하세요.";
        if(sale && mode==1) return "판매는 수량 또는 전량을 선택하세요.";
        if(reserve<0) return "비상금은 0 이상이어야 합니다.";
        if(mode!=2 && value<1) return "1 이상의 정수를 입력하세요.";
        return "";
    }
    private static String amountProblem(boolean sale,int amount,int maximum) {
        if(maximum<1) return sale?"판매할 재고가 없습니다.":"금화·비상금·남은 화물 공간을 확인하세요.";
        if(amount<1) return "입력 금액으로 1개도 구매할 수 없습니다.";
        if(amount>maximum) return "거래 가능한 최대 수량은 "+maximum+"개입니다.";
        return "";
    }
    public boolean execute(Engine e) {
        if(used) return Adventure.fail(e,"이미 처리한 거래입니다.");
        if(!valid()) return Adventure.fail(e,error);
        if(!snapshot.equals(e.encode())) return Adventure.fail(e,"보유 금화나 시세가 바뀌었습니다. 새 견적을 확인하세요.");
        boolean success=selling?e.sell(good,quantity):e.buy(good,quantity);
        if(success) used=true;
        return success;
    }
}
