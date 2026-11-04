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
        ClubTest.run(); autoAbilities(); settingsRoundTrip(); kboSeason(); difficultyAndEditing(); deckAndScoring(); manualControls();
        System.out.println(checks + " engine checks passed");
    }
    static void kboSeason() {
        GameEngine game = new GameEngine(1);
        java.util.Set<String> teams = new java.util.HashSet<>();
        int legends = 0;
        for (GameEngine.Player p : game.roster) {
            if (p.year == 2026) teams.add(p.team);
            if (p.legend) legends++;
            check(!p.record.isEmpty() && !p.source.isEmpty(), "Record provenance");
        }
        check(teams.size() == 10, "2026 ten clubs");
        check(legends >= 10, "Historical season legends");
    }
    static void autoAbilities() {
        GameEngine weak = new GameEngine(10), strong = new GameEngine(10);
        weak.editRatings(weak.opponentPitcher,40,40,1);
        strong.editRatings(strong.opponentPitcher,40,40,99);
        int weakHits=0, strongHits=0;
        for(int i=0;i<1000;i++) {
            weak.start(); strong.start(); weak.batting=strong.batting=true;
            weak.autoPlate(); strong.autoPlate();
            if(weak.bases[0] || weak.home>0) weakHits++;
            if(strong.bases[0] || strong.home>0) strongHits++;
        }
        check(weakHits > strongHits + 50,"Opponent pitcher affects manager mode");
        weak = new GameEngine(10); strong = new GameEngine(10);
        weak.editRatings(weak.opponentHitter,1,1,40);
        strong.editRatings(strong.opponentHitter,99,99,40);
        int weakRuns=0, strongRuns=0;
        for(int i=0;i<1000;i++) {
            weak.start(); strong.start(); weak.autoPlate(); strong.autoPlate();
            weakRuns+=weak.away; strongRuns+=strong.away;
        }
        check(strongRuns>weakRuns+10,"Opponent hitter affects manager mode");
    }
    static void settingsRoundTrip() {
        GameEngine game = new GameEngine(1), restored = new GameEngine(2);
        game.editRatings(7, 99, 98, 97); game.setDifficulty(2); game.selectOpponent(7,29);
        String saved = GameSettings.capture(game);
        check(GameSettings.restore(restored,saved), "Restore settings");
        check(restored.player(7).contact == 99 && restored.difficulty == 2, "Persist ratings/difficulty");
        check(restored.opponentHitter == 7 && restored.opponentPitcher == 29, "Persist opponent");
        check(!GameSettings.restore(restored,saved + ";0,0,0,0"), "Reject bad ratings");
        check(GameSettings.capture(restored).equals(saved), "Atomic settings restore");
        check(!GameSettings.DECK_KEY.equals("deck"), "Isolate old roster deck keys");
        check(!GameSettings.slotKey(0).equals("slot0"), "Isolate old saved slots");
    }
    static void difficultyAndEditing() {
        GameEngine easy = new GameEngine(42), hard = new GameEngine(42);
        easy.setDifficulty(0); hard.setDifficulty(2);
        check(easy.pitchDuration() > hard.pitchDuration(), "Pitch speed");
        check(easy.timingWindow() > hard.timingWindow(), "Timing window");
        expectReject(() -> easy.setDifficulty(3));
        easy.editRatings(0, 99, 98, 97);
        check(easy.player(0).contact == 99 && easy.player(0).power == 98, "Edit real player");
        expectReject(() -> easy.editRatings(0, 0, 50, 50));
        check(easy.player(0).contact == 99, "Atomic edit");
        int a = 0, b = 0;
        for (int i = 0; i < 500; i++) {
            easy.start(); hard.start(); easy.batting = hard.batting = true;
            easy.swing(.60); hard.swing(.60);
            if (easy.bases[0] || easy.home > 0) a++;
            if (hard.bases[0] || hard.home > 0) b++;
        }
        check(a > b + 50, "Easy is more forgiving");
        easy.selectOpponent(7, 29);
        check(easy.opponentHitter == 7 && easy.opponentPitcher == 29, "Opponent selection");
        expectReject(() -> easy.selectOpponent(29, 7));
        check(easy.opponentHitter == 7, "Atomic opponent selection");
    }
}
