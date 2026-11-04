package com.deckball;

import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.Map;

public final class CareerSave {
    public static final String KEY="career2026v1";
    private CareerSave() {}
    public static String capture(ClubCareer club) {
        StringBuilder text=new StringBuilder((club.active?1:0)+","+club.day+","+club.funds+";");
        for(int id:club.signed) text.append(id).append(',');
        text.append(';');
        for(Map.Entry<Integer,Integer> entry:club.reports.entrySet()) text.append(entry.getKey()).append(',').append(entry.getValue()).append('|');
        text.append(';');for(int id:club.favorites) text.append(id).append(',');
        return text.toString();
    }
    public static boolean restore(ClubCareer club,String saved) {
        if(saved==null) return false;
        try {
            String[] parts=saved.split(";",-1);
            if(parts.length!=3 && parts.length!=4) return false;
            int[] header=header(parts[0]);
            int active=header[0],day=header[1],funds=header[2];
            Set<Integer> signed=owned(club,parts[1]);
            Map<Integer,Integer> reports=tasks(club,parts[2]);
            Set<Integer> favorites=favorites(club,parts.length==4?parts[3]:"");
            club.active=active==1; club.day=day; club.funds=funds;
            club.signed.clear(); club.signed.addAll(signed); club.reports.clear(); club.reports.putAll(reports);
            club.favorites.clear();club.favorites.addAll(favorites);
            return true;
        } catch(IllegalArgumentException e) { return false; }
    }
    private static int[] header(String text) {
        String[] parts=text.split(",",-1);
        if(parts.length!=3) throw new IllegalArgumentException("구단 저장 헤더 오류");
        int active=Integer.parseInt(parts[0]),day=Integer.parseInt(parts[1]),funds=Integer.parseInt(parts[2]);
        if(active<0 || active>1 || day<1 || funds<0) throw new IllegalArgumentException("구단 저장 값 오류");
        return new int[]{active,day,funds};
    }
    private static Set<Integer> favorites(ClubCareer club,String text) {
        Set<Integer> values=new LinkedHashSet<>();
        if(text.isEmpty()) return values;
        for(String part:text.split(",")) {
            int id=Integer.parseInt(part);club.game.player(id);values.add(id);
        }
        return values;
    }
    private static Set<Integer> owned(ClubCareer club,String text) {
        Set<Integer> values=new LinkedHashSet<>();
        for(String part:text.split(",")) {
            int id=Integer.parseInt(part); club.game.player(id); values.add(id);
        }
        for(int i=0;i<9;i++) if(!values.contains(i)) throw new IllegalArgumentException("기본 선수 누락");
        if(!values.contains(27)) throw new IllegalArgumentException("기본 투수 누락");
        return values;
    }
    private static Map<Integer,Integer> tasks(ClubCareer club,String text) {
        Map<Integer,Integer> values=new LinkedHashMap<>();
        if(text.isEmpty()) return values;
        for(String part:text.split("\\|")) {
            String[] pair=part.split(",",-1);
            if(pair.length!=2) throw new IllegalArgumentException("조사 작업 오류");
            int id=Integer.parseInt(pair[0]),day=Integer.parseInt(pair[1]); club.game.player(id);
            if(day<1 || values.containsKey(id)) throw new IllegalArgumentException("조사 기한 오류");
            values.put(id,day);
        }
        return values;
    }
}
