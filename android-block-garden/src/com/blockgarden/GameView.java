package com.blockgarden;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.view.*;
import java.util.Random;
public class GameView extends View {
 private final Paint paint=new Paint(3);
 private final SharedPreferences prefs;
 private Engine engine=new Engine();
 private final Random random=new Random();
 private final int[] colors={0xff26364d,0xff64d9bc,0xffffbd69,0xff9e9bff,0xffff8198,0xff6abaff};
 private final int[][][] shapes={{{0,0}},{{0,0},{0,1}},{{0,0},{1,0}},{{0,0},{0,1},{0,2}},{{0,0},{1,0},{2,0}},{{0,0},{0,1},{1,0},{1,1}},{{0,0},{1,0},{1,1}},{{0,1},{1,0},{1,1},{1,2}},{{0,0},{1,0},{2,0},{2,1}},{{0,0},{0,1},{0,2},{0,3}},{{0,0},{0,1},{1,1},{1,2}},{{0,0},{0,1},{0,2},{1,0},{1,1},{1,2},{2,0},{2,1},{2,2}}};
 private int[] hand=new int[3],tones=new int[3];
 private int selected=-1,best,hoverR=-1,hoverC=-1;
 private float unit,left,top,scale,tray;
 private boolean over=false,drag=false;
 private String message="블록을 골라 보드에 놓아보세요";
 public GameView(Context context){super(context);prefs=context.getSharedPreferences("game",0);best=prefs.getInt("best",0);deal();restore();setContentDescription("블록 가든 게임 보드");}
 private void deal(){for(int i=0;i<3;i++){hand[i]=random.nextInt(shapes.length);tones[i]=1+random.nextInt(5);}}
 private void text(Canvas c,String s,float x,float y,float size,int color){paint.setColor(color);paint.setTextSize(size*scale);paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));c.drawText(s,x,y,paint);}
 @Override protected void onDraw(Canvas c){
  scale=Math.min(getWidth()/400f,getHeight()/850f);unit=43*scale;left=(getWidth()-unit*8)/2;top=190*scale;tray=top+unit*8+70*scale;
  c.drawColor(0xff101c30);text(c,"BLOCK GARDEN",left,44*scale,18,0xff64d9bc);
  text(c,"블록 가든",left,87*scale,32,Color.WHITE);text(c,"광고 없이, 나만의 퍼즐 시간",left,116*scale,12,0xff9baac0);
  text(c,"점수  "+engine.score,left,161*scale,22,Color.WHITE);text(c,"최고  "+best,240*scale,161*scale,15,0xffffbd69);
  drawBoard(c);text(c,message,left,top+unit*8+35*scale,12,0xffb6c5d9);drawHand(c);
  text(c,"블록 선택 → 보드 터치 · 끌어서 놓기도 가능",left,tray+120*scale,11,0xff9baac0);
  text(c,"새 게임 ↻",left,tray+168*scale,17,0xff64d9bc);
  if(over)text(c,"놓을 공간이 없어요. 다시 도전해요!",left,tray+204*scale,16,0xffffbd69);
 }
 private void tile(Canvas c,float x,float y,float size,int color){paint.setColor(color);c.drawRoundRect(x+2*scale,y+2*scale,x+size-2*scale,y+size-2*scale,6*scale,6*scale,paint);paint.setColor(0x22ffffff);c.drawRoundRect(x+5*scale,y+4*scale,x+size-5*scale,y+8*scale,2,2,paint);}
 private void drawBoard(Canvas c){
  for(int r=0;r<8;r++)for(int col=0;col<8;col++)tile(c,left+col*unit,top+r*unit,unit,colors[engine.board[r][col]]);
  if(selected<0||hoverR<0)return;
  int[][] shape=shapes[hand[selected]];boolean valid=engine.fits(shape,hoverR,hoverC);
  for(int[] p:shape){int r=hoverR+p[0],col=hoverC+p[1];if(inBoard(r,col))tile(c,left+col*unit,top+r*unit,unit,valid?0xffb4ffe8:0xffa45369);}
 }
 private void drawHand(Canvas c){
  for(int i=0;i<3;i++){if(hand[i]<0)continue;float x=left+i*118*scale;
   if(i==selected){paint.setColor(0xff31465f);c.drawRoundRect(x-5*scale,tray-12*scale,x+108*scale,tray+90*scale,12,12,paint);}
   for(int[] p:shapes[hand[i]])tile(c,x+p[1]*23*scale,tray+p[0]*23*scale,23*scale,colors[tones[i]]);
  }
 }
 private boolean inBoard(int r,int c){return r>=0&&c>=0&&r<8&&c<8;}
 private void selectHand(float x){int slot=(int)((x-left)/(118*scale));if(x<left||slot<0||slot>=3)return;if(hand[slot]<0)return;selected=slot;drag=true;invalidate();}
 private boolean touchDown(float x,float y){
  if(y>tray+140*scale&&y<tray+190*scale){confirmReset();return true;}
  if(over)return true;
  if(y>=tray-20*scale&&y<tray+95*scale){selectHand(x);return true;}
  preview(x,y);return true;
 }
 @Override public boolean onTouchEvent(android.view.MotionEvent event){
  float x=event.getX(),y=event.getY();int action=event.getActionMasked();
  if(action==MotionEvent.ACTION_DOWN)return touchDown(x,y);
  if(action==MotionEvent.ACTION_MOVE){preview(x,y);return true;}
  if(action==MotionEvent.ACTION_UP){preview(x,y);place();drag=false;performClick();return true;}
  if(action==MotionEvent.ACTION_CANCEL){hoverR=-1;drag=false;invalidate();}return true;
 }
 @Override public boolean performClick(){super.performClick();return true;}
 private void preview(float x,float y){hoverR=-1;hoverC=-1;if(y>=top&&y<top+8*unit&&x>=left&&x<left+8*unit){hoverR=(int)((y-top)/unit);hoverC=(int)((x-left)/unit);}invalidate();}
 private void place(){
  if(selected<0||hoverR<0||over)return;
  if(!engine.place(shapes[hand[selected]],hoverR,hoverC,tones[selected])){message="여기는 놓을 수 없어요";invalidate();return;}
  hand[selected]=-1;selected=-1;hoverR=-1;best=Math.max(best,engine.score);
  if(hand[0]<0&&hand[1]<0&&hand[2]<0)deal();
  over=!hasMove();message=over?"게임 종료 · 새 게임으로 다시 시작":"좋아요! 가로 또는 세로를 채워보세요";save();invalidate();
 }
 private boolean hasMove(){for(int id:hand)if(id>=0&&engine.canFit(shapes[id]))return true;return false;}
 private void confirmReset(){new AlertDialog.Builder(getContext()).setTitle("새 게임을 시작할까요?").setMessage("최고 기록은 유지됩니다.").setNegativeButton("계속하기",null).setPositiveButton("새 게임",(dialog,which)->reset()).show();}
 private void reset(){engine=new Engine();selected=-1;hoverR=-1;over=false;message="블록을 골라 보드에 놓아보세요";deal();save();invalidate();}
 private void save(){
  StringBuilder data=new StringBuilder();for(int[] row:engine.board)for(int value:row)data.append(value);
  SharedPreferences.Editor edit=prefs.edit().putString("board",data.toString()).putInt("score",engine.score).putInt("best",best);
  for(int i=0;i<3;i++)edit.putInt("hand"+i,hand[i]).putInt("tone"+i,tones[i]);edit.apply();
 }
 private void restore(){
  String data=prefs.getString("board","");if(data.length()!=64)return;
  for(int r=0;r<8;r++)for(int c=0;c<8;c++)engine.board[r][c]=data.charAt(r*8+c)-'0';
  engine.score=prefs.getInt("score",0);for(int i=0;i<3;i++){hand[i]=prefs.getInt("hand"+i,0);tones[i]=prefs.getInt("tone"+i,1);}over=!hasMove();
 }
}

