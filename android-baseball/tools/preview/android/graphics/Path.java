package android.graphics;
public class Path {
 final java.awt.geom.Path2D.Float value=new java.awt.geom.Path2D.Float();
 public void moveTo(float x,float y) { value.moveTo(x,y); }
 public void lineTo(float x,float y) { value.lineTo(x,y); }
 public void close() { value.closePath(); }
}
