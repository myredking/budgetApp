package com.bluehorizon;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.zip.CRC32;

public final class SaveCodec {
    public static String encode(Engine e) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeInt(1);
            int[] values = {e.gold,e.port,e.day,e.hull,e.level,e.fame,e.visited,e.chapter,e.profit};
            for (int value : values) out.writeInt(value);
            for (int value : e.inventory) out.writeInt(value);
            for (int value : e.influence) out.writeInt(value);
            out.writeUTF(e.message); out.writeInt(e.log.size());
            for (String line : e.log) out.writeUTF(line);
            out.flush();
            byte[] data = bytes.toByteArray(); CRC32 crc = new CRC32(); crc.update(data);
            return "BLUE1:" + crc.getValue() + ":" + Base64.getEncoder().encodeToString(data);
        } catch (IOException ex) { throw new IllegalStateException(ex); }
    }
    public static Engine decode(String text) {
        if (text == null || text.length() > 30000) return null;
        try {
            String[] parts = text.trim().split(":", 3);
            if (parts.length != 3 || !parts[0].equals("BLUE1")) return null;
            byte[] data = Base64.getDecoder().decode(parts[2]);
            CRC32 crc = new CRC32(); crc.update(data);
            if (crc.getValue() != Long.parseLong(parts[1])) return null;
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
            if (in.readInt() != 1) return null;
            Engine e = readState(in);
            if (in.available() != 0 || !valid(e)) return null;
            return e;
        } catch (IOException | IllegalArgumentException ex) { return null; }
    }
    private static Engine readState(DataInputStream in) throws IOException {
        Engine e = new Engine();
        e.gold=in.readInt(); e.port=in.readInt(); e.day=in.readInt(); e.hull=in.readInt();
        e.level=in.readInt(); e.fame=in.readInt(); e.visited=in.readInt();
        e.chapter=in.readInt(); e.profit=in.readInt();
        for (int i=0; i<6; i++) e.inventory[i]=in.readInt();
        for (int i=0; i<5; i++) e.influence[i]=in.readInt();
        e.message=in.readUTF(); e.log.clear();
        int count=in.readInt();
        if (count < 0 || count > 12) throw new IOException("Invalid log size");
        for (int i=0; i<count; i++) e.log.add(in.readUTF());
        return e;
    }
    private static boolean valid(Engine e) {
        if (!range(e.port,0,4) || !range(e.level,1,3) || !range(e.chapter,0,4)) return false;
        if (!range(e.gold,0,100000000) || !range(e.day,1,10000000)) return false;
        return validProgress(e) && validCargo(e) && validInfluence(e);
    }
    private static boolean validProgress(Engine e) {
        if (!range(e.fame,0,100000000) || !range(e.profit,0,100000000)) return false;
        if (!range(e.hull,1,e.maxHull()) || !range(e.visited,1,31)) return false;
        return (e.visited & (1 << e.port)) != 0;
    }
    private static boolean validCargo(Engine e) {
        for (int value : e.inventory) if (!range(value,0,e.capacity())) return false;
        return e.cargo() <= e.capacity();
    }
    private static boolean validInfluence(Engine e) {
        for (int value : e.influence) if (!range(value,0,3)) return false;
        return true;
    }
    private static boolean range(int value, int min, int max) { return value >= min && value <= max; }
}
