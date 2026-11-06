package com.bluehorizon;
import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.view.View;

public final class BattleSceneView extends View {
    private final Bitmap ships;
    private final Paint paint=new Paint(3);
    private final int tier;
    private final long began=SystemClock.uptimeMillis();
    public BattleSceneView(Context context,int level) { super(context);tier=level-1;ships=SceneView.load(context,"sailing-ships-v1.png"); }
    @Override protected void onDraw(Canvas canvas) {
        canvas.drawColor(0xff153f51);float w=getWidth(),h=getHeight();
        paint.setStrokeWidth(2);paint.setColor(0x4476b8bb);
        for(int i=0;i<9;i++) canvas.drawLine(0,i*h/8,w,i*h/8+12,paint);
        vessel(canvas,tier,w*.25f,h*.65f,55);vessel(canvas,2,w*.75f,h*.35f,-125);
        float progress=Math.min(1,(SystemClock.uptimeMillis()-began)/900f);
        paint.setColor(0xffffd37e);canvas.drawCircle(w*(.30f+.4f*progress),h*(.60f-.2f*progress),5,paint);
        if(progress<1) postInvalidateOnAnimation();
    }
    private void vessel(Canvas canvas,int ship,float x,float y,float angle) {
        if(ships==null) return;
        int cell=ships.getWidth()/3;float size=Math.min(getWidth()*.22f,getHeight()*.42f);
        canvas.save();canvas.translate(x,y);canvas.rotate(angle);paint.setAlpha(255);
        canvas.drawBitmap(ships,new Rect(ship*cell,0,(ship+1)*cell,ships.getHeight()),new RectF(-size/2,-size,size/2,size),paint);canvas.restore();
    }
}
