package com.deckball;

import java.util.ArrayList;
import java.util.List;

public final class ScoutingDesk {
    private ScoutingDesk() {}
    public static boolean matches(ClubCareer club,GameEngine.Player p,String team,int role) {
        if(!team.equals("전체") && !team.equals(p.team)) return false;
        if(role==1) return !p.position.equals("P");
        if(role==2) return p.position.equals("P");
        if(role==3) return p.legend;
        return role!=4 || club.isFavorite(p.id);
    }
    private static int rating(GameEngine.Player p) {
        return p.position.equals("P")?PlayerSkills.control(p):(PlayerSkills.contact(p,false)+PlayerSkills.power(p))/2;
    }
    public static int gain(ClubCareer club,int id) {
        GameEngine.Player p=club.game.player(id);
        if(p.position.equals("P")) return rating(p)-rating(club.game.player(club.game.pitcher));
        int weakest=110;
        for(int slot:club.game.lineup) weakest=Math.min(weakest,rating(club.game.player(slot)));
        return rating(p)-weakest;
    }
    public static double value(ClubCareer club,int id) { return gain(club,id)/(double)club.price(id); }
    public static List<GameEngine.Player> recommendations(ClubCareer club) {
        List<GameEngine.Player> choices=new ArrayList<>();
        for(GameEngine.Player p:club.game.roster) {
            int fee=club.hasReport(p.id)?0:ClubCareer.SCOUT_FEE;
            if(club.owns(p.id) || club.price(p.id)+fee>club.funds) continue;
            if(gain(club,p.id)>0) choices.add(p);
        }
        choices.sort((a,b)->{
            int order=Double.compare(value(club,b.id),value(club,a.id));
            return order==0?Integer.compare(a.id,b.id):order;
        });
        return choices;
    }
}
