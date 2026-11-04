package com.blockgarden;
public class TouchTest {
 public static void main(String[] args){
  TouchControl t=new TouchControl();t.begin(100,600,true);
  t.move(103,603,10);check(!t.dragging,"jitter stays tap");
  t.move(100,500,10);check(t.dragging,"drag threshold");
  int[] p=t.anchor(200,300,28,190,43,2,2);
  check(p[0]==0&&p[1]==3,"lift and center");
  p=t.anchor(370,400,28,190,43,2,3);check(p[1]==5,"right edge clamp");
  t.cancel();check(!t.active&&!t.dragging,"cancel clears gesture");
  t.begin(100,300,false);t.move(120,320,10);
  p=t.anchor(28+43*7+20,190+43*7+20,28,190,43,1,1);
  check(p[0]==7&&p[1]==7,"tap keeps exact cell");
  p=t.anchor(27,200,28,190,43,1,1);check(p==null,"outside rejects");
  System.out.println("7 touch checks passed");
 }
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
