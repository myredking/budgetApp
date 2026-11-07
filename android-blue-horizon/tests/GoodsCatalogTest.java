package com.bluehorizon;
import java.nio.file.*;
import java.util.List;

public final class GoodsCatalogTest {
    public static void main(String[] args) throws Exception {
        prices();
        identities();
        savedProgress();
        System.out.println("PASS: 336 baseline prices and stable IDs / BLUE9 cargo and progress");
    }
    private static void prices() throws Exception {
        List<String> rows=Files.readAllLines(Paths.get("tests/fixtures/legacy-goods-prices.csv"));
        if(rows.size()!=World.COUNT) throw new AssertionError("missing baseline ports");
        for(int port=0;port<World.COUNT;port++) {
            String[] prices=rows.get(port).split(",");
            if(prices.length!=6) throw new AssertionError("missing baseline goods");
            for(int good=0;good<prices.length;good++) {
                if(World.price(port,good)!=Integer.parseInt(prices[good])) throw new AssertionError("legacy price changed "+port+"/"+good);
            }
        }
    }
    private static void identities() {
        String[] ids={"wood","cloth","ceramics","spices","tea","silver"};
        for(int slot=0;slot<ids.length;slot++) {
            if(GoodsCatalog.slot(ids[slot])!=slot) throw new AssertionError("legacy slot changed: "+ids[slot]);
        }
        if(GoodsCatalog.slot("unknown")!=-1) {
            throw new AssertionError("legacy IDs changed");
        }
    }
    private static void savedProgress() throws Exception {
        Engine old=Engine.decode(Files.readString(Paths.get("tests/fixtures/v9-trading-save.txt")));
        if(old==null || old.chapter!=1 || old.inventory[1]!=2 || old.port!=1 || old.life.events!=2 || old.crew.first!=1) {
            throw new AssertionError("old cargo/story/crew progress lost");
        }
        Engine roundTrip=Engine.decode(old.encode());
        if(roundTrip==null || !roundTrip.encode().equals(old.encode())) throw new AssertionError("save round trip changed");
    }
}
