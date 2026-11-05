package com.blockgarden;
public final class GameLayout {
 public final float scale,unit,left,top,tray;
 public GameLayout(int width,int height){
  scale=Math.min(width/400f,height/740f);unit=Math.min(46*scale,(height-420*scale)/8);
  left=(width-unit*8)/2;top=138*scale;tray=top+unit*8+38*scale;
 }
}
