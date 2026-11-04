package com.towerfront;
import java.io.*;
import java.util.Base64;

public final class SaveCodec {
    private SaveCodec() { }
    public static String encode(Engine e) {
        try {
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            DataOutputStream out=new DataOutputStream(bytes);
            out.writeInt(0x54465733);out.writeInt(e.level);out.writeInt(e.difficulty);
            out.writeDouble(e.elapsed);out.writeDouble(e.aiTime);
            out.writeInt(e.captures);out.writeInt(e.dispatched);
            out.writeInt(e.towers.size());
            for(Engine.Tower t:e.towers) writeTower(out,t);
            out.writeInt(e.fleets.size());
            for(Engine.Fleet f:e.fleets) writeFleet(out,f);
            out.flush();return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch(IOException ex) {throw new IllegalStateException(ex);}
    }
    private static void writeTower(DataOutputStream out,Engine.Tower t) throws IOException {
        out.writeDouble(t.x);out.writeDouble(t.y);out.writeDouble(t.growth);
        out.writeInt(t.owner);out.writeInt(t.units);out.writeInt(t.kind);out.writeInt(t.rank);
    }
    private static void writeFleet(DataOutputStream out,Engine.Fleet f) throws IOException {
        out.writeInt(f.from);out.writeInt(f.to);out.writeInt(f.owner);out.writeInt(f.units);
        out.writeDouble(f.progress);out.writeDouble(f.duration);
    }
    public static Engine decode(String text) {
        if(text==null || text.length()>200000) return null;
        try {
            DataInputStream in=new DataInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(text)));
            if(in.readInt()!=0x54465733) return null;
            int stage=integer(in,1,12),mode=integer(in,0,2);
            Engine e=new Engine(stage,mode);
            e.elapsed=number(in,0,1e9);e.aiTime=number(in,0,4);
            e.captures=integer(in,0,1000000);e.dispatched=integer(in,0,1000000);
            int count=integer(in,6,8);
            if(count!=e.towers.size()) return null;
            e.towers.clear();
            for(int i=0;i<count;i++) e.towers.add(readTower(in));
            int fleets=integer(in,0,2000);
            for(int i=0;i<fleets;i++) e.fleets.add(readFleet(in,count));
            return in.available()==0?e:null;
        } catch(IOException | IllegalArgumentException ex) {return null;}
    }
    private static Engine.Tower readTower(DataInputStream in) throws IOException {
        double x=number(in,0,1),y=number(in,0,1),growth=number(in,0,1);
        Engine.Tower t=new Engine.Tower(x,y,integer(in,0,2),integer(in,0,99));
        t.growth=growth;t.kind=integer(in,0,3);t.rank=integer(in,1,3);
        return t;
    }
    private static Engine.Fleet readFleet(DataInputStream in,int count) throws IOException {
        int from=integer(in,0,count-1),to=integer(in,0,count-1);
        int owner=integer(in,1,2),units=integer(in,1,99);
        double progress=number(in,0,1),duration=number(in,.1,10);
        Engine.Fleet f=new Engine.Fleet(from,to,owner,units,duration);
        f.progress=progress;return f;
    }
    private static int integer(DataInputStream in,int min,int max) throws IOException {
        int n=in.readInt();
        if(n<min || n>max) throw new IOException("Invalid save range");
        return n;
    }
    private static double number(DataInputStream in,double min,double max) throws IOException {
        double n=in.readDouble();
        if(!Double.isFinite(n) || n<min || n>max) throw new IOException("Invalid save value");
        return n;
    }
}
