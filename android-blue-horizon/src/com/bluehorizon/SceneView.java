package com.bluehorizon;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

public class SceneView extends View {
    private static final HashMap<String,Bitmap> CACHE=new HashMap<String,Bitmap>();
    private final Bitmap atlas;
    private final int cell;
    private final boolean town;
    private final Paint paint=new Paint(3);
    public SceneView(Context context,String name,int index) {
        super(context);cell=index;town=name.equals("town-scenes-v1.png");atlas=load(context,name);
        setContentDescription("항구의 건물과 사람들이 그려진 장면");
    }
    static Bitmap load(Context context,String name) {
        if(CACHE.containsKey(name)) return CACHE.get(name);
        try(InputStream stream=context.getAssets().open(name)) {
            Bitmap bitmap=BitmapFactory.decodeStream(stream);CACHE.put(name,bitmap);return bitmap;
        } catch(IOException error) { return null; }
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);if(atlas==null) return;
        int[] box=town?TownLayout.townCell(cell,atlas.getWidth(),atlas.getHeight()):TownLayout.cell(cell,atlas.getWidth(),atlas.getHeight());
        canvas.drawBitmap(atlas,new Rect(box[0],box[1],box[2],box[3]),new Rect(0,0,getWidth(),getHeight()),paint);
    }
}
