package com.bluehorizon;
import java.util.ArrayList;
import java.util.List;

public final class ShipMotion {
    public static float spriteSize(int width,int height) {
        return Math.max(1,Math.min(width*.78f,(height-150)/2f));
    }
    public static float fitSize(int width,int height,float heading) {
        double angle=Math.toRadians(heading),c=Math.abs(Math.cos(angle)),s=Math.abs(Math.sin(angle));
        return (float)Math.max(1,Math.min((width-48)/(c+2*s),(height-190)/(s+2*c)));
    }
    public float heading(double progress) {
        float[] a=position(Math.max(0,progress-.003)),b=position(Math.min(1,progress+.003));
        double dx=b[0]-a[0];if(dx>180) dx-=360;if(dx< -180) dx+=360;
        dx*=Math.cos(Math.toRadians((a[1]+b[1])/2));
        return (float)Math.toDegrees(Math.atan2(dx,b[1]-a[1]));
    }
    public final List<float[]> points=new ArrayList<float[]>();
    private final List<Double> ends=new ArrayList<Double>();
    private double distance;
    public ShipMotion(int from,int to) {
        Route route=WorldRoutes.between(from,to);
        points.add(new float[]{World.LON[from],World.LAT[from]});ends.add(0.0);
        for(int stop=1;stop<route.ports.length;stop++) {
            float[][] leg=WorldRoutes.leg(route.ports[stop-1],route.ports[stop]);
            for(int i=1;i<leg.length;i++) append(leg[i]);
        }
    }
    private void append(float[] point) {
        float[] last=points.get(points.size()-1);
        double dx=(point[0]-last[0])*Math.cos(Math.toRadians((point[1]+last[1])/2)),dy=point[1]-last[1];
        distance+=Math.sqrt(dx*dx+dy*dy);points.add(point.clone());ends.add(distance);
    }
    public float[] position(double fraction) {
        if(fraction<=0) return points.get(0).clone();
        if(fraction>=1) return points.get(points.size()-1).clone();
        double target=distance*fraction;
        for(int i=1;i<points.size();i++) {
            if(ends.get(i)<target) continue;
            double length=ends.get(i)-ends.get(i-1),part=length==0?0:(target-ends.get(i-1))/length;
            float[] a=points.get(i-1),b=points.get(i);
            return new float[]{(float)(a[0]+(b[0]-a[0])*part),(float)(a[1]+(b[1]-a[1])*part)};
        }
        return points.get(points.size()-1).clone();
    }
}
