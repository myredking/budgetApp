package com.blockgarden;
public final class PlacementGuide {
 public static final class Preview {
  public final boolean valid;public final int rows,cols,lines;
  public Preview(boolean valid,int rows,int cols){this.valid=valid;this.rows=rows;this.cols=cols;lines=Integer.bitCount(rows)+Integer.bitCount(cols);}
 }
 public static final class Hint {
  public final int slot,row,col,lines;
  public Hint(int slot,int row,int col,int lines){this.slot=slot;this.row=row;this.col=col;this.lines=lines;}
 }
 public static Preview preview(Engine e,int[][] shape,int row,int col){
  if(!e.fits(shape,row,col))return new Preview(false,0,0);
  int[] rows=new int[8],cols=new int[8];
  for(int r=0;r<8;r++)for(int c=0;c<8;c++)if(e.board[r][c]!=0){rows[r]|=1<<c;cols[c]|=1<<r;}
  for(int[] p:shape){rows[row+p[0]]|=1<<(col+p[1]);cols[col+p[1]]|=1<<(row+p[0]);}
  return new Preview(true,fullMask(rows),fullMask(cols));
 }
 private static int fullMask(int[] masks){int result=0;for(int i=0;i<8;i++)if(masks[i]==255)result|=1<<i;return result;}
 public static Hint hint(Engine e,int[] hand,int[][][] shapes){
  Hint best=null;int score=-1;
  for(int slot=0;slot<hand.length;slot++){
   if(hand[slot]<0)continue;
   Hint candidate=bestForShape(e,shapes[hand[slot]],slot);
   if(candidate==null)continue;
   int value=value(e,candidate,shapes[hand[slot]]);
   if(value>score){score=value;best=candidate;}
  }return best;
 }
 private static Hint bestForShape(Engine e,int[][] shape,int slot){
  Hint best=null;int score=-1;
  for(int r=0;r<8;r++)for(int c=0;c<8;c++){
   Preview p=preview(e,shape,r,c);if(!p.valid)continue;
   Hint candidate=new Hint(slot,r,c,p.lines);int value=value(e,candidate,shape);
   if(value>score){score=value;best=candidate;}
  }return best;
 }
 private static int value(Engine e,Hint hint,int[][] shape){
  Preview p=preview(e,shape,hint.row,hint.col);int stars=0;
  for(int r=0;r<8;r++)for(int c=0;c<8;c++)if(e.stars[r][c]&&removed(p,r,c))stars++;
  return p.lines*100+stars*50+shape.length;
 }
 private static boolean removed(Preview p,int row,int col){return (p.rows&(1<<row))!=0||(p.cols&(1<<col))!=0;}
}
