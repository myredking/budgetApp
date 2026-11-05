package com.bluehorizon;

public final class TradeOrderTest {
    private static int checks;
    private static void check(boolean value,String text) { checks++;if(!value) throw new AssertionError(text); }
    private static void quantityAndBudget() {
        Engine e=new Engine();int price=e.buyPrice(0),gold=e.gold;
        TradeOrder q=TradeOrder.quote(e,0,false,0,7,200);
        check(q.valid() && q.quantity==7 && q.total==7*price,"quantity quote uses current price");
        check(q.goldAfter==gold-q.total && q.cargoAfter==7,"receipt previews balance and hold");
        check(e.gold==gold && e.cargo()==0,"preview never mutates game");
        q=TradeOrder.quote(e,0,false,1,price*8+price-1,200);
        check(q.quantity==8,"budget buys whole units without rounding up");
        q=TradeOrder.quote(e,0,false,1,Integer.MAX_VALUE,1700);
        check(q.total<=100 && q.goldAfter>=1700,"budget also respects reserve and available gold");
        q=TradeOrder.quote(e,0,false,2,0,0);
        check(q.quantity==Math.min(e.capacity(),gold/price),"maximum respects hold and gold");
        check(!TradeOrder.quote(e,0,false,0,999,0).valid(),"explicit quantity never silently reduced");
        check(!TradeOrder.quote(e,0,false,1,price-1,0).valid(),"budget below one unit cannot trade");
    }
    private static void limitsAndParsing() {
        Engine e=new Engine();e.inventory[1]=24;
        check(TradeOrder.quote(e,0,false,2,0,0).quantity==1,"other goods occupy hold");
        e.inventory[1]=25;
        check(!TradeOrder.quote(e,0,false,2,0,0).valid(),"full hold gives clear rejection");
        check(TradeOrder.number("1,200")==1200 && TradeOrder.number(" 12 ")==12,"friendly numeric input");
        for(String input:new String[]{"", "-1", "1.5", "1,,000", "2,00", "999999999999", "abc"})
            check(TradeOrder.number(input)==-1,"invalid/overflow input rejected");
        check(!TradeOrder.quote(e,-1,false,0,1,0).valid(),"invalid good safe");
        check(!TradeOrder.quote(e,0,false,9,1,0).valid(),"invalid mode safe");
        check(!TradeOrder.quote(e,0,false,0,1,-1).valid(),"negative reserve safe");
        e.gold=0;TradeOrder invalid=TradeOrder.quote(e,0,false,0,5,0);
        check(!invalid.valid() && invalid.bonusGold==0,"invalid buy cannot invent a story reward");
    }
    private static void sellingAndCommit() {
        Engine e=new Engine();e.inventory[0]=9;
        TradeOrder q=TradeOrder.quote(e,0,true,2,0,200);
        int price=e.sellPrice(0),gold=e.gold;
        check(q.quantity==9 && q.total==9*price,"sell all prices held goods");
        check(q.execute(e) && e.inventory[0]==0 && e.gold==gold+q.total,"confirmed quote sells exactly once");
        check(!q.execute(e) && e.gold==gold+q.total,"double tap cannot repeat transaction");
        check(!TradeOrder.quote(e,0,true,0,1,0).valid(),"empty sale rejected");
        check(!TradeOrder.quote(e,0,true,1,100,0).valid(),"budget sale mode unsupported");
        e=new Engine();q=TradeOrder.quote(e,0,false,0,3,0);e.work();gold=e.gold;
        check(!q.execute(e) && e.gold==gold && e.cargo()==0,"stale receipt rejects rather than reprices");
        q=TradeOrder.quote(e,0,false,0,3,0);
        check(q.execute(e) && e.inventory[0]==3,"fresh buy quote succeeds");
        e=new Engine();TravelFlow.begin(e,1,1);
        check(!TradeOrder.quote(e,0,false,0,1,0).valid(),"cannot trade before arrival");
    }
    private static void storyReceipt() {
        Engine e=new Engine();e.buy(0,5);e.sail(1,0);
        TradeOrder quote=TradeOrder.quote(e,0,true,0,5,0);
        check(quote.bonusGold==Story.REWARD,"receipt includes first chapter sale reward");
        check(quote.execute(e) && e.gold==quote.goldAfter,"post-sale balance includes story reward");
    }
    public static void main(String[] args) {
        quantityAndBudget();limitsAndParsing();sellingAndCommit();storyReceipt();
        System.out.println("PASS: "+checks+" purchase quote checks");
    }
}
