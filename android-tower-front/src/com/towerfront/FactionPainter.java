package com.towerfront;
import android.graphics.*;

/** One high-contrast identity mark for every visual representation of a team. */
public final class FactionPainter {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path=new Path();
    public void draw(Canvas c,int owner,float x,float y,float radius) {
        paint.setStyle(Paint.Style.FILL);paint.setColor(SceneStyle.team(owner));
        c.drawCircle(x,y,radius+1,paint);
        paint.setColor(SceneStyle.INK);c.drawCircle(x,y,radius,paint);
        path.rewind();float size=radius*.65f;
        path.moveTo(x+FactionMark.x(owner,0)*size,y+FactionMark.y(owner,0)*size);
        for(int i=1;i<FactionMark.vertices(owner);i++) path.lineTo(x+FactionMark.x(owner,i)*size,y+FactionMark.y(owner,i)*size);
        if(FactionMark.vertices(owner)>2) path.close();
        paint.setColor(SceneStyle.PAPER);paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(.8f,radius*.19f));paint.setStrokeJoin(Paint.Join.ROUND);
        c.drawPath(path,paint);paint.setStyle(Paint.Style.FILL);
    }
}
