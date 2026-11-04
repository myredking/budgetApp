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
 @Override public void onCreate(Bundle state){
  super.onCreate(state);getWindow().setStatusBarColor(0xff101c30);getWindow().setNavigationBarColor(0xff101c30);showMenu();
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
  panel.addView(label("진행 상황 자동 저장 · 광고 없이 오프라인 플레이",12,0xff9baac0));
  scroll.addView(panel);setContentView(scroll);
 }
 private void addMode(LinearLayout panel,String title,String description,boolean mission){
  Button button=new Button(this);button.setText(title+"\n"+description);button.setAllCaps(false);button.setTextSize(17);button.setTextColor(Color.WHITE);button.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);button.setPadding(dp(20),dp(18),dp(20),dp(18));button.setMinHeight(dp(100));
  GradientDrawable bg=new GradientDrawable();bg.setColor(0xff26364d);bg.setCornerRadius(dp(18));bg.setStroke(dp(1),mission?0xffffbd69:0xff64d9bc);button.setBackground(bg);
  LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.topMargin=dp(18);panel.addView(button,params);
  button.setOnClickListener(view->startGame(mission));
 }
 private void startGame(boolean mission){navigation.choose(mission);game=new GameView(this,navigation.isMission(),()->returnHome());setContentView(game);}
 private void returnHome(){if(game!=null)game.persist();showMenu();}
 @Override public void onBackPressed(){if(!navigation.isMenu()){returnHome();return;}super.onBackPressed();}
 @Override protected void onPause(){if(game!=null)game.persist();super.onPause();}
}
