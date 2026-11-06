package com.towerfront;

/** Safe-area layout preserves physical touch sizes independently of the map. */
public final class ScreenLayout {
    public final float scale,left,top,height,buttonHeight,fontFactor,pixelDensity;
    public final float boardLeft=14,boardRight=386,boardTop,boardBottom;
    public final float skillTop,commandTop,energyTop,energyHeight,noticeY;
    public final float overlayTitle,overlayPrimaryTop,overlayRetryTop,overlayMenuTop;
    public final Panel panel;
    public final boolean compact;
    public ScreenLayout(float width,float availableHeight,float density,float fontScale) {
        float safeDensity=Math.max(.5f,density);
        scale=Math.max(.001f,Math.min(Math.min(Math.max(1,width),520*safeDensity)/400,Math.max(1,availableHeight)/500));
        left=Math.max(0,(width-400*scale)/2);top=0;height=availableHeight/scale;
        pixelDensity=safeDensity;fontFactor=Math.max(1,Math.min(1.3f,fontScale));
        buttonHeight=Math.max(52,48*safeDensity/scale);compact=height<740;
        skillTop=height-12-buttonHeight;commandTop=skillTop-8-buttonHeight;
        energyHeight=Math.max(20,font(10)*1.4f);energyTop=commandTop-8-energyHeight;
        noticeY=energyTop-10;
        boardTop=headerEnd();boardBottom=noticeY-Math.max(18,font(12)*1.1f);
        panel=measurePanel();overlayTitle=panel.title;overlayPrimaryTop=panel.primary;
        overlayRetryTop=panel.retry;overlayMenuTop=panel.menu;
    }
    public float font(float base) {return Math.max(base,12*pixelDensity/scale)*fontFactor;}
    public float titleY() {return Math.max(compact?28:40,font(compact?18:21)*.9f+6);}
    public float nameY() {return Math.max(compact?52:70,titleY()+font(compact?18:21)*.3f+font(12)*.9f+5);}
    public float pauseTop() {return compact?8:23;}
    public float statusY() {
        if(compact) return Math.max(84,nameY()+font(12)*.3f+font(14)*.9f+6);
        return Math.max(111,pauseTop()+buttonHeight+font(14)*.9f+6);
    }
    public float objectiveY() {return Math.max(compact?108:132,statusY()+font(14)*.3f+font(10)*.9f+6);}
    public float statusTop() {return statusY()-font(18)*.9f-4;}
    public float statusBottom() {return statusY()+font(14)*.3f+1;}
    public float objectiveTop() {return objectiveY()-font(10)*.9f-3;}
    public float detailsY() {return objectiveY()+font(10)*1.2f+5;}
    public float legendY() {return detailsY()+font(10)*1.2f+5;}
    private float headerEnd() {
        float content=compact?objectiveY():legendY();
        return Math.max(compact?124:166,Math.max(pauseTop()+buttonHeight+8,content+font(10)*.3f+8));
    }
    public float toX(float screenX) {return (screenX-left)/scale;}
    public float toY(float screenY) {return (screenY-top)/scale;}
    public float zoomLeft() {return boardRight-buttonHeight-8;}
    public float zoomTop() {return boardTop+Math.max(0,Math.min(6,(boardBottom-boardTop-buttonHeight)/2));}
    public static boolean inside(float x,float y,float left,float top,float width,float height) {
        return x>=left && x<=left+width && y>=top && y<=top+height;
    }
    public static final class Panel {
        public float title,subtitle,stars,stats,goal,primary,retry,menu,footer;
        public boolean hideSubtitle,footerVisible;
        private void shift(float offset) {
            title+=offset;subtitle+=offset;stars+=offset;stats+=offset;goal+=offset;
            primary+=offset;retry+=offset;menu+=offset;footer+=offset;
        }
    }
    private Panel measurePanel() {
        Panel p=new Panel();p.hideSubtitle=height<600 || (fontFactor>1.15f && height<680);
        p.title=font(32)*.9f+12;p.subtitle=p.title+font(32)*.3f+font(15)*.9f+8;
        float previous=p.hideSubtitle?p.title:p.subtitle,previousFont=p.hideSubtitle?font(32):font(15);
        p.stars=previous+previousFont*.3f+font(34)*.85f+10;
        p.stats=p.stars+font(34)*.3f+font(13)*.85f+8;
        p.goal=p.stats+font(13)*.3f+font(10)*.85f+6;
        p.primary=p.goal+font(10)*.3f+10;p.retry=p.primary+buttonHeight+10;
        p.menu=p.retry+buttonHeight+10;p.footer=p.menu+buttonHeight+font(12)+18;
        p.footerVisible=p.footer+font(12)*.3f<height-8;
        float bottom=p.footerVisible?p.footer+font(12)*.3f:p.menu+buttonHeight+12;
        p.shift(Math.max(0,(height-bottom)/2));return p;
    }
}
