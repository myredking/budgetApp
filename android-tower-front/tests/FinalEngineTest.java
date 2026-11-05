package com.towerfront;

public final class FinalEngineTest {
    private static void check(boolean ok,String label) {
        if(!ok) throw new AssertionError(label);
    }
    public static void main(String[] args) {
        campaignChecks();
        saveChecks();
        recordChecks();
        actionChecks();
        malformedSaveChecks();
        System.out.println("Final campaign, persistence and action checks passed");
    }
    private static void campaignChecks() {
        for(int stage=1;stage<=Campaign.COUNT;stage++) {
            Engine e=new Engine(stage,1);
            check(e.result()==0,"each stage starts active");
            check(!Campaign.name(stage).isEmpty(),"named operation");
            for(Engine.Tower t:e.towers) {
                check(t.units<=99 && t.units>=0,"bounded troops");
                check(t.x>=.1 && t.x<=.9,"map x bounds");
            }
        }
        Engine easy=new Engine(6,0),hard=new Engine(6,2);
        check(easy.towers.get(1).units<hard.towers.get(1).units,"difficulty troops");
        check(new Engine(999,1).level==100,"mission stage clamped");
        check(Campaign.next(100)==100,"finite campaign");
    }
    private static void saveChecks() {
        Engine e=new Engine(5,2);
        e.towers.get(0).units=90;e.upgrade(0);e.send(0,2);e.update(.3);
        String encoded=SaveCodec.encode(e);
        Engine restored=SaveCodec.decode(encoded);
        check(restored!=null,"valid save restores");
        check(encoded.equals(SaveCodec.encode(restored)),"exact state round trip");
        e.update(.4);restored.update(.4);
        check(SaveCodec.encode(e).equals(SaveCodec.encode(restored)),"deterministic continuation");
        check(SaveCodec.decode("invalid")==null,"corrupt save safe");
        check(SaveCodec.decode("")==null,"missing save safe");
        check(SaveCodec.decode(encoded.substring(0,encoded.length()/2))==null,"truncated save safe");
    }
    private static void recordChecks() {
        Profile p=new Profile();
        check(p.unlocked(1)==1,"starts at stage one");
        check(!p.available(2,1),"future stages locked");
        p.record(1,1,2);p.record(1,1,1);
        check(p.stars(1,1)==2,"best score retained");
        check(p.available(2,1) && !p.available(2,2),"difficulty records independent");
        Profile restored=Profile.decode(p.encode());
        check(restored.stars(1,1)==2,"records persist");
        p.record(12,1,3);
        check(p.unlocked(1)<=12,"unlock capped");
        check(Profile.decode("broken").unlocked(1)==1,"corrupt records safe");
    }
    private static void actionChecks() {
        Engine e=new Engine(1);
        String before=SaveCodec.encode(e);
        e.update(-1);e.update(Double.NaN);
        check(before.equals(SaveCodec.encode(e)),"invalid dt ignored");
        check(!e.send(0,99),"invalid target blocked");
        for(Engine.Tower t:e.towers) t.owner=1;
        check(!e.send(0,2) && !e.upgrade(0),"finished battle immutable");
        check(e.medals()>=1 && e.medals()<=3,"victory medals bounded");
    }
    private static void malformedSaveChecks() {
        Engine e=new Engine(1);e.send(0,2);
        byte[] raw=java.util.Base64.getDecoder().decode(SaveCodec.encode(e));
        byte[] bad=raw.clone();
        java.nio.ByteBuffer.wrap(bad).putDouble(40,Double.NaN);
        check(rejected(bad),"NaN coordinate rejected");
        bad=raw.clone();java.nio.ByteBuffer.wrap(bad).putInt(68,100);
        check(rejected(bad),"out of range troops rejected");
        bad=raw.clone();java.nio.ByteBuffer.wrap(bad).putInt(36,7);
        check(rejected(bad),"wrong tower count rejected");
        bad=raw.clone();java.nio.ByteBuffer.wrap(bad).putInt(44+40*e.towers.size(),99);
        check(rejected(bad),"invalid fleet index rejected");
        check(rejected(java.util.Arrays.copyOf(raw,raw.length+1)),"trailing bytes rejected");
    }
    private static boolean rejected(byte[] raw) {
        return SaveCodec.decode(java.util.Base64.getEncoder().encodeToString(raw))==null;
    }
}
