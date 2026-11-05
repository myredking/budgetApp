package com.blockgarden;
public class FeelTest {
 public static void main(String[] args){
  for(int[] size:new int[][]{{360,640},{400,850},{1080,2200},{1440,2800},{600,800}}){
   GameLayout l=new GameLayout(size[0],size[1]);
   if(l.left<0||l.left+8*l.unit>size[0]+1||l.tray+232*l.scale>size[1])throw new AssertionError("layout bounds");
  }
  dealTest();
 }
 static void dealTest(){
  Engine e=new Engine();java.util.Random random=new java.util.Random(8);
  int[][][] shapes={{{0,0}},{{0,0},{0,1}},{{0,0},{1,0}},{{0,0},{0,1},{1,0},{1,1}}};
  for(int[] row:e.board)java.util.Arrays.fill(row,1);e.board[7][6]=0;e.board[7][7]=0;
  int[] hand=BlockDealer.deal(random,e,shapes);
  boolean fits=false;for(int id:hand){if(id==0)throw new AssertionError("single");fits|=e.canFit(shapes[id]);}
  if(!fits)throw new AssertionError("avoidable dead deal");
  if(e.board[7][6]!=0||e.moves!=0)throw new AssertionError("dealer mutated state");
 }
}
