package com.deckball;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public final class GameEngine {
    public static final class Player {
        public final int id, year;
        public int contact, power, control;
        public final boolean legend;
        public final String record, source;
        public final String name, position, team;
        public Player(int id, String name, String position, String team, int contact, int power, int control) {
            this(id,name,position,team,contact,power,control,0,false,"사용자 선수","사용자 입력");
        }
        public Player(int id, String name, String position, String team, int contact, int power, int control,
                      int year, boolean legend, String record, String source) {
            this.year = year; this.legend = legend; this.record = record; this.source = source;
            this.id = id; this.name = name; this.position = position; this.team = team;
            this.contact = contact; this.power = power; this.control = control;
        }
        public String label() {
            return (legend ? "★ " : "") + name + " '" + year + " · " + team + " · " + position + "  컨택 " + contact + " / 파워 " + power;
        }
    }
    public final List<Player> roster = new ArrayList<>();
    public int difficulty = 1, opponentHitter = 3, opponentPitcher = 27;
    public int[] lineup = {0,1,2,3,4,5,6,7,8};
    public int pitcher = 27, inning = 1, outs, balls, strikes, home, away, batter;
    public boolean batting, finished;
    public boolean[] bases = new boolean[3];
    public String message = "나만의 덱으로 경기를 시작하세요";
    private final Random random;

    public GameEngine(long seed) {
        random = new Random(seed);
        KboRoster.populate(roster);
    }

    public void selectOpponent(int hitter, int pitcherId) {
        if (player(hitter).position.equals("P")) throw new IllegalArgumentException("상대 타자 오류");
        if (!player(pitcherId).position.equals("P")) throw new IllegalArgumentException("상대 투수 오류");
        opponentHitter = hitter; opponentPitcher = pitcherId;
    }
    public void setDifficulty(int value) {
        if (value < 0 || value > 2) throw new IllegalArgumentException("난이도 범위");
        difficulty = value;
    }
    public int pitchDuration() { return new int[]{2300,1700,1200}[difficulty]; }
    public double timingWindow() { return new double[]{.12,.07,.045}[difficulty]; }
    public double timingQuality(double timing, double target) {
        return Math.max(0, 1 - Math.abs(timing - target) * (.28 / timingWindow()));
    }
    public void editRatings(int id, int contact, int power, int control) {
        validateRating(contact); validateRating(power); validateRating(control);
        Player p = player(id); p.contact = contact; p.power = power; p.control = control;
    }
    private void validateRating(int value) {
        if (value < 1 || value > 99) throw new IllegalArgumentException("능력치는 1~99");
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
        double quality = timingQuality(timing, .78);
        if (random.nextDouble() > quality * (.60 + hitter.contact / 200.0 - player(opponentPitcher).control / 500.0)) { strike(); return; }
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
        double precision = timingQuality(timing, .65);
        double roll = random.nextDouble();
        double zone = location == 4 ? .1 : .24;
        if (roll < zone * (1.3 - control * precision)) { ball(); return; }
        if (roll < .64 + precision * .16 + kind * .025 - (player(opponentHitter).contact - 75) / 500.0) { strike(); return; }
        if (roll > 1 - player(opponentHitter).power / 2400.0) { hit(4); return; }
        if (roll > .83) { hit(1); return; }
        message = "범타 아웃!"; out();
    }
    public void autoPlate() {
        if (finished) return;
        Player hitter = player(batting ? lineup[batter] : opponentHitter);
        Player bowler = player(batting ? opponentPitcher : pitcher);
        double advantage = (batting ? 1 - difficulty : difficulty - 1) * .04;
        double homeRun = Math.max(.002, .01 + hitter.power / 1800.0 - bowler.control / 5000.0 + advantage / 3);
        double skill = Math.max(.01, .10 + hitter.contact / 400.0 - bowler.control / 650.0 + advantage);
        double roll = random.nextDouble();
        if (roll < homeRun) hit(4);
        else if (roll < homeRun + skill) hit(1);
        else { message = "자동 진행 · 아웃"; out(); }
    }
}
