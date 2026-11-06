package com.bluehorizon;
import android.app.*;
import android.view.*;
import android.widget.*;
import java.lang.reflect.*;

public final class TradeUiChecks {
    public static AlertDialog sample(MainActivity activity) throws Exception {
        TradeDialog popup=new TradeDialog(activity,new Engine(),0,false,5,success->{});popup.show();
        click(popup,"금액 지정");((EditText)field(popup,"input")).setText("500");
        return (AlertDialog)field(popup,"dialog");
    }
    private static Object field(Object target,String name) throws Exception {
        Field field=target.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(target);
    }
    private static void check(boolean value,String text) { if(!value) throw new AssertionError(text); }
    public static void run(MainActivity activity,Instrumentation runner) throws Exception {
        final Throwable[] failure={null};
        runner.runOnMainSync(()->{try { quantityAndBudget(activity); } catch(Throwable error) { failure[0]=error; }});
        if(failure[0]!=null) throw new AssertionError(failure[0]);
    }
    private static void quantityAndBudget(MainActivity activity) throws Exception {
        Engine game=new Engine();int gold=game.gold;
        TradeDialog popup=new TradeDialog(activity,game,0,false,5,success->{});popup.show();
        try {
            EditText input=(EditText)field(popup,"input");input.setText("0");
            check(!((Button)field(popup,"confirm")).isEnabled(),"zero quantity disables confirm");
            input.setText("3");check(((TradeOrder)field(popup,"quote")).quantity==3,"typed quantity updates receipt");
            click(popup,"금액 지정");input=(EditText)field(popup,"input");input.setText("500");
            TradeOrder quote=(TradeOrder)field(popup,"quote");int amount=quote.quantity;
            check(quote.total<=500 && amount>0,"budget is a spending ceiling");
            check(game.gold==gold,"dialog preview does not spend");
            ((Button)field(popup,"confirm")).performClick();
            check(game.inventory[0]==amount && game.gold==gold-quote.total,"confirm commits displayed receipt");
            maximumAndSale(activity,game);
        } finally { ((AlertDialog)field(popup,"dialog")).dismiss(); }
    }
    private static void maximumAndSale(MainActivity activity,Engine game) throws Exception {
        TradeDialog popup=new TradeDialog(activity,game,0,false,1,success->{});popup.show();
        try {
            click(popup,"최대 구매");TradeOrder quote=(TradeOrder)field(popup,"quote");
            check(quote.goldAfter>=200,"maximum retains checked reserve");
            ((CheckBox)field(popup,"reserve")).setChecked(false);
            check(((TradeOrder)field(popup,"quote")).quantity>=quote.quantity,"reserve toggle refreshes maximum");
        } finally { ((AlertDialog)field(popup,"dialog")).dismiss(); }
        popup=new TradeDialog(activity,game,0,true,1,success->{});popup.show();
        try {
            click(popup,"전량 판매");check(((TradeOrder)field(popup,"quote")).quantity==game.inventory[0]-5,"sale all keeps story cargo by default");
            ((CheckBox)field(popup,"protect")).setChecked(false);
            check(((TradeOrder)field(popup,"quote")).quantity==game.inventory[0],"explicit protection opt out allows full sale");
        } finally { ((AlertDialog)field(popup,"dialog")).dismiss(); }
    }
    private static void click(TradeDialog popup,String title) throws Exception {
        Button button=find((View)field(popup,"body"),title);
        if(button==null) throw new AssertionError("Missing trade control: "+title);button.performClick();
    }
    private static Button find(View view,String title) {
        if(view instanceof Button && ((Button)view).getText().toString().equals(title)) return (Button)view;
        if(!(view instanceof ViewGroup)) return null;
        ViewGroup group=(ViewGroup)view;
        for(int i=0;i<group.getChildCount();i++) { Button found=find(group.getChildAt(i),title);if(found!=null) return found; }
        return null;
    }
}
