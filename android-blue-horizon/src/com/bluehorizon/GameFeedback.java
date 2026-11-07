package com.bluehorizon;
public final class GameFeedback {
    public static String battle(boolean success,int choice,boolean active,boolean won) {
        if(!success) return "error";
        if(won) return "victory";
        if(!active) return choice==2?"retreat":"defeat";
        return choice==1?"defend":"cannon";
    }
}
