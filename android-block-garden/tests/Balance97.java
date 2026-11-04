package com.blockgarden;
public class Balance97 {
 public static void main(String[] args)throws Exception{
  int wins=0;java.io.PrintWriter proof=new java.io.PrintWriter(args[0],"UTF-8");
  proof.println("stage,seed,hand0,hand1,hand2,slot,row,column");
  for(int seed=0;seed<300;seed++){
   int moves=Playability.play(97,seed);
   if(moves<0)continue;
   Playability.replay(97,Playability.trace.toString());
   if(wins==0)proof.print(Playability.trace);wins++;
  }
  proof.close();System.out.println("97,"+wins+",300");
  if(wins<60)throw new AssertionError("balance success below 20%");
 }
}
