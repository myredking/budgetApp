package com.blockgarden;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.view.*;
import java.util.Random;
public class GameView extends View {
 private final Paint paint=new Paint(3);
 private final BlockStyle blockStyle=new BlockStyle();
 private final Typeface font=Typeface.create("sans-serif",Typeface.BOLD);
 private Bitmap boardBackdrop;
 private int touchSlop;
 private float fingerX,fingerY;
 private boolean resultShown=false,pendingHint=false;
 private AlertDialog resultDialog;
 private PlacementGuide.Preview guide;
 private GameSnapshot undo;
 private boolean undoUsed=false;
 private final SharedPreferences prefs;
 private final Runnable home;
 private final GameAudio audio;
 private final int[][] burst=new int[8][8];
 private long burstStarted=0;
 private int burstLines=0,burstPoints=0,burstCombo=0;
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
 public GameView(Context context,boolean missionMode,Runnable home,GameAudio audio){super(context);prefs=context.getSharedPreferences("game",0);best=prefs.getInt("best",0);this.missionMode=missionMode;this.home=home;this.audio=audio;mission=new Mission(prefs.getInt("stage",1));deal();restore();save();touchSlop=ViewConfiguration.get(context).getScaledTouchSlop();setContentDescription("블록 가든 게임 보드");}
 private void deal(){hand=BlockDealer.deal(random,engine,shapes);for(int i=0;i<3;i++)tones[i]=1+random.nextInt(5);}
 private void text(Canvas c,String s,float x,float y,float size,int color){paint.setColor(color);paint.setTextSize(size*scale);paint.setTypeface(font);c.drawText(s,x,y,paint);}
 @Override protected void onDraw(Canvas c){

  c.drawColor(0xff101c30);text(c,"BLOCK GARDEN",left,26*scale,12,0xff64d9bc);
  text(c,"블록 가든",left,58*scale,27,Color.WHITE);text(c,missionMode?"미션 "+mission.stage+" / "+Mission.COUNT:"자유 모드 · 광고 없음",left,83*scale,12,0xff9baac0);text(c,"모드 선택",265*scale,83*scale,13,0xff64d9bc);
  text(c,"점수  "+engine.score,left,117*scale,22,Color.WHITE);text(c,"최고  "+best,240*scale,117*scale,15,0xffffbd69);
  drawAnimatedBoard(c);text(c,missionMode?mission.label(engine):message,left,top+unit*8+20*scale,12,0xffb6c5d9);drawHand(c);drawFloating(c);
  text(c,"힌트 보기",left,tray+112*scale,15,0xff64d9bc);
  text(c,"완성될 줄이 빛으로 표시돼요",left+115*scale,tray+112*scale,11,0xff9baac0);
  text(c,actionLabel(),left,tray+156*scale,17,0xff64d9bc);
  text(c,"소리 · 진동",left,tray+208*scale,13,0xff9baac0);
  text(c,undoUsed?"되돌리기 사용 완료":"1회 되돌리기",left+190*scale,tray+208*scale,13,undo!=null&&!undoUsed?0xff64d9bc:0xff9baac0);
  if(over)text(c,missionMode?(mission.complete(engine)?"미션 성공! 다음 스테이지를 누르세요":"미션 실패 · 다시 도전해요!"):"놓을 공간이 없어요. 다시 도전해요!",left,tray+184*scale,16,0xffffbd69);
 }
 @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){
  super.onSizeChanged(w,h,oldw,oldh);if(w<=0||h<=0)return;
  GameLayout layout=new GameLayout(w,h);scale=layout.scale;unit=layout.unit;left=layout.left;top=layout.top;tray=layout.tray;
  blockStyle.clear();boardBackdrop=Bitmap.createBitmap(Math.max(1,(int)Math.ceil(unit*8)),Math.max(1,(int)Math.ceil(unit*8)),Bitmap.Config.ARGB_8888);
  Canvas board=new Canvas(boardBackdrop);for(int r=0;r<8;r++)for(int col=0;col<8;col++)tile(board,col*unit,r*unit,unit,colors[0]);
 }
 private void tile(Canvas c,float x,float y,float size,int color){blockStyle.draw(c,x,y,size,color,color==colors[0]);}
 private void drawBoard(Canvas c){
  if(boardBackdrop!=null)c.drawBitmap(boardBackdrop,left,top,null);
  for(int r=0;r<8;r++)for(int col=0;col<8;col++)if(engine.board[r][col]!=0)tile(c,left+col*unit,top+r*unit,unit,colors[engine.board[r][col]]);
  drawStars(c);
  if(selected<0||hoverR<0)return;
  int[][] shape=shapes[hand[selected]];boolean valid=engine.fits(shape,hoverR,hoverC);
  for(int[] p:shape){int r=hoverR+p[0],col=hoverC+p[1];if(inBoard(r,col))tile(c,left+col*unit,top+r*unit,unit,valid?0xffb4ffe8:0xffa45369);}
  drawGuide(c);
 }
 private void drawGuide(Canvas c){
  if(guide==null||!guide.valid||guide.lines==0)return;
  paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2*scale);paint.setColor(0xffffe4a4);
  for(int i=0;i<8;i++){
   if((guide.rows&(1<<i))!=0)c.drawRoundRect(left+2*scale,top+i*unit+2*scale,left+8*unit-2*scale,top+(i+1)*unit-2*scale,6*scale,6*scale,paint);
   if((guide.cols&(1<<i))!=0)c.drawRoundRect(left+i*unit+2*scale,top+2*scale,left+(i+1)*unit-2*scale,top+8*unit-2*scale,6*scale,6*scale,paint);
  }paint.setStyle(Paint.Style.FILL);
 }
 private void refreshGuide(){
  guide=null;if(selected<0||hoverR<0)return;
  guide=PlacementGuide.preview(engine,shapes[hand[selected]],hoverR,hoverC);
 }
 private void showHint(){
  if(over)return;
  PlacementGuide.Hint hint=PlacementGuide.hint(engine,hand,shapes);if(hint==null)return;
  selected=hint.slot;hoverR=hint.row;hoverC=hint.col;refreshGuide();
  android.widget.Toast.makeText(getContext(),"표시된 블록과 위치를 참고하세요. 클리어를 보장하는 힌트는 아니에요.",android.widget.Toast.LENGTH_SHORT).show();invalidate();
 }
 private boolean hintTap(float y){if(y<tray+98*scale||y>tray+132*scale)return false;pendingHint=true;return true;}
 private void drawStars(Canvas c){
  for(int r=0;r<8;r++)for(int col=0;col<8;col++)if(engine.stars[r][col])text(c,"★",left+col*unit+10*scale,top+r*unit+30*scale,23,0xffffbd69);
 }
 private String actionLabel(){
  if(!missionMode)return "새 게임";
  if(!mission.complete(engine))return "미션 다시 시작";
  return mission.stage==Mission.COUNT?"전체 미션 완료! 처음부터":"미션 성공! 다음 스테이지 →";
 }
 public void persist(){save();}
 private void modeMenu(){cancelTouch();save();home.run();}
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
 private void drawFloating(Canvas c){
  if(!touch.dragging||selected<0)return;
  int[][] shape=shapes[hand[selected]];int width=1,height=1;
  for(int[] p:shape){width=Math.max(width,p[1]+1);height=Math.max(height,p[0]+1);}
  float x=fingerX-width*unit/2,y=fingerY-unit*1.5f-height*unit/2;
  for(int[] p:shape)tile(c,x+p[1]*unit,y+p[0]*unit,unit,colors[tones[selected]]);
 }
 private boolean inBoard(int r,int c){return r>=0&&c>=0&&r<8&&c<8;}
 private boolean selectHand(float x){int slot=(int)((x-left)/(118*scale));if(x<left||slot<0||slot>=3)return false;if(hand[slot]<0)return false;selected=slot;hoverR=-1;hoverC=-1;guide=null;invalidate();return true;}
 private boolean settingsTap(float x,float y){if(y<=tray+190*scale||y>=tray+232*scale)return false;if(x>left+170*scale)undoMove();else audio.settings();return true;}
 private boolean handTap(float x,float y){
  if(y<tray-20*scale||y>=tray+95*scale)return false;
  if(selectHand(x))touch.begin(x,y,true);return true;
 }
 private boolean touchDown(float x,float y){
  if(settingsTap(x,y))return true;
  if(y>64*scale&&y<96*scale){modeMenu();return true;}
  if(y>tray+134*scale&&y<tray+178*scale){stageAction();return true;}
  if(over)return true;
  if(hintTap(y))return true;
  if(handTap(x,y))return true;
  if(selected>=0)touch.begin(x,y,false);
  preview(x,y);return true;
 }
 @Override public boolean onTouchEvent(android.view.MotionEvent event){
  int action=event.getActionMasked();
  if(action==MotionEvent.ACTION_DOWN){touch.cancel();activePointer=event.getPointerId(0);if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);return touchDown(event.getX(),event.getY());}
  if(action==MotionEvent.ACTION_CANCEL){cancelTouch();return true;}
  if(action==MotionEvent.ACTION_POINTER_UP){if(event.getPointerId(event.getActionIndex())==activePointer)cancelTouch();return true;}
  int index=event.findPointerIndex(activePointer);if(index<0)return true;
  float x=event.getX(index),y=event.getY(index);
  if(action==MotionEvent.ACTION_MOVE){updateTouch(x,y);return true;}
  if(action==MotionEvent.ACTION_UP){releaseTouch(x,y);return true;}
  return true;
 }
 private void releaseTouch(float x,float y){
  boolean hint=pendingHint;if(touch.active){updateTouch(x,y);place();performClick();}cancelTouch();if(hint)showHint();
 }
 private void cancelTouch(){pendingHint=false;guide=null;touch.cancel();activePointer=-1;hoverR=-1;hoverC=-1;if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);invalidate();}
 @Override public boolean performClick(){super.performClick();return true;}
 private void updateTouch(float x,float y){fingerX=x;fingerY=y;touch.move(x,y,touchSlop);preview(x,y);invalidate();}
 private void preview(float x,float y){
  int oldRow=hoverR,oldCol=hoverC;hoverR=-1;hoverC=-1;if(selected<0){invalidate();return;}
  int height=1,width=1;for(int[] p:shapes[hand[selected]]){height=Math.max(height,p[0]+1);width=Math.max(width,p[1]+1);}
  int[] anchor=touch.anchor(x,y,left,top,unit,height,width);
  if(anchor!=null){hoverR=anchor[0];hoverC=anchor[1];}if(oldRow!=hoverR||oldCol!=hoverC)refreshGuide();invalidate();
 }
 private void place(){
  if(selected<0||hoverR<0||over)return;
  int previousLines=engine.lines,previousScore=engine.score;
  GameSnapshot before=undoUsed?null:new GameSnapshot(engine,hand,tones);
  if(!engine.place(shapes[hand[selected]],hoverR,hoverC,tones[selected])){audio.play("invalid");message="여기는 놓을 수 없어요";invalidate();return;}
  undo=before;placementFeedback(previousLines,previousScore);
  hand[selected]=-1;selected=-1;hoverR=-1;best=Math.max(best,engine.score);
  if(handEmpty())deal();
  finishPlacement();
 }
 private void finishPlacement(){
  over=finished();if(over)audio.play(missionWon()?"win":"fail");message=over?"게임 종료 · 새 게임으로 다시 시작":"좋아요! 가로 또는 세로를 채워보세요";save();postInvalidateOnAnimation();if(over)post(()->showResult());
 }
 @Override protected void onAttachedToWindow(){super.onAttachedToWindow();if(over)post(()->showResult());}
 @Override protected void onDetachedFromWindow(){dismissResult();super.onDetachedFromWindow();}
 private void dismissResult(){if(resultDialog!=null){resultDialog.dismiss();resultDialog=null;}}
 private void showResult(){
  if(!over||resultShown||!isAttachedToWindow())return;
  resultShown=true;boolean success=missionWon();
  String title=EndSummary.title(missionMode,mission,engine);
  String description=EndSummary.message(missionMode,mission,engine,best).split("\n\n")[0];
  String primary=success?(mission.stage==Mission.COUNT?"처음부터 도전":"다음 스테이지로"):"다시 도전하기";
  resultDialog=ResultPopup.show(getContext(),success,title,description,engine.score,best,engine.lines,engine.moves,primary,
   ()->{if(success)mission=new Mission(mission.nextStage());reset();},()->modeMenu());
 }
 private void undoMove(){
  if(undoUsed||undo==null){message="이번 실행에서 배치한 블록을 1회 되돌릴 수 있어요";invalidate();return;}
  dismissResult();cancelTouch();engine=undo.engine();hand=undo.hand();tones=undo.tones();undo=null;undoUsed=true;
  selected=-1;burstStarted=0;resultShown=false;over=finished();message="직전 배치를 되돌렸어요";save();invalidate();
 }
 private boolean missionWon(){return missionMode&&mission.complete(engine);}
 private void placementFeedback(int oldLines,int oldScore){
  int lines=engine.lines-oldLines;audio.play(lines>0?"clear":"place");
  if(audio.enabled("haptics"))performHapticFeedback(lines>0?HapticFeedbackConstants.LONG_PRESS:HapticFeedbackConstants.KEYBOARD_TAP);
  if(lines==0)return;
  burstStarted=android.os.SystemClock.uptimeMillis();burstLines=lines;burstCombo=engine.combo;burstPoints=engine.score-oldScore;
  for(int r=0;r<8;r++)System.arraycopy(engine.cleared[r],0,burst[r],0,8);
 }
 private void drawAnimatedBoard(Canvas c){
  float t=(android.os.SystemClock.uptimeMillis()-burstStarted)/550f;
  boolean animate=burstStarted>0&&t<1;
  c.save();if(animate)c.translate((float)Math.sin(t*48)*5*scale*(1-t),0);drawBoard(c);c.restore();
  if(!animate)return;
  drawBurst(c,t);text(c,(burstCombo>1?burstCombo+"콤보! ":"")+(burstLines>1?burstLines+"줄 동시 제거! ":"줄 제거! ")+"+"+burstPoints,left,top+unit*4,18,0xffffbd69);postInvalidateOnAnimation();
 }
 private void drawBurst(Canvas c,float t){
  for(int r=0;r<8;r++)for(int col=0;col<8;col++){
   int color=burst[r][col];if(color==0)continue;
   float x=left+(col+0.5f)*unit,y=top+(r+0.5f)*unit;
   paint.setColor(colors[color]);paint.setAlpha((int)(255*(1-t)));
   float half=unit*0.45f*(1-t);c.drawRoundRect(x-half,y-half,x+half,y+half,6*scale,6*scale,paint);
   for(int i=0;i<4;i++)drawParticle(c,x,y,t,i);
  }paint.setAlpha(255);
 }
 private void drawParticle(Canvas c,float x,float y,float t,int direction){
  double angle=direction*Math.PI/2+0.7;float distance=t*unit*1.5f;
  c.drawCircle(x+(float)Math.cos(angle)*distance,y+(float)Math.sin(angle)*distance+t*t*unit,4*scale*(1-t),paint);
 }
 private boolean finished(){if(missionMode&&mission.complete(engine))return true;if(missionMode&&mission.failed(engine))return true;return !hasMove();}
 private boolean handEmpty(){return hand[0]<0&&hand[1]<0&&hand[2]<0;}
 private boolean hasMove(){for(int id:hand)if(id>=0&&engine.canFit(shapes[id]))return true;return false;}
 private void confirmReset(){new AlertDialog.Builder(getContext()).setTitle("새 게임을 시작할까요?").setMessage("최고 기록은 유지됩니다.").setNegativeButton("계속하기",null).setPositiveButton("새 게임",(dialog,which)->reset()).show();}
 private void reset(){undo=null;undoUsed=false;dismissResult();resultShown=false;burstStarted=0;engine=new Engine();selected=-1;hoverR=-1;over=false;message="블록을 골라 보드에 놓아보세요";if(missionMode)mission.setup(engine);deal();save();invalidate();}
 private SharedPreferences slot(){return getContext().getSharedPreferences(missionMode?"mission":"game",0);}
 private void save(){
  prefs.edit().putBoolean("missionMode",missionMode).putInt("best",best).putInt("stage",mission.stage).apply();
  StringBuilder data=new StringBuilder();for(int[] row:engine.board)for(int value:row)data.append(value);
  SharedPreferences.Editor edit=slot().edit().putBoolean("undoUsed",undoUsed).putString("board",data.toString()).putInt("score",engine.score).putInt("best",best);
  StringBuilder stars=new StringBuilder();for(boolean[] row:engine.stars)for(boolean star:row)stars.append(star?'1':'0');
  edit.putString("stars",stars.toString()).putInt("combo",engine.combo).putInt("lines",engine.lines).putInt("moves",engine.moves).putInt("savedStage",mission.stage);
  for(int i=0;i<3;i++)edit.putInt("hand"+i,hand[i]).putInt("tone"+i,tones[i]);edit.apply();
 }
 private void restore(){
  SharedPreferences saved=slot();undoUsed=saved.getBoolean("undoUsed",false);
  String data=saved.getString("board","");
  int[] savedHand=new int[3],savedTones=new int[3];
  for(int i=0;i<3;i++){savedHand[i]=saved.getInt("hand"+i,0);savedTones[i]=saved.getInt("tone"+i,1);}
  if(missionMode)mission=new Mission(saved.getInt("savedStage",mission.stage));
  if(!SavedState.valid(data,savedHand,savedTones,shapes.length)){if(missionMode)mission.setup(engine);return;}
  for(int r=0;r<8;r++)for(int c=0;c<8;c++)engine.board[r][c]=data.charAt(r*8+c)-'0';
  BlockDealer.migrate(savedHand,random,shapes.length);
  hand=savedHand;tones=savedTones;engine.score=Math.max(0,saved.getInt("score",0));
  engine.combo=Math.max(0,saved.getInt("combo",0));
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
