package com.bluehorizon;

public final class ShipPerformance {
    private static final int[] PERCENT={100,80,65};
    public static int days(Engine e,int base) {
        if(base==0) return 0;
        return Math.max(2,(base*PERCENT[e.level-1]+99)/100);
    }
    public static String description(int level) {
        return "선박 성능 · 기본 항해일수 "+PERCENT[level-1]+"% 적용 (올림·최소 이틀) · 돛 개조 추가 반영";
    }
}
