package com.blockgarden;
public class VerifyCampaign {
 public static void main(String[] args)throws Exception{
  java.io.PrintWriter proof=new java.io.PrintWriter(args[0],"UTF-8");
  proof.println("stage,seed,hand0,hand1,hand2,slot,row,column");
  for(int stage=1;stage<=100;stage++)verify(stage,proof);
  proof.close();System.out.println("100/100 cleared and replayed with combo scoring");
 }
 static void verify(int stage,java.io.PrintWriter proof){
  for(int seed=0;seed<300;seed++){
   int moves=Playability.play(stage,seed);if(moves<0)continue;
   Playability.replay(stage,Playability.trace.toString());proof.print(Playability.trace);return;
  }throw new AssertionError("no clear stage "+stage);
 }
}
