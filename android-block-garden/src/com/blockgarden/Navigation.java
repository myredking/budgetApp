package com.blockgarden;
public final class Navigation {
 private int screen=-1;
 public boolean isMenu(){return screen==-1;}
 public boolean isMission(){return screen==1;}
 public void choose(boolean mission){screen=mission?1:0;}
 public void home(){screen=-1;}
}
