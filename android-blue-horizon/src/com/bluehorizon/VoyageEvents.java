package com.bluehorizon;

public final class VoyageEvents {
    public static boolean choose(Engine e,int choice) {
        if(!validChoice(e,choice)) return Adventure.fail(e,"진행 중인 사건과 선택을 확인하세요.");
        switch(e.adventure.pending) {
            case 0:return storm(e,choice);
            case 1:return pirates(e,choice);
            case 2:return wreck(e,choice);
            case 3:return rescue(e,choice);
            case 4:return festival(e,choice);
            case 5:return deal(e,choice);
            case 6:return winds(e,choice);
            default:return island(e,choice);
        }
    }
    private static boolean validChoice(Engine e,int choice) { return choice>=0 && choice<=2 && e.adventure.pending>=0; }
    private static boolean storm(Engine e,int choice) {
        if(choice==0) { Adventure.damage(e,4);e.day++;return Adventure.finish(e,"돛을 줄여 폭풍을 견뎠다. 선체 −4, 하루 경과."); }
        if(choice==1) {
            if(e.adventure.roll<65) { Adventure.damage(e,1);e.fame+=2;return Adventure.finish(e,"파도 사이의 길을 찾았다! 선체 −1, 명성 +2."); }
            Adventure.damage(e,16);Adventure.takeCargo(e);return Adventure.finish(e,"높은 파도가 갑판을 덮었다. 선체 −16, 보유 화물 1개 유실.");
        }
        if(!Adventure.spend(e,40)) return false;
        e.day+=2;return Adventure.finish(e,"안전한 만에서 바람을 기다렸다. 보급 −40 G, 이틀 경과.");
    }
    private static boolean pirates(Engine e,int choice) {
        if(e.adventure.battleHP>0) return SeaBattle.choose(e,choice);
        if(choice==0) {
            e.adventure.battleHP=30+e.level*8+World.region(e.port)*2;
            e.note("해상전 시작. 포격·회피 사격·도주 중 전술을 선택하세요.");return true;
        }
        if(choice==1) {
            int toll=80+World.region(e.port)*20;if(!Adventure.spend(e,toll)) return false;
            return Adventure.finish(e,"교섭으로 충돌을 피했다. 통행료 −"+toll+" G.");
        }
        Adventure.damage(e,6);return Adventure.finish(e,"안개 속으로 항로를 바꿔 해적을 따돌렸다. 선체 −6.");
    }
    private static boolean wreck(Engine e,int choice) {
        if(choice==0) {
            e.gold+=120;e.day+=2;Adventure.damage(e,3);return Adventure.finish(e,"난파선 갑판에서 물자를 건졌다. +120 G, 선체 −3, 이틀 경과.");
        }
        if(choice==1) {
            if(e.adventure.roll<CrewAbilities.explorationChance(e,60)) {
                Adventure.souvenir(e);e.gold+=250;e.fame+=4;Adventure.damage(e,5);
                return Adventure.finish(e,"잠긴 선실에서 해역의 기념품을 발견했다! +250 G, 명성 +4, 선체 −5.");
            }
            Adventure.damage(e,12);return Adventure.finish(e,"거센 물살 때문에 수색을 중단했다. 선체 −12.");
        }
        e.fame++;return Adventure.finish(e,"난파선 위치를 항해자들에게 알렸다. 명성 +1.");
    }
    private static boolean rescue(Engine e,int choice) {
        if(choice==0) {
            if(!Adventure.spend(e,50)) return false;
            e.fame+=6;e.adventure.rescues++;return Adventure.finish(e,"조난자를 구조하고 보급을 나눴다. −50 G, 명성 +6.");
        }
        if(choice==1) {
            e.day+=2;Adventure.damage(e,4);e.fame+=4;e.adventure.rescues++;
            return Adventure.finish(e,"조난선을 항구까지 예인했다. 이틀 경과, 선체 −4, 명성 +4.");
        }
        e.fame++;return Adventure.finish(e,"근처 구조선에 위치를 전했다. 명성 +1.");
    }
    private static boolean festival(Engine e,int choice) {
        if(choice==0) {
            if(!Adventure.takeCargo(e)) return Adventure.fail(e,"기증할 화물이 없습니다. 다른 방식으로 축제에 참여할 수 있어요.");
            e.fame+=5;e.gold+=100;return Adventure.finish(e,"화물 1개를 축제에 기증했다. 감사 선물 +100 G, 명성 +5.");
        }
        if(choice==1) { e.day++;e.fame+=2;return Adventure.finish(e,"광장에서 항해 이야기를 나눴다. 하루 경과, 명성 +2."); }
        e.gold+=60;return Adventure.finish(e,"항구의 환영 선물을 받았다. +60 G.");
    }
    private static boolean deal(Engine e,int choice) {
        if(choice==0) {
            if(!Adventure.spend(e,120)) return false;
            int value=e.adventure.roll<60?300:20;e.gold+=value;
            return Adventure.finish(e,"밀봉 상자를 열었다. 구입 −120 G, 발견한 물건의 가치 +"+value+" G.");
        }
        if(choice==1) { e.day+=2;e.gold+=80;e.fame+=2;return Adventure.finish(e,"서류를 확인해 정식 거래를 도왔다. 이틀 경과, +80 G, 명성 +2."); }
        e.fame++;return Adventure.finish(e,"수상한 제안을 정중히 거절했다. 명성 +1.");
    }
    private static boolean winds(Engine e,int choice) {
        if(choice==0) { e.day++;e.fame+=3;e.adventure.surveys++;return Adventure.finish(e,"돌고래를 따라 해류를 기록했다. 하루 경과, 명성 +3."); }
        if(choice==1) { e.gold+=90;return Adventure.finish(e,"순풍을 이용해 보급을 절약했다. +90 G."); }
        e.hull=Math.min(e.maxHull(),e.hull+8);e.fame++;return Adventure.finish(e,"잔잔한 바다에서 갑판을 정비했다. 선체 +8(최대치까지), 명성 +1.");
    }
    private static boolean island(Engine e,int choice) {
        if(choice==0) {
            e.day++;Adventure.souvenir(e);e.gold+=100;e.fame+=2;
            return Adventure.finish(e,"섬의 해변에서 해역 기념품을 찾았다. 하루 경과, +100 G, 명성 +2.");
        }
        if(choice==1) {
            e.day+=3;
            if(e.adventure.roll<CrewAbilities.explorationChance(e,65)) {
                Adventure.souvenir(e);e.gold+=220;e.fame+=5;
                return Adventure.finish(e,"오래된 관측소의 흔적을 발견했다! 사흘 경과, 기념품, +220 G, 명성 +5.");
            }
            Adventure.damage(e,7);return Adventure.finish(e,"거친 해안 때문에 빈손으로 돌아왔다. 사흘 경과, 선체 −7.");
        }
        e.fame++;return Adventure.finish(e,"해도에 섬의 위치를 남겼다. 명성 +1.");
    }
}
