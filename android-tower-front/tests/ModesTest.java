package com.towerfront;
import java.util.HashSet;

public final class ModesTest {
    private static void check(boolean ok,String text) {
        if(!ok) throw new AssertionError(text);
    }
    public static void main(String[] args) {
        customChecks();missionChecks();migrationChecks();hostileChecks();invalidSettingsChecks();
        System.out.println("100 missions, ten enemy factions and legacy migration checks passed");
    }
    private static void customChecks() {
        for(int map=0;map<Skirmish.MAPS;map++) for(int count=1;count<=10;count++) {
            Engine e=Skirmish.create(map,count,1,1234);
            HashSet<Integer> teams=new HashSet<>();
            for(Engine.Tower t:e.towers) if(t.owner>0) teams.add(t.owner);
            check(teams.size()==count+1,"requested factions spawn");
            String save=SaveCodec.encode(e);
            check(save.equals(SaveCodec.encode(Skirmish.create(map,count,1,1234))),"seed reproducibility");
            Engine restored=SaveCodec.decode(save);
            check(restored!=null && restored.skirmish && restored.enemyCount==count,"custom save restores");
            check(save.equals(SaveCodec.encode(restored)),"exact custom state restore");
        }
        Engine e=Skirmish.create(0,10,1,7);e.update(3);
        HashSet<Integer> armies=new HashSet<>();
        for(Engine.Fleet f:e.fleets) armies.add(f.owner);
        check(armies.size()==10,"all ten AI factions issue orders");
        HashSet<Integer> colors=new HashSet<>();
        for(int owner=1;owner<=11;owner++) colors.add(SceneStyle.team(owner));
        check(colors.size()==11,"distinct faction colors");
        for(Engine.Tower t:e.towers) t.owner=1;
        e.fleets.clear();e.fleets.add(new Engine.Fleet(0,1,11,5,1));
        check(e.result()==0,"last enemy fleet still prevents victory");
        check(e.medals()==0,"free play awards no mission stars");
    }
    private static void missionChecks() {
        check(Campaign.COUNT==100,"one hundred missions");
        HashSet<String> layouts=new HashSet<>();
        for(int stage=1;stage<=100;stage++) for(int mode=0;mode<3;mode++) {
            Engine e=new Engine(stage,mode);
            check(!e.skirmish && e.result()==0,"mission starts active");
            check(SaveCodec.decode(SaveCodec.encode(e))!=null,"all missions save and restore");
            for(Engine.Tower t:e.towers) check(t.units>=0 && t.units<=99,"bounded mission units");
            if(mode==1) layouts.add(configuration(e));
        }
        check(layouts.size()==100,"distinct mission configurations");
        Profile p=new Profile();
        for(int stage=1;stage<=100;stage++) p.record(stage,1,3);
        check(p.unlocked(1)==100 && p.total(1)==300,"all mission progression");
        check(Profile.decode(p.encode()).stars(100,1)==3,"stage one hundred persists");
        p.recordBattle(Skirmish.create(0,1,1,2));check(p.total(1)==300,"free play isolated from missions");
    }
    private static void migrationChecks() {
        String old="TF1:"+"300000000000"+"230000000000"+"100000000000";
        Profile p=Profile.decode(old);
        check(p.stars(1,0)==3 && p.stars(2,1)==3 && p.stars(1,2)==1,"legacy rows migrate");
        check(p.stars(13,1)==0,"new missions start uncompleted");
        Engine legacy=new Engine(1);
        byte[] newer=java.util.Base64.getDecoder().decode(SaveCodec.encode(legacy));
        byte[] oldSave=java.util.Arrays.copyOf(newer,settingsOffset(newer));
        java.nio.ByteBuffer.wrap(oldSave).putInt(0,0x54465733);
        check(SaveCodec.decode(java.util.Base64.getEncoder().encodeToString(oldSave))!=null,"v3 battle save migration");
    }
    private static void hostileChecks() {
        Engine e=Skirmish.create(0,2,1,5);
        for(Engine.Tower t:e.towers) {t.owner=3;t.units=0;}
        e.towers.get(0).owner=1;e.towers.get(0).units=99;
        e.towers.get(1).owner=2;e.towers.get(1).units=40;e.update(3);
        boolean attacked=false;
        for(Engine.Fleet f:e.fleets) if(f.owner==2 && e.towers.get(f.to).owner==3) attacked=true;
        check(attacked,"AI attacks a different AI faction");
        e=Skirmish.create(1,10,1,10);
        check(e.remainingEnemies()==10,"all ten enemies shown alive");
        for(Engine.Tower t:e.towers) t.owner=1;
        e.fleets.clear();check(e.result()==1 && e.medals()==0,"free victory has no mission stars");
        Profile profile=new Profile();profile.recordBattle(e);
        check(profile.total(1)==0,"free victory cannot unlock missions");
    }
    private static void invalidSettingsChecks() {
        byte[] data=java.util.Base64.getDecoder().decode(SaveCodec.encode(Skirmish.create(0,10,1,5)));
        byte[] bad=data.clone();java.nio.ByteBuffer.wrap(bad).putInt(settingsOffset(bad)+9,11);
        check(rejected(bad),"eleven enemy factions rejected");
        bad=data.clone();java.nio.ByteBuffer.wrap(bad).putInt(settingsOffset(bad)+5,9);
        check(rejected(bad),"invalid map rejected");
        bad=data.clone();bad[settingsOffset(bad)+4]=2;
        check(rejected(bad),"invalid mode flag rejected");
    }
    private static String configuration(Engine e) {
        StringBuilder result=new StringBuilder();
        for(Engine.Tower t:e.towers) result.append(t.x).append(',').append(t.y).append(':')
            .append(t.owner).append('/').append(t.units).append('/').append(t.kind).append('/').append(t.rank).append(';');
        return result.toString();
    }
    private static int settingsOffset(byte[] data) {
        java.nio.ByteBuffer b=java.nio.ByteBuffer.wrap(data);
        int fleetsAt=40+40*b.getInt(36);
        return fleetsAt+4+32*b.getInt(fleetsAt);
    }
    private static boolean rejected(byte[] data) {
        return SaveCodec.decode(java.util.Base64.getEncoder().encodeToString(data))==null;
    }
}
