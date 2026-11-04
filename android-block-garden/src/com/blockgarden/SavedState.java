package com.blockgarden;
public final class SavedState {
 public static boolean valid(String board,int[] hand,int[] tones,int shapeCount){
  return validBoard(board)&&validRange(hand,-1,shapeCount-1)&&validRange(tones,1,5);
 }
 private static boolean validBoard(String board){
  if(board==null||board.length()!=64)return false;
  for(int i=0;i<64;i++)if(board.charAt(i)<'0'||board.charAt(i)>'5')return false;
  return true;
 }
 private static boolean validRange(int[] values,int min,int max){
  if(values==null||values.length!=3)return false;
  for(int value:values)if(value<min||value>max)return false;
  return true;
 }
}
