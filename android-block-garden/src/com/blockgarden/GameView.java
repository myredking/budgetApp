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
 private boolean over=false;
 private final TouchControl touch=new TouchControl();
 private int activePointer=-1;
 private boolean missionMode=false;
 private Mission mission=new Mission(1);
 private String message="블록을 골라 보드에 놓아보세요";
 public GameView(Context context){super(context);prefs=context.getSharedPreferences("game",0);best=prefs.getInt("best",0);missionMode=prefs.getBoolean("missionMode",false);mission=new Mission(prefs.getInt("stage",1));deal();restore();save();setContentDescription("블록 가든 게임 보드");}
 private void deal(){for(int i=0;i<3;i++){hand[i]=random.nextInt(shapes.length);tones[i]=1+random.nextInt(5);}}
 private void text(Canvas c,String s,float x,float y,float size,int color){paint.setColor(color);paint.setTextSize(size*scale);paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));c.drawText(s,x,y,paint);}
 @Override protected void onDraw(Canvas c){
  scale=Math.min(getWidth()/400f,getHeight()/850f);unit=43*scale;left=(getWidth()-unit*8)/2;top=190*scale;tray=top+unit*8+70*scale;
  c.drawColor(0xff101c30);text(c,"BLOCK GARDEN",left,44*scale,18,0xff64d9bc);
  text(c,"블록 가든",left,87*scale,32,Color.WHITE);text(c,missionMode?"미션 "+mission.stage+" / "+Mission.COUNT:"자유 모드 · 광고 없음",left,116*scale,12,0xff9baac0);text(c,"모드 선택",265*scale,116*scale,13,0xff64d9bc);
  text(c,"점수  "+engine.score,left,161*scale,22,Color.WHITE);text(c,"최고  "+best,240*scale,161*scale,15,0xffffbd69);
  drawBoard(c);text(c,missionMode?mission.label(engine):message,left,top+unit*8+35*scale,12,0xffb6c5d9);drawHand(c);
  text(c,"블록 선택 → 보드 터치 · 끌어서 놓기도 가능",left,tray+120*scale,11,0xff9baac0);
  text(c,actionLabel(),left,tray+168*scale,17,0xff64d9bc);
  if(over)text(c,missionMode?(mission.complete(engine)?"미션 성공! 다음 스테이지를 누르세요":"미션 실패 · 다시 도전해요!"):"놓을 공간이 없어요. 다시 도전해요!",left,tray+204*scale,16,0xffffbd69);
 }
 private void tile(Canvas c,float x,float y,float size,int color){paint.setColor(color);c.drawRoundRect(x+2*scale,y+2*scale,x+size-2*scale,y+size-2*scale,6*scale,6*scale,paint);paint.setColor(0x22ffffff);c.drawRoundRect(x+5*scale,y+4*scale,x+size-5*scale,y+8*scale,2,2,paint);}
 private void drawBoard(Canvas c){
  for(int r=0;r<8;r++)for(int col=0;col<8;col++)tile(c,left+col*unit,top+r*unit,unit,colors[engine.board[r][col]]);
  drawStars(c);
  if(selected<0||hoverR<0)return;
  int[][] shape=shapes[hand[selected]];boolean valid=engine.fits(shape,hoverR,hoverC);
  for(int[] p:shape){int r=hoverR+p[0],col=hoverC+p[1];if(inBoard(r,col))tile(c,left+col*unit,top+r*unit,unit,valid?0xffb4ffe8:0xffa45369);}
 }
 private void drawStars(Canvas c){
  for(int r=0;r<8;r++)for(int col=0;col<8;col++)if(engine.stars[r][col])text(c,"★",left+col*unit+10*scale,top+r*unit+30*scale,23,0xffffbd69);
 }
 private String actionLabel(){
  if(!missionMode)return "새 게임";
  if(!mission.complete(engine))return "미션 다시 시작";
  return mission.stage==Mission.COUNT?"전체 미션 완료! 처음부터":"미션 성공! 다음 스테이지 →";
 }
 private void modeMenu(){
  new AlertDialog.Builder(getContext()).setTitle("모드 선택").setItems(new String[]{"자유 모드 이어하기","미션 모드 이어하기"},(dialog,which)->switchMode(which==1)).show();
 }
 private void switchMode(boolean enabled){
  save();missionMode=enabled;mission=new Mission(prefs.getInt("stage",1));engine=new Engine();selected=-1;hoverR=-1;over=false;deal();restore();save();invalidate();
 }
 private void stageAction(){
  if(missionMode&&mission.complete(engine)){int next=mission.nextStage();mission=new Mission(next);reset();return;}
  confirmReset();
 }
 private void drawHand(Canvas c){
  for(int i=0;i<3;i++){if(hand[i]<0)continue;float x=left+i*118*scale;
   if(i==selected){paint.setColor(0xff31465f);c.drawRoundRect(x-5*scale,tray-12*scale,x+108*scale,tray+90*scale,12,12,paint);}
   for(int[] p:shapes[hand[i]])tile(c,x+p[1]*23*scale,tray+p[0]*23*scale,23*scale,colors[tones[i]]);
  }
 }
 private boolean inBoard(int r,int c){return r>=0&&c>=0&&r<8&&c<8;}
 private boolean selectHand(float x){int slot=(int)((x-left)/(118*scale));if(x<left||slot<0||slot>=3)return false;if(hand[slot]<0)return false;selected=slot;invalidate();return true;}
 private boolean touchDown(float x,float y){
  if(y>90*scale&&y<135*scale){modeMenu();return true;}
  if(y>tray+140*scale&&y<tray+190*scale){stageAction();return true;}
  if(over)return true;
  if(y>=tray-20*scale&&y<tray+95*scale){if(selectHand(x))touch.begin(x,y,true);return true;}
  if(selected>=0)touch.begin(x,y,false);
  preview(x,y);return true;
 }
 @Override public boolean onTouchEvent(android.view.MotionEvent event){
  int action=event.getActionMasked();
  if(action==MotionEvent.ACTION_DOWN){touch.cancel();activePointer=event.getPointerId(0);getParent().requestDisallowInterceptTouchEvent(true);return touchDown(event.getX(),event.getY());}
  if(action==MotionEvent.ACTION_CANCEL){cancelTouch();return true;}
  if(action==MotionEvent.ACTION_POINTER_UP){if(event.getPointerId(event.getActionIndex())==activePointer)cancelTouch();return true;}
  int index=event.findPointerIndex(activePointer);if(index<0)return true;
  float x=event.getX(index),y=event.getY(index);
  if(action==MotionEvent.ACTION_MOVE){updateTouch(x,y);return true;}
  if(action==MotionEvent.ACTION_UP){if(touch.active){updateTouch(x,y);place();performClick();}cancelTouch();return true;}
  return true;
 }
 private void cancelTouch(){touch.cancel();activePointer=-1;hoverR=-1;hoverC=-1;getParent().requestDisallowInterceptTouchEvent(false);invalidate();}
 @Override public boolean performClick(){super.performClick();return true;}
 private void updateTouch(float x,float y){touch.move(x,y,ViewConfiguration.get(getContext()).getScaledTouchSlop());preview(x,y);}
 private void preview(float x,float y){
  hoverR=-1;hoverC=-1;if(selected<0){invalidate();return;}
  int height=1,width=1;for(int[] p:shapes[hand[selected]]){height=Math.max(height,p[0]+1);width=Math.max(width,p[1]+1);}
  int[] anchor=touch.anchor(x,y,left,top,unit,height,width);
  if(anchor!=null){hoverR=anchor[0];hoverC=anchor[1];}postInvalidateOnAnimation();
 }
 private void place(){
  if(selected<0||hoverR<0||over)return;
  if(!engine.place(shapes[hand[selected]],hoverR,hoverC,tones[selected])){message="여기는 놓을 수 없어요";invalidate();return;}
  hand[selected]=-1;selected=-1;hoverR=-1;best=Math.max(best,engine.score);
  if(handEmpty())deal();
  over=finished();message=over?"게임 종료 · 새 게임으로 다시 시작":"좋아요! 가로 또는 세로를 채워보세요";save();invalidate();
 }
 private boolean finished(){if(missionMode&&mission.complete(engine))return true;if(missionMode&&mission.failed(engine))return true;return !hasMove();}
 private boolean handEmpty(){return hand[0]<0&&hand[1]<0&&hand[2]<0;}
 private boolean hasMove(){for(int id:hand)if(id>=0&&engine.canFit(shapes[id]))return true;return false;}
 private void confirmReset(){new AlertDialog.Builder(getContext()).setTitle("새 게임을 시작할까요?").setMessage("최고 기록은 유지됩니다.").setNegativeButton("계속하기",null).setPositiveButton("새 게임",(dialog,which)->reset()).show();}
 private void reset(){engine=new Engine();selected=-1;hoverR=-1;over=false;message="블록을 골라 보드에 놓아보세요";deal();if(missionMode)mission.setup(engine);save();invalidate();}
 private SharedPreferences slot(){return getContext().getSharedPreferences(missionMode?"mission":"game",0);}
 private void save(){
  prefs.edit().putBoolean("missionMode",missionMode).putInt("best",best).putInt("stage",mission.stage).apply();
  StringBuilder data=new StringBuilder();for(int[] row:engine.board)for(int value:row)data.append(value);
  SharedPreferences.Editor edit=slot().edit().putString("board",data.toString()).putInt("score",engine.score).putInt("best",best);
  StringBuilder stars=new StringBuilder();for(boolean[] row:engine.stars)for(boolean star:row)stars.append(star?'1':'0');
  edit.putString("stars",stars.toString()).putInt("lines",engine.lines).putInt("moves",engine.moves).putInt("savedStage",mission.stage);
  for(int i=0;i<3;i++)edit.putInt("hand"+i,hand[i]).putInt("tone"+i,tones[i]);edit.apply();
 }
 private void restore(){
  SharedPreferences saved=slot();
  String data=saved.getString("board","");
  int[] savedHand=new int[3],savedTones=new int[3];
  for(int i=0;i<3;i++){savedHand[i]=saved.getInt("hand"+i,0);savedTones[i]=saved.getInt("tone"+i,1);}
  if(missionMode)mission=new Mission(saved.getInt("savedStage",mission.stage));
  if(!SavedState.valid(data,savedHand,savedTones,shapes.length)){if(missionMode)mission.setup(engine);return;}
  for(int r=0;r<8;r++)for(int c=0;c<8;c++)engine.board[r][c]=data.charAt(r*8+c)-'0';
  hand=savedHand;tones=savedTones;engine.score=Math.max(0,saved.getInt("score",0));
  engine.lines=Math.max(0,saved.getInt("lines",0));engine.moves=Math.max(0,saved.getInt("moves",0));
  restoreStars(saved);
  if(handEmpty())deal();
  best=Math.max(best,engine.score);over=finished();
  if(over)message="게임 종료 · 새 게임으로 다시 시작";
 }
 private void restoreStars(SharedPreferences saved){
  if(!missionMode)return;
  String stars=saved.getString("stars","");
  if(!SavedState.validStars(stars)){mission.setup(engine);return;}
  for(int r=0;r<8;r++)for(int c=0;c<8;c++)engine.stars[r][c]=stars.charAt(r*8+c)=='1';
 }

}
