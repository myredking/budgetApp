package com.deckball;

import java.util.ArrayList;
import java.util.List;

public final class GameSettings {
    public static final String DECK_KEY = "deck2026";
    public static final String SETTINGS_KEY = "settings2026v1";
    private GameSettings() {}
    public static String slotKey(int slot) { return "slot2026_" + slot; }
    public static String capture(GameEngine game) {
        StringBuilder text = new StringBuilder(game.difficulty + "," + game.opponentHitter + "," + game.opponentPitcher);
        for (GameEngine.Player p : game.roster)
            text.append(';').append(p.id).append(',').append(p.contact).append(',').append(p.power).append(',').append(p.control);
        return text.toString();
    }
    public static boolean restore(GameEngine game, String text) {
        try {
            String[] entries = text.split(";");
            int[] header = numbers(entries[0],3);
            GameEngine validation = new GameEngine(0);
            validation.roster.clear(); validation.roster.addAll(game.roster);
            validation.setDifficulty(header[0]); validation.selectOpponent(header[1],header[2]);
            List<int[]> rows = ratingRows(game,entries);
            for (int[] row : rows) game.editRatings(row[0],row[1],row[2],row[3]);
            game.setDifficulty(header[0]); game.selectOpponent(header[1],header[2]);
            return true;
        } catch (IllegalArgumentException e) { return false; }
    }
    private static int[] numbers(String value, int count) {
        String[] parts=value.split(",");
        if(parts.length!=count) throw new IllegalArgumentException("잘못된 설정");
        int[] values=new int[count];
        for(int i=0;i<count;i++) values[i]=Integer.parseInt(parts[i]);
        return values;
    }
    private static List<int[]> ratingRows(GameEngine game, String[] entries) {
        List<int[]> rows=new ArrayList<>();
        for(int i=1;i<entries.length;i++) {
            int[] row=numbers(entries[i],4); game.player(row[0]);
            for(int j=1;j<4;j++) {
                if(row[j]<1 || row[j]>99) throw new IllegalArgumentException("잘못된 능력치");
            }
            rows.add(row);
        }
        return rows;
    }
}
