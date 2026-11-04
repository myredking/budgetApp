package com.deckball;

public final class ClubTest {
    private ClubTest() {}
    static void check(boolean value,String message) { EngineTest.check(value,message); }
    static void reject(Runnable action) { EngineTest.expectReject(action); }
    public static void run() { skills(); scouting(); careerSave(); invalidInputs(); }
    static void skills() {
        GameEngine game=new GameEngine(1); GameEngine.Player p=game.player(0);
        game.assignSkills(0,PlayerSkills.CLUTCH);
        int base=game.contactRating(p);
        game.bases[1]=true;
        check(game.contactRating(p)>base,"Clutch only with scoring runner");
        game.assignSkills(0,PlayerSkills.CONTACT | PlayerSkills.POWER);
        check(game.powerRating(p)>p.power,"Power skill changes effective rating");
        reject(() -> game.assignSkills(0,31));
        reject(() -> game.assignSkills(0,PlayerSkills.ACE));
        reject(() -> game.assignSkills(game.pitcher,PlayerSkills.POWER));
        GameEngine restored=new GameEngine(2);
        check(GameSettings.restore(restored,GameSettings.capture(game)),"Skills settings restore");
        check(restored.player(0).skills==p.skills,"Skill mask preserved");
    }
    static void scouting() {
        GameEngine game=new GameEngine(1); ClubCareer club=game.career;
        club.activate(); int initial=club.funds;
        reject(() -> game.select(0,12));
        club.scout(12);
        check(club.funds==initial-ClubCareer.SCOUT_FEE,"Scouting fee charged once");
        reject(() -> club.scout(12));
        reject(() -> club.sign(12));
        club.advanceDay(); check(!club.ready(12),"Scouting takes two days");
        club.advanceDay(); check(club.ready(12),"Report completes");
        check(club.report(12).contains("이만수"),"Report player identity");
        int before=club.funds,price=club.price(12);
        club.sign(12); check(club.owns(12) && club.funds==before-price,"Sign player");
        reject(() -> club.sign(12)); game.select(0,12);
        int[] saved=game.lineup.clone();
        check(!game.loadDeck("27,10,1,2,3,4,5,6,7,8"),"Unsigned deck blocked");
        check(java.util.Arrays.equals(saved,game.lineup),"Rejected deck atomic");
        club.funds=0; reject(() -> club.scout(13));
        check(!club.hasReport(13),"No report without funds");
    }
    static void careerSave() {
        GameEngine game=new GameEngine(1); game.career.activate(); game.career.scout(12);
        game.career.advanceDay(); String saved=CareerSave.capture(game.career);
        GameEngine restored=new GameEngine(2);
        check(CareerSave.restore(restored.career,saved),"Career restore");
        check(restored.career.day==2 && restored.career.active,"Career fields persist");
        check(restored.career.hasReport(12) && !restored.career.ready(12),"Scouting task persists");
        check(!CareerSave.restore(restored.career,saved+";broken"),"Corrupt career rejected");
        check(saved.equals(CareerSave.capture(restored.career)),"Career restore atomic");
        game.start(); while(!game.finished) game.out();
        int reward=game.career.funds; game.out();
        check(reward==game.career.funds,"No duplicate match reward");
    }
    static void invalidInputs() {
        GameEngine game=new GameEngine(1); game.start();
        reject(() -> game.hit(0)); reject(() -> game.hit(5));
        reject(() -> game.pitch(99,0,.65)); reject(() -> game.pitch(0,99,.65));
        reject(() -> game.pitch(0,0,Double.NaN));
        game.batting=true; reject(() -> game.swing(Double.POSITIVE_INFINITY));
        check(game.home==0 && game.away==0,"Rejected actions do not score");
        check(!game.loadDeck(null),"Null deck rejected");
        check(!GameSettings.restore(game,null),"Null settings rejected");
    }
}
