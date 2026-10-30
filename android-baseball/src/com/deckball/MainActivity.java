package com.deckball;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.ArrayList;
import java.util.List;

public final class MainActivity extends Activity {
    final GameEngine engine = new GameEngine(System.nanoTime());
    final Handler handler = new Handler();
    final int bg = Color.rgb(9,20,30), panel = Color.rgb(20,39,52), ink = Color.rgb(236,244,247);
    LinearLayout root, controls;
    FieldView field;
    TextView status, score;
    boolean playing, automatic, inFlight;
    long pitchStart;
    int kind, location = 4;
    final Runnable ticker = new Runnable() {
        @Override public void run() { tick(); }
    };
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(bg); getWindow().setNavigationBarColor(bg);
        restoreCustom();
        engine.loadDeck(getPreferences(0).getString("deck", engine.encodeDeck()));
        home();
    }
    LinearLayout column() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setPadding(20,12,20,12); view.setBackgroundColor(bg);
        return view;
    }
    void screen(String title, String subtitle) {
        playing = false; automatic = false; inFlight = false; handler.removeCallbacks(ticker);
        root = column();
        TextView heading = text(title, 28); root.addView(heading);
        root.addView(text(subtitle, 13));
        setContentView(root);
    }
    TextView text(String value, int size) {
        TextView view = new TextView(this); view.setText(value); view.setTextSize(size);
        view.setTextColor(ink); view.setPadding(4,8,4,8);
        return view;
    }
    Button button(String label, Runnable action) {
        Button view = new Button(this); view.setText(label); view.setAllCaps(false);
        view.setTextColor(ink); view.setTextSize(15); view.setBackgroundTintList(android.content.res.ColorStateList.valueOf(panel));
        view.setOnClickListener(v -> action.run());
        return view;
    }
    void addButton(String label, Runnable action) { root.addView(button(label, action)); }
    void home() {
        screen("DECK / BALL", "나의 선수. 나의 라인업. 나의 야구.");
        root.addView(text("OFFLINE BASEBALL  /  3 INNINGS", 12));
        FieldView preview = new FieldView(this, engine);
        root.addView(preview, new LinearLayout.LayoutParams(-1,0,1));
        root.addView(text("자유롭게 구성하고\n직접 승부하세요.", 27));
        addButton("▶  나의 덱으로 경기 시작", () -> { engine.start(); match(); });
        addButton("선수 덱 / 타순 편집", this::deck);
        addButton("플레이 방법", this::help);
        root.addView(text("독립 제작 2D 프로토타입 · 기본 선수는 가상 데이터", 11));
    }
    void help() {
        new AlertDialog.Builder(this).setTitle("플레이 방법")
            .setMessage("3이닝 동안 수비와 공격을 번갈아 진행합니다.\n\n타격: '투구 요청'을 누른 뒤 공이 노란 타이밍 구간에 오면 '스윙'을 누르세요. 지켜보기로 볼을 고를 수 있습니다.\n\n투구: 구종과 코스를 고르고 '투구 준비' 후 노란 구간에서 '릴리스'를 누르세요.\n\n감독 모드: 자동 진행을 켜거나 끌 수 있습니다.\n\n덱: 선수를 눌러 교체하고 ↑로 타순을 바꿉니다. 커스텀 선수는 이름과 능력치를 직접 입력하세요. 기본 선수와 능력치는 실제 기록이 아닙니다.\n\n경기 도중 나가면 해당 경기는 종료됩니다.")
            .setPositiveButton("확인", null).show();
    }
    void deck() {
        screen("MY LINEUP", "타자 9명 + 선발투수 1명 · 포지션 자유 편성");
        ScrollView scroll = new ScrollView(this); LinearLayout list = column();
        scroll.addView(list); root.addView(scroll, new LinearLayout.LayoutParams(-1,0,1));
        for (int i = 0; i < 9; i++) addSlot(list, i);
        GameEngine.Player p = engine.player(engine.pitcher);
        list.addView(button("SP  " + p.name + "  제구 " + p.control, () -> choose(-1)));
        addButton("+  커스텀 선수 만들기", this::customDialog);
        LinearLayout saves = new LinearLayout(this);
        saves.addView(button("덱 저장", () -> savedDeck(true)), new LinearLayout.LayoutParams(0,-2,1));
        saves.addView(button("덱 불러오기", () -> savedDeck(false)), new LinearLayout.LayoutParams(0,-2,1));
        root.addView(saves); addButton("완료 / 홈", () -> { persist(); home(); });
    }
    void addSlot(LinearLayout list, int index) {
        GameEngine.Player player = engine.player(engine.lineup[index]);
        LinearLayout row = new LinearLayout(this);
        row.addView(button((index + 1) + "  " + player.name + " · " + player.position + "\n컨택 " + player.contact + "  파워 " + player.power,
            () -> choose(index)), new LinearLayout.LayoutParams(0, -2, 1));
        if (index > 0) row.addView(button("↑", () -> { engine.swap(index, index - 1); persist(); deck(); }));
        list.addView(row);
    }
    void choose(int slot) {
        List<GameEngine.Player> choices = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (GameEngine.Player player : engine.roster) {
            if ((slot == -1) != player.position.equals("P")) continue;
            choices.add(player);
            labels.add(slot == -1 ? player.name + " · " + player.team + " · 제구 " + player.control : player.label());
        }
        new AlertDialog.Builder(this).setTitle(slot == -1 ? "선발투수 선택" : (slot + 1) + "번 타자 선택")
            .setItems(labels.toArray(new String[0]), (dialog, which) -> {
                try {
                    if (slot == -1) engine.selectPitcher(choices.get(which).id);
                    else engine.select(slot, choices.get(which).id);
                    persist(); deck();
                } catch (IllegalArgumentException e) { toast(e.getMessage()); }
            }).setNegativeButton("취소", null).show();
    }
    void savedDeck(boolean save) {
        new AlertDialog.Builder(this).setTitle(save ? "저장할 슬롯" : "불러올 슬롯")
            .setItems(new String[]{"DECK 01", "DECK 02", "DECK 03"}, (dialog, slot) -> {
                String key = "slot" + slot;
                if (save) { getPreferences(0).edit().putString(key, engine.encodeDeck()).apply(); toast("덱을 저장했습니다"); }
                else if (engine.loadDeck(getPreferences(0).getString(key, ""))) { persist(); deck(); }
                else toast("저장된 덱이 없거나 읽을 수 없습니다");
            }).show();
    }
    EditText input(LinearLayout box, String hint, boolean number) {
        EditText value = new EditText(this); value.setHint(hint); value.setTextColor(ink);
        value.setHintTextColor(Color.LTGRAY); value.setSingleLine(true);
        if (number) value.setInputType(2);
        box.addView(value); return value;
    }
    void customDialog() {
        LinearLayout box = column();
        EditText name = input(box,"선수 이름",false);
        EditText team = input(box,"구단 / 연도 (예: 부산 1992)",false);
        Spinner pos = new Spinner(this);
        pos.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item,
            new String[]{"C","1B","2B","3B","SS","LF","CF","RF","DH","P"}));
        box.addView(pos);
        EditText contact = input(box,"컨택 1~99",true);
        EditText power = input(box,"파워 1~99",true);
        EditText control = input(box,"제구 1~99",true);
        ScrollView form = new ScrollView(this); form.addView(box);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("나만의 선수")
            .setView(form).setPositiveButton("선수 추가",null).setNegativeButton("취소",null).create();
        dialog.setOnShowListener(v -> dialog.getButton(-1).setOnClickListener(w -> {
            try {
                String playerName = name.getText().toString().trim();
                String teamName = team.getText().toString().trim();
                if (playerName.isEmpty() || teamName.isEmpty()) throw new IllegalArgumentException("이름과 구단을 입력하세요");
                engine.roster.add(new GameEngine.Player(engine.roster.size(),playerName,pos.getSelectedItem().toString(),teamName,
                    rating(contact),rating(power),rating(control)));
                saveCustom(); dialog.dismiss(); deck(); toast("선수 목록에서 새 선수를 선택하세요");
            } catch (IllegalArgumentException e) { toast("이름·구단과 능력치 1~99를 확인하세요"); }
        }));
        dialog.show();
    }
    int rating(EditText input) {
        int rating = Integer.parseInt(input.getText().toString());
        if (rating < 1 || rating > 99) throw new IllegalArgumentException();
        return rating;
    }
    void persist() { getPreferences(0).edit().putString("deck",engine.encodeDeck()).apply(); }
    void saveCustom() {
        org.json.JSONArray data = new org.json.JSONArray();
        for (int i = 33; i < engine.roster.size(); i++) {
            GameEngine.Player p = engine.player(i);
            org.json.JSONArray row = new org.json.JSONArray();
            row.put(p.name).put(p.position).put(p.team).put(p.contact).put(p.power).put(p.control); data.put(row);
        }
        getPreferences(0).edit().putString("custom",data.toString()).apply();
    }
    void restoreCustom() {
        try {
            org.json.JSONArray data = new org.json.JSONArray(getPreferences(0).getString("custom","[]"));
            for (int i = 0; i < data.length(); i++) {
                org.json.JSONArray p = data.getJSONArray(i);
                engine.roster.add(new GameEngine.Player(engine.roster.size(),p.getString(0),p.getString(1),p.getString(2),p.getInt(3),p.getInt(4),p.getInt(5)));
            }
        } catch (org.json.JSONException e) { toast("커스텀 선수 데이터를 읽을 수 없습니다"); }
    }
    void match() {
        screen("DECK / BALL", "EXHIBITION  /  3 INNINGS"); playing = true;
        score = text("",22); score.setGravity(Gravity.CENTER); root.addView(score);
        field = new FieldView(this,engine); root.addView(field,new LinearLayout.LayoutParams(-1,0,1));
        status = text("",17); status.setGravity(Gravity.CENTER); root.addView(status);
        controls = new LinearLayout(this); controls.setOrientation(1); root.addView(controls);
        refresh(); handler.postDelayed(ticker,30);
    }
    void refresh() {
        score.setText("VISITOR  " + engine.away + "   :   " + engine.home + "  MY DECK");
        status.setText(engine.message); controls.removeAllViews();
        if (engine.finished) {
            controls.addView(button("다시 경기", () -> { engine.start(); refresh(); }));
        } else if (automatic) {
            controls.addView(button("감독 모드 · 자동 진행 중 / 직접 조작으로 전환", () -> { automatic = false; refresh(); }));
        } else manualControls();
        controls.addView(button("홈으로", this::leaveMatch));
        field.invalidate();
    }
    void manualControls() {
        if (engine.batting) {
            controls.addView(button(inFlight ? "스윙!" : "투구 요청", this::batAction));
            controls.addView(button("지켜보기", () -> {
                if (!inFlight) { toast("먼저 투구를 요청하세요"); return; }
                engine.take(); resolve();
            }));
        } else {
            LinearLayout options = new LinearLayout(this);
            options.addView(button("구종: " + new String[]{"직구","슬라이더","커브"}[kind], () -> {
                if (!inFlight) { kind = (kind + 1) % 3; refresh(); }
            }),new LinearLayout.LayoutParams(0,-2,1));
            options.addView(button("코스: " + new String[]{"좌상","우상","좌하","우하","중앙"}[location], this::chooseLocation),new LinearLayout.LayoutParams(0,-2,1));
            controls.addView(options);
            controls.addView(button(inFlight ? "릴리스!" : "투구 준비", this::pitchAction));
        }
        controls.addView(button("감독 모드 / 자동 진행", () -> { inFlight = false; automatic = true; pitchStart = System.currentTimeMillis(); refresh(); }));
    }
    void chooseLocation() {
        if (inFlight) return;
        new AlertDialog.Builder(this).setTitle("투구 코스")
            .setItems(new String[]{"좌측 높은 공","우측 높은 공","좌측 낮은 공","우측 낮은 공","중앙"},
                (dialog, which) -> { location = which; refresh(); }).show();
    }
    double progress() { return Math.min(1, (System.currentTimeMillis() - pitchStart) / 1700.0); }
    void beginPitch() { inFlight = true; pitchStart = System.currentTimeMillis(); refresh(); }
    void resolve() { inFlight = false; field.progress = -1; refresh(); }
    void batAction() {
        if (!inFlight) { beginPitch(); return; }
        engine.swing(progress()); resolve();
    }
    void pitchAction() {
        if (!inFlight) { beginPitch(); return; }
        engine.pitch(kind,location,progress()); resolve();
    }
    void tick() {
        if (!playing) return;
        if (automatic && !engine.finished && System.currentTimeMillis() - pitchStart > 900) {
            engine.autoPlate(); pitchStart = System.currentTimeMillis(); refresh();
        }
        if (inFlight) {
            field.progress = (float)progress(); field.invalidate();
            if (progress() >= 1) {
                if (engine.batting) engine.take(); else engine.pitch(kind,location,1);
                resolve();
            }
        }
        handler.postDelayed(ticker,30);
    }
    void leaveMatch() {
        handler.removeCallbacks(ticker);
        final long pausedAt = System.currentTimeMillis();
        AlertDialog exit = new AlertDialog.Builder(this).setTitle("경기를 종료할까요?")
            .setMessage("덱은 보관되며 진행 중인 경기는 종료됩니다.")
            .setPositiveButton("홈으로",(d,w) -> home()).setNegativeButton("계속 경기",null).create();
        exit.setOnDismissListener(d -> {
            pitchStart += System.currentTimeMillis() - pausedAt;
            if (playing) { handler.removeCallbacks(ticker); handler.postDelayed(ticker,30); }
        });
        exit.show();
    }
    void toast(String message) { Toast.makeText(this,message,Toast.LENGTH_SHORT).show(); }
    @Override public void onBackPressed() { if (playing) leaveMatch(); else home(); }
    @Override protected void onPause() {
        super.onPause(); handler.removeCallbacks(ticker); persist();
        inFlight = false; if (field != null) field.progress = -1;
    }
    @Override protected void onResume() {
        super.onResume();
        if (playing) { refresh(); handler.removeCallbacks(ticker); handler.postDelayed(ticker,30); }
    }
}
