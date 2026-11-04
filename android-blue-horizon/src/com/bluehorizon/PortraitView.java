package com.bluehorizon;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;
import java.io.IOException;
import java.io.InputStream;

/** Source rectangles share one atlas bitmap; no six-fold bitmap allocation. */
public final class PortraitView extends View {
    private static Bitmap atlas;
    private final int person;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    public PortraitView(Context context,int index) {
        super(context);person=index;setContentDescription(Crew.NAMES[index]+"의 초상화");
        if(atlas==null) load(context);
    }
    private static void load(Context context) {
        try(InputStream input=context.getAssets().open("crew-portraits-v1.png")) {
            atlas=BitmapFactory.decodeStream(input);
        } catch(IOException ex) { android.util.Log.w("BlueHorizon","Portrait atlas unavailable",ex); }
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(atlas==null) return;
        int[] cell=Crew.cell(person,atlas.getWidth(),atlas.getHeight());
        Rect source=new Rect(cell[0],cell[1],cell[2],cell[3]);
        canvas.drawBitmap(atlas,source,new RectF(0,0,getWidth(),getHeight()),paint);
    }
}
