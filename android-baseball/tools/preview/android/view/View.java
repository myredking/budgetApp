package android.view;
import android.content.Context;
import android.graphics.Canvas;
public class View {
 public static final int LAYER_TYPE_SOFTWARE=1;
 public static int WIDTH=1440,HEIGHT=810;
 public void setLayerType(int kind,Object paint) {}
 public View(Context context) {}
 public int getWidth() { return WIDTH; }
 public int getHeight() { return HEIGHT; }
 protected void onDraw(Canvas c) {}
}
