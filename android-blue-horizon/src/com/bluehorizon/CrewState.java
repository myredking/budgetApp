package com.bluehorizon;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public final class CrewState {
    public int first=1,second=-1,stories;
    public void write(DataOutputStream out) throws IOException { out.writeInt(first);out.writeInt(second);out.writeInt(stories); }
    public static CrewState read(DataInputStream in) throws IOException {
        CrewState state=new CrewState();state.first=in.readInt();state.second=in.readInt();state.stories=in.readInt();return state;
    }
    public boolean valid(int chapter) {
        if(stories<0 || stories>63) return false;
        if(first>=0 && first==second) return false;
        return validPerson(first,chapter) && validPerson(second,chapter);
    }
    private static boolean validPerson(int person,int chapter) {
        if(person==-1) return true;
        return person>=1 && person<=5 && Crew.available(person,chapter);
    }
}
