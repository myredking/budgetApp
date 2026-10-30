package com.deckball;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public final class GameEngine {
    public static final class Player {
        public final int id, contact, power, control;
        public final String name, position, team;
        public Player(int id, String name, String position, String team, int contact, int power, int control) {
            this.id = id; this.name = name; this.position = position; this.team = team;
            this.contact = contact; this.power = power; this.control = control;
        }
        public String label() {
            return name + " · " + team + " · " + position + "  컨택 " + contact + " / 파워 " + power;
        }
    }
    public final List<Player> roster = new ArrayList<>();
    public int[] lineup = {0,1,2,3,4,5,6,7,8};
    public int pitcher = 27, inning = 1, outs, balls, strikes, home, away, batter;
    public boolean batting, finished;
    public boolean[] bases = new boolean[3];
    public String message = "나만의 덱으로 경기를 시작하세요";
    private final Random random;

    public GameEngine(long seed) {
        random = new Random(seed);
        String[] names = {"강태양","윤도현","차민준","백시우","서지호","한유찬","문건우","오준서","정이안"};
        String[] positions = {"C","1B","2B","3B","SS","LF","CF","RF","DH"};
        String[] teams = {"서울 스톰","부산 웨이브","광주 피닉스"};
        for (int team = 0; team < 3; team++) {
            for (int slot = 0; slot < 9; slot++) {
                int id = team * 9 + slot;
                roster.add(new Player(id, names[slot] + (team == 0 ? "" : " " + (team + 1)), positions[slot], teams[team], 65 + id % 30, 62 + id * 7 % 35, 40));
            }
        }
        for (int i = 27; i < 33; i++) {
            roster.add(new Player(i, "에이스 " + (i - 26), "P", teams[i % 3], 40, 40, 65 + i % 30));
        }
    }
    public Player player(int id) {
        if (id < 0 || id >= roster.size()) throw new IllegalArgumentException("없는 선수입니다");
        return roster.get(id);
    }
    public void select(int slot, int id) {
        if (slot < 0 || slot > 8) throw new IllegalArgumentException("잘못된 타순입니다");
        if (player(id).position.equals("P")) throw new IllegalArgumentException("타자를 선택하세요");
        for (int i = 0; i < 9; i++) {
            if (i != slot && lineup[i] == id) throw new IllegalArgumentException("이미 덱에 있는 선수입니다");
        }
        lineup[slot] = id;
    }
    public void selectPitcher(int id) {
        if (!player(id).position.equals("P")) throw new IllegalArgumentException("투수를 선택하세요");
        pitcher = id;
    }
    public void swap(int a, int b) {
        if (a < 0 || a > 8 || b < 0 || b > 8) throw new IllegalArgumentException("잘못된 타순입니다");
        int value = lineup[a]; lineup[a] = lineup[b]; lineup[b] = value;
    }
    public String encodeDeck() {
        StringBuilder text = new StringBuilder(Integer.toString(pitcher));
        for (int id : lineup) text.append(',').append(id);
        return text.toString();
    }
    public boolean loadDeck(String text) {
        try {
            String[] values = text.split(",");
            if (values.length != 10) return false;
            GameEngine candidate = new GameEngine(0);
            candidate.roster.clear(); candidate.roster.addAll(roster);
            candidate.lineup = new int[]{-1,-1,-1,-1,-1,-1,-1,-1,-1};
            candidate.selectPitcher(Integer.parseInt(values[0]));
            for (int i = 0; i < 9; i++) candidate.select(i, Integer.parseInt(values[i + 1]));
            lineup = candidate.lineup.clone(); pitcher = candidate.pitcher;
            return true;
        } catch (IllegalArgumentException e) { return false; }
    }
    public void start() {
        inning = 1; outs = balls = strikes = home = away = batter = 0;
        batting = finished = false; Arrays.fill(bases, false);
        message = "1회 초 · 구종과 코스를 고른 뒤 투구하세요";
    }
    public void resetCount() { balls = strikes = 0; }
    public void score(int runs) {
        if (batting) home += runs; else away += runs;
    }
    public void nextBatter() {
        resetCount();
        if (batting) batter = (batter + 1) % 9;
    }
    public void out() {
        if (finished) return;
        nextBatter(); outs++;
        if (outs < 3) return;
        outs = 0; Arrays.fill(bases, false);
        if (batting) { inning++; batting = false; } else batting = true;
        if (inning > 3) {
            finished = true;
            message = home == away ? "경기 종료 · 무승부" : (home > away ? "경기 종료 · 승리!" : "경기 종료 · 패배");
        } else message = inning + "회 " + (batting ? "말 · 공격" : "초 · 수비");
    }
    public void hit(int distance) {
        if (finished) return;
        int runs = distance == 4 ? 1 : 0;
        boolean[] advanced = new boolean[3];
        for (int i = 0; i < 3; i++) {
            if (!bases[i]) continue;
            if (i + distance >= 3) runs++; else advanced[i + distance] = true;
        }
        if (distance < 4) advanced[distance - 1] = true;
        bases = advanced; score(runs); nextBatter();
        message = distance == 4 ? "HOME RUN!  홈런!" : distance + "루타!";
        if (runs > 0) message += "  +" + runs + "점";
    }
    public void strike() {
        if (finished) return;
        strikes++; message = "스트라이크!";
        if (strikes == 3) { message = "삼진 아웃!"; out(); }
    }
    public void ball() {
        if (finished) return;
        balls++; message = "볼";
        if (balls < 4) return;
        if (bases[0] && bases[1] && bases[2]) score(1);
        if (bases[0] && bases[1]) bases[2] = true;
        if (bases[0]) bases[1] = true;
        bases[0] = true; nextBatter(); message = "볼넷 · 1루 진루";
    }
    public void swing(double timing) {
        if (finished || !batting) return;
        Player hitter = player(lineup[batter]);
        double quality = Math.max(0, 1 - Math.abs(timing - .78) * 4);
        if (random.nextDouble() > quality * (.45 + hitter.contact / 200.0)) { strike(); return; }
        double roll = random.nextDouble();
        if (quality > .85 && roll < hitter.power / 230.0) { hit(4); return; }
        if (roll < .20) { message = "뜬공 아웃"; out(); return; }
        hit(roll > .78 ? 2 : 1);
    }
    public void take() {
        if (finished || !batting) return;
        if (random.nextDouble() < .32) ball(); else strike();
    }
    public void pitch(int kind, int location, double timing) {
        if (finished || batting) return;
        double control = player(pitcher).control / 100.0;
        double precision = Math.max(0, 1 - Math.abs(timing - .65) * 3);
        double roll = random.nextDouble();
        double zone = location == 4 ? .1 : .24;
        if (roll < zone * (1.3 - control * precision)) { ball(); return; }
        if (roll < .64 + precision * .16 + kind * .025) { strike(); return; }
        if (roll > .96) { hit(4); return; }
        if (roll > .83) { hit(1); return; }
        message = "범타 아웃!"; out();
    }
    public void autoPlate() {
        if (finished) return;
        double skill = batting ? player(lineup[batter]).contact / 400.0 : .2;
        double roll = random.nextDouble();
        if (roll < .055) hit(4);
        else if (roll < .055 + skill) hit(1);
        else { message = "자동 진행 · 아웃"; out(); }
    }
}
