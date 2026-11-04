package com.blockgarden;
import java.util.*;
public class Playability {
 static final int[][][] SHAPES={{{0,0}},{{0,0},{0,1}},{{0,0},{1,0}},{{0,0},{0,1},{0,2}},{{0,0},{1,0},{2,0}},{{0,0},{0,1},{1,0},{1,1}},{{0,0},{1,0},{1,1}},{{0,1},{1,0},{1,1},{1,2}},{{0,0},{1,0},{2,0},{2,1}},{{0,0},{0,1},{0,2},{0,3}},{{0,0},{0,1},{1,1},{1,2}},{{0,0},{0,1},{0,2},{1,0},{1,1},{1,2},{2,0},{2,1},{2,2}}};
 static Engine copy(Engine e){Engine n=new Engine();n.combo=e.combo;n.score=e.score;n.lines=e.lines;n.moves=e.moves;for(int r=0;r<8;r++){n.board[r]=e.board[r].clone();n.stars[r]=e.stars[r].clone();}return n;}
 static double value(Engine e,Engine old,Random random){
  return (e.lines-old.lines)*180+(old.starCount()-e.starCount())*70+structure(e)+random.nextDouble()*8;
 }
 static double structure(Engine e){
  double score=0;int filled=0;
  for(int r=0;r<8;r++){int row=0,col=0;for(int c=0;c<8;c++){if(e.board[r][c]!=0)row++;if(e.board[c][r]!=0)col++;}score+=row*row+col*col;filled+=row;}
  for(int r=0;r<8;r++)for(int c=0;c<8;c++)if(e.board[r][c]==0)score-=holeCost(e,r,c);
  return score-filled*3;
 }
 static int holeCost(Engine e,int r,int c){
  int n=0;if(empty(e,r-1,c))n++;if(empty(e,r+1,c))n++;
  if(empty(e,r,c-1))n++;if(empty(e,r,c+1))n++;
  return n==0?80:(n==1?20:0);
 }
 static boolean empty(Engine e,int r,int c){return r>=0&&r<8&&c>=0&&c<8&&e.board[r][c]==0;}
 static int[] choose(Engine e,int[] hand,Random random){
  double best=-1e9;int[] choice=null;
  for(int slot=0;slot<3;slot++){if(hand[slot]<0)continue;
   for(int r=0;r<8;r++)for(int c=0;c<8;c++){
    if(!e.fits(SHAPES[hand[slot]],r,c))continue;
    Engine n=copy(e);n.place(SHAPES[hand[slot]],r,c,1);double v=value(n,e,random);
    if(v>best){best=v;choice=new int[]{slot,r,c};}
   }
  }return choice;
 }
 static final StringBuilder trace=new StringBuilder();
 static int play(int stage,int seed){
  trace.setLength(0);
  Mission m=new Mission(stage);Engine e=new Engine();m.setup(e);Random random=new Random(seed);int[] hand={-1,-1,-1};
  while(e.moves<200){
   if(m.complete(e))return e.moves;if(m.failed(e))return -1;
   if(hand[0]+hand[1]+hand[2]==-3)for(int i=0;i<3;i++)hand[i]=random.nextInt(SHAPES.length);
   int[] p=choose(e,hand,random);if(p==null)return -1;
   trace.append(stage+","+seed+","+hand[0]+","+hand[1]+","+hand[2]+","+p[0]+","+p[1]+","+p[2]+"\n");
   if(!e.place(SHAPES[hand[p[0]]],p[1],p[2],1))throw new AssertionError("illegal move");hand[p[0]]=-1;
  }return -1;
 }
 static void replay(int stage,String data){
  Engine e=new Engine();Mission m=new Mission(stage);m.setup(e);int[] expected={-1,-1,-1};
  for(String line:data.split("\n")){
   String[] cells=line.split(",");int[] h=new int[3];for(int i=0;i<3;i++)h[i]=Integer.parseInt(cells[i+2]);
   if(expected[0]+expected[1]+expected[2]!=-3&&!Arrays.equals(expected,h))throw new AssertionError("hand mismatch");
   if(m.complete(e)||m.failed(e))throw new AssertionError("move after end");
   int slot=Integer.parseInt(cells[5]),r=Integer.parseInt(cells[6]),c=Integer.parseInt(cells[7]);
   if(!e.place(SHAPES[h[slot]],r,c,1))throw new AssertionError("replay illegal");h[slot]=-1;expected=h;
  }
  if(!m.complete(e))throw new AssertionError("replay incomplete "+stage);
 }
 public static void main(String[] args)throws Exception{
  java.io.PrintWriter proof=new java.io.PrintWriter(args[0],"UTF-8");
  proof.println("stage,seed,hand0,hand1,hand2,slot,row,column");
  int won=0;for(int stage=1;stage<=100;stage++){int wins=0,first=-1,moves=-1;
   for(int seed=0;seed<300;seed++){int result=play(stage,seed);if(result>=0){wins++;if(first<0){first=seed;moves=result;replay(stage,trace.toString());proof.print(trace);}}}
   if(wins>0)won++;System.out.println(stage+","+wins+",300,"+first+","+moves);
  }proof.close();System.out.println("CLEARED="+won+"/100");if(won<100)System.exit(1);
 }
}

