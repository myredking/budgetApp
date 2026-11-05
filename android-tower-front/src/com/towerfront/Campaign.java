package com.towerfront;

public final class Campaign {
    public static final int COUNT=100;
    private static final String[] NAMES={"첫 교두보","갈림길","기동 작전","생산 기지",
        "양면 전선","보급 차단","중앙 돌파","삼각 포위","산개 전투","역습","최후 방어선","새벽의 승리"};
    private Campaign() { }
    public static String name(int stage) {
        int index=Math.max(1,Math.min(COUNT,stage));
        if(index<=12) return NAMES[index-1];
        String[] regions={"초원","해안","고원","협곡","산림","군도","성채","분지","국경","최후의 전선"};
        return regions[(index-1)/10]+" 작전 "+index;
    }
    public static int enemies(int stage) {return Math.min(10,2+(stage-13)/9);}
    public static int towerCount(int stage) {
        if(stage<=12) return stage==1?6:stage<4?7:8;
        return Skirmish.towerCount((stage-13)%Skirmish.MAPS,enemies(stage));
    }
    public static void advanced(Engine e) {
        Skirmish.populate(e,(e.level-13)%Skirmish.MAPS,enemies(e.level),e.level*104729L);
        for(Engine.Tower t:e.towers) {
            if(t.owner==0) continue;
            t.units=t.owner==1?Math.min(90,42+e.level/2):Math.min(75,18+e.level/3+e.difficulty*6);
            t.rank=t.owner==1?Math.min(3,1+e.level/35):Math.min(3,1+e.level/50);
        }
        if(e.level>=40) {Engine.Tower support=e.towers.get(e.enemyCount+1);support.owner=1;support.units=20;support.kind=Engine.FORTRESS;}
    }
    public static String difficultyName(int mode) {
        return new String[]{"쉬움","보통","어려움"}[Math.max(0,Math.min(2,mode))];
    }
    public static int next(int stage) {return Math.min(COUNT,stage+1);}
    public static void configure(Engine e) {
        Engine.Tower enemy=e.towers.get(1);
        enemy.units=Math.max(12,enemy.units+(e.difficulty-1)*8);
        configureDifficulty(e);
        if(e.level>=4) e.towers.get(5).kind=Engine.FORTRESS;
        if(e.level>=5) advancedLayout(e);
        if(e.level>=7) e.towers.get(6).kind=Engine.FACTORY;
        if(e.level>=10) enemy.rank=2;
    }
    private static void configureDifficulty(Engine e) {
        if(e.difficulty==2) return;
        e.towers.get(1).units=19+e.level;
        if(e.level==1) e.towers.get(1).units=16;
        e.towers.get(0).units=32+(e.level-1)*2;
        if(e.difficulty==0) e.towers.get(0).units+=12;
        if(e.level<=1) return;
        Engine.Tower extra=e.towers.get(6);
        extra.units=8;
        if(e.difficulty==0 || e.level<7) extra.owner=0;
        else {e.towers.get(5).owner=1;e.towers.get(5).units=12;}
    }
    private static void advancedLayout(Engine e) {
        double[][] points={{.18,.84},{.82,.16},{.18,.58},{.82,.42},
            {.18,.16},{.82,.84},{.5,.25},{.5,.7}};
        boolean mirror=e.level%2==0;
        for(int i=0;i<e.towers.size();i++) {
            Engine.Tower t=e.towers.get(i);
            t.x=mirror?1-points[i][0]:points[i][0];t.y=points[i][1];
        }
        if(e.level%3==0) e.towers.get(2).kind=Engine.RELAY;
    }
}
