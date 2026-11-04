package android.graphics;
import java.awt.*;
import java.awt.geom.*;
import java.util.Stack;
public class Canvas {
 final Graphics2D g;
 final Stack<AffineTransform> transforms=new Stack<>();
 public Canvas(Graphics2D value) { g=value; g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON); }
 public void save() { transforms.push(g.getTransform()); }
 public void restore() { g.setTransform(transforms.pop()); }
 public void scale(float x,float y) { g.scale(x,y); }
 public void translate(float x,float y) { g.translate(x,y); }
 void apply(Paint p) {
  g.setPaint(new java.awt.Color(p.color,true));
  if(p.shader instanceof LinearGradient) g.setPaint(((LinearGradient)p.shader).value);
  g.setStroke(new BasicStroke(p.width,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
 }
 public void drawRoundRect(float x,float y,float xx,float yy,float rx,float ry,Paint p) { apply(p); g.fill(new RoundRectangle2D.Float(x,y,xx-x,yy-y,rx*2,ry*2)); }
 public void drawRect(float x,float y,float xx,float yy,Paint p) { apply(p); g.fill(new Rectangle2D.Float(x,y,xx-x,yy-y)); }
 public void drawOval(float x,float y,float xx,float yy,Paint p) { apply(p); g.fill(new Ellipse2D.Float(x,y,xx-x,yy-y)); }
 public void drawCircle(float x,float y,float r,Paint p) { drawOval(x-r,y-r,x+r,y+r,p); }
 public void drawLine(float x,float y,float xx,float yy,Paint p) { apply(p); g.draw(new Line2D.Float(x,y,xx,yy)); }
 public void drawPath(Path path,Paint p) { apply(p); g.fill(path.value); }
 public void drawText(String text,float x,float y,Paint p) {
  apply(p); g.setFont(new Font("Malgun Gothic",Font.PLAIN,Math.round(p.size)));
  g.drawString(text,x-(p.align==Paint.Align.CENTER?g.getFontMetrics().stringWidth(text)/2f:0),y);
 }
}
