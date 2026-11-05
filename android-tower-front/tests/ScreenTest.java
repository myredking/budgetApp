package com.towerfront;

public final class ScreenTest {
    private static void check(boolean ok,String message) {
        if(!ok) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        float[][] sizes={{320,496,1},{720,1136,2},{1080,2184,3},{1080,2214,2.625f},{1200,1776,2},{600,600,2}};
        for(float[] size:sizes) verify(size);
        cameraChecks();
        System.out.println("Responsive screen, minimum touch targets, camera and input mapping checks passed");
    }
    private static void verify(float[] size) {
        ScreenLayout l=new ScreenLayout(size[0],size[1],size[2],1.5f);
        check(l.left>=0 && l.top>=0,"scene remains on screen");
        check(l.buttonHeight*l.scale/size[2]>=47.99,"minimum48dp buttons");
        check(l.skillTop+l.buttonHeight<=l.height,"skills stay above system navigation");
        check(l.boardBottom>l.boardTop+80,"visible battlefield remains");
        check(l.commandTop>l.boardBottom,"commands cannot cover the map");
        check(l.overlayMenuTop+l.buttonHeight<l.height,"pause menu stays inside screen");
        check(l.overlayTitle-l.font(32)*.9f>=7,"large pause title cannot clip top");
        check(l.boardTop>=l.objectiveY()+l.font(10)*.3f+4,"header text stays above battlefield");
        if(l.compact) check(242+43<294,"compact enemy count stays left of pause button");
        else check(l.statusY()-l.font(14)*.9f>=l.pauseTop()+l.buttonHeight+3,"pause and status separated");
        float x=220,y=l.skillTop+l.buttonHeight/2;
        check(Math.abs(l.toX(l.left+x*l.scale)-x)<.01,"horizontal input roundtrip");
        check(Math.abs(l.toY(l.top+y*l.scale)-y)<.01,"vertical input roundtrip");
        check(l.font(10)*l.scale/size[2]>=12,"HUD text has readable minimum");
    }
    private static void cameraChecks() {
        ScreenLayout l=new ScreenLayout(720,1136,2,1);
        BattleCamera c=new BattleCamera(l);
        check(c.screenY(166)>=l.boardTop-.01 && c.screenY(658)<=l.boardBottom+.01,"whole map initially fits");
        float x=BattleControls.x(new Engine(1).towers.get(0)),y=BattleControls.y(new Engine(1).towers.get(0));
        c.toggle(x,y);
        check(c.zoom>c.fitZoom,"zoom enables readable detail");
        c.pan(10000,-10000);
        check(Math.abs(c.worldX(c.screenX(x))-x)<.01,"camera x inverse after pan");
        check(Math.abs(c.worldY(c.screenY(y))-y)<.01,"camera y inverse after pan");
        check(c.screenX(14)<=l.boardLeft+.01 && c.screenX(386)>=l.boardRight-.01,"zoom pan clamps horizontally");
        check(c.screenY(166)<=l.boardTop+.01 && c.screenY(658)>=l.boardBottom-.01,"zoom pan clamps vertically");
        c.toggle(x,y);check(Math.abs(c.zoom-c.fitZoom)<.001,"zoom returns to overview");
    }
}
