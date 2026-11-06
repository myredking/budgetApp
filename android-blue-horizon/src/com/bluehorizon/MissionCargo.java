package com.bluehorizon;

/** Each consuming objective needs its own cargo; expired assignments release it. */
public final class MissionCargo {
    public static int held(Engine e,int good) {
        if(good<0 || good>=6) return 0;
        int count=story(e,good);AdventureState a=e.adventure;RivalState r=e.rivals;
        if(a.target>=0 && a.good==good && e.day<=a.deadline) count+=a.amount;
        if(r.raceTarget>=0 && r.raceGood==good && e.day<=r.raceDeadline) count+=r.raceAmount;
        return Math.min(e.inventory[good],count);
    }
    private static int story(Engine e,int good) {
        if(good!=storyGood(e.chapter)) return 0;
        return Story.deliveryReady(e,good)?0:5;
    }
    private static int storyGood(int chapter) {
        switch(chapter) { case 0:return 0;case 4:return 3;case 5:return 4;default:return -1; }
    }
}
