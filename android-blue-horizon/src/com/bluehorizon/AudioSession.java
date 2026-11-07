package com.bluehorizon;

/** Platform-independent lifecycle and preference policy. */
public final class AudioSession {
    public interface Output {
        boolean startMusic(String scene);
        void pauseMusic();
        void volume(float value);
        void effect(String name);
    }
    private final Output output;
    private boolean active,focused,ducked,bgm,sfx;
    private String scene="sea",running="";
    private int musicLevel=100;
    public AudioSession(Output sink,boolean music,boolean effects) { output=sink;bgm=music;sfx=effects; }
    public boolean playing() { return !running.isEmpty(); }
    public void resume(boolean focusGranted) { active=true;focused=focusGranted;ducked=false;sync(); }
    public void pause() { active=false;focused=false;sync(); }
    public void failed() { output.pauseMusic();running=""; }
    public void focus(boolean granted,boolean duck) { focused=granted;ducked=duck;sync(); }
    public void musicEnabled(boolean enabled) { bgm=enabled;sync(); }
    public void effectsEnabled(boolean enabled) { sfx=enabled; }
    public void musicLevel(int value) { musicLevel=AudioMix.level(value);sync(); }
    public void scene(String next) {
        if(!next.equals("sea") && !next.equals("harbor")) throw new IllegalArgumentException("Unknown audio scene");
        scene=next;sync();
    }
    public void effect(String name) { if(active && focused && sfx) output.effect(name); }
    private void sync() {
        if(!active || !focused || !bgm) { output.pauseMusic();running="";return; }
        if(!running.equals(scene)) running=output.startMusic(scene)?scene:"";
        output.volume(AudioMix.music(musicLevel,ducked));
    }
}
