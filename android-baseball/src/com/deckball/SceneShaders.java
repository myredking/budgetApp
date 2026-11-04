package com.deckball;

public final class SceneShaders {
    private SceneShaders() {}
    public static final String VERTEX=
        "attribute vec3 aPosition; attribute vec3 aNormal; attribute vec3 aColor;\n"+
        "uniform mat4 uCamera; varying vec3 vPosition; varying vec3 vNormal; varying vec3 vColor;\n"+
        "void main(){ vPosition=aPosition; vNormal=aNormal; vColor=aColor; gl_Position=uCamera*vec4(aPosition,1.0); }\n";
    public static final String FRAGMENT=
        "precision mediump float; varying vec3 vPosition; varying vec3 vNormal; varying vec3 vColor;\n"+
        "float noise(vec2 p){vec2 q=fract(p*vec2(0.1031,0.11369));q+=dot(q,q.yx+vec2(19.19));return fract(q.x*q.y*0.17);}\n"+
        "void main(){\n"+
        "vec3 n=normalize(vNormal); vec3 sun=normalize(vec3(-0.45,0.8,-0.3));\n"+
        "vec3 base=vColor; float grain=noise(vPosition.xz*65.0);\n"+
        "if(n.y>0.8 && base.g>base.r*1.25 && base.g>base.b*1.2){\n"+
        "float stripe=mod(floor(vPosition.z/5.0),2.0); base*=0.91+0.09*stripe+0.08*grain; }\n"+
        "else{base*=0.96+0.06*grain;}\n"+
        "float light=0.45+0.18*max(0.0,n.y)+0.48*max(0.0,dot(n,sun));\n"+
        "float rim=pow(1.0-abs(n.z),4.0)*0.045; vec3 color=base*light+vec3(rim);\n"+
        "float fog=smoothstep(75.0,215.0,length(vPosition.xz));\n"+
        "color=mix(color,vec3(0.64,0.77,0.83),fog*0.72); gl_FragColor=vec4(color,1.0); }\n";
}
