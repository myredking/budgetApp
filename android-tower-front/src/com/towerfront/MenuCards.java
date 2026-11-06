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
        result.setCornerRadius(dp(18));result.setStroke(dp(1),border);return result;
    }
    private TextView text(String value,int size,int color,boolean bold) {
        TextView view=new TextView(context);view.setText(value);view.setTextSize(size);view.setTextColor(color);
        if(bold) {view.setTypeface(null,Typeface.BOLD);view.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_BALANCED);}
        return view;
    }
    public void add(LinearLayout parent,String title,String description,String code,int accent,Runnable action) {
        LinearLayout card=new LinearLayout(context);card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16),dp(18),dp(16),dp(18));card.setMinimumHeight(dp(88));
        card.setBackground(new RippleDrawable(ColorStateList.valueOf(0x182678bf),shape(MainActivity.CARD,0x20334455),null));
        card.setElevation(dp(2));card.setFocusable(true);card.setClickable(true);
        card.setOnClickListener(view->action.run());
        TextView badge=text(code,15,accent,true);badge.setGravity(Gravity.CENTER);
        badge.setBackground(shape(0xffedf1eb,0x15334455));
        card.addView(badge,new LinearLayout.LayoutParams(dp(42),dp(46)));
        LinearLayout copy=new LinearLayout(context);copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(14),0,dp(4),0);
        copy.addView(text(title,16,SceneStyle.INK,true));
        TextView detail=text(description,12,MainActivity.MUTED,false);detail.setPadding(0,dp(5),0,0);copy.addView(detail);
        card.addView(copy,new LinearLayout.LayoutParams(0,-2,1));
        card.addView(text("›",26,accent,false));
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.topMargin=dp(12);
        parent.addView(card,params);
    }
}
