package com.towerfront;

public final class Profile {
    private final int[][] best=new int[3][Campaign.COUNT];
    public int stars(int stage,int mode) {
        if(!valid(stage,mode)) return 0;
        return best[mode][stage-1];
    }
    private boolean valid(int stage,int mode) {
        return stage>=1 && stage<=Campaign.COUNT && mode>=0 && mode<3;
    }
    public void record(int stage,int mode,int stars) {
        if(!valid(stage,mode)) return;
        best[mode][stage-1]=Math.max(best[mode][stage-1],Math.min(3,Math.max(0,stars)));
    }
    public int unlocked(int mode) {
        int stage=1;
        while(stage<Campaign.COUNT && stars(stage,mode)>0) stage++;
        return stage;
    }
    public boolean available(int stage,int mode) {
        return valid(stage,mode) && stage<=unlocked(mode);
    }
    public int total(int mode) {
        int count=0;
        for(int i=1;i<=Campaign.COUNT;i++) count+=stars(i,mode);
        return count;
    }
    public String encode() {
        StringBuilder out=new StringBuilder("TF1:");
        for(int[] mode:best) for(int stars:mode) out.append(stars);
        return out.toString();
    }
    public static Profile decode(String text) {
        Profile result=new Profile();
        if(text==null || !text.matches("TF1:[0-3]{36}")) return result;
        for(int i=0;i<36;i++) result.best[i/12][i%12]=text.charAt(i+4)-'0';
        return result;
    }
}
