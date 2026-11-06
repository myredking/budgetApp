package com.towerfront;

public final class MusicTest {
    private static void check(boolean ok,String message) {
        if(!ok) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        MusicState state=new MusicState(true);
        check(!state.playing() && !state.wantsFocus(),"background launch stays quiet");
        state.foreground(true);check(state.wantsFocus() && !state.playing(),"request focus before playback");
        state.focus(1);check(state.playing(),"granted focus starts music");
        float normal=state.volume();state.focus(-3);
        check(state.playing() && state.volume()<normal,"duck instead of fighting other audio");
        state.focus(-2);check(!state.playing(),"temporary focus loss pauses");
        state.focus(1);check(state.playing() && state.volume()==normal,"gain restores volume and playback");
        state.foreground(false);state.focus(1);
        check(!state.playing(),"late focus gain cannot play in background");
        state.foreground(true);check(!state.playing(),"resume requires a new grant");
        state.focus(1);state.enabled(false);state.focus(1);
        check(!state.playing() && !state.wantsFocus(),"muted music ignores late gain");
        state.enabled(true);check(!state.playing(),"reenabling waits for focus");
        state.focus(1);check(state.playing(),"reenabling can regain focus");
        state.focus(-1);check(!state.playing(),"permanent focus loss pauses");
        System.out.println("Music focus, ducking, mute and lifecycle checks passed");
    }
}
