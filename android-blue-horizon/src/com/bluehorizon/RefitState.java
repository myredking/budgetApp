package com.bluehorizon;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public final class RefitState {
    public final int[] levels=new int[4];
    public int used() { int total=0;for(int tier:levels) total+=tier;return total; }
    public boolean valid(int hullLevel) {
        for(int tier:levels) if(tier<0 || tier>3) return false;
        return used()<=hullLevel*2+1;
    }
    public void write(DataOutputStream out) throws IOException { for(int tier:levels) out.writeInt(tier); }
    public static RefitState read(DataInputStream in) throws IOException {
        RefitState state=new RefitState();for(int i=0;i<4;i++) state.levels[i]=in.readInt();return state;
    }
}
