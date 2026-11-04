package com.bluehorizon;

/** One-way quotes for newly bought goods, excluding repair and return costs. */
public final class TradeHint {
    public final int port,net,cost;
    private TradeHint(int target,int profit,int required) { port=target;net=profit;cost=required; }
    public static TradeHint best(Engine game,int good,int amount) {
        if(good<0 || good>=6 || amount<1 || amount>game.capacity()) return null;
        TradeHint best=null;
        for(int target=0;target<World.COUNT;target++) {
            if(target==game.port) continue;
            int buy=game.buyPrice(good)*amount;
            int sale=World.price(target,good)*(85+game.influence[target]*2)/100*amount;
            int cost=buy+game.travelCost(target,0),net=sale-cost;
            if(best==null || net>best.net) best=new TradeHint(target,net,cost);
        }
        return best;
    }
}
