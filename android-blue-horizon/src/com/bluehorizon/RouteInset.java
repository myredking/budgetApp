package com.bluehorizon;
import android.content.Context;
import android.graphics.*;

/** Coastline rendered once; only the gold position marker moves each frame. */
public final class RouteInset {
    private final Bitmap map=Bitmap.createBitmap(360,180,Bitmap.Config.RGB_565);
    private final MapProjection projection;
    private final Paint paint=new Paint(3);
    public RouteInset(Context context,int from,int to,ShipMotion motion) {
        projection=new MapProjection(World.region(from)==World.region(to)?World.region(from):-1);
        Canvas canvas=new Canvas(map);canvas.drawColor(0xff12384b);canvas.scale(1,.6f);
        for(float[] ring:Coastline.load(context)) land(canvas,ring);
        paint.setColor(0xff65c9b5);paint.setStrokeWidth(2);
        for(int i=1;i<motion.points.size();i++) {
            float[] a=motion.points.get(i-1),b=motion.points.get(i);
            if(Math.abs(a[0]-b[0])>180) continue;
            float[] x=projection.point(a[0],a[1]),y=projection.point(b[0],b[1]);canvas.drawLine(x[0],x[1],y[0],y[1],paint);
        }
    }
    private void land(Canvas canvas,float[] ring) {
        Path path=new Path();float[] first=projection.point(ring[0],ring[1]);path.moveTo(first[0],first[1]);
        for(int i=2;i<ring.length;i+=2) { float[] p=projection.point(ring[i],ring[i+1]);path.lineTo(p[0],p[1]); }
        path.close();paint.setColor(0xff7d8e76);canvas.drawPath(path,paint);
    }
    public void draw(Canvas canvas,int width,float[] location) {
        float w=Math.min(280,width-24),left=(width-w)/2f;RectF box=new RectF(left,8,left+w,8+w/2);
        paint.setAlpha(255);canvas.drawBitmap(map,null,box,paint);
        float[] p=projection.point(location[0],location[1]);paint.setColor(0xffffd88a);
        canvas.drawCircle(left+p[0]*w/360f,8+p[1]*w/600f,5,paint);
    }
}
