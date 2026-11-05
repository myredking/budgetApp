package com.blockgarden;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
public final class ResultPopup {
 private final Context context;
 private final LinearLayout panel;
 private final int accent;
 private final AlertDialog dialog;
 private ResultPopup(Context context,boolean success){
  this.context=context;accent=success?0xff64d9bc:0xffffbd69;
  panel=new LinearLayout(context);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(24),dp(24),dp(24),dp(20));
  panel.setBackground(background(0xff1a2a43,0xff101c30,24));
  ScrollView scroll=new ScrollView(context);scroll.setFillViewport(true);scroll.addView(panel);
  dialog=new AlertDialog.Builder(context).setView(scroll).create();
 }
 private int dp(int value){return Math.round(value*context.getResources().getDisplayMetrics().density);}
 private GradientDrawable background(int top,int bottom,int radius){
  GradientDrawable drawable=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{top,bottom});drawable.setCornerRadius(dp(radius));drawable.setStroke(dp(1),0xff354760);return drawable;
 }
 private TextView text(String value,int size,int color){
  TextView view=new TextView(context);view.setText(value);view.setTextColor(color);view.setTextSize(size);view.setGravity(Gravity.CENTER);view.setPadding(0,dp(5),0,dp(5));return view;
 }
 private void heading(String title,String description,boolean success){
  panel.addView(text(success?"✦  MISSION CLEAR  ✦":"BLOCK GARDEN",12,accent));
  TextView heading=text(title,26,Color.WHITE);heading.setTypeface(null,Typeface.BOLD);panel.addView(heading);
  panel.addView(text(description,14,0xffb6c5d9));
 }
 private void stats(int score,int best,int lines,int moves){
  panel.addView(text("이번 점수",12,0xff9baac0));TextView number=text(String.format(java.util.Locale.KOREA,"%,d",score),42,accent);number.setTypeface(null,Typeface.BOLD);panel.addView(number);
  LinearLayout row=new LinearLayout(context);row.setPadding(0,dp(8),0,dp(16));
  stat(row,"최고 기록",best);stat(row,"제거한 줄",lines);stat(row,"배치 횟수",moves);panel.addView(row);
 }
 private void stat(LinearLayout row,String name,int value){
  LinearLayout box=new LinearLayout(context);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(3),dp(8),dp(3),dp(8));box.setBackground(background(0xff26364d,0xff203047,12));
  box.addView(text(String.valueOf(value),19,Color.WHITE));box.addView(text(name,11,0xffb6c5d9));
  LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,-2,1);params.setMargins(dp(3),0,dp(3),0);row.addView(box,params);
 }
 private void button(String title,boolean primary,Runnable action){
  Button button=new Button(context);button.setText(title);button.setTextSize(16);button.setAllCaps(false);button.setMinHeight(dp(52));button.setTextColor(primary?0xff101c30:0xffd9e6f5);
  android.graphics.drawable.Drawable shape=background(primary?accent:0xff26364d,primary?accent:0xff203047,14);
  button.setBackground(new RippleDrawable(android.content.res.ColorStateList.valueOf(0x33ffffff),shape,null));
  LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.topMargin=dp(9);panel.addView(button,params);
  button.setOnClickListener(view->{dialog.dismiss();action.run();});
 }
 public static AlertDialog show(Context context,boolean success,String title,String description,int score,int best,int lines,int moves,String primary,Runnable restart,Runnable home){
  ResultPopup popup=new ResultPopup(context,success);popup.heading(title,description,success);popup.stats(score,best,lines,moves);
  popup.button(primary,true,restart);popup.button("모드 선택",false,home);popup.button("보드 다시 보기",false,()->{});popup.dialog.show();
  Window window=popup.dialog.getWindow();if(window!=null){window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));int width=Math.min(context.getResources().getDisplayMetrics().widthPixels-popup.dp(32),popup.dp(390));window.setLayout(width,WindowManager.LayoutParams.WRAP_CONTENT);window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);window.setDimAmount(0.75f);}
  return popup.dialog;
 }
}
