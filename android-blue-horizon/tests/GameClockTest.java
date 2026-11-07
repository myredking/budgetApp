package com.bluehorizon;

public final class GameClockTest {
    public static void main(String[] args) {
        Engine e=new Engine();
        GameClock.advance(e,6);
        if(e.day!=7 || GameClock.pastDeadline(e,7)) throw new AssertionError("deadline includes due day");
        GameClock.advance(e,1);
        if(e.day!=8 || !GameClock.pastDeadline(e,7)) throw new AssertionError("day after deadline expires");
        try {
            GameClock.advance(e,-1);
            throw new AssertionError("negative time accepted");
        } catch(IllegalArgumentException expected) {
            if(e.day!=8) throw new AssertionError("failed time change mutated day");
        }
        Engine restored=Engine.decode(e.encode());
        if(restored==null || restored.day!=8) throw new AssertionError("day failed to restore");
        System.out.println("PASS: shared game clock, deadlines and saved day");
    }
}
