package com.bluehorizon;

import java.util.ArrayList;
import java.util.List;

/** Deterministic offline simulation; Android-independent for JVM tests. */
public final class Engine {
    public static final String[] PORTS = {"루미아", "베르단", "사피라", "나마르", "하란"};
    public static final String[] GOODS = {"목재", "직물", "도자기", "향신료", "차", "은"};
    public static final String[] SHIPS = {"바람돛 슬루프", "새벽빛 캐러벨", "푸른별 갤리온"};
    public static final int[][] PRICES = {
        {24, 65, 105, 145, 95, 190}, {65, 30, 90, 125, 110, 170},
        {70, 85, 42, 115, 95, 210}, {75, 95, 110, 35, 70, 180},
        {85, 100, 115, 130, 32, 95}
    };
    public static final float[][] POS = {{.20f,.28f},{.36f,.46f},{.58f,.25f},{.72f,.55f},{.85f,.78f}};
    public int gold = 1800, port = 0, day = 1, hull = 100, level = 1;
    public int fame = 0, visited = 1, chapter = 0, profit = 0;
    public final int[] inventory = new int[6], influence = new int[5];
    public final List<String> log = new ArrayList<String>();
    public String message = "서린 선장, 베르단에 목재 5개를 전해 주세요.";

    public Engine() { note("루미아에서 첫 출항 준비. 아버지의 미완성 해도를 이어 간다."); }
    public int capacity() { return new int[]{0, 25, 45, 70}[level]; }
    public int maxHull() { return 80 + level * 20; }
    public int cargo() {
        int total = 0;
        for (int value : inventory) total += value;
        return total;
    }
    public int buyPriceAt(int harbor, int good) { return PRICES[harbor][good]; }
    public int buyPrice(int good) { return PRICES[port][good]; }
    public int sellPrice(int good) { return PRICES[port][good] * (85 + influence[port] * 2) / 100; }
    private boolean goodAmount(int good, int amount) {
        return good >= 0 && good < GOODS.length && amount > 0 && amount <= 70;
    }
    public boolean buy(int good, int amount) {
        if (!goodAmount(good, amount)) return fail("거래 수량을 확인해 주세요.");
        if (cargo() + amount > capacity()) return fail("적재 공간이 부족합니다.");
        int cost = buyPrice(good) * amount;
        if (gold < cost) return fail("금화가 부족합니다.");
        gold -= cost; inventory[good] += amount;
        note(GOODS[good] + " " + amount + "개 매입 · −" + cost + " G");
        return true;
    }
    public boolean sell(int good, int amount) {
        if (!goodAmount(good, amount)) return fail("거래 수량을 확인해 주세요.");
        if (inventory[good] < amount) return fail("판매할 화물이 부족합니다.");
        int earned = sellPrice(good) * amount;
        inventory[good] -= amount; gold += earned; profit += earned;
        fame += Math.max(1, amount / 5);
        note(GOODS[good] + " " + amount + "개 판매 · +" + earned + " G");
        deliveryStory(good, amount);
        advanceStory();
        return true;
    }
    private void deliveryStory(int good, int amount) {
        if (amount < 5) return;
        if (chapter == 0 && port == 1 && good == 0) completeChapter();
        if (chapter == 2 && port == 4 && good == 3 && level >= 2) completeChapter();
    }
    public int travelDays(int target) {
        double dx = POS[port][0] - POS[target][0], dy = POS[port][1] - POS[target][1];
        return Math.max(2, (int) Math.ceil(Math.sqrt(dx * dx + dy * dy) * 16));
    }
    public int travelCost(int target, int mode) { return travelDays(target) * (mode == 1 ? 24 : 16); }
    public boolean sail(int target, int mode) {
        if (target < 0 || target >= PORTS.length || target == port) return fail("다른 항구를 선택하세요.");
        if (mode < 0 || mode > 1) return fail("항로를 확인하세요.");
        if (hull < 25) return fail("선체를 먼저 수리하세요. 선체 25 이상 필요.");
        int cost = travelCost(target, mode), days = travelDays(target);
        if (gold < cost) return fail("보급비가 부족합니다. 항구 일거리로 금화를 모으세요.");
        gold -= cost; day += days; port = target; visited |= 1 << target;
        int damage = mode == 1 ? 2 : 5 + (day * 7 + target * 3) % 14;
        hull = Math.max(1, hull - damage);
        note(PORTS[port] + " 도착 · " + days + "일 항해 · 보급 −" + cost + " G");
        if (mode == 0) note("거친 바람을 통과했다. 선체 −" + damage + ". 조선소에서 수리할 수 있다.");
        advanceStory();
        return true;
    }
    public int repairCost() { return (maxHull() - hull) * 3; }
    public boolean repair() {
        int cost = repairCost();
        if (cost == 0) return fail("선체 상태가 완벽합니다.");
        if (gold < cost) return fail("수리비가 부족합니다. 항구 일거리를 이용하세요.");
        gold -= cost; hull = maxHull(); note("선체 수리 완료 · −" + cost + " G");
        return true;
    }
    public int upgradeCost() { return level * 2400; }
    public boolean upgrade() {
        if (level == 3) return fail("최고 등급의 선박입니다.");
        int cost = upgradeCost();
        if (gold < cost) return fail("새 선박 구입비가 부족합니다.");
        gold -= cost; level++; hull = maxHull(); fame += 5;
        note(SHIPS[level - 1] + " 진수! 적재량 " + capacity() + "개.");
        advanceStory();
        return true;
    }
    public boolean invest() {
        if (influence[port] == 3) return fail("이 항구와의 동맹이 완성되었습니다.");
        int cost = (influence[port] + 1) * 300;
        if (gold < cost) return fail("투자할 금화가 부족합니다.");
        gold -= cost; influence[port]++; fame += 3;
        note(PORTS[port] + " 교역소에 투자. 판매가 우대와 명성 +3.");
        advanceStory(); return true;
    }
    public boolean work() {
        gold += 120; day++; note("부두 운송을 도왔다. 하루 경과 · +120 G");
        advanceStory(); return true;
    }
    public void advanceStory() {
        if (chapter == 1 && (visited & 12) == 12 && fame >= 10) completeChapter();
        if (chapter == 3 && port == 0 && gold >= 5000) completeChapter();
    }
    private void completeChapter() {
        chapter++; gold += 400;
        note(Story.REVEALS[chapter - 1]);
    }
    public void note(String text) {
        message = text; log.add("[" + day + "일] " + text);
        while (log.size() > 12) log.remove(0);
    }
    private boolean fail(String text) { message = text; return false; }
    public String encode() { return SaveCodec.encode(this); }
    public static Engine decode(String text) { return SaveCodec.decode(text); }
}
