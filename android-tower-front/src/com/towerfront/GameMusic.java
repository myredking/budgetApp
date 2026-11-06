package com.towerfront;

import android.content.Context;
import android.media.*;

/** Activity-owned music session; never plays as a background service. */
public final class GameMusic {
    private final MusicState state;
    private final AudioManager manager;
    private final AudioFocusRequest request;
    private MediaPlayer player;
    private boolean requested;
    public GameMusic(Context context,boolean enabled) {
        state=new MusicState(enabled);manager=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        AudioAttributes attributes=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
        request=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener(this::focusChanged).build();
        int resource=context.getResources().getIdentifier("island_theme","raw",context.getPackageName());
        try {
            if(resource!=0) player=MediaPlayer.create(context,resource,attributes,0);
            if(player!=null) {player.setLooping(true);player.setOnErrorListener((media,what,extra)->{releasePlayer();return true;});}
        } catch(RuntimeException ex) {releasePlayer();}
    }
    public void enabled(boolean value) {state.enabled(value);refresh();}
    public void foreground(boolean value) {state.foreground(value);refresh();}
    private void refresh() {
        if(player==null || manager==null) return;
        if(state.wantsFocus() && !requested) {
            int result=manager.requestAudioFocus(request);requested=result==AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
            state.focus(requested?AudioManager.AUDIOFOCUS_GAIN:AudioManager.AUDIOFOCUS_LOSS);
        } else if(!state.wantsFocus() && requested) {
            requested=false;manager.abandonAudioFocusRequest(request);state.focus(AudioManager.AUDIOFOCUS_LOSS);
        }
        sync();
    }
    private void focusChanged(int change) {
        if(!requested) {state.focus(AudioManager.AUDIOFOCUS_LOSS);sync();return;}
        if(change==AudioManager.AUDIOFOCUS_LOSS) requested=false;
        state.focus(change);sync();
    }
    private void sync() {
        if(player==null) return;
        try {
            player.setVolume(state.volume(),state.volume());
            if(state.playing()) {if(!player.isPlaying()) player.start();}
            else if(player.isPlaying()) player.pause();
        } catch(IllegalStateException ex) {releasePlayer();}
    }
    private void releasePlayer() {
        state.foreground(false);
        if(requested && manager!=null) manager.abandonAudioFocusRequest(request);
        requested=false;if(player!=null) {player.release();player=null;}
    }
    public void close() {releasePlayer();}
}
