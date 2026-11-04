package com.bluehorizon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/** Designed maritime graph. Cross-sea routes use the Cape, not modern canals. */
public final class WorldRoutes {
    public static final int[][] EDGES={
        {0,1},{0,7},{7,2},{2,3},{3,4},{4,6},{6,5},
        {0,15},{15,14},{14,8},{8,9},{9,10},{10,11},{11,12},{11,13},{13,14},
        {0,16},{16,17},{17,18},{18,19},{19,20},{20,21},{21,22},{22,23},
        {23,30},{30,31},{31,24},{24,25},{25,26},{25,27},{27,28},{28,29},{29,30},
        {29,33},{33,32},{32,34},{34,38},{38,35},{35,36},{36,39},{36,37},
        {32,46},{46,40},{40,41},{41,42},{42,43},{43,44},{44,45},{45,47},{47,40},{47,37},
        {0,48},{18,53},{48,49},{49,50},{48,51},{49,52},{52,53},{53,54},{54,55}
    };
    private static final double[][] COST=costs();
    private static final Route[][] CACHE=new Route[World.COUNT][World.COUNT];
    private static double[][] costs() {
        double[][] costs=new double[World.COUNT][World.COUNT];
        for(double[] row:costs) Arrays.fill(row,Double.POSITIVE_INFINITY);
        for(int i=0;i<World.COUNT;i++) costs[i][i]=0;
        for(int[] edge:EDGES) {
            float[][] points=leg(edge[0],edge[1]); double total=0;
            for(int i=1;i<points.length;i++) total+=distance(points[i-1],points[i]);
            costs[edge[0]][edge[1]]=total;costs[edge[1]][edge[0]]=total;
        }
        return costs;
    }
    private static double distance(float[] a,float[] b) {
        double lat1=Math.toRadians(a[1]),lat2=Math.toRadians(b[1]);
        double dlat=lat2-lat1,dlon=Math.toRadians(b[0]-a[0]);
        double h=Math.pow(Math.sin(dlat/2),2)+Math.cos(lat1)*Math.cos(lat2)*Math.pow(Math.sin(dlon/2),2);
        return 3440.065*2*Math.asin(Math.sqrt(Math.min(1,h)));
    }
    public static float[][] leg(int from,int to) {
        for(int i=0;i<EDGES.length;i++) {
            if(EDGES[i][0]==from && EDGES[i][1]==to) return SeaPaths.points(i);
            if(EDGES[i][0]==to && EDGES[i][1]==from) return reversed(SeaPaths.points(i));
        }
        throw new IllegalArgumentException("Not a graph edge");
    }
    private static float[][] reversed(float[][] points) {
        float[][] reverse=new float[points.length][];
        for(int i=0;i<points.length;i++) reverse[i]=points[points.length-1-i];
        return reverse;
    }
    public static Route between(int from,int to) {
        if(!World.validPort(from) || !World.validPort(to)) throw new IllegalArgumentException("Invalid port");
        if(CACHE[from][to]==null) CACHE[from][to]=shortest(from,to);
        return CACHE[from][to];
    }
    private static Route shortest(int from,int to) {
        double[] distances=new double[World.COUNT];Arrays.fill(distances,Double.POSITIVE_INFINITY);
        int[] parent=new int[World.COUNT];Arrays.fill(parent,-1);
        boolean[] done=new boolean[World.COUNT];distances[from]=0;
        for(int step=0;step<World.COUNT;step++) {
            int node=nearest(distances,done);if(node<0) break;
            done[node]=true;if(node==to) break;
            relax(node,distances,parent);
        }
        ArrayList<Integer> reverse=new ArrayList<Integer>();
        for(int node=to;node>=0;node=parent[node]) reverse.add(node);
        Collections.reverse(reverse);int[] path=new int[reverse.size()];
        for(int i=0;i<path.length;i++) path[i]=reverse.get(i);
        return new Route(path,distances[to]);
    }
    private static int nearest(double[] distances,boolean[] done) {
        int selected=-1;double best=Double.POSITIVE_INFINITY;
        for(int i=0;i<World.COUNT;i++) {
            if(!done[i] && distances[i]<best) { best=distances[i];selected=i; }
        }
        return selected;
    }
    private static void relax(int node,double[] distances,int[] parent) {
        for(int other=0;other<World.COUNT;other++) {
            double candidate=distances[node]+COST[node][other];
            if(candidate<distances[other]) { distances[other]=candidate;parent[other]=node; }
        }
    }
}
