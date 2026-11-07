package com.bluehorizon;
public final class AudioMix {
    public static int level(int percent) { return Math.max(0,Math.min(100,percent)); }
    public static float music(int percent,boolean ducked) { return level(percent)/100f*(ducked?.07f:.24f); }
    public static float effect(int percent) { return level(percent)/100f*.45f; }
}
