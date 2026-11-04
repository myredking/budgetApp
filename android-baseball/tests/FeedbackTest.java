package com.deckball;

public final class FeedbackTest {
    private FeedbackTest() {}
    public static void run() {
        EngineTest.check(TimingFeedback.grade(.78,.78,.07).startsWith("PERFECT"),"Perfect timing feedback");
        EngineTest.check(TimingFeedback.grade(.83,.78,.07).startsWith("GOOD"),"Good timing feedback");
        EngineTest.check(TimingFeedback.grade(.4,.78,.07).startsWith("EARLY"),"Early timing feedback");
        EngineTest.check(TimingFeedback.grade(.95,.78,.07).startsWith("LATE"),"Late timing feedback");
        EngineTest.expectReject(()->TimingFeedback.grade(Double.NaN,.78,.07));
        GameEngine game=new GameEngine(1);game.start();game.batting=true;
        game.assignSkills(0,PlayerSkills.CLUTCH);game.bases[1]=true;
        EngineTest.check(PlayerSkills.activeDescription(game.player(0),true).contains("해결사"),"Clutch active");
        EngineTest.check(!PlayerSkills.activeDescription(game.player(0),false).contains("해결사"),"Clutch waits without scoring runner");
        game.swing(.78);
        EngineTest.check(game.feedback.startsWith("PERFECT"),"Action records timing grade");
        EngineTest.check(game.lastSkills.contains("해결사"),"Capture active skills before runners advance");
        game.start();EngineTest.check(game.feedback.isEmpty() && game.lastSkills.isEmpty(),"Restart clears feedback");
        game.pitch(0,0,.2);EngineTest.check(game.feedback.startsWith("EARLY"),"Pitch release feedback");
    }
}
