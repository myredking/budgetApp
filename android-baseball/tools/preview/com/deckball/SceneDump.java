package com.deckball;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

public final class SceneDump {
    private SceneDump() {}
    public static void main(String[] args) throws Exception {
        try(DataOutputStream out=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(args[0])))) {
            for(float value:Camera3D.matrix(16f/9f,0)) out.writeFloat(value);
            write(out,BallparkScene.stadium());write(out,BallparkScene.actors(.70f,0,TeamPalette.color("KIA"),TeamPalette.color("해태"),0));
        }
        Files.write(Paths.get(args[1]),SceneShaders.VERTEX.getBytes(StandardCharsets.UTF_8));
        Files.write(Paths.get(args[2]),SceneShaders.FRAGMENT.getBytes(StandardCharsets.UTF_8));
    }
    static void write(DataOutputStream out,Mesh3D mesh) throws Exception {
        float[] values=mesh.array();out.writeInt(values.length);
        for(float value:values) out.writeFloat(value);
    }
}
