package com.deckball;

import java.util.Arrays;

public final class Mesh3D {
    private float[] data=new float[8192];
    private int used;
    public void reset() { used=0; }
    public void copyTo(java.nio.FloatBuffer buffer) { buffer.put(data,0,used); }
    public int size() { return used/9; }
    public float[] array() { return Arrays.copyOf(data,used); }
    public static float[] vector(float x,float y,float z) { return new float[]{x,y,z}; }
    static float[] normal(float[] v) {
        float length=(float)Math.sqrt(v[0]*v[0]+v[1]*v[1]+v[2]*v[2]);
        if(length<.000001f) return vector(0,1,0);
        return vector(v[0]/length,v[1]/length,v[2]/length);
    }
    static float[] cross(float[] a,float[] b) { return vector(a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]); }
    void vertex(float[] p,float[] n,int color) {
        if(used+9>data.length) data=Arrays.copyOf(data,data.length*2);
        for(float value:p) data[used++]=value;
        for(float value:n) data[used++]=value;
        data[used++]=((color>>16)&255)/255f; data[used++]=((color>>8)&255)/255f; data[used++]=(color&255)/255f;
    }
    public void triangle(float[] a,float[] b,float[] c,int color) {
        float[] u=vector(b[0]-a[0],b[1]-a[1],b[2]-a[2]),v=vector(c[0]-a[0],c[1]-a[1],c[2]-a[2]);
        float[] n=normal(cross(u,v)); vertex(a,n,color);vertex(b,n,color);vertex(c,n,color);
    }
    public void quad(float[] a,float[] b,float[] c,float[] d,int color) { triangle(a,b,c,color);triangle(a,c,d,color); }
    public void box(float x,float y,float z,float w,float h,float depth,int color) {
        float[][] p=new float[8][];
        for(int i=0;i<8;i++) p[i]=vector(x+((i&1)==0?-w:w)/2,y+((i&2)==0?-h:h)/2,z+((i&4)==0?-depth:depth)/2);
        int[][] faces={{0,4,6,2},{1,3,7,5},{0,1,5,4},{2,6,7,3},{0,2,3,1},{4,5,7,6}};
        for(int[] f:faces) quad(p[f[0]],p[f[1]],p[f[2]],p[f[3]],color);
    }
    public void ellipsoid(float x,float y,float z,float rx,float ry,float rz,int color) {
        for(int row=0;row<8;row++) {
            float a=(float)(-Math.PI/2+row*Math.PI/8),b=(float)(a+Math.PI/8);
            for(int col=0;col<12;col++) spherePatch(x,y,z,rx,ry,rz,a,b,(float)(col*Math.PI/6),color);
        }
    }
    private void spherePatch(float x,float y,float z,float rx,float ry,float rz,float a,float b,float u,int color) {
        float[][] p=new float[4][],n=new float[4][];
        for(int i=0;i<4;i++) {
            float lat=i<2?a:b,lon=u+(i%2)*(float)Math.PI/6;
            float xx=(float)(Math.cos(lat)*Math.cos(lon)),yy=(float)Math.sin(lat),zz=(float)(Math.cos(lat)*Math.sin(lon));
            p[i]=vector(x+xx*rx,y+yy*ry,z+zz*rz);n[i]=normal(vector(xx/rx,yy/ry,zz/rz));
        }
        for(int index:new int[]{0,2,1,1,2,3}) vertex(p[index],n[index],color);
    }
    public void tube(float[] a,float[] b,float r0,float r1,int color) {
        float[] axis=normal(vector(b[0]-a[0],b[1]-a[1],b[2]-a[2]));
        float[] tangent=normal(cross(axis,Math.abs(axis[1])>.9f?vector(1,0,0):vector(0,1,0)));
        float[] side=cross(axis,tangent);
        for(int i=0;i<14;i++) tubePatch(a,b,r0,r1,tangent,side,i,color);
    }
    private void tubePatch(float[] a,float[] b,float r0,float r1,float[] tangent,float[] side,int index,int color) {
        float[][] p=new float[4][],n=new float[4][];
        for(int i=0;i<4;i++) {
            float angle=(index+i%2)*(float)Math.PI/7,cos=(float)Math.cos(angle),sin=(float)Math.sin(angle);
            float[] radial=normal(vector(tangent[0]*cos+side[0]*sin,tangent[1]*cos+side[1]*sin,tangent[2]*cos+side[2]*sin));
            float[] point=i<2?a:b;float r=i<2?r0:r1;
            p[i]=vector(point[0]+radial[0]*r,point[1]+radial[1]*r,point[2]+radial[2]*r);n[i]=radial;
        }
        for(int k:new int[]{0,1,2,1,3,2}) vertex(p[k],n[k],color);
    }
    public void merge(Mesh3D other,float x,float y,float z,float yaw) {
        float cos=(float)Math.cos(yaw),sin=(float)Math.sin(yaw);
        int needed=used+other.used;
        if(needed>data.length) data=Arrays.copyOf(data,Math.max(needed,data.length*2));
        for(int i=0;i<other.used;i+=9) {
            data[used++]=x+other.data[i]*cos+other.data[i+2]*sin;
            data[used++]=y+other.data[i+1];
            data[used++]=z-other.data[i]*sin+other.data[i+2]*cos;
            data[used++]=other.data[i+3]*cos+other.data[i+5]*sin;
            data[used++]=other.data[i+4];
            data[used++]=-other.data[i+3]*sin+other.data[i+5]*cos;
            data[used++]=other.data[i+6];data[used++]=other.data[i+7];data[used++]=other.data[i+8];
        }
    }
}
