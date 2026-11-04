package com.bluehorizon;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Handler;
import android.os.Looper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/** Owns native players; every background/focus-loss path stops audible output. */
public final class AudioController implements AudioSession.Output {
    private final Context context;
    private final SharedPreferences prefs;
    private final AudioManager manager;
    private final AudioAttributes attributes;
    private final AudioFocusRequest focusRequest;
    private final AudioSession session;
    private final SoundPool sounds;
    private final HashMap<String,Integer> ids=new HashMap<String,Integer>();
    private final HashSet<Integer> ready=new HashSet<Integer>();
    private final ArrayList<Integer> streams=new ArrayList<Integer>();
    private MediaPlayer music;
    private boolean active;
    public AudioController(Context app,SharedPreferences preferences) {
        context=app;prefs=preferences;manager=(AudioManager)app.getSystemService(Context.AUDIO_SERVICE);
        attributes=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
        session=new AudioSession(this,musicEnabled(),effectsEnabled());
        focusRequest=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(attributes)
            .setAcceptsDelayedFocusGain(true).setWillPauseWhenDucked(false)
            .setOnAudioFocusChangeListener(change->focusChanged(change),new Handler(Looper.getMainLooper())).build();
        sounds=new SoundPool.Builder().setMaxStreams(4).setAudioAttributes(attributes).build();
        sounds.setOnLoadCompleteListener((pool,id,status)->{if(status==0) ready.add(id);});
        loadEffects();
    }
    private void loadEffects() {
        for(String name:new String[]{"click","trade","depart","chapter","error","upgrade"}) {
            try(AssetFileDescriptor file=context.getAssets().openFd(name+".wav")) {
                ids.put(name,sounds.load(file,1));
            } catch(IOException ex) { android.util.Log.w("BlueHorizon","Effect unavailable: "+name,ex); }
        }
    }
    public boolean musicEnabled() { return prefs.getBoolean("music_enabled",true); }
    public boolean effectsEnabled() { return prefs.getBoolean("effects_enabled",true); }
    public void musicEnabled(boolean value) {
        prefs.edit().putBoolean("music_enabled",value).apply();session.musicEnabled(value);refreshFocus();
    }
    public void effectsEnabled(boolean value) {
        prefs.edit().putBoolean("effects_enabled",value).apply();session.effectsEnabled(value);
        if(!value) stopEffects();refreshFocus();
    }
    private boolean acquireFocus() {
        if(!musicEnabled() && !effectsEnabled()) return false;
        return manager.requestAudioFocus(focusRequest)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }
    private void refreshFocus() {
        if(!active) return;
        if(!musicEnabled() && !effectsEnabled()) manager.abandonAudioFocusRequest(focusRequest);
        session.focus(acquireFocus(),false);
    }
    public void resume() { active=true;session.resume(acquireFocus()); }
    public void pause() {
        active=false;session.pause();stopEffects();manager.abandonAudioFocusRequest(focusRequest);
    }
    public void release() { pause();sounds.release();ready.clear(); }
    public void scene(String scene) { session.scene(scene); }
    public void play(String name) { session.effect(name); }
    private void focusChanged(int change) {
        if(change==AudioManager.AUDIOFOCUS_GAIN) { session.focus(true,false);return; }
        if(change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) { session.focus(true,true);return; }
        session.focus(false,false);stopEffects();
    }
    @Override public boolean startMusic(String scene) {
        pauseMusic();MediaPlayer player=new MediaPlayer();
        try(AssetFileDescriptor file=context.getAssets().openFd(scene+".wav")) {
            player.setAudioAttributes(attributes);
            player.setDataSource(file.getFileDescriptor(),file.getStartOffset(),file.getLength());
            player.setLooping(true);player.setVolume(.24f,.24f);player.prepare();
            player.setOnErrorListener((failed,what,extra)->{if(music==failed) session.failed();return true;});
            music=player;player.start();return true;
        } catch(IOException | RuntimeException ex) {
            player.release();music=null;android.util.Log.w("BlueHorizon","Music unavailable",ex);return false;
        }
    }
    @Override public void pauseMusic() {
        if(music!=null) { music.release();music=null; }
    }
    @Override public void volume(float value) { if(music!=null) music.setVolume(value,value); }
    @Override public void effect(String name) {
        Integer id=ids.get(name);if(id==null || !ready.contains(id)) return;
        int stream=sounds.play(id,.45f,.45f,1,0,1);
        if(stream!=0) streams.add(stream);
        while(streams.size()>8) sounds.stop(streams.remove(0));
    }
    private void stopEffects() { for(int stream:streams) sounds.stop(stream);streams.clear(); }
}
