package com.deckball;

public final class TeamPalette {
    private static final String[] TEAMS={"KIA","해태","삼성","LG","MBC","한화","빙그레","SSG","NC","키움","현대","KT"};
    private static final int[] COLORS={0xa72d3b,0xa72d3b,0x1452a0,0x8e244e,0x8e244e,0xd96527,0xd96527,0xa72a3b,0x243e61,0x68243c,0x68243c,0x30333a};
    private TeamPalette() {}
    public static int color(String team) {
        for(int i=0;i<TEAMS.length;i++) if(TEAMS[i].equals(team)) return COLORS[i];
        return 0x1d3049;
    }
}
