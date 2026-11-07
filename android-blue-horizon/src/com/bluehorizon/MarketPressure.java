package com.bluehorizon;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/** Independent sale pressure per port and commodity. */
public final class MarketPressure {
    private static final int GOODS=GoodsCatalog.names().length;
    private static final int SIZE=World.COUNT*GOODS;
    private static final Rule[] RULES=loadRules();
    private final int[] units=new int[SIZE], lastSaleDay=new int[SIZE];

    private static final class Rule {
        final int unitsPerStep, discount, recovery, maximum;
        Rule(int[] row) {
            unitsPerStep=row[2];discount=row[3];recovery=row[4];
            maximum=unitsPerStep*row[5];
        }
    }

    private static Rule[] loadRules() {
        Rule[] rules=new Rule[SIZE];
        for(int[] row:GoodsCatalog.MARKET_RULES) rules[key(row[0],row[1])]=new Rule(row);
        return rules;
    }

    private static int key(int port,int good) { return port*GOODS+good; }

    public static boolean applies(int port,int good) { return RULES[key(port,good)]!=null; }

    public static int recoveryDays(int port,int good) { return RULES[key(port,good)].recovery; }

    public static int discount(Engine game,int port,int good) {
        int key=key(port,good);
        Rule rule=RULES[key];
        if(rule==null) return 0;
        return game.market.remaining(key,game.day,rule)/rule.unitsPerStep*rule.discount;
    }

    public void recordSale(Engine game,int good,int quantity) {
        int key=key(game.port,good);
        Rule rule=RULES[key];
        if(rule==null) return;
        units[key]=Math.min(rule.maximum,remaining(key,game.day,rule)+quantity);
        lastSaleDay[key]=game.day;
    }

    private int remaining(int key,int day,Rule rule) {
        int steps=Math.max(0,day-lastSaleDay[key])/rule.recovery;
        return Math.max(0,Math.min(units[key],rule.maximum)-steps*rule.unitsPerStep);
    }

    public void write(DataOutputStream out) throws IOException {
        int count=0;
        for(int value:units) if(value>0) count++;
        out.writeInt(count);
        for(int key=0;key<SIZE;key++) {
            if(units[key]==0) continue;
            out.writeInt(units[key]);out.writeInt(lastSaleDay[key]);
            out.writeInt(key/GOODS);out.writeInt(key%GOODS);
        }
    }

    public void read(DataInputStream in) throws IOException {
        int count=in.readInt();
        if(count<0 || count>SIZE) throw new IOException("Invalid market count");
        for(int row=0;row<count;row++) readEntry(in,false);
    }

    public void readPilot(DataInputStream in) throws IOException { readEntry(in,true); }

    private void readEntry(DataInputStream in,boolean pilot) throws IOException {
        int quantity=in.readInt(), day=in.readInt(), port=in.readInt(), good=in.readInt();
        if(pilot && emptyPilot(quantity,day,port,good)) return;
        if(!World.validPort(port) || good<0 || good>=GOODS) throw new IOException("Invalid market key");
        int key=key(port,good);
        if(quantity<1 || quantity>2000 || day<1 || units[key]!=0) throw new IOException("Invalid market state");
        units[key]=quantity;lastSaleDay[key]=day;
    }

    private static boolean emptyPilot(int quantity,int day,int port,int good) {
        return quantity==0 && day==0 && port==-1 && good==-1;
    }

    public boolean valid(int day) {
        for(int last:lastSaleDay) if(last>day) return false;
        return true;
    }
}
