package com.deckball;

public final class EngineTest {
    static int checks = 0;
    static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
    static void expectReject(Runnable action) {
        boolean rejected = false;
        try { action.run(); } catch (IllegalArgumentException e) { rejected = true; }
        check(rejected, "Invalid deck must be rejected");
    }
    static void manualControls() {
        GameEngine game = new GameEngine(72);
        game.start();
        game.swing(.78); game.take();
        check(game.strikes == 0 && game.balls == 0, "No batting during defense");
        game.batting = true;
        game.pitch(0, 0, .65);
        check(game.strikes == 0 && game.balls == 0, "No pitching during attack");
        game.selectPitcher(28);
        check(game.pitcher == 28, "Pitcher choice");
        expectReject(() -> game.selectPitcher(0));
        int goodHits = 0, badHits = 0, goodStrikes = 0, badStrikes = 0;
        for (int i = 0; i < 400; i++) {
            game.start(); game.batting = true; game.swing(.78);
            if (game.bases[0] || game.bases[1] || game.home > 0) goodHits++;
            game.start(); game.batting = true; game.swing(0);
            if (game.bases[0] || game.bases[1] || game.home > 0) badHits++;
            game.start(); game.pitch(1, 0, .65); goodStrikes += game.strikes;
            game.start(); game.pitch(1, 0, 0); badStrikes += game.strikes;
        }
        check(goodHits > badHits + 100, "Swing timing affects contact");
        check(goodStrikes > badStrikes, "Release timing affects pitching");
        game.start(); game.batting = true; game.take();
        check(game.balls + game.strikes == 1, "Take resolves one pitch");
    }
    static void deckAndScoring() {
        GameEngine game = new GameEngine(42);
        check(game.roster.size() >= 30, "Player choices");
        int replacement = game.roster.get(12).id;
        game.select(0, replacement);
        check(game.lineup[0] == replacement, "Free selection");
        expectReject(() -> game.select(1, replacement));
        expectReject(() -> game.select(0, game.pitcher));
        expectReject(() -> game.select(0, -1));
        int first = game.lineup[0];
        game.swap(0, 1);
        check(game.lineup[1] == first, "Batting order");
        String deck = game.encodeDeck();
        GameEngine restored = new GameEngine(4);
        check(restored.loadDeck(deck), "Load deck");
        check(restored.encodeDeck().equals(deck), "Round trip");
        check(!restored.loadDeck("broken"), "Corrupt deck");
        check(restored.encodeDeck().equals(deck), "Atomic load");
        game.roster.add(new GameEngine.Player(33,"내 타자","DH","내 팀",90,90,40));
        game.roster.add(new GameEngine.Player(34,"내 투수","P","내 팀",40,40,90));
        game.select(0,33); game.selectPitcher(34);
        String customDeck = game.encodeDeck();
        check(game.loadDeck(customDeck), "Custom player deck load");
        check(game.encodeDeck().equals(customDeck), "Custom player deck round trip");
        gameplay(game);
    }
    static void gameplay(GameEngine game) {
        game.start();
        check(!game.batting, "Pitch first");
        for (int i = 0; i < 3; i++) game.out();
        check(game.batting && game.inning == 1, "Switch sides");
        game.hit(4);
        check(game.home == 1, "Solo home run");
        game.hit(1);
        game.hit(4);
        check(game.home == 3, "Runner scores");
        for (int i = 0; i < 3; i++) game.strike();
        check(game.outs == 1 && game.strikes == 0, "Strikeout");
        for (int i = 0; i < 4; i++) game.ball();
        check(game.bases[0] && game.balls == 0, "Walk");
        finishAndRestart(game);
    }
    static void finishAndRestart(GameEngine game) {
        int guard = 0;
        while (!game.finished && guard++ < 1000) game.autoPlate();
        check(game.finished, "Finite three inning game");
        int score = game.home + game.away;
        game.hit(4);
        game.out();
        check(score == game.home + game.away, "Finished game frozen");
        game.start();
        check(game.home == 0 && game.away == 0 && !game.finished, "Restart");
    }
    public static void main(String[] args) {
        deckAndScoring(); manualControls();
        System.out.println(checks + " engine checks passed");
    }
}
