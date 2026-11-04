package com.deckball;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.SystemClock;
import android.widget.FrameLayout;

public final class BallparkView extends FrameLayout {
    final GLSurfaceView surface;
    final BallparkRenderer renderer=new BallparkRenderer();
    final MatchOverlay overlay;
    final GameEngine game;
    float progress=-1;
    int camera;
    long swingStarted,contactStarted;
    public BallparkView(Context context,GameEngine game) {
        super(context);this.game=game;
        surface=new GLSurfaceView(context);surface.setEGLContextClientVersion(2);
        surface.setEGLConfigChooser(8,8,8,0,16,0);surface.setPreserveEGLContextOnPause(true);
        surface.setRenderer(renderer);surface.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        addView(surface,new LayoutParams(-1,-1));
        overlay=new MatchOverlay(context,game);addView(overlay,new LayoutParams(-1,-1));refresh();
    }
    public void refresh() {
        long elapsed=SystemClock.elapsedRealtime()-swingStarted;
        float swing=swingStarted>0 && elapsed<650?(float)Math.sin(elapsed*Math.PI/650):0;
        long contactElapsed=SystemClock.elapsedRealtime()-contactStarted;
        float batted=contactStarted>0 && contactElapsed<1000?Math.max(.001f,contactElapsed/1000f):0;
        int hitter=game.batting?game.lineup[game.batter]:game.opponentHitter;
        int pitcher=game.batting?game.opponentPitcher:game.pitcher;
        renderer.update(progress,swing,batted,TeamPalette.color(game.player(hitter).team),TeamPalette.color(game.player(pitcher).team),camera);surface.requestRender();
        overlay.progress=progress;overlay.invalidate();
    }
    public void contact() {contactStarted=SystemClock.elapsedRealtime();refresh();}
    public void swing() {swingStarted=SystemClock.elapsedRealtime();refresh();}
    public void switchCamera() {camera=1-camera;refresh();}
    public void pause() {surface.onPause();}
    public void resume() {surface.onResume();refresh();}
}
