package com.bluehorizon;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/** Sale pressure for the first data-configured market. */
public final class MarketPressure {
    private int units, lastSaleDay, port=-1, good=-1;

    public static boolean applies(int port,int good) {
        return port==GoodsCatalog.MARKET_PORT && good==GoodsCatalog.MARKET_GOOD;
    }

    public static int discount(Engine game,int port,int good) {
        if(!applies(port,good) || !game.market.matches(port,good)) return 0;
        return game.market.remaining(game.day)/GoodsCatalog.MARKET_UNITS*GoodsCatalog.MARKET_DISCOUNT;
    }

    public void recordSale(Engine game,int good,int quantity) {
        if(!applies(game.port,good)) return;
        int previous=matches(game.port,good)?remaining(game.day):0;
        units=Math.min(maximumUnits(),previous+quantity);
        lastSaleDay=game.day;
        port=game.port;
        this.good=good;
    }

    private boolean matches(int port,int good) { return this.port==port && this.good==good; }

    private int remaining(int day) {
        int steps=Math.max(0,day-lastSaleDay)/GoodsCatalog.MARKET_RECOVERY;
        return Math.max(0,Math.min(units,maximumUnits())-steps*GoodsCatalog.MARKET_UNITS);
    }

    private static int maximumUnits() { return GoodsCatalog.MARKET_UNITS*GoodsCatalog.MARKET_MAX_STEPS; }

    public void write(DataOutputStream out) throws IOException {
        out.writeInt(units);
        out.writeInt(lastSaleDay);
        out.writeInt(port);
        out.writeInt(good);
    }

    public void read(DataInputStream in) throws IOException {
        units=in.readInt();
        lastSaleDay=in.readInt();
        port=in.readInt();
        good=in.readInt();
    }

    public boolean valid(int day) {
        if(units==0) return lastSaleDay==0 && port==-1 && good==-1;
        return units>0 && units<=2000 && lastSaleDay>0 && lastSaleDay<=day
            && World.validPort(port) && good>=0 && good<Engine.GOODS.length;
    }
}
