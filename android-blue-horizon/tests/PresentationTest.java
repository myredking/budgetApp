package com.bluehorizon;

public final class PresentationTest {
    private static int checks;
    private static void check(boolean condition,String label) {
        checks++;if(!condition) throw new AssertionError(label);
    }
    private static final class FakeAudio implements AudioSession.Output {
        int starts,stops,effects;String scene="";float volume;boolean fail;
        public boolean startMusic(String next) { starts++;scene=next;return !fail; }
        public void pauseMusic() { stops++; }
        public void volume(float value) { volume=value; }
        public void effect(String name) { effects++; }
    }
    private static void audioLifecycle() {
        FakeAudio out=new FakeAudio();AudioSession audio=new AudioSession(out,true,true);
        audio.scene("sea");audio.effect("click");
        check(out.starts==0 && out.effects==0,"no playback before visible resume");
        audio.resume(true);check(out.starts==1 && out.scene.equals("sea"),"resume selected scene");
        audio.scene("sea");check(out.starts==1,"same scene does not restart loop");
        audio.scene("harbor");check(out.starts==2 && out.scene.equals("harbor"),"scene switch");
        audio.focus(false,false);int effects=out.effects;audio.effect("trade");
        check(!audio.playing() && out.effects==effects,"focus loss stops music and effects");
        audio.focus(true,false);check(audio.playing(),"focus regained while active");
        float normal=out.volume;audio.focus(true,true);check(out.volume<normal,"ducking lowers music");
        audio.pause();audio.focus(true,false);
        check(!audio.playing(),"late focus gain after background does not restart");
        audio.resume(false);check(!audio.playing(),"focus denied stays silent");
        audio.focus(true,false);check(audio.playing(),"delayed focus gain can start");
    }
    private static void audioSettingsAndFailures() {
        FakeAudio out=new FakeAudio();AudioSession audio=new AudioSession(out,false,true);
        audio.resume(true);check(!audio.playing(),"music preference honored on launch");
        audio.effect("click");check(out.effects==1,"effects work with music muted");
        audio.effectsEnabled(false);audio.effect("click");check(out.effects==1,"effects mute independent");
        audio.musicEnabled(true);check(audio.playing(),"unmute music live");
        audio.musicEnabled(false);check(!audio.playing(),"mute music immediately");
        audio.pause();audio.musicEnabled(true);check(!audio.playing(),"setting never starts in background");
        out.fail=true;audio.resume(true);check(!audio.playing(),"failed player is not treated as running");
        out.fail=false;audio.scene("sea");check(audio.playing(),"audio can recover on next scene");
        audio.failed();check(!audio.playing(),"asynchronous native error invalidates playback state");
        audio.scene("sea");check(audio.playing(),"same scene retries after native player error");
    }
    private static void crew() {
        check(Crew.NAMES.length==6,"six cast portraits");
        check(Crew.available(0,0) && Crew.available(1,0),"captain and Ian initially aboard");
        check(!Crew.available(2,1) && Crew.available(2,2),"Mira joins with northern archive");
        check(Crew.available(5,5) && !Crew.available(5,4),"Ayu joins at Malacca chapter");
        for(int i=0;i<6;i++) {
            int[] rect=Crew.cell(i,1536,1024);
            check(rect[2]-rect[0]==512 && rect[3]-rect[1]==512,"equal square atlas regions");
            check(rect[0]>=0 && rect[2]<=1536 && rect[1]>=0 && rect[3]<=1024,"portrait crop in bounds");
        }
        check(Crew.guideRegion(0)==0 && Crew.guideRegion(5)==5,"goal shortcut follows story geography");
        check(Crew.guideRegion(8)==-1,"free play opens world view");
    }
    private static void tradeSuggestions() {
        Engine e=new Engine();TradeHint hint=TradeHint.best(e,0,5);
        check(hint!=null && hint.port!=e.port,"recommend another harbor");
        int sale=e.sellPriceAt(hint.port,0);
        check(hint.net==(sale-e.buyPrice(0))*5-e.travelCost(hint.port,0),"quote includes one-way provisions");
        check(hint.cost==e.buyPrice(0)*5+e.travelCost(hint.port,0),"quoted required gold");
        for(int port=0;port<World.COUNT;port++) {
            if(port==e.port) continue;
            int price=e.sellPriceAt(port,0);
            check(hint.net>=(price-e.buyPrice(0))*5-e.travelCost(port,0),"suggest highest net route");
        }
        check(TradeHint.best(e,0,0)==null && TradeHint.best(e,-1,5)==null,"reject invalid hint request");
    }
    public static void main(String[] args) {
        audioLifecycle();audioSettingsAndFailures();crew();tradeSuggestions();
        System.out.println("PASS: "+checks+" audio/crew/trade-hint checks");
    }
}
