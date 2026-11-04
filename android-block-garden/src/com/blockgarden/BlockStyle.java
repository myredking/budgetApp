package com.blockgarden;
import android.graphics.*;
import android.util.SparseArray;
public final class BlockStyle {
 private final SparseArray<SparseArray<Bitmap>> sprites=new SparseArray<>();
 private final Paint paint=new Paint(3);
 public void clear(){sprites.clear();}
 public void draw(Canvas canvas,float x,float y,float size,int color,boolean empty){
  int pixels=Math.max(1,Math.round(size));SparseArray<Bitmap> sizes=sprites.get(color);
  if(sizes==null){sizes=new SparseArray<>();sprites.put(color,sizes);}
  Bitmap sprite=sizes.get(pixels);
  if(sprite==null){sprite=create(pixels,color,empty);sizes.put(pixels,sprite);}
  canvas.drawBitmap(sprite,x,y,null);
 }
 private Bitmap create(int size,int color,boolean empty){
  Bitmap image=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(image);
  float gap=size*0.055f,round=size*0.19f;
  paint.setShader(null);paint.setColor(empty?0xff1a2940:0xff080f1e);
  c.drawRoundRect(gap,gap+size*0.04f,size-gap,size-gap,round,round,paint);
  if(empty){paint.setColor(0xff22334b);c.drawRoundRect(gap,gap,size-gap,size-gap*1.6f,round,round,paint);return image;}
  paint.setShader(new LinearGradient(0,0,0,size,new int[]{light(color,0.25f),color,light(color,-0.2f)},null,Shader.TileMode.CLAMP));
  c.drawRoundRect(gap,gap,size-gap,size-gap*1.8f,round,round,paint);paint.setShader(null);
  paint.setColor(0x55ffffff);c.drawRoundRect(size*.16f,size*.12f,size*.84f,size*.23f,round/2,round/2,paint);
  paint.setColor(0x18ffffff);c.drawRoundRect(size*.18f,size*.29f,size*.8f,size*.69f,round/2,round/2,paint);
  paint.setColor(0x70ffffff);c.drawCircle(size*.23f,size*.35f,size*.035f,paint);
  return image;
 }
 private int light(int color,float amount){
  int target=amount>0?255:0;float weight=Math.abs(amount);
  return Color.rgb(Math.round(Color.red(color)*(1-weight)+target*weight),Math.round(Color.green(color)*(1-weight)+target*weight),Math.round(Color.blue(color)*(1-weight)+target*weight));
 }
}
