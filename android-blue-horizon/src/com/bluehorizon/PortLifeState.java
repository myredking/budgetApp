package com.bluehorizon;
import java.io.*;

public final class PortLifeState {
    public long events,generous,chats,practical;
    public void write(DataOutputStream out) throws IOException {
        out.writeLong(events);out.writeLong(generous);out.writeLong(chats);out.writeLong(practical);
    }
    public static PortLifeState read(DataInputStream in) throws IOException {
        PortLifeState state=new PortLifeState();state.events=in.readLong();state.generous=in.readLong();
        state.chats=in.readLong();state.practical=in.readLong();return state;
    }
    public boolean valid(Engine e) {
        if(events<0 || (events>>>56)!=0 || (events & ~e.visited)!=0 || (generous & ~events)!=0) return false;
        if(chats<0 || (chats>>>35)!=0 || (practical & ~chats)!=0) return false;
        return validChats(e);
    }
    private boolean validChats(Engine e) {
        for(int region=0;region<7;region++) for(int person=1;person<=5;person++) {
            if((chats & (1L<<(region*5+person-1)))==0) continue;
            if(!Crew.available(person,e.chapter) || (e.visited & (255L<<(region*8)))==0) return false;
        }
        return true;
    }
}
