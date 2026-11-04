package com.bluehorizon;

public final class Route {
    public final int[] ports;
    public final double miles;
    public Route(int[] path,double distance) { ports=path;miles=distance; }
    public boolean contains(int port) {
        for(int stop:ports) if(stop==port) return true;
        return false;
    }
    public int days() { return miles==0?0:Math.max(2,(int)Math.ceil(miles/150.0)); }
    public String description() {
        StringBuilder text=new StringBuilder();
        for(int i=0;i<ports.length;i++) {
            if(i>0) text.append(" → ");text.append(World.PORTS[ports[i]]);
        }
        return text.toString();
    }
}
