package com.bluehorizon;

public final class VoyageTimelineTest {
    private static int checks;
    private static void check(boolean value,String text) { checks++;if(!value) throw new AssertionError(text); }
    private static int duration(int speed) {
        VoyageTimeline time=new VoyageTimeline();time.speed(speed);int frames=0;
        while(!time.done()) { time.advance(50);frames++;if(frames>200) throw new AssertionError("never finishes"); }
        return frames*50;
    }
    public static void main(String[] args) {
        check(duration(1)==5500,"normal voyage duration unchanged");
        check(duration(2)==2750 && duration(4)==1400,"speed choices reduce only visual duration");
        VoyageTimeline time=new VoyageTimeline();time.advance(50);long before=time.elapsed();
        time.pause();time.advance(200);check(time.elapsed()==before,"paused timeline cannot progress");
        time.resume();time.advance(50);check(time.elapsed()==before+50,"resume preserves elapsed time");
        check(!time.speed(3) && !time.speed(0),"invalid preferences rejected");
        time.advance(-100);check(time.elapsed()==before+50,"clock cannot move backward");
        while(!time.done()) time.advance(100);
        time.advance(100);check(time.elapsed()==5500,"finished timeline clamped at arrival");
        System.out.println("PASS: "+checks+" voyage speed checks");
    }
}
