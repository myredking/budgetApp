package com.towerfront;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.widget.*;

/** Native, wrapping menu cards retain large text and accessible touch behavior. */
public final class MenuCards {
    private final Context context;
    public MenuCards(Context value) {context=value;}
    private int dp(int value) {return Math.round(value*context.getResources().getDisplayMetrics().density);}
    private GradientDrawable shape(int color,int border) {
        GradientDrawable result=new GradientDrawable();result.setColor(color);
        result.setCornerRadius(dp(24));result.setStroke(dp(2),border);return result;
    }
    private TextView text(String value,int size,int color,boolean bold) {
        TextView view=new TextView(context);view.setText(value);view.setTextSize(size);view.setTextColor(color);
        view.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_BALANCED);
        if(bold) view.setTypeface(null,Typeface.BOLD);
        return view;
    }
    public void add(LinearLayout parent,String title,String description,String code,int accent,Runnable action) {
        LinearLayout card=new LinearLayout(context);card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16),dp(18),dp(16),dp(18));card.setMinimumHeight(dp(88));
        GradientDrawable skin=shape(MainActivity.CARD,0xffc6a77a);skin.setColors(new int[]{0xfffffae6,0xfff4d9ad});
        card.setBackground(new RippleDrawable(ColorStateList.valueOf(0x182678bf),skin,null));
        card.setElevation(dp(4));card.setFocusable(true);card.setClickable(true);
        card.setOnClickListener(view->action.run());
        ImageView badge=new ImageView(context);badge.setImageDrawable(new GameIconDrawable(code.equals("01")?GameIcons.CASTLE:GameIcons.FLAGS,dp(60)));
        badge.setImportantForAccessibility(android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        card.addView(badge,new LinearLayout.LayoutParams(dp(60),dp(64)));
        LinearLayout copy=new LinearLayout(context);copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(14),0,dp(4),0);
        copy.addView(text(title,16,SceneStyle.INK,true));
        TextView detail=text(description,12,MainActivity.MUTED,false);detail.setPadding(0,dp(5),0,0);copy.addView(detail);
        card.addView(copy,new LinearLayout.LayoutParams(0,-2,1));
        ImageView arrow=new ImageView(context);arrow.setImageDrawable(new GameIconDrawable(GameIcons.PLAY,dp(24)));
        arrow.setImportantForAccessibility(android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO);card.addView(arrow);
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.topMargin=dp(12);
        parent.addView(card,params);
    }
}
