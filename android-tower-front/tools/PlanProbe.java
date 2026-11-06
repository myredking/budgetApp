package com.towerfront;

/** Native QA taps are derived from the same geometry as the game. */
public final class PlanProbe {
    private static void point(ScreenLayout s,String name,float x,float y) {
        System.out.println(name+","+(s.left+x*s.scale)+","+(y*s.scale));
    }
    public static void main(String[] args) {
        ScreenLayout s=new ScreenLayout(Float.parseFloat(args[0]),Float.parseFloat(args[1]),
            Float.parseFloat(args[2]),Float.parseFloat(args[3]));
        PauseMenuLayout pause=new PauseMenuLayout(s);
        point(s,"inspect",200,pause.buttonTop(1)+s.buttonHeight/2);
        point(s,"zoom",s.zoomLeft()+s.buttonHeight/2,s.zoomTop()+s.buttonHeight/2);
        point(s,"map1",130,s.boardTop+(s.boardBottom-s.boardTop)*.7f);
        point(s,"map2",260,s.boardTop+(s.boardBottom-s.boardTop)*.3f);
        point(s,"skill",76,s.skillTop+s.buttonHeight/2);
        point(s,"resume",200,s.commandTop+s.buttonHeight/2);
    }
}
