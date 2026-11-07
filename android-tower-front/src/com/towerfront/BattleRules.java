package com.towerfront;
import java.io.*;

public final class BattleRules {
    public static final int CONQUEST=0,HOLD=1,SURVIVE=2,CONTROL=3;
    public static final int REINFORCE=0,SHIELD=1,HASTE=2;
    private static final int[] COSTS={35,30,25},COOLDOWNS={18,20,16};
    private static final String[] SKILL_NAMES={"긴급 증원","보호막","이동 가속"};
    private final Engine engine;
    public int objective=CONQUEST,supplyIndex=-1;
    public double progress,goalSeconds=30,energy=60;
    public final double[] cooldowns=new double[3],shields,haste;
    private boolean wasEligible,interrupted;
    BattleRules(Engine e) {
        engine=e;shields=new double[e.towers.size()];haste=new double[e.towers.size()];
    }
    public static BattleRules start(Engine e) {
        BattleRules rules=new BattleRules(e);rules.designateSupply();
        rules.objective=e.skirmish?CONQUEST:objectiveFor(e.level);
        rules.goalSeconds=durationFor(e.level);
        return rules;
    }
    public static int objectiveFor(int stage) {
        if(stage<4 || stage==100) return CONQUEST;
        return new int[]{HOLD,SURVIVE,CONTROL,CONQUEST}[stage%4];
    }
    public static int durationFor(int stage) {
        int objective=objectiveFor(stage);
        if(objective==SURVIVE) return 60+(stage/20)*10;
        return objective==CONTROL?12:30;
    }
    public static String missionLabel(int stage) {
        int type=objectiveFor(stage),seconds=durationFor(stage);
        if(type==HOLD) return "보급 "+seconds+"초 사수";
        if(type==SURVIVE) return seconds+"초 생존";
        return type==CONTROL?"3거점 12초 유지":"적 진영 전멸";
    }
    public static String briefing(int stage) {
        int type=objectiveFor(stage);
        if(type==HOLD) return "중앙 보급 거점을 30초 연속 지키세요. 빼앗기면 타이머가 초기화됩니다.";
        if(type==SURVIVE) return durationFor(stage)+"초 동안 생존하세요. 종료 시 3거점 이상은 별3개, 2거점은 별2개입니다.";
        if(type==CONTROL) return "아군 거점 3개 이상을 12초 동안 연속 유지하세요.";
        return "모든 적 거점과 이동 병력을 제거하세요.";
    }
    private void designateSupply() {
        double closest=2;int neutral=-1;
        for(int i=0;i<engine.towers.size();i++) {
            Engine.Tower t=engine.towers.get(i);double distance=Math.hypot(t.x-.5,t.y-.5);
            if(distance<closest) {closest=distance;supplyIndex=i;}
            if(t.owner==0) neutral=i;
        }
        if(neutral>=0 && engine.towers.get(supplyIndex).owner!=0) swapRoles(supplyIndex,neutral);
    }
    private void swapRoles(int first,int second) {
        Engine.Tower a=engine.towers.get(first),b=engine.towers.get(second);
        int owner=a.owner,units=a.units,kind=a.kind,rank=a.rank;double growth=a.growth;
        a.owner=b.owner;a.units=b.units;a.kind=b.kind;a.rank=b.rank;a.growth=b.growth;
        b.owner=owner;b.units=units;b.kind=kind;b.rank=rank;b.growth=growth;
    }
    public static int cost(int type) {return COSTS[type];}
    public static int cooldown(int type) {return COOLDOWNS[type];}
    public static String skillName(int type) {return SKILL_NAMES[type];}
    public boolean ready(int type,int target) {
        if(type<0 || type>2 || target<0 || target>=engine.towers.size()) return false;
        if(engine.towers.get(target).owner!=1 || engine.result()!=0) return false;
        if(cooldowns[type]>0 || energy<COSTS[type]) return false;
        return canApply(type,target);
    }
    private boolean canApply(int type,int target) {
        if(type==REINFORCE) return engine.towers.get(target).units<99;
        return type==SHIELD?shields[target]<=0:haste[target]<=0;
    }
    boolean cast(int type,int target) {
        if(!ready(type,target)) return false;
        energy-=COSTS[type];cooldowns[type]=COOLDOWNS[type];
        if(type==REINFORCE) engine.towers.get(target).units=Math.min(99,engine.towers.get(target).units+20);
        if(type==SHIELD) shields[target]=6;
        if(type==HASTE) haste[target]=6;
        return true;
    }
    void beginStep(double dt) {
        wasEligible=eligible();interrupted=false;
        energy=Math.min(100,energy+dt*(supplyOwner()==1?5:3));
        for(int i=0;i<3;i++) cooldowns[i]=Math.max(0,cooldowns[i]-dt);
        for(int i=0;i<shields.length;i++) {
            shields[i]=Math.max(0,shields[i]-dt);haste[i]=Math.max(0,haste[i]-dt);
        }
    }
    void finishStep(double dt) {
        if(objective==CONQUEST) return;
        if(objective==SURVIVE) {
            setProgress(engine.elapsed);
            if(progress>=goalSeconds) engine.elapsed=Math.max(engine.elapsed,goalSeconds);
            return;
        }
        if(!eligible() || interrupted) {progress=0;return;}
        if(wasEligible) setProgress(progress+dt);
    }
    private void setProgress(double value) {
        progress=Math.min(goalSeconds,value);
        if(goalSeconds-progress<1e-8) progress=goalSeconds;
    }
    private boolean eligible() {
        if(objective==HOLD) return supplyOwner()==1;
        if(objective==CONTROL) return ownedCount()>=3;
        return true;
    }
    public int ownedCount() {
        int count=0;for(Engine.Tower t:engine.towers) if(t.owner==1) count++;
        return count;
    }
    public int supplyOwner() {return supplyIndex<0?0:engine.towers.get(supplyIndex).owner;}
    public double productionBonus(int owner) {return owner>0 && owner==supplyOwner()?1.35:1;}
    public double fleetSpeed(Engine.Fleet f) {return f.owner==1 && haste[f.from]>0?1.75:1;}
    public int damage(Engine.Fleet f,Engine.Tower target) {
        double factor=target.kind==Engine.FORTRESS?.75:1;
        if(shields[f.to]>0 && target.owner==1) factor*=.5;
        return Math.max(1,(int)Math.ceil(f.units*factor));
    }
    void captured(int target) {
        shields[target]=0;haste[target]=0;
        if(objective==HOLD && target==supplyIndex) interrupted=true;
        if(objective==CONTROL && ownedCount()<3) interrupted=true;
    }
    public boolean won() {return objective!=CONQUEST && progress>=goalSeconds;}
    public String objectiveText() {
        String timer=(int)progress+" / "+(int)goalSeconds+"초";
        if(objective==HOLD) return "보급 사수 · "+timer;
        if(objective==SURVIVE) return "생존 · "+timer;
        if(objective==CONTROL) return "거점 "+ownedCount()+"/3개 유지 · "+timer;
        String team=supplyOwner()==0?"중립":supplyOwner()==1?"아군":"AI "+(supplyOwner()-1);
        return "적 전멸 · 보급: "+(supplyIndex<0?"없음":team);
    }
    void write(DataOutputStream out) throws IOException {
        out.writeInt(objective);out.writeInt(supplyIndex);out.writeDouble(goalSeconds);
        out.writeDouble(progress);out.writeDouble(energy);
        for(double timer:cooldowns) out.writeDouble(timer);
        for(int i=0;i<shields.length;i++) {out.writeDouble(shields[i]);out.writeDouble(haste[i]);}
    }
    void read(DataInputStream in) throws IOException {
        objective=integer(in,0,3);supplyIndex=integer(in,-1,shields.length-1);
        goalSeconds=number(in,1,600);progress=number(in,0,goalSeconds);energy=number(in,0,100);
        for(int i=0;i<3;i++) cooldowns[i]=number(in,0,COOLDOWNS[i]);
        for(int i=0;i<shields.length;i++) {
            shields[i]=number(in,0,6);haste[i]=number(in,0,6);
            if(engine.towers.get(i).owner!=1 && shields[i]+haste[i]>0) throw new IOException("Foreign buff");
        }
        if(engine.skirmish && objective!=CONQUEST) throw new IOException("Invalid free objective");
        if(objective==HOLD && supplyIndex<0) throw new IOException("Missing hold target");
    }
    private static int integer(DataInputStream in,int min,int max) throws IOException {
        int value=in.readInt();if(value<min || value>max) throw new IOException("Invalid rules");return value;
    }
    private static double number(DataInputStream in,double min,double max) throws IOException {
        double value=in.readDouble();
        if(!Double.isFinite(value) || value<min || value>max) throw new IOException("Invalid timer");return value;
    }
}
