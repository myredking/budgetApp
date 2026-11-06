package com.towerfront;

/** Read-only goals derived from earned stars; ranks do not change battle power. */
public final class CampaignProgress {
    private CampaignProgress() { }
    public static int completed(Profile p,int mode) {
        int total=0;for(int stage=1;stage<=Campaign.COUNT;stage++) if(p.stars(stage,mode)>0) total++;
        return total;
    }
    public static int perfect(Profile p,int mode) {
        int total=0;for(int stage=1;stage<=Campaign.COUNT;stage++) if(p.stars(stage,mode)==3) total++;
        return total;
    }
    public static int chapterStars(Profile p,int mode,int chapter) {
        if(chapter<0 || chapter>9) return 0;
        int stars=0;for(int stage=chapter*10+1;stage<=chapter*10+10;stage++) stars+=p.stars(stage,mode);
        return stars;
    }
    public static int chapterCompleted(Profile p,int mode,int chapter) {
        if(chapter<0 || chapter>9) return 0;
        int count=0;for(int stage=chapter*10+1;stage<=chapter*10+10;stage++) if(p.stars(stage,mode)>0) count++;
        return count;
    }
    public static int next(Profile p,int mode) {
        for(int stage=1;stage<=Campaign.COUNT;stage++) if(p.stars(stage,mode)==0) return stage;
        for(int stage=1;stage<=Campaign.COUNT;stage++) if(p.stars(stage,mode)<3) return stage;
        return Campaign.COUNT;
    }
    public static String rank(Profile p,int mode) {
        int stars=p.total(mode);
        if(stars>=300) return "전설의 지휘관";
        if(stars>=200) return "전략 사령관";
        if(stars>=100) return "숙련 사령관";
        return stars>=30?"전선 지휘관":"신입 지휘관";
    }
}
