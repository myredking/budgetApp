package com.towerfront;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import java.util.Arrays;

/** Short, bounded polyphonic effects; never resume stale sounds after a pause. */
public final class GameSound {
    public static final int ACTION=0,ERROR=1,HIT=2,CAPTURE=3,REINFORCE=4,WIN=5,LOSS=6;
    public static final int SKILL_REINFORCE=7,SKILL_SHIELD=8,SKILL_HASTE=9;
    private static final String[] NAMES={"action","error","hit","capture","reinforce","win","loss","skill_reinforce","skill_shield","skill_haste"};
    private final SoundPool pool;
    private final int[] samples=new int[NAMES.length],streams=new int[6];
    private final boolean[] ready=new boolean[NAMES.length];
    private int next;
    private boolean closed;
    public GameSound(Context context) {
        AudioAttributes attributes=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
        pool=new SoundPool.Builder().setMaxStreams(6).setAudioAttributes(attributes).build();
        pool.setOnLoadCompleteListener((sound,id,status)->loaded(id,status));
        try {load(context);} catch(RuntimeException ex) {close();}
    }
    private void load(Context context) {
        for(int i=0;i<NAMES.length;i++) {
            int resource=context.getResources().getIdentifier("sfx_"+NAMES[i],"raw",context.getPackageName());
            if(resource!=0) samples[i]=pool.load(context,resource,1);
        }
    }
    private void loaded(int id,int status) {
        if(closed) return;
        for(int i=0;i<samples.length;i++) if(samples[i]==id) ready[i]=status==0;
    }
    public void play(int kind) {
        if(closed || kind<0 || kind>=samples.length || !ready[kind]) return;
        int stream=pool.play(samples[kind],.45f,.45f,1,0,1);
        if(stream!=0) {streams[next]=stream;next=(next+1)%streams.length;}
    }
    public void battle(int kind) {
        if(kind==Engine.CAPTURE) play(CAPTURE);
        else play(kind==Engine.REINFORCE?REINFORCE:HIT);
    }
    public void stop() {
        if(closed) return;
        pool.autoPause();
        for(int stream:streams) if(stream!=0) pool.stop(stream);
        Arrays.fill(streams,0);
    }
    public void close() {
        if(closed) return;
        stop();closed=true;pool.release();
    }
}
