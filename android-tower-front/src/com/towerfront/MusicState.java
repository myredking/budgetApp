package com.towerfront;

/** Playback is allowed only while visible, enabled and holding audio focus. */
public final class MusicState {
    private boolean enabled,foreground,focused,ducked;
    public MusicState(boolean initial) {enabled=initial;}
    public void enabled(boolean value) {
        enabled=value;if(!value) {focused=false;ducked=false;}
    }
    public void foreground(boolean value) {
        foreground=value;if(!value) {focused=false;ducked=false;}
    }
    public boolean wantsFocus() {return enabled && foreground;}
    public void focus(int change) {
        focused=wantsFocus() && (change==1 || change==-3);ducked=change==-3;
    }
    public boolean playing() {return wantsFocus() && focused;}
    public float volume() {return ducked?.045f:.16f;}
}
