package com.deckball;

public final class Camera3D {
    private Camera3D() {}
    public static float[] matrix(float aspect,int camera) {
        float[] eye=camera==0?new float[]{-.15f,1.65f,-4.8f}:new float[]{7,4,-9};
        float[] target=camera==0?new float[]{0,1.35f,18.44f}:new float[]{0,1,12};
        float[] view=lookAt(eye,target),projection=new float[16];
        float f=1f/(float)Math.tan(Math.toRadians(52)/2);
        projection[0]=f/Math.max(.1f,aspect);projection[5]=f;projection[10]=-250.12f/249.88f;
        projection[11]=-1;projection[14]=-60f/249.88f;
        return multiply(projection,view);
    }
    private static float[] lookAt(float[] eye,float[] target) {
        float[] f=Mesh3D.normal(new float[]{target[0]-eye[0],target[1]-eye[1],target[2]-eye[2]});
        float[] s=Mesh3D.normal(Mesh3D.cross(f,new float[]{0,1,0})),u=Mesh3D.cross(s,f);
        return new float[]{s[0],u[0],-f[0],0,s[1],u[1],-f[1],0,s[2],u[2],-f[2],0,
            -dot(s,eye),-dot(u,eye),dot(f,eye),1};
    }
    private static float dot(float[] a,float[] b) { return a[0]*b[0]+a[1]*b[1]+a[2]*b[2]; }
    private static float[] multiply(float[] a,float[] b) {
        float[] result=new float[16];
        for(int col=0;col<4;col++) for(int row=0;row<4;row++)
            for(int k=0;k<4;k++) result[col*4+row]+=a[k*4+row]*b[col*4+k];
        return result;
    }
    public static float[] project(float[] matrix,float[] point) {
        float[] result=new float[4];
        for(int row=0;row<4;row++) result[row]=matrix[row]*point[0]+matrix[4+row]*point[1]+matrix[8+row]*point[2]+matrix[12+row];
        return result;
    }
}
