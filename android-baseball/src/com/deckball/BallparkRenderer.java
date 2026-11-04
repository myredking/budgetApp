package com.deckball;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public final class BallparkRenderer implements GLSurfaceView.Renderer {
    private int program,position,normal,color,cameraUniform,staticBuffer;
    private int staticCount,width=1,height=1;
    private float cachedProgress=-2,cachedSwing=-2,cachedBall=-2;
    private int cachedUniform=-1,cachedPitcher=-1;
    private final Mesh3D actorMesh=new Mesh3D();
    private FloatBuffer actors;
    private int actorCount;
    volatile Motion motion=new Motion(-1,0,0,PlayerModel.NAVY,PlayerModel.RED,0);
    static final class Motion {
        final float progress,swing,batted;
        final int uniform,pitcher,camera;
        Motion(float progress,float swing,float batted,int uniform,int pitcher,int camera) {
            this.progress=progress;this.swing=swing;this.batted=batted;this.uniform=uniform;this.pitcher=pitcher;this.camera=camera;
        }
    }
    public void update(float progress,float swing,float batted,int uniform,int pitcher,int camera) {
        motion=new Motion(progress,swing,batted,uniform,pitcher,camera);
    }
    @Override public void onSurfaceCreated(GL10 gl,EGLConfig config) {
        GLES20.glClearColor(.64f,.77f,.83f,1);GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        program=GLES20.glCreateProgram();
        GLES20.glAttachShader(program,shader(GLES20.GL_VERTEX_SHADER,SceneShaders.VERTEX));
        GLES20.glAttachShader(program,shader(GLES20.GL_FRAGMENT_SHADER,SceneShaders.FRAGMENT));
        GLES20.glLinkProgram(program);
        int[] status=new int[1];GLES20.glGetProgramiv(program,GLES20.GL_LINK_STATUS,status,0);
        if(status[0]==0) throw new IllegalStateException(GLES20.glGetProgramInfoLog(program));
        position=GLES20.glGetAttribLocation(program,"aPosition");normal=GLES20.glGetAttribLocation(program,"aNormal");
        color=GLES20.glGetAttribLocation(program,"aColor");cameraUniform=GLES20.glGetUniformLocation(program,"uCamera");
        float[] vertices=BallparkScene.stadium().array();staticCount=vertices.length/9;
        int[] buffers=new int[1];GLES20.glGenBuffers(1,buffers,0);staticBuffer=buffers[0];
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER,staticBuffer);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER,vertices.length*4,buffer(vertices),GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER,0);
    }
    static int shader(int kind,String source) {
        int shader=GLES20.glCreateShader(kind);GLES20.glShaderSource(shader,source);GLES20.glCompileShader(shader);
        int[] status=new int[1];GLES20.glGetShaderiv(shader,GLES20.GL_COMPILE_STATUS,status,0);
        if(status[0]==0) throw new IllegalStateException(GLES20.glGetShaderInfoLog(shader));
        return shader;
    }
    static FloatBuffer buffer(float[] values) {
        FloatBuffer buffer=ByteBuffer.allocateDirect(values.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        buffer.put(values).position(0);return buffer;
    }
    @Override public void onSurfaceChanged(GL10 gl,int width,int height) {this.width=width;this.height=Math.max(1,height);GLES20.glViewport(0,0,width,height);}
    @Override public void onDrawFrame(GL10 gl) {
        Motion state=motion;
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);GLES20.glUseProgram(program);
        GLES20.glUniformMatrix4fv(cameraUniform,1,false,Camera3D.matrix(width/(float)height,state.camera),0);
        GLES20.glEnableVertexAttribArray(position);GLES20.glEnableVertexAttribArray(normal);GLES20.glEnableVertexAttribArray(color);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER,staticBuffer);
        GLES20.glVertexAttribPointer(position,3,GLES20.GL_FLOAT,false,36,0);
        GLES20.glVertexAttribPointer(normal,3,GLES20.GL_FLOAT,false,36,12);
        GLES20.glVertexAttribPointer(color,3,GLES20.GL_FLOAT,false,36,24);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,staticCount);GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER,0);
        updateActors(state); drawActors();
    }
    void updateActors(Motion state) {
        if(state.progress==cachedProgress && state.swing==cachedSwing && state.uniform==cachedUniform && state.pitcher==cachedPitcher && state.batted==cachedBall) return;
        BallparkScene.fillActors(actorMesh,state.progress,state.swing,state.uniform,state.pitcher,state.batted);
        int floats=actorMesh.size()*9;
        if(actors==null || actors.capacity()<floats)
            actors=ByteBuffer.allocateDirect(floats*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        actors.clear();actorMesh.copyTo(actors);actors.flip();actorCount=actorMesh.size();
        cachedProgress=state.progress;cachedSwing=state.swing;cachedUniform=state.uniform;cachedPitcher=state.pitcher;cachedBall=state.batted;
    }
    void drawActors() {
        actors.position(0);GLES20.glVertexAttribPointer(position,3,GLES20.GL_FLOAT,false,36,actors);
        actors.position(3);GLES20.glVertexAttribPointer(normal,3,GLES20.GL_FLOAT,false,36,actors);
        actors.position(6);GLES20.glVertexAttribPointer(color,3,GLES20.GL_FLOAT,false,36,actors);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,actorCount);
    }
}
