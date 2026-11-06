package com.bluehorizon;

/** Visual clock only; game costs and outcomes remain owned by TravelFlow. */
public final class VoyageTimeline {
    private long elapsed;
    private int speed=1;
    private boolean paused;
    public boolean speed(int value) {
        if(value!=1 && value!=2 && value!=4) return false;
        speed=value;return true;
    }
    public void advance(long delta) {
        if(paused || delta<=0) return;
        elapsed=Math.min(5500,elapsed+Math.min(250,delta)*speed);
    }
    public long elapsed() { return elapsed; }
    public boolean done() { return elapsed>=5500; }
    public boolean paused() { return paused; }
    public void pause() { paused=true; }
    public void resume() { paused=false; }
}
