package com.deckball;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import android.graphics.Canvas;
public class Preview {
 public static void main(String[] args) throws Exception {
  BufferedImage image=new BufferedImage(1440,810,BufferedImage.TYPE_INT_RGB);
  GameEngine game=new GameEngine(42); game.start(); game.batting=true;
  game.lineup[0]=7; game.selectOpponent(3,29);
  game.message="1회 말 · 노란 구간에 맞춰 스윙하세요";
  FieldView field=new FieldView(new android.content.Context(),game);
  field.progress=.70f; field.onDraw(new Canvas(image.createGraphics()));
  ImageIO.write(image,"png",new File(args[0]));
 }
}
