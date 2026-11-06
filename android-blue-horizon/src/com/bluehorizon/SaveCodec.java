package com.bluehorizon;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.zip.CRC32;

public final class SaveCodec {
    public static final int VERSION=8;
    public static final String PREFIX="BLUE"+VERSION+":";
    public static String encode(Engine e) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeInt(VERSION);
            int[] values = {e.gold,e.port,e.day,e.hull,e.level,e.fame,e.chapter,e.profit};
            for (int value : values) out.writeInt(value);
            out.writeLong(e.visited);
            for (int value : e.inventory) out.writeInt(value);
            for (int value : e.influence) out.writeInt(value);
            out.writeUTF(e.message); out.writeInt(e.log.size());
            for (String line : e.log) out.writeUTF(line);
            e.adventure.write(out);
            e.crew.write(out);
            e.refits.write(out);
            e.rivals.write(out);
            e.town.write(out);
            e.expedition.write(out);
            out.flush();
            byte[] data = bytes.toByteArray(); CRC32 crc = new CRC32(); crc.update(data);
            return PREFIX + crc.getValue() + ":" + Base64.getEncoder().encodeToString(data);
        } catch (IOException ex) { throw new IllegalStateException(ex); }
    }
    public static Engine decode(String text) {
        if (text == null || text.length() > 30000) return null;
        try {
            String[] parts = text.trim().split(":", 3);
            if (parts.length != 3) return null;
            int version=version(parts[0]);if(version==0) return null;
            byte[] data = Base64.getDecoder().decode(parts[2]);
            CRC32 crc = new CRC32(); crc.update(data);
            if (crc.getValue() != Long.parseLong(parts[1])) return null;
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
            if (in.readInt() != version) return null;
            return validatedState(in,version);
        } catch (IOException | IllegalArgumentException ex) { return null; }
    }
    private static Engine validatedState(DataInputStream in,int version) throws IOException {
        Engine e=readState(in,version);
        if(in.available()!=0 || !valid(e)) return null;
        if(version==1) { if(!validLegacy(e)) return null;migrate(e); }
        return e;
    }
    private static int version(String header) {
        if(header.equals("BLUE1")) return 1;
        if(header.equals("BLUE2")) return 2;
        if(header.equals("BLUE3")) return 3;
        if(header.equals("BLUE4")) return 4;
        if(header.equals("BLUE5")) return 5;
        if(header.equals("BLUE6")) return 6;
        if(header.equals("BLUE7")) return 7;
        if(header.equals("BLUE8")) return 8;
        return 0;
    }
    private static Engine readState(DataInputStream in,int version) throws IOException {
        Engine e = new Engine();
        e.gold=in.readInt(); e.port=in.readInt(); e.day=in.readInt(); e.hull=in.readInt();
        e.level=in.readInt(); e.fame=in.readInt();
        if(version==1) e.visited=in.readInt();
        e.chapter=in.readInt(); e.profit=in.readInt();
        if(version>=2) e.visited=in.readLong();
        for (int i=0; i<6; i++) e.inventory[i]=in.readInt();
        int ports=version==1?5:World.COUNT;
        for (int i=0; i<ports; i++) e.influence[i]=in.readInt();
        e.message=in.readUTF(); e.log.clear();
        int count=in.readInt();
        if (count < 0 || count > 12) throw new IOException("Invalid log size");
        for (int i=0; i<count; i++) e.log.add(in.readUTF());
        readExtras(in,version,e);
        return e;
    }
    private static void readExtras(DataInputStream in,int version,Engine e) throws IOException {
        if(version>=3) e.adventure=AdventureState.read(in);
        if(version>=4) e.crew=CrewState.read(in);
        if(version>=5) e.refits=RefitState.read(in);
        if(version>=6) e.rivals=RivalState.read(in);else Rivals.initialise(e);
        if(version>=7) e.town=TownState.read(in);
        if(version>=8) e.expedition=ExpeditionState.read(in);
    }
    private static boolean valid(Engine e) {
        if (!World.validPort(e.port) || !range(e.level,1,3) || !range(e.chapter,0,Story.REVEALS.length)) return false;
        if (!range(e.gold,0,100000000) || !range(e.day,1,10000000)) return false;
        return validProgress(e) && validCargo(e) && validInfluence(e) && validExtras(e);
    }
    private static boolean validExtras(Engine e) { return e.adventure.valid() && e.crew.valid(e.chapter) && e.refits.valid(e.level) && e.rivals.valid(e) && e.town.valid(e) && e.expedition.valid(e); }
    private static boolean validProgress(Engine e) {
        if (!range(e.fame,0,100000000) || !range(e.profit,0,100000000)) return false;
        if (!range(e.hull,1,e.maxHull()) || e.visited<=0 || (e.visited>>>World.COUNT)!=0) return false;
        return e.hasVisited(e.port);
    }
    private static boolean validCargo(Engine e) {
        for (int value : e.inventory) if (!range(value,0,e.capacity())) return false;
        return e.cargo() <= e.capacity();
    }
    private static boolean validInfluence(Engine e) {
        for (int value : e.influence) if (!range(value,0,3)) return false;
        return true;
    }
    private static boolean validLegacy(Engine e) {
        return e.port<5 && e.visited<=31 && e.chapter<=4;
    }
    private static void migrate(Engine e) {
        long visits=0;int[] influence=e.influence.clone();
        java.util.Arrays.fill(e.influence,0);
        for(int i=0;i<5;i++) {
            if((e.visited & (1L<<i))!=0) visits|=1L<<World.LEGACY_PORTS[i];
            e.influence[World.LEGACY_PORTS[i]]=influence[i];
        }
        e.visited=visits;e.port=World.LEGACY_PORTS[e.port];e.chapter=0;e.log.clear();
        Rivals.initialise(e);
        e.note("세계 확장 버전으로 이전했습니다. 금화·화물·선박·명성·투자는 유지되며 새 이야기는 첫 장부터 시작합니다.");
    }
    private static boolean range(int value, int min, int max) { return value >= min && value <= max; }
}
