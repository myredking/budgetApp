package com.bluehorizon;

import android.content.Context;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class Coastline {
    private static List<float[]> cache;
    public static List<float[]> load(Context context) {
        if(cache!=null) return cache;
        cache=new ArrayList<float[]>();
        try(DataInputStream in=new DataInputStream(context.getAssets().open("land.bin"))) {
            int rings=in.readInt();
            for(int i=0;i<rings;i++) {
                int count=in.readInt();float[] ring=new float[count*2];
                for(int j=0;j<ring.length;j++) ring[j]=in.readFloat();
                cache.add(ring);
            }
        } catch(IOException ex) { cache.clear(); }
        return cache;
    }
}
