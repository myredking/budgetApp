package com.blockgarden;
public final class TouchControl {
 public boolean active=false,dragging=false;
 private boolean fromHand=false;
 private float startX,startY;
 public void begin(float x,float y,boolean hand){active=true;dragging=false;fromHand=hand;startX=x;startY=y;}
 public void move(float x,float y,float slop){
  if(!active||!fromHand)return;
  float dx=x-startX,dy=y-startY;if(dx*dx+dy*dy>slop*slop)dragging=true;
 }
 public void cancel(){active=false;dragging=false;fromHand=false;}
 public int[] anchor(float x,float y,float left,float top,float unit,int height,int width){
  if(!active||unit<=0)return null;
  float targetY=dragging?y-unit*1.5f:y;
  if(x<left||x>=left+unit*8)return null;
  if(targetY<top||targetY>=top+unit*8)return null;
  int row=(int)((targetY-top)/unit),col=(int)((x-left)/unit);
  if(dragging){row=Math.max(0,Math.min(8-height,(int)Math.floor((targetY-top)/unit-(height-1)/2f)));col=Math.max(0,Math.min(8-width,(int)Math.floor((x-left)/unit-(width-1)/2f)));}
  return new int[]{row,col};
 }
}
