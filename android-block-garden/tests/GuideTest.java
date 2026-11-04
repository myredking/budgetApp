package com.blockgarden;
public class GuideTest {
 public static void main(String[] args){
  Engine e=new Engine();int[][] dot={{0,0}};
  for(int i=1;i<8;i++){e.board[0][i]=1;e.board[i][0]=2;}
  PlacementGuide.Preview p=PlacementGuide.preview(e,dot,0,0);
  check(p.valid&&p.lines==2&&p.rows==1&&p.cols==1,"cross preview");
  check(e.board[0][0]==0&&e.score==0&&e.lines==0,"preview no mutation");
  check(!PlacementGuide.preview(e,dot,0,1).valid,"overlap preview");
  check(!PlacementGuide.preview(e,dot,8,0).valid,"outside preview");
  hintTests(e,dot);
 }
 static void hintTests(Engine e,int[][] dot){
  int[][][] shapes={dot,{{0,0},{0,1}}};
  PlacementGuide.Hint h=PlacementGuide.hint(e,new int[]{-1,0,1},shapes);
  check(h!=null&&h.slot==1&&h.row==0&&h.col==0&&h.lines==2,"hint prefers clear");
  check(e.board[0][0]==0&&e.moves==0,"hint no mutation");
  check(PlacementGuide.hint(e,new int[]{-1,-1,-1},shapes)==null,"empty hand");
  for(int[] row:e.board)java.util.Arrays.fill(row,1);
  check(PlacementGuide.hint(e,new int[]{0,0,0},shapes)==null,"no moves");
  System.out.println("8 guide checks passed");
 }
 static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
