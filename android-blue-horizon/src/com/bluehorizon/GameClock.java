package com.bluehorizon;

/** Simulation days advance only through game actions, independent of wall time. */
public final class GameClock {
    private GameClock() {}

    public static void advance(Engine game, int days) {
        if(days<0) throw new IllegalArgumentException("Days must be non-negative");
        game.day=Math.addExact(game.day,days);
    }

    public static boolean pastDeadline(Engine game, int deadline) {
        return game.day>deadline;
    }
}
