package com.blockgarden;
public class Engine {
 public final int[][] board=new int[8][8];
 public int score=0,lines=0,moves=0;
 public final boolean[][] stars=new boolean[8][8];
 public int starCount(){int count=0;for(boolean[] row:stars)for(boolean star:row)if(star)count++;return count;}
 public boolean fits(int[][] shape,int r,int c){
  for(int[] p:shape){int y=r+p[0],x=c+p[1];
   if(y<0||x<0||y>=8||x>=8)return false;
   if(board[y][x]!=0)return false;
  }return true;
 }
 public boolean place(int[][] shape,int r,int c,int color){
  if(!fits(shape,r,c))return false;
  for(int[] p:shape)board[r+p[0]][c+p[1]]=color;
  int cleared=clear();lines+=cleared;moves++;score+=shape.length*10+cleared*cleared*100;return true;
 }
 public int clear(){
  boolean[] rows=new boolean[8],cols=new boolean[8];int count=0;
  for(int i=0;i<8;i++){rows[i]=full(i,true);cols[i]=full(i,false);
   if(rows[i])count++;if(cols[i])count++;
  }
  for(int r=0;r<8;r++)for(int c=0;c<8;c++)if(rows[r]||cols[c]){board[r][c]=0;stars[r][c]=false;}
  return count;
 }
 private boolean full(int index,boolean row){
  for(int i=0;i<8;i++)if((row?board[index][i]:board[i][index])==0)return false;
  return true;
 }
 public boolean canFit(int[][] shape){
  for(int r=0;r<8;r++)for(int c=0;c<8;c++)if(fits(shape,r,c))return true;
  return false;
 }
}
