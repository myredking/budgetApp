package com.bluehorizon;

import java.io.DataInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/** Low-resolution coastline guard; harbor approaches within ~55nm are exempt. */
public final class SeaRouteTest {
    private static final List<float[]> LAND=new ArrayList<float[]>();
    private static int checks;
    private static void load() throws Exception {
        try(DataInputStream in=new DataInputStream(Files.newInputStream(Paths.get("assets/land.bin")))) {
            int count=in.readInt();
            for(int i=0;i<count;i++) {
                float[] ring=new float[in.readInt()*2];
                for(int j=0;j<ring.length;j++) ring[j]=in.readFloat();
                LAND.add(ring);
            }
        }
    }
    private static boolean inRing(float lon,float lat,float[] ring) {
        boolean inside=false;
        for(int i=0,j=ring.length-2;i<ring.length;j=i,i+=2) {
            if((ring[i+1]>lat)!=(ring[j+1]>lat)) {
                float cross=(ring[j]-ring[i])*(lat-ring[i+1])/(ring[j+1]-ring[i+1])+ring[i];
                if(lon<cross) inside=!inside;
            }
        }
        return inside;
    }
    private static boolean onLand(float lon,float lat) {
        for(float[] ring:LAND) if(inRing(lon,lat,ring)) return true;
        return false;
    }
    private static double separation(float lon,float lat,int port) {
        double dx=(lon-World.LON[port])*Math.cos(Math.toRadians(lat)),dy=lat-World.LAT[port];
        return Math.sqrt(dx*dx+dy*dy);
    }
    private static int inspectSegment(float[] a,float[] b,int from,int to) {
        int samples=(int)Math.ceil(Math.max(Math.abs(a[0]-b[0]),Math.abs(a[1]-b[1]))*4)+1;
        int inland=0;
        for(int step=1;step<samples;step++) {
            float fraction=step/(float)samples;
            float lon=a[0]+(b[0]-a[0])*fraction,lat=a[1]+(b[1]-a[1])*fraction;
            checks++;
            if(separation(lon,lat,from)<.9 || separation(lon,lat,to)<.9) continue;
            if(onLand(lon,lat)) {
                if(inland==0) System.out.printf(java.util.Locale.US,"  inland near %.2f, %.2f%n",lon,lat);
                inland++;
            }
        }
        return inland;
    }
    public static void main(String[] args) throws Exception {
        load();int failures=0;
        for(int[] edge:WorldRoutes.EDGES) {
            float[][] points=WorldRoutes.leg(edge[0],edge[1]);int inland=0;
            for(int i=1;i<points.length;i++) inland+=inspectSegment(points[i-1],points[i],edge[0],edge[1]);
            if(inland>0) {
                failures++;System.out.println("LAND CROSSING: "+World.PORTS[edge[0]]+" -> "+World.PORTS[edge[1]]+" ("+inland+")");
            }
        }
        if(failures>0) throw new AssertionError(failures+" maritime edges cross inland polygons");
        System.out.println("PASS: "+checks+" coastline route samples");
    }
}
