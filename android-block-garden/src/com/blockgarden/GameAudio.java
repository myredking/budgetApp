package com.blockgarden;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.media.*;
import android.app.AlertDialog;
import java.io.IOException;
import java.util.*;
public final class GameAudio {
 private final Context context;
 private final SharedPreferences prefs;
 private final AudioManager manager;
 private final AudioFocusRequest focus;
 private final SoundPool pool;
 private final Map<String,Integer> sounds=new HashMap<>();
 private final Set<Integer> loaded=new HashSet<>();
 private MediaPlayer music;
 private boolean foreground=false,focused=false,closed=false,ducked=false;
 public GameAudio(Context context){
  this.context=context;prefs=context.getSharedPreferences("feedback",0);
  manager=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
  AudioAttributes attributes=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
  focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(attributes).setOnAudioFocusChangeListener(change->focusChanged(change)).build();
  pool=new SoundPool.Builder().setMaxStreams(5).setAudioAttributes(attributes).build();
  pool.setOnLoadCompleteListener((soundPool,id,status)->{if(status==0)loaded.add(id);});
  load(attributes);
 }
 private void load(AudioAttributes attributes){
  try{
   music=new MediaPlayer();music.setAudioAttributes(attributes);
   try(AssetFileDescriptor file=context.getAssets().openFd("bgm.wav")){music.setDataSource(file.getFileDescriptor(),file.getStartOffset(),file.getLength());}
   music.setLooping(true);music.setVolume(0.22f,0.22f);music.prepare();
   music.setOnErrorListener((player,what,extra)->{player.release();music=null;return true;});
  }catch(IOException|IllegalStateException exception){if(music!=null)music.release();music=null;}
  for(String name:new String[]{"place","clear","win","fail","invalid"})loadSound(name);
 }
 private void loadSound(String name){
  try(AssetFileDescriptor file=context.getAssets().openFd(name+".wav")){sounds.put(name,pool.load(file,1));}
  catch(IOException exception){android.util.Log.w("BlockGarden","Sound unavailable: "+name);}
 }
 public boolean enabled(String key){return prefs.getBoolean(key,true);}
 public void resume(){foreground=true;ducked=false;if(music!=null)music.setVolume(0.22f,0.22f);update();}
 public void pause(){foreground=false;stopAudio();manager.abandonAudioFocusRequest(focus);focused=false;}
 private void stopAudio(){if(music!=null&&music.isPlaying())music.pause();pool.autoPause();}
 private void update(){
  if(closed||!foreground)return;
  if(!enabled("bgm")&&!enabled("effects")){stopAudio();manager.abandonAudioFocusRequest(focus);focused=false;return;}
  if(!focused)focused=manager.requestAudioFocus(focus)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
  updateMusic();
 }
 private void updateMusic(){
  if(music==null)return;
  if(enabled("bgm")&&focused){if(!music.isPlaying())music.start();}
  else if(music.isPlaying())music.pause();
 }
 private void focusChanged(int change){
  if(closed)return;
  if(change==AudioManager.AUDIOFOCUS_GAIN){ducked=false;focused=true;if(music!=null)music.setVolume(0.22f,0.22f);update();return;}
  if(change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK){ducked=true;pool.autoPause();if(music!=null)music.setVolume(0.06f,0.06f);return;}
  focused=false;stopAudio();
 }
 public void play(String name){
  if(closed||!foreground||!focused||ducked||!enabled("effects"))return;
  Integer id=sounds.get(name);if(id!=null&&loaded.contains(id))pool.play(id,0.8f,0.8f,1,0,1f);
 }
 public void settings(){
  String[] keys={"bgm","effects","haptics"};boolean[] values={enabled(keys[0]),enabled(keys[1]),enabled(keys[2])};
  new AlertDialog.Builder(context).setTitle("소리와 진동").setMultiChoiceItems(new String[]{"배경음악","효과음","진동"},values,(dialog,which,value)->{prefs.edit().putBoolean(keys[which],value).apply();if(!enabled("effects"))pool.autoPause();update();}).setPositiveButton("닫기",null).show();
 }
 public void close(){if(closed)return;pause();closed=true;if(music!=null)music.release();pool.release();}
}
