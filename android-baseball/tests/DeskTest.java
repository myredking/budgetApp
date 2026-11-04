package com.deckball;

public final class DeskTest {
    private DeskTest() {}
    public static void run() {
        GameEngine game=new GameEngine(1);ClubCareer club=game.career;club.activate();
        club.setFavorite(12,true);
        EngineTest.check(ScoutingDesk.matches(club,game.player(12),"전체",4),"Shortlist filter");
        EngineTest.check(!ScoutingDesk.matches(club,game.player(13),"전체",4),"Nonshortlisted hidden");
        EngineTest.check(ScoutingDesk.matches(club,game.player(29),"해태",2),"Team/pitcher filter");
        String saved=CareerSave.capture(club);GameEngine restored=new GameEngine(2);
        EngineTest.check(CareerSave.restore(restored.career,saved),"Shortlist save accepted");
        EngineTest.check(restored.career.isFavorite(12),"Shortlist survives restore");
        String legacy=saved.substring(0,saved.lastIndexOf(';'));
        EngineTest.check(CareerSave.restore(restored.career,legacy),"Older career save accepted");
        EngineTest.check(!restored.career.isFavorite(12),"Older save starts with empty shortlist");
        club.setFavorite(12,false);EngineTest.check(!club.isFavorite(12),"Remove shortlist");
        java.util.List<GameEngine.Player> picks=ScoutingDesk.recommendations(club);
        EngineTest.check(!picks.isEmpty(),"Affordable improvements recommended");
        double previous=Double.POSITIVE_INFINITY;
        for(GameEngine.Player p:picks) {
            EngineTest.check(!club.owns(p.id),"Never recommend owned player");
            EngineTest.check(ScoutingDesk.gain(club,p.id)>0,"Recommendation improves team");
            EngineTest.check(club.price(p.id)+ClubCareer.SCOUT_FEE<=club.funds,"Include investigation fee in budget");
            double value=ScoutingDesk.value(club,p.id);
            EngineTest.check(value<=previous,"Sort recommendations by improvement per cost");previous=value;
        }
        club.funds=0;EngineTest.check(ScoutingDesk.recommendations(club).isEmpty(),"No unaffordable recommendations");
        for(int width:new int[]{480,640,800,1280}) {
            int rail=LayoutPolicy.railWidth(width);
            EngineTest.check(rail>=150 && rail<=260,"Readable action rail");
            EngineTest.check(width-rail>=width*.6,"Reserve most width for player list");
        }
    }
}
