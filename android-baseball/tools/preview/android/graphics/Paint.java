package android.graphics;
public class Paint {
 int color=Color.WHITE; float width=1,size=16; Shader shader;
 public enum Align { CENTER }
 public Paint(int flags) {}
 public void setColor(int c) { color=c; }
 public void setStrokeWidth(float w) { width=w; }
 public void setTextSize(float s) { size=s; }
 public void setTypeface(Typeface t) {}
 public void setTextAlign(Align a) {}
 public void setShader(Shader s) { shader=s; }
}
