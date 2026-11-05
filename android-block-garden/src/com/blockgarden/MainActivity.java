package com.blockgarden;
import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.*;
public class MainActivity extends Activity {
 private final Navigation navigation=new Navigation();
 private GameView game;
 private GameAudio audio;
 @Override public void onCreate(Bundle state){
  super.onCreate(state);getWindow().setStatusBarColor(0xff101c30);getWindow().setNavigationBarColor(0xff101c30);getWindow().getDecorView().setOnApplyWindowInsetsListener((view,insets)->{view.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets.consumeSystemWindowInsets();});audio=new GameAudio(this);showMenu();
 }
 private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
 private TextView label(String text,int size,int color){
  TextView view=new TextView(this);view.setText(text);view.setTextSize(size);view.setTextColor(color);view.setPadding(0,dp(10),0,dp(10));return view;
 }
 private void showMenu(){
  navigation.home();game=null;
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(0xff101c30);
  LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setGravity(Gravity.CENTER_VERTICAL);panel.setPadding(dp(28),dp(32),dp(28),dp(32));
  panel.addView(label("BLOCK GARDEN",17,0xff64d9bc));
  TextView title=label("블록 가든",36,Color.WHITE);title.setTypeface(null,Typeface.BOLD);panel.addView(title);
  panel.addView(label("오늘은 어떤 퍼즐을 즐길까요?",16,0xffb6c5d9));
  addMode(panel,"자유 모드","제한 없이 최고 점수에 도전하세요",false);
  int stage=getSharedPreferences("mission",0).getInt("savedStage",1);stage=new Mission(stage).stage;
  addMode(panel,"미션 모드","100개 미션 · "+stage+"단계부터 이어하기",true);
  Button sound=new Button(this);sound.setText("소리 · 진동 설정");sound.setOnClickListener(view->audio.settings());panel.addView(sound);
  Button help=new Button(this);help.setText("게임 방법");help.setOnClickListener(view->showHelp());panel.addView(help);
  panel.addView(label("진행 상황 자동 저장 · 광고 없이 오프라인 플레이",12,0xff9baac0));
  scroll.addView(panel);setContentView(scroll);
 }
 private void addMode(LinearLayout panel,String title,String description,boolean mission){
  Button button=new Button(this);button.setText(title+"\n"+description);button.setAllCaps(false);button.setTextSize(17);button.setTextColor(Color.WHITE);button.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);button.setPadding(dp(20),dp(18),dp(20),dp(18));button.setMinHeight(dp(100));
  GradientDrawable bg=new GradientDrawable();bg.setColor(0xff26364d);bg.setCornerRadius(dp(18));bg.setStroke(dp(1),mission?0xffffbd69:0xff64d9bc);button.setBackground(bg);
  LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.topMargin=dp(18);panel.addView(button,params);
  button.setOnClickListener(view->startGame(mission));
 }
 private void showHelp(){
  new android.app.AlertDialog.Builder(this).setTitle("게임 방법")
   .setMessage("① 아래 블록을 끌어 보드에 놓으세요. 선택 후 보드 칸을 눌러도 됩니다.\n\n② 가로나 세로 한 줄을 채우면 블록이 사라져요. 블록 회전은 없어요.\n\n③ 블록 3개를 모두 쓰면 새 블록이 나와요. 놓을 공간이 없으면 게임이 끝나요.\n\n④ 미션 모드에서는 표시된 목표를 제한 횟수 안에 달성하세요. 별·장애물이 있는 줄을 완성하면 해당 목표도 제거돼요.\n\n⑤ 힌트 보기는 놓을 수 있는 위치를 알려줘요. 연속 배치로 줄을 지우면 2콤보부터 보너스를 받아요.\n\n⑥ 게임당 한 번 직전 배치를 되돌릴 수 있어요. 앱을 다시 켜면 이전 배치의 되돌리기 기록은 사라져요.")
   .setPositiveButton("알겠어요",null).show();
 }
 private void startGame(boolean mission){navigation.choose(mission);game=new GameView(this,navigation.isMission(),()->returnHome(),audio);setContentView(game);}
 private void returnHome(){if(game!=null)game.persist();showMenu();}
 @Override public void onBackPressed(){if(!navigation.isMenu()){returnHome();return;}super.onBackPressed();}
 @Override protected void onPause(){if(game!=null)game.persist();if(audio!=null)audio.pause();super.onPause();}
 @Override protected void onResume(){super.onResume();if(audio!=null)audio.resume();}
 @Override protected void onDestroy(){if(audio!=null)audio.close();super.onDestroy();}
}
