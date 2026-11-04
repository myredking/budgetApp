package com.deckball;

public final class TimingFeedback {
    private TimingFeedback() {}
    public static String grade(double timing,double target,double window) {
        if(Double.isNaN(timing) || Double.isInfinite(timing) || timing<0 || timing>1)
            throw new IllegalArgumentException("잘못된 타이밍");
        double delta=timing-target,error=Math.abs(delta);
        if(error<=window*.35) return "PERFECT · 완벽";
        if(error<=window) return "GOOD · 좋음";
        return delta<0?"EARLY · 빠름":"LATE · 늦음";
    }
}
