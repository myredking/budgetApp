package com.bluehorizon;

/** Voyage effects commit before presentation; arrival/skip only clears the UI intent. */
public final class TravelFlow {
    public static boolean begin(Engine e,int target,int mode) {
        if(e.town.moving()) return Adventure.fail(e,"이미 항해 중입니다.");
        int from=e.port,day=e.day,chapter=e.chapter;
        if(!Adventure.sail(e,target,mode)) return false;
        e.town.from=from;e.town.startDay=day;e.town.previousChapter=chapter;
        e.town.morale=Math.max(0,e.town.morale-Math.min(15,3+(e.day-day)/10));
        return true;
    }
    public static int arrive(Engine e) {
        if(!e.town.moving()) return -1;
        int previous=e.town.previousChapter;e.town.clearJourney();return previous;
    }
}
