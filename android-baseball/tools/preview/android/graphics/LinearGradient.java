package android.graphics;
public class LinearGradient extends Shader {
 final java.awt.GradientPaint value;
 public LinearGradient(float x,float y,float xx,float yy,int a,int b,TileMode mode) {
  value=new java.awt.GradientPaint(x,y,new java.awt.Color(a,true),xx,yy,new java.awt.Color(b,true));
 }
}
