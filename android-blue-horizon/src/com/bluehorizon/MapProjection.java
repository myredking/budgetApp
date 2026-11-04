package com.bluehorizon;

/** Shared 360x300 map coordinates, usable by Canvas and JVM layout checks. */
public final class MapProjection {
    private float west=-180,east=180,south=-58,north=76;
    public final int region;
    public MapProjection(int selected) {
        region=selected;
        if(selected<0) return;
        west=180;east=-180;south=90;north=-90;
        for(int port:World.portsIn(selected)) {
            west=Math.min(west,World.LON[port]);east=Math.max(east,World.LON[port]);
            south=Math.min(south,World.LAT[port]);north=Math.max(north,World.LAT[port]);
        }
        float padX=Math.max(4,(east-west)*.18f),padY=Math.max(4,(north-south)*.18f);
        west-=padX;east+=padX;south-=padY;north+=padY;
    }
    public float[] point(float lon,float lat) {
        return new float[]{16+(lon-west)/(east-west)*328,24+(north-lat)/(north-south)*236};
    }
    public boolean visible(int port) { return region<0 || World.region(port)==region; }
    public int hit(float x,float y) {
        int nearest=-1;float best=24*24;
        for(int port:World.portsIn(region)) {
            float[] p=point(World.LON[port],World.LAT[port]);
            float distance=(x-p[0])*(x-p[0])+(y-p[1])*(y-p[1]);
            if(distance<best) { best=distance;nearest=port; }
        }
        return nearest;
    }
}
