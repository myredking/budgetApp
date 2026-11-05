package com.towerfront;

/** Camera affects drawing and input only; battle positions are never changed. */
public final class BattleCamera {
    private final ScreenLayout layout;
    public final float fitZoom;
    public float zoom,panX,panY;
    public BattleCamera(ScreenLayout screen) {
        layout=screen;fitZoom=Math.max(.01f,Math.min((screen.boardRight-screen.boardLeft)/372,(screen.boardBottom-screen.boardTop)/492));
        zoom=fitZoom;
    }
    private float centerX() {return (layout.boardLeft+layout.boardRight)/2;}
    private float centerY() {return (layout.boardTop+layout.boardBottom)/2;}
    public float screenX(float worldX) {return centerX()+panX+(worldX-200)*zoom;}
    public float screenY(float worldY) {return centerY()+panY+(worldY-412)*zoom;}
    public float worldX(float x) {return 200+(x-centerX()-panX)/zoom;}
    public float worldY(float y) {return 412+(y-centerY()-panY)/zoom;}
    public boolean overview() {return Math.abs(zoom-fitZoom)<.001;}
    public void toggle(float focusX,float focusY) {
        if(!overview()) {zoom=fitZoom;panX=0;panY=0;return;}
        zoom=Math.max(1.4f,fitZoom*1.6f);
        panX=(200-focusX)*zoom;panY=(412-focusY)*zoom;clamp();
    }
    public void pan(float dx,float dy) {
        panX+=dx;panY+=dy;clamp();
    }
    private void clamp() {
        float x=Math.max(0,(372*zoom-(layout.boardRight-layout.boardLeft))/2);
        float y=Math.max(0,(492*zoom-(layout.boardBottom-layout.boardTop))/2);
        panX=Math.max(-x,Math.min(x,panX));panY=Math.max(-y,Math.min(y,panY));
    }
}
