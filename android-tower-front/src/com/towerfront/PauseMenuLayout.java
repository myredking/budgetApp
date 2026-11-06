package com.towerfront;

/** Four pause actions keep their physical size even in a short, enlarged UI. */
public final class PauseMenuLayout {
    private final ScreenLayout screen;
    private final float first;
    public final float title,subtitle;
    public final boolean showSubtitle;
    public PauseMenuLayout(ScreenLayout value) {
        screen=value;showSubtitle=screen.height>=640;
        float heading=screen.font(28)*1.2f;
        float caption=showSubtitle?screen.font(12)*1.4f+8:0;
        float total=heading+caption+16+4*screen.buttonHeight+24;
        float start=Math.max(12,(screen.height-total)/2);
        title=start+screen.font(28)*.9f;
        subtitle=title+screen.font(28)*.3f+screen.font(12)*.9f+6;
        first=start+heading+caption+16;
    }
    public float buttonTop(int index) {return first+index*(screen.buttonHeight+8);}
    public int buttonAt(float x,float y) {
        for(int i=0;i<4;i++) if(ScreenLayout.inside(x,y,56,buttonTop(i),288,screen.buttonHeight)) return i;
        return -1;
    }
}
