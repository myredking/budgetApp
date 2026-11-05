package com.blockgarden;
public final class BlockDealer {
 public static int draw(java.util.Random random,int shapeCount){return 1+random.nextInt(shapeCount-1);}
 public static void migrate(int[] hand,java.util.Random random,int shapeCount){for(int i=0;i<hand.length;i++)if(hand[i]==0)hand[i]=draw(random,shapeCount);}
}
