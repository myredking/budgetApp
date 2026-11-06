package com.bluehorizon;
import android.content.Context;
import android.graphics.*;
import android.view.View;

public final class TreasureMapView extends View {
    private final Engine game;
    private final MapProjection projection;
    private final Paint paint=new Paint(3);
    private final java.util.List<float[]> coast;
    public TreasureMapView(Context context,Engine e) {
        super(context);game=e;projection=new MapProjection(World.region(e.port));coast=Coastline.load(context);
        setContentDescription("현재 해역의 보물지도. 금색은 찾은 단서, 붉은 원은 보물 탐사 항구입니다.");
    }
    @Override protected void onDraw(Canvas canvas) {
        canvas.drawColor(0xffe9d6a4);canvas.save();canvas.scale(getWidth()/360f,getHeight()/300f);
        for(float[] ring:coast) land(canvas,ring);
        paint.setStrokeWidth(1);paint.setColor(0x44695836);
        for(int x=20;x<360;x+=40) canvas.drawLine(x,0,x,300,paint);
        for(int y=20;y<300;y+=40) canvas.drawLine(0,y,360,y,paint);
        for(int port:World.portsIn(World.region(game.port))) marker(canvas,port);
        paint.setColor(0xff584023);paint.setTextSize(12);paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("기록된 단서 "+Math.min(3,Expeditions.fragments(game,World.region(game.port)))+" / 3",12,286,paint);canvas.restore();
    }
    private void land(Canvas canvas,float[] ring) {
        Path path=new Path();float[] first=projection.point(ring[0],ring[1]);path.moveTo(first[0],first[1]);
        for(int i=2;i<ring.length;i+=2) { float[] p=projection.point(ring[i],ring[i+1]);path.lineTo(p[0],p[1]); }
        path.close();paint.setColor(0xffada572);canvas.drawPath(path,paint);
    }
    private void marker(Canvas canvas,int port) {
        float[] p=projection.point(World.LON[port],World.LAT[port]);boolean found=(game.expedition.clues & (1L<<port))!=0;
        paint.setColor(found?0xffbe7f24:0xff4c6458);canvas.drawCircle(p[0],p[1],found?5:3,paint);
        if(Expeditions.fragments(game,World.region(game.port))>=3 && port==Expeditions.site(World.region(game.port))) {
            paint.setStyle(Paint.Style.STROKE);paint.setColor(0xffa04233);paint.setStrokeWidth(2);
            canvas.drawCircle(p[0],p[1],9,paint);paint.setStyle(Paint.Style.FILL);
        }
    }
}
