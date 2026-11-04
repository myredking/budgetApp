package com.blockgarden;
public class EngineTest {
 public static void main(String[] args) {
  Engine e=new Engine();
  check(e.fits(new int[][]{{0,0}},7,7),"edge");
  check(!e.fits(new int[][]{{0,0},{1,0}},7,7),"bounds");
  for(int i=0;i<8;i++){e.board[0][i]=1;e.board[i][0]=1;}
  check(e.clear()==2,"simultaneous cross");
  check(e.board[0][0]==0,"cleared");
  e.board[2][2]=1;
  check(!e.place(new int[][]{{0,0}},2,2,2),"overlap");
  check(e.place(new int[][]{{0,0}},3,3,2),"placement");
  check(e.score==10,"score");
  for(int[] row:e.board)java.util.Arrays.fill(row,1);
  check(!e.canFit(new int[][]{{0,0}}),"game over");
  Engine lines=new Engine();
  for(int i=0;i<7;i++)lines.board[0][i]=1;
  check(lines.place(new int[][]{{0,0}},0,7,1),"complete line");
  check(lines.score==110,"line score");
  int before=e.score;check(!e.place(new int[][]{{0,0}},0,0,2),"reject full");
  check(e.score==before&&e.board[0][0]==1,"failure unchanged");
  check(SavedState.valid("0".repeat(64),new int[]{-1,0,11},new int[]{1,2,5},12),"valid save");
  check(!SavedState.valid("9".repeat(64),new int[]{0,0,0},new int[]{1,1,1},12),"bad board");
  check(!SavedState.valid("0".repeat(64),new int[]{12,0,0},new int[]{1,1,1},12),"bad shape");
  check(!SavedState.valid("0".repeat(64),new int[]{0,0,0},new int[]{0,1,1},12),"bad color");
  check(!SavedState.valid(null,new int[]{0,0,0},new int[]{1,1,1},12),"missing save");
  System.out.println("17 checks passed");
 }
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
