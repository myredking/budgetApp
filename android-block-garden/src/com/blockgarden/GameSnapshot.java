package com.blockgarden;
public final class GameSnapshot {
 private final Engine state;
 private final int[] hand,tones;
 public GameSnapshot(Engine engine,int[] hand,int[] tones){state=copy(engine);this.hand=hand.clone();this.tones=tones.clone();}
 public Engine engine(){return copy(state);}
 public int[] hand(){return hand.clone();}
 public int[] tones(){return tones.clone();}
 private Engine copy(Engine source){
  Engine target=new Engine();target.combo=source.combo;target.score=source.score;target.moves=source.moves;target.lines=source.lines;
  for(int r=0;r<8;r++){System.arraycopy(source.board[r],0,target.board[r],0,8);System.arraycopy(source.stars[r],0,target.stars[r],0,8);}return target;
 }
}
