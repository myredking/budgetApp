package com.bluehorizon;

public final class AudioMixTest {
    private static int checks;
    private static void check(boolean ok,String reason) { checks++;if(!ok) throw new AssertionError(reason); }
    public static void main(String[] args) {
        check(AudioMix.level(-20)==0 && AudioMix.level(150)==100,"volume bounds");
        check(AudioMix.music(0,false)==0 && AudioMix.effect(0)==0,"zero is silent");
        check(Math.abs(AudioMix.music(100,false)-.24f)<.001,"music maximum is safe baseline");
        check(AudioMix.music(50,true)<AudioMix.music(50,false),"ducking preserves chosen level");
        check(Math.abs(AudioMix.effect(50)-.225f)<.001,"effects independently scaled");
        check(GameFeedback.battle(true,2,false,false).equals("retreat"),"retreat never fires cannon");
        check(GameFeedback.battle(true,1,true,false).equals("defend"),"defensive turn has separate cue");
        check(GameFeedback.battle(true,0,false,true).equals("victory"),"winning shot celebrates result");
        check(GameFeedback.battle(true,0,false,false).equals("defeat"),"forced withdrawal uses defeat cue");
        check(GameFeedback.battle(false,0,true,false).equals("error"),"failed action has no victory cue");
        check(GameFeedback.battle(true,0,true,false).equals("cannon"),"ordinary attack fires cannon");
        System.out.println("PASS: "+checks+" audio mix and outcome checks");
    }
}
