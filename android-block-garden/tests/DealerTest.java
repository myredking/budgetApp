package com.blockgarden;
public class DealerTest {
 public static void main(String[] args){
  java.util.Random random=new java.util.Random(42);boolean[] seen=new boolean[12];
  for(int i=0;i<10000;i++){int id=BlockDealer.draw(random,12);if(id<1||id>=12)throw new AssertionError("single cell drawn");seen[id]=true;}
  for(int id=1;id<12;id++)if(!seen[id])throw new AssertionError("shape missing");
  int[] hand={0,-1,8};BlockDealer.migrate(hand,random,12);
  if(hand[0]<1||hand[1]!=-1||hand[2]!=8)throw new AssertionError("legacy migration");
 }
}
