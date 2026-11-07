package com.bluehorizon;

import java.util.ArrayList;
import java.util.List;

/** Deterministic offline simulation; Android-independent for JVM tests. */
public final class Engine {
    public final MarketPressure market=new MarketPressure();
    public transient int battleEnemyAfter=-1;
    public static final String[] PORTS = World.PORTS;
    public static final String[] GOODS = GoodsCatalog.names();
    public static final String[] SHIPS = {"바람돛 슬루프", "새벽빛 캐러벨", "푸른별 갤리온"};
    public int gold = 1800, port = 0, day = 1, hull = 100, level = 1;
    public int fame = 0, chapter = 0, profit = 0;
    public long visited = 1L;
    public final int[] inventory = new int[6], influence = new int[World.COUNT];
    public final List<String> log = new ArrayList<String>();
    public AdventureState adventure=new AdventureState();
    public CrewState crew=new CrewState();
    public RefitState refits=new RefitState();
    public RivalState rivals=new RivalState();
    public TownState town=new TownState();
    public ExpeditionState expedition=new ExpeditionState();
    public PortLifeState life=new PortLifeState();
    public String message = "서린 선장, 세비야에 목재 5개를 전해 주세요.";

    public Engine() { note("리스본에서 첫 출항 준비. 일곱 바다의 공동 해도를 만들기 위한 항해가 시작된다."); }
    public int capacity() { return new int[]{0, 25, 45, 70}[level]+refits.levels[3]*8; }
    public int maxHull() { return 80 + level * 20+refits.levels[2]*15; }
    public int cargo() {
        int total = 0;
        for (int value : inventory) total += value;
        return total;
    }
    public int buyPriceAt(int harbor, int good) { return Rivals.buyPrice(this,harbor,CrewAbilities.buyAt(this,harbor,good)); }
    public int buyPrice(int good) { return buyPriceAt(port,good); }
    public int sellPriceAt(int harbor,int good) {
        int quote=World.price(harbor,good)*Math.min(94,CrewAbilities.salePercent(this,harbor)+Rivals.saleBonus(this,harbor))/100;
        int cheapest=World.price(harbor,good)*95/100;
        int base=Math.min(quote,Math.max(1,cheapest-1));
        return base*(100-MarketPressure.discount(this,harbor,good))/100;
    }
    public int sellPrice(int good) { return sellPriceAt(port,good); }
    public boolean hasVisited(int harbor) { return World.validPort(harbor) && (visited & (1L << harbor))!=0; }
    public int visitedCount() { return Long.bitCount(visited); }
    private boolean goodAmount(int good, int amount) {
        return good >= 0 && good < GOODS.length && amount > 0 && amount <= capacity();
    }
    public boolean buy(int good, int amount) {
        if(busy()) return fail("항해 사건을 먼저 처리하세요.");
        if (!goodAmount(good, amount)) return fail("거래 수량을 확인해 주세요.");
        if (cargo() + amount > capacity()) return fail("적재 공간이 부족합니다.");
        int cost = buyPrice(good) * amount;
        if (gold < cost) return fail("금화가 부족합니다.");
        gold -= cost; inventory[good] += amount;
        note(GOODS[good] + " " + amount + "개 매입 · −" + cost + " G");
        return true;
    }
    public boolean sell(int good, int amount) {
        if(busy()) return fail("항해 사건을 먼저 처리하세요.");
        if (!goodAmount(good, amount)) return fail("거래 수량을 확인해 주세요.");
        if (inventory[good] < amount) return fail("판매할 화물이 부족합니다.");
        int earned = sellPrice(good) * amount;
        inventory[good] -= amount; gold += earned; profit += earned;
        fame += Math.max(1, amount / 5);
        Rivals.onSale(this,amount);market.recordSale(this,good,amount);
        note(GOODS[good] + " " + amount + "개 판매 · +" + earned + " G");
        deliveryStory(good, amount);
        advanceStory();
        return true;
    }
    private void deliveryStory(int good, int amount) {
        if (amount < 5) return;
        if (Story.deliveryReady(this,good)) completeChapter();
    }
    public int travelDays(int target) {
        return Refits.days(this,ShipPerformance.days(this,WorldRoutes.between(port,target).days()));
    }
    public int travelCost(int target, int mode) { return TownRpg.supplyCost(this,CrewAbilities.supply(this,travelDays(target) * (mode == 1 ? 24 : 16))); }
    public int travelDamage(int target,int mode) {
        int days=travelDays(target);
        int raw=mode==1?Math.max(2,(int)Math.ceil(days/15.0)):Math.min(40,5+(day+target)%10+days/10);
        return Refits.damage(this,CrewAbilities.damage(this,raw));
    }
    public boolean sail(int target, int mode) {
        if(busy()) return fail("항해 사건을 먼저 처리하세요.");
        if (!World.validPort(target) || target == port) return fail("다른 항구를 선택하세요.");
        if (mode < 0 || mode > 1) return fail("항로를 확인하세요.");
        if (hull < 25) return fail("선체를 먼저 수리하세요. 선체 25 이상 필요.");
        int cost = travelCost(target, mode), days = travelDays(target);
        if (gold < cost) return fail("보급비가 부족합니다. 항구 일거리로 금화를 모으세요.");
        int damage=travelDamage(target,mode);
        if(!hasVisited(target)) fame++;
        gold -= cost; GameClock.advance(this,days); port = target; visited |= 1L << target;
        hull = Math.max(1, hull - damage);
        Rivals.advance(this);
        note(PORTS[port] + " 도착 · " + days + "일 항해 · 보급 −" + cost + " G");
        note("항로를 무사히 통과했다. 선체 −" + damage + ". 경유 항구는 직접 정박해야 방문 기록에 남는다.");
        advanceStory();
        return true;
    }
    public int repairCost() { return (maxHull() - hull) * 3; }
    public boolean repair() {
        if(busy()) return fail("항해 사건을 먼저 처리하세요.");
        int cost = repairCost();
        if (cost == 0) return fail("선체 상태가 완벽합니다.");
        if (gold < cost) return fail("수리비가 부족합니다. 항구 일거리를 이용하세요.");
        gold -= cost; hull = maxHull(); note("선체 수리 완료 · −" + cost + " G");
        return true;
    }
    public int upgradeCost() { return level * 2400; }
    public boolean upgrade() {
        if(busy()) return fail("항해 사건을 먼저 처리하세요.");
        if (level == 3) return fail("최고 등급의 선박입니다.");
        int cost = upgradeCost();
        if (gold < cost) return fail("새 선박 구입비가 부족합니다.");
        gold -= cost; level++; hull = maxHull(); fame += 5;
        note(SHIPS[level - 1] + " 진수! 적재량 " + capacity() + "개.");
        advanceStory();
        return true;
    }
    public boolean invest() {
        if(busy()) return fail("항해 사건을 먼저 처리하세요.");
        if (influence[port] == 3) return fail("이 항구와의 동맹이 완성되었습니다.");
        int cost = (influence[port] + 1) * 300;
        if (gold < cost) return fail("투자할 금화가 부족합니다.");
        gold -= cost; influence[port]++; fame += 3;
        Rivals.onInvest(this);
        note(PORTS[port] + " 교역소에 투자. 판매가 우대와 명성 +3.");
        advanceStory(); return true;
    }
    public boolean work() {
        if(busy()) return fail("항해 사건을 먼저 처리하세요.");
        gold += 120; GameClock.advance(this,1);Rivals.advance(this); note("부두 운송을 도왔다. 하루 경과 · +120 G");
        advanceStory(); return true;
    }
    public void advanceStory() {
        if (Story.ready(this)) completeChapter();
    }
    private void completeChapter() {
        chapter++; gold += Story.REWARD;
        note(Story.REVEALS[chapter - 1]);
    }
    public void note(String text) {
        message = text; log.add("[" + day + "일] " + text);
        while (log.size() > 12) log.remove(0);
    }
    private boolean fail(String text) { message = text; return false; }
    public boolean actionBlocked() { return adventure.pending>=0 || town.moving() || expedition.active(); }
    private boolean busy() { return actionBlocked(); }
    public String encode() { return SaveCodec.encode(this); }
    public static Engine decode(String text) { return SaveCodec.decode(text); }
}
