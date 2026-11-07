package com.bluehorizon;

import java.io.*;

public final class MarketPressureTest {
    public static void main(String[] args) throws Exception {
        trading();
        cap();
        previousRule();
        System.out.println("PASS: market price impact, recovery, scope and save");
    }

    private static void cap() {
        Engine game=new Engine();
        game.port=2;game.visited|=1L<<2;game.gold=10000;
        int good=GoodsCatalog.slot("ceramics");
        if(!game.buy(good,25) || !game.sell(good,25)) throw new AssertionError("bulk trade failed");
        if(MarketPressure.discount(game,2,good)!=20) throw new AssertionError("discount exceeded cap");
        GameClock.advance(game,8);
        if(MarketPressure.discount(game,2,good)!=0) throw new AssertionError("capped pressure failed to recover");
    }

    private static void trading() {
        Engine game=new Engine();
        game.port=2;game.visited|=1L<<2;
        int good=GoodsCatalog.slot("ceramics");
        int normal=game.sellPrice(good);
        if(!game.buy(good,10)) throw new AssertionError("stock purchase failed");
        TradeOrder order=TradeOrder.quote(game,good,true,0,5,0);
        if(order.total!=5*normal || !order.execute(game)) throw new AssertionError("first sale repriced");
        int reduced=game.sellPrice(good);
        if(reduced!=normal*95/100) throw new AssertionError("five units should reduce the next price");
        Engine restored=Engine.decode(game.encode());
        if(restored==null || restored.sellPrice(good)!=reduced) throw new AssertionError("pressure lost on reload");
        GameClock.advance(restored,2);
        if(restored.sellPrice(good)!=normal) throw new AssertionError("price did not recover");
        if(MarketPressure.discount(game,1,good)!=0 || MarketPressure.discount(game,2,0)!=0) {
            throw new AssertionError("pressure leaked to another market");
        }
    }

    private static void previousRule() throws IOException {
        Engine game=new Engine();
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        DataOutputStream out=new DataOutputStream(bytes);
        for(int value:new int[]{2000,1,1,2}) out.writeInt(value);
        game.market.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
        Engine restored=Engine.decode(game.encode());
        if(restored==null || MarketPressure.discount(restored,2,2)!=0) {
            throw new AssertionError("changing the pilot market must preserve saves without carrying over pressure");
        }
    }
}
