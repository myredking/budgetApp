package com.deckball;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.ArrayList;
import java.util.List;

public final class MainActivity extends Activity {
    final GameEngine engine = new GameEngine(System.nanoTime());
    final ClubScreens office=new ClubScreens(this);
    final Handler handler = new Handler();
    final int bg = Color.rgb(9,20,30), panel = Color.rgb(20,39,52), ink = Color.rgb(236,244,247);
    LinearLayout root, controls, tools;
    BallparkView field;
    TextView status, score;
    boolean playing, automatic, inFlight, completionSaved;
    long pitchStart;
    int kind, location = 4;
    final Runnable ticker = new Runnable() {
        @Override public void run() { tick(); }
    };
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(bg); getWindow().setNavigationBarColor(bg);
        restoreCustom();
        if (!getPreferences(0).contains("deck2026") && getPreferences(0).contains("deck"))
            toast("2026 시즌 명단으로 변경되었습니다. 덱을 다시 선택해 주세요. 커스텀 선수는 유지됩니다.");
        String career=getPreferences(0).getString(CareerSave.KEY, "");
        if(!career.isEmpty() && !CareerSave.restore(engine.career,career)) toast("구단 저장 정보를 복원하지 못했습니다");
        engine.loadDeck(getPreferences(0).getString(GameSettings.DECK_KEY, engine.encodeDeck()));
        restoreRatings();
        home();
    }
    int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    LinearLayout column() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setPadding(20,12,20,12); view.setBackgroundColor(bg);
        return view;
    }
    void screen(String title, String subtitle) {
        if(field!=null) {field.pause();field=null;}
        playing = false; automatic = false; inFlight = false; handler.removeCallbacks(ticker);
        root = column();
        TextView heading = text(title, 28); root.addView(heading);
        root.addView(text(subtitle, 13));
        setContentView(root);
    }
    int railWidth() {
        android.util.DisplayMetrics metrics=getResources().getDisplayMetrics();
        return dp(LayoutPolicy.railWidth(Math.round(metrics.widthPixels/metrics.density)));
    }
    LinearLayout workspace(String title,String subtitle) {
        screen("", "");root.removeAllViews();root.setOrientation(0);
        LinearLayout body=column();root.addView(body,new LinearLayout.LayoutParams(0,-1,1));
        body.addView(text(title,22));body.addView(text(subtitle,12));
        ScrollView scroll=new ScrollView(this);tools=column();scroll.addView(tools);
        root.addView(scroll,new LinearLayout.LayoutParams(railWidth(),-1));return body;
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
        screen("", ""); root.removeAllViews(); root.setOrientation(0);
        field = new BallparkView(this, engine);
        root.addView(field, new LinearLayout.LayoutParams(0,-1,1));
        LinearLayout menu = column();
        ScrollView scroll = new ScrollView(this); scroll.addView(menu);
        root.addView(scroll,new LinearLayout.LayoutParams(railWidth(),-1));
        menu.addView(text("DECK / BALL",26));
        menu.addView(text("2026 SEASON × LEGENDS · 3D",13));
        menu.addView(text("나의 선수, 나의 야구",20));
        menu.addView(button("▶ 경기 시작", () -> { engine.start(); match(); }));
        menu.addView(button("선수 덱 / 타순 편집",this::deck));
        menu.addView(button("구단 운영 / 스카우트",office::show));
        menu.addView(button("능력치 직접 수정",this::editPlayer));
        menu.addView(button("상대 대표 선수 선택",this::opponentDialog));
        menu.addView(button("난이도 · " + difficultyName(),this::difficultyDialog));
        menu.addView(button("선수 기록 / 산정 기준",this::dataInfo));
        menu.addView(button("플레이 방법",this::help));
    }
    void opponentDialog() {
        new AlertDialog.Builder(this).setTitle("상대 대표 선수")
            .setItems(new String[]{"상대 타자 선택","상대 투수 선택"},(d,w) -> opponentChoice(w==1))
            .setNegativeButton("취소",null).show();
    }
    void opponentChoice(boolean pitcher) {
        List<GameEngine.Player> choices = new ArrayList<>(); List<String> labels = new ArrayList<>();
        for (GameEngine.Player p : engine.roster) {
            if (p.position.equals("P") != pitcher) continue;
            choices.add(p); labels.add(p.label());
        }
        new AlertDialog.Builder(this).setTitle("상대 "+(pitcher?"투수":"타자"))
            .setItems(labels.toArray(new String[0]),(d,w) -> {
                int id=choices.get(w).id;
                if(pitcher) engine.selectOpponent(engine.opponentHitter,id);
                else engine.selectOpponent(id,engine.opponentPitcher);
                saveRatings(); toast("상대 선수 선택 완료"); home();
            }).setNegativeButton("취소",null).show();
    }
    String difficultyName() { return new String[]{"쉬움","보통","어려움"}[engine.difficulty]; }
    void difficultyDialog() {
        new AlertDialog.Builder(this).setTitle("난이도 · 공 속도 / 타이밍 허용 범위")
            .setSingleChoiceItems(new String[]{"쉬움 · 느린 공, 넓은 타이밍","보통 · 기본 속도","어려움 · 빠른 공, 좁은 타이밍"},engine.difficulty,
                (d,w) -> { engine.setDifficulty(w); saveRatings(); d.dismiss(); home(); })
            .setNegativeButton("닫기",null).show();
    }
    void dataInfo() {
        new AlertDialog.Builder(this).setTitle("2026 시즌 기록 · 2026.11.04 조회")
            .setMessage("KBO 공식 정규시즌 기록 기반 대표 선수 18명 + 역대 선수 15명. 전체 선수 명단은 아닙니다.\n\n컨택 = 40 + 타율×140, 파워 = 45 + 홈런×1.15, 투수 제구 = 115 - WHIP×25 (1~99).\n\n역대 타격왕은 타율만 반영하며 파워는 공통 65입니다. 이승엽은 2003년 56홈런으로 파워 99, 컨택은 공통 65입니다. 역대 투수는 65+승수로 환산한 게임용 제구입니다. 실제 제구 측정치가 아닙니다.\n\n구단은 해당 기록 시즌 기준. 수정해도 원래 기록은 유지됩니다.\n출처: koreabaseball.com 기록실 / 역대 기록")
            .setPositiveButton("확인",null).show();
    }
    void editPlayer() {
        String[] names = new String[engine.roster.size()];
        for (int i=0;i<names.length;i++) names[i] = engine.player(i).label();
        new AlertDialog.Builder(this).setTitle("능력치를 수정할 선수")
            .setItems(names,(d,w) -> ratingDialog(w)).setNegativeButton("취소",null).show();
    }
    void ratingDialog(int id) {
        GameEngine.Player p = engine.player(id); LinearLayout box = column();
        box.addView(text(p.name + " · " + p.team + " · " + p.year,18));
        box.addView(text(p.record,14));
        box.addView(text(PlayerSkills.describe(p),13));
        box.addView(text("컨택 / 파워 / 제구 · 각 1~99",13));
        EditText contact=input(box,"컨택",true),power=input(box,"파워",true),control=input(box,"제구",true);
        contact.setText(""+p.contact); power.setText(""+p.power); control.setText(""+p.control);
        ScrollView form = new ScrollView(this); form.addView(box);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("능력치 직접 수정").setView(form)
            .setNeutralButton("특수 능력",(d,w)->office.skills(id))
            .setPositiveButton("저장",null).setNegativeButton("취소",null).create();
        dialog.setOnShowListener(v -> dialog.getButton(-1).setOnClickListener(w -> {
            try { engine.editRatings(id,rating(contact),rating(power),rating(control)); saveRatings(); dialog.dismiss(); home(); }
            catch (IllegalArgumentException e) { toast("능력치 1~99를 입력하세요"); }
        })); dialog.show();
    }
    void saveRatings() {
        getPreferences(0).edit().putString(GameSettings.SETTINGS_KEY,GameSettings.capture(engine)).apply();
    }
    void restoreRatings() {
        String saved = getPreferences(0).getString(GameSettings.SETTINGS_KEY, "");
        if (!saved.isEmpty() && !GameSettings.restore(engine,saved)) toast("수정 설정을 복원하지 못했습니다");
    }
    void help() {
        new AlertDialog.Builder(this).setTitle("플레이 방법")
            .setMessage("3이닝 동안 수비와 공격을 번갈아 진행합니다.\n\n타격: '투구 요청'을 누른 뒤 공이 노란 타이밍 구간에 오면 '스윙'을 누르세요. 지켜보기로 볼을 고를 수 있습니다.\n\n투구: 구종과 코스를 고르고 '투구 준비' 후 노란 구간에서 '릴리스'를 누르세요.\n\n감독 모드: 자동 진행을 켜거나 끌 수 있습니다.\n\n덱: 선수를 눌러 교체하고 ↑로 타순을 바꿉니다. 커스텀 선수는 이름과 능력치를 직접 입력하세요. 2026 선수는 KBO 기록에서 능력치를 환산했습니다. 홈의 능력치 직접 수정에서 변경하세요.\n\n경기 도중 나가면 해당 경기는 종료됩니다.")
            .setPositiveButton("확인", null).show();
    }
    void deck() {
        LinearLayout body=workspace("MY LINEUP · 2026",engine.career.active?"구단 운영 · 영입한 선수만 편성":"자유 편성 · 선수 버튼으로 교체 / ↑ 타순 변경");
        ScrollView scroll=new ScrollView(this);LinearLayout list=column();scroll.addView(list);
        body.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        for(int i=0;i<9;i++) addSlot(list,i);
        GameEngine.Player p=engine.player(engine.pitcher);
        list.addView(button("SP  "+p.name+" · "+p.team+" · "+p.year+"  제구 "+p.control,()->choose(-1)));
        tools.addView(text("LINEUP TOOLS",17));
        tools.addView(button("+ 커스텀 선수 만들기",this::customDialog));
        tools.addView(button("덱 저장",()->savedDeck(true)));
        tools.addView(button("덱 불러오기",()->savedDeck(false)));
        tools.addView(button("능력치 / 특수 능력",this::editPlayer));
        tools.addView(button("스카우트 / 영입",office::show));
        tools.addView(button("완료 / 홈",()->{persist();home();}));
    }
    void addSlot(LinearLayout list, int index) {
        GameEngine.Player player = engine.player(engine.lineup[index]);
        LinearLayout row = new LinearLayout(this);
        row.addView(button((index + 1) + "  " + player.name + " '" + player.year + " · " + player.position + "\n컨택 " + player.contact + "  파워 " + player.power,
            () -> choose(index)), new LinearLayout.LayoutParams(0, -2, 1));
        if (index > 0) row.addView(button("↑", () -> { engine.swap(index, index - 1); persist(); deck(); }));
        list.addView(row);
    }
    void choose(int slot) {
        List<GameEngine.Player> choices = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (GameEngine.Player player : engine.roster) {
            if ((slot == -1) != player.position.equals("P")) continue;
            if(engine.career.active && !engine.career.owns(player.id)) continue;
            choices.add(player);
            labels.add(slot == -1 ? player.label() + " · 제구 " + player.control : player.label());
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
                String key = GameSettings.slotKey(slot);
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
    void persist() { getPreferences(0).edit().putString(GameSettings.DECK_KEY,engine.encodeDeck()).apply(); }
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
        screen("", ""); root.removeAllViews(); root.setOrientation(0); playing = true;completionSaved=false;
        field = new BallparkView(this,engine);
        root.addView(field,new LinearLayout.LayoutParams(0,-1,1));
        ScrollView rail = new ScrollView(this); LinearLayout box = column(); rail.addView(box);
        root.addView(rail,new LinearLayout.LayoutParams(railWidth(),-1));
        box.addView(text("DECK / BALL  ’26",18));
        score = text("",15); box.addView(score);
        status = text("",14); box.addView(status);
        controls = new LinearLayout(this); controls.setOrientation(1); box.addView(controls);
        refresh(); handler.postDelayed(ticker,30);
    }
    void restartMatch() {
        engine.start();automatic=false;inFlight=false;completionSaved=false;field.progress=-1;
        refresh();handler.removeCallbacks(ticker);handler.postDelayed(ticker,30);
    }
    void refresh() {
        score.setText("상대 " + engine.away + " : " + engine.home + " 내 팀");
        status.setText(engine.message); controls.removeAllViews();
        if(engine.finished && !completionSaved) {office.save();completionSaved=true;}
        if (engine.finished) {
            controls.addView(button("다시 경기",this::restartMatch));
        } else if (automatic) {
            controls.addView(button("감독 모드 · 자동 진행 중 / 직접 조작으로 전환", () -> { automatic = false; refresh(); }));
        } else manualControls();
        controls.addView(button("카메라 전환",()->field.switchCamera()));
        controls.addView(button("홈으로", this::leaveMatch));
        field.refresh();
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
            options.addView(button(new String[]{"직구","슬라이더","커브"}[kind]+" ▾", () -> {
                if (!inFlight) { kind = (kind + 1) % 3; refresh(); }
            }),new LinearLayout.LayoutParams(0,-2,1));
            options.addView(button(new String[]{"좌상","우상","좌하","우하","중앙"}[location]+" ▾", this::chooseLocation),new LinearLayout.LayoutParams(0,-2,1));
            controls.addView(options);
            controls.addView(button(inFlight ? "릴리스!" : "투구 준비", this::pitchAction));
        }
        controls.addView(button("감독 모드 / 자동 진행", () -> { inFlight = false; field.progress=-1; automatic = true; pitchStart = SystemClock.elapsedRealtime(); refresh(); }));
    }
    void chooseLocation() {
        if (inFlight) return;
        new AlertDialog.Builder(this).setTitle("투구 코스")
            .setItems(new String[]{"좌측 높은 공","우측 높은 공","좌측 낮은 공","우측 낮은 공","중앙"},
                (dialog, which) -> { location = which; refresh(); }).show();
    }
    double progress() { return Math.min(1, (SystemClock.elapsedRealtime() - pitchStart) / (double)engine.pitchDuration()); }
    void beginPitch() { engine.feedback="";engine.lastSkills="";engine.lastHit=0; inFlight = true; pitchStart = SystemClock.elapsedRealtime(); refresh(); }
    void resolve() { if(engine.lastHit>0) field.contact(); inFlight = false; field.progress = -1; refresh(); }
    void batAction() {
        if (!inFlight) { beginPitch(); return; }
        field.swing(); engine.swing(progress()); resolve();
    }
    void pitchAction() {
        if (!inFlight) { beginPitch(); return; }
        engine.pitch(kind,location,progress()); resolve();
    }
    void autoTick() {
        if(!automatic || SystemClock.elapsedRealtime()-pitchStart<=900) return;
        engine.autoPlate();if(engine.lastHit>0) field.contact();pitchStart=SystemClock.elapsedRealtime();refresh();
    }
    void flightTick() {
        if(!inFlight) return;
        field.progress=(float)progress();field.refresh();
        if(progress()<1) return;
        if(engine.batting) engine.take();else engine.pitch(kind,location,1);
        resolve();
    }
    void tick() {
        if(!playing || engine.finished) return;
        autoTick();flightTick();field.refresh();
        if(!engine.finished) handler.postDelayed(ticker,30);
    }
    void leaveMatch() {
        handler.removeCallbacks(ticker);
        final long pausedAt = SystemClock.elapsedRealtime();
        AlertDialog exit = new AlertDialog.Builder(this).setTitle("경기를 종료할까요?")
            .setMessage("덱은 보관되며 진행 중인 경기는 종료됩니다.")
            .setPositiveButton("홈으로",(d,w) -> home()).setNegativeButton("계속 경기",null).create();
        exit.setOnDismissListener(d -> {
            pitchStart += SystemClock.elapsedRealtime() - pausedAt;
            if (playing) { handler.removeCallbacks(ticker); handler.postDelayed(ticker,30); }
        });
        exit.show();
    }
    void toast(String message) { Toast.makeText(this,message,Toast.LENGTH_SHORT).show(); }
    @Override public void onBackPressed() { if (playing) leaveMatch(); else home(); }
    @Override protected void onPause() {
        super.onPause(); handler.removeCallbacks(ticker); persist(); saveRatings(); office.save();
        inFlight = false; if (field != null) {field.progress = -1;field.pause();}
    }
    @Override protected void onResume() {
        super.onResume();
        if(field!=null) field.resume();
        if (playing) { refresh(); handler.removeCallbacks(ticker); handler.postDelayed(ticker,30); }
    }
}
