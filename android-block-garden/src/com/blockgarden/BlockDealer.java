package com.blockgarden;
public final class BlockDealer {
 public static int draw(java.util.Random random,int shapeCount){return 1+random.nextInt(shapeCount-1);}
 public static int[] deal(java.util.Random random,Engine engine,int[][][] shapes){
  int[] result={draw(random,shapes.length),draw(random,shapes.length),draw(random,shapes.length)};
  boolean playable=false;for(int id:result)if(engine.canFit(shapes[id]))playable=true;
  if(playable)return result;
  java.util.ArrayList<Integer> valid=new java.util.ArrayList<>();
  for(int id=1;id<shapes.length;id++)if(engine.canFit(shapes[id]))valid.add(id);
  if(!valid.isEmpty())result[random.nextInt(3)]=valid.get(random.nextInt(valid.size()));
  return result;
 }
 public static void migrate(int[] hand,java.util.Random random,int shapeCount){for(int i=0;i<hand.length;i++)if(hand[i]==0)hand[i]=draw(random,shapeCount);}
}
