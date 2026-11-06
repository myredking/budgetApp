package com.bluehorizon;
import android.app.*;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.util.Locale;
import java.util.function.Consumer;

/** A live, inspectable trade receipt with quantity, budget and maximum modes. */
public final class TradeDialog {
    private final Activity activity;
    private final Engine game;
    private final int good;
    private final boolean selling;
    private final Consumer<Boolean> complete;
    private final AlertDialog dialog;
    private final LinearLayout body;
    private final LinearLayout footer;
    private EditText input;
    private TextView receipt,hint;
    private Button confirm;
    private SeekBar slider;
    private CheckBox reserve;
    private CheckBox protect;
    private TradeOrder quote;
    private int mode,initial;
    private boolean keepReserve=true;
    private boolean keepProtected=true;
    public TradeDialog(Activity owner,Engine state,int item,boolean sale,int quantity,Consumer<Boolean> callback) {
        activity=owner;game=state;good=item;selling=sale;initial=quantity;complete=callback;
        body=column();body.setPadding(dp(18),dp(12),dp(18),dp(12));body.setBackgroundColor(MainActivity.NAVY);
        ScrollView scroll=new ScrollView(activity);scroll.setTag("trade-scroll");scroll.setFillViewport(true);scroll.addView(body);
        LinearLayout frame=column();frame.setBackgroundColor(MainActivity.NAVY);
        frame.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        footer=column();footer.setPadding(dp(18),dp(4),dp(18),dp(8));frame.addView(footer,new LinearLayout.LayoutParams(-1,-2));
        dialog=new AlertDialog.Builder(activity).setView(frame).setNegativeButton("닫기",null).create();
        render();
    }
    public void show() {
        dialog.show();dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        dialog.getWindow().setLayout(-1,-1);
    }
    private void render() {
        body.removeAllViews();
        text(body,"PORT MERCHANT  /  "+(selling?"판매 명세서":"매입 주문서"),12,MainActivity.TEAL);
        TextView title=text(body,Engine.GOODS[good]+" · "+World.PORTS[game.port],26,MainActivity.GOLD);
        title.setTypeface(Typeface.SERIF,Typeface.BOLD);
        text(body,"개당 "+money(selling?game.sellPrice(good):game.buyPrice(good))+"  ·  보유 "+game.inventory[good]+"개",15,MainActivity.INK);
        modeChoices();
        text(body,mode==1?"사용할 예산 (G)":"거래 수량 (개)",14,MainActivity.GOLD);
        input=new EditText(activity);input.setSingleLine();input.setInputType(2);input.setTextColor(MainActivity.INK);
        input.setTag("trade-input");
        input.setTextSize(22);input.setSelectAllOnFocus(true);input.setText(String.valueOf(mode==1?500:initial));
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(13)});
        input.setEnabled(mode!=2);body.addView(input,new LinearLayout.LayoutParams(-1,dp(54)));
        if(mode==0) quantityControls();
        if(mode==1) budgetControls();
        if(!selling) reserveControl();
        if(selling) protectionControl();
        receipt=text(body,"",17,0xff382d21);receipt.setPadding(dp(16),dp(14),dp(16),dp(14));
        receipt.setBackground(shape(0xffefe0bb));
        hint=text(body,"",13,MainActivity.TEAL);
        footer.removeAllViews();confirm=button("거래 확정",true);confirm.setTag("trade-confirm");footer.addView(confirm,params());confirm.setOnClickListener(v->submit());
        input.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int count,int after) { }
            public void onTextChanged(CharSequence s,int start,int before,int count) { refresh(); }
            public void afterTextChanged(Editable value) { }
        });refresh();
    }
    private void modeChoices() {
        LinearLayout choices=row();body.addView(choices);
        choice(choices,"수량 지정",0);if(!selling) choice(choices,"금액 지정",1);
        choice(choices,selling?"전량 판매":"최대 구매",2);
    }
    private void choice(LinearLayout parent,String title,int value) {
        Button button=button(title,mode==value);parent.addView(button,weight());
        button.setOnClickListener(v->{mode=value;render();});
    }
    private void quantityControls() {
        LinearLayout buttons=row();body.addView(buttons);
        for(int step:new int[]{-1,1,5,10}) {
            Button button=button(step>1?step+"개":(step<0?"−":"+"),false);buttons.addView(button,weight());
            button.setOnClickListener(v->{int current=Math.max(1,TradeOrder.number(input.getText().toString()));
                long next=step>1?step:(long)current+step;input.setText(String.valueOf(Math.max(1,Math.min(Integer.MAX_VALUE,next))));});
        }
        slider=new SeekBar(activity);body.addView(slider,new LinearLayout.LayoutParams(-1,dp(42)));
        slider.setContentDescription("거래 수량 조절");
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar view,int progress,boolean user) { if(user) input.setText(String.valueOf(progress+1)); }
            public void onStartTrackingTouch(SeekBar view) { }
            public void onStopTrackingTouch(SeekBar view) { }
        });
    }
    private void budgetControls() {
        LinearLayout buttons=row();body.addView(buttons);
        for(int budget:new int[]{100,500,1000}) {
            Button button=button(budget+" G",false);buttons.addView(button,weight());
            button.setOnClickListener(v->input.setText(String.valueOf(budget)));
        }
    }
    private void reserveControl() {
        reserve=new CheckBox(activity);reserve.setText("비상금 200 G 남기기");reserve.setTextColor(MainActivity.INK);
        reserve.setMinHeight(dp(48));reserve.setChecked(keepReserve);body.addView(reserve);
        reserve.setOnCheckedChangeListener((v,checked)->{keepReserve=checked;refresh();});
    }
    private void protectionControl() {
        protect=new CheckBox(activity);protect.setText("이야기·의뢰·경주용 화물 남기기");protect.setTextColor(MainActivity.INK);
        protect.setMinHeight(dp(48));protect.setChecked(keepProtected);body.addView(protect);
        protect.setOnCheckedChangeListener((v,checked)->{keepProtected=checked;refresh();});
    }
    private void refresh() {
        int value=TradeOrder.number(input.getText().toString());
        quote=TradeOrder.quote(game,good,selling,mode,value,selling?0:(keepReserve?200:0),selling && keepProtected);
        updateControls(value);
        receipt.setText(receiptText());
        hint.setText(quote.valid()?detail(value):quote.error);
        confirm.setEnabled(quote.valid());confirm.setAlpha(quote.valid()?1f:.45f);
        confirm.setText(quote.valid()?(selling?"판매":"매입")+" 확정 · "+money(quote.total):"거래 조건을 확인하세요");
    }
    private void updateControls(int value) {
        if(mode==2 && !input.getText().toString().equals(String.valueOf(quote.quantity))) input.setText(String.valueOf(quote.quantity));
        if(mode==0 && slider!=null) {
            slider.setMax(Math.max(0,quote.maximum-1));slider.setProgress(Math.max(0,Math.min(value,quote.maximum)-1));
        }
    }
    private String receiptText() {
        return (quote.reserved>0?"목표용 보관        "+quote.reserved+"개\n":"")+"수량                  "+quote.quantity+"개\n단가                  "+money(quote.unit)
            +"\n────────────────\n"+(selling?"받는 금액          ":"결제 금액          ")+money(quote.total)
            +(quote.bonusGold>0?"\n이야기 완료 보상   +"+money(quote.bonusGold):"")
            +"\n거래 후 금화       "+money(quote.goldAfter)+"\n화물 공간            "+quote.cargoAfter+" / "+game.capacity();
    }
    private String detail(int value) {
        if(mode==1) return "예산 "+money(value)+" 중 "+money(quote.total)+" 사용 · 남는 예산 "+money(value-quote.total)+"\n금화·비상금·화물 공간을 모두 반영한 정수 수량입니다.";
        return "거래 가능 최대 "+quote.maximum+"개 · 표시 가격에 수수료와 교역 우대를 반영했습니다.";
    }
    private void submit() {
        InputMethodManager keyboard=(InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        keyboard.hideSoftInputFromWindow(input.getWindowToken(),0);
        boolean success=quote.execute(game);dialog.dismiss();complete.accept(success);
    }
    private String money(int value) { return String.format(Locale.KOREA,"%,d G",value); }
    private LinearLayout column() { LinearLayout box=new LinearLayout(activity);box.setOrientation(1);return box; }
    private LinearLayout row() { LinearLayout box=new LinearLayout(activity);box.setOrientation(0);return box; }
    private TextView text(LinearLayout parent,String value,int size,int color) {
        TextView view=new TextView(activity);view.setText(value);view.setTextSize(size);view.setTextColor(color);
        view.setPadding(0,dp(6),0,dp(6));view.setLineSpacing(dp(3),1);parent.addView(view);return view;
    }
    private Button button(String title,boolean active) {
        Button view=new Button(activity);view.setText(title);view.setAllCaps(false);view.setTextSize(13);
        view.setTextColor(active?MainActivity.NAVY:MainActivity.INK);view.setMinHeight(dp(48));view.setMinimumWidth(0);
        view.setPadding(dp(4),dp(4),dp(4),dp(4));view.setBackground(shape(active?MainActivity.GOLD:0xff234151));return view;
    }
    private GradientDrawable shape(int color) {
        GradientDrawable shape=new GradientDrawable();shape.setColor(color);shape.setCornerRadius(dp(10));return shape;
    }
    private LinearLayout.LayoutParams params() {
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.setMargins(dp(2),dp(5),dp(2),dp(5));return params;
    }
    private LinearLayout.LayoutParams weight() { LinearLayout.LayoutParams params=params();params.width=0;params.weight=1;return params; }
    private int dp(int value) { return Math.round(value*activity.getResources().getDisplayMetrics().density); }
}
