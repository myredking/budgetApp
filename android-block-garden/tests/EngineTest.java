package com.blockgarden;
public class EngineTest {
 public static void main(String[] args) {
  Engine e=new Engine();
  check(e.fits(new int[][]{{0,0}},7,7),"edge");
  check(!e.fits(new int[][]{{0,0},{1,0}},7,7),"bounds");
  for(int i=0;i<8;i++){e.board[0][i]=1;e.board[i][0]=1;}
  check(e.clear()==2,"simultaneous cross");
  check(e.board[0][0]==0,"cleared");
  e.board[2][2]=1;
  check(!e.place(new int[][]{{0,0}},2,2,2),"overlap");
  check(e.place(new int[][]{{0,0}},3,3,2),"placement");
  check(e.score==10,"score");
  for(int[] row:e.board)java.util.Arrays.fill(row,1);
  check(!e.canFit(new int[][]{{0,0}}),"game over");
  Engine lines=new Engine();
  for(int i=0;i<7;i++)lines.board[0][i]=1;
  check(lines.place(new int[][]{{0,0}},0,7,1),"complete line");
  check(lines.score==110,"line score");
  int before=e.score;check(!e.place(new int[][]{{0,0}},0,0,2),"reject full");
  check(e.score==before&&e.board[0][0]==1,"failure unchanged");
  check(SavedState.valid("0".repeat(64),new int[]{-1,0,11},new int[]{1,2,5},12),"valid save");
  check(!SavedState.valid("9".repeat(64),new int[]{0,0,0},new int[]{1,1,1},12),"bad board");
  check(!SavedState.valid("0".repeat(64),new int[]{12,0,0},new int[]{1,1,1},12),"bad shape");
  check(!SavedState.valid("0".repeat(64),new int[]{0,0,0},new int[]{0,1,1},12),"bad color");
  check(!SavedState.valid(null,new int[]{0,0,0},new int[]{1,1,1},12),"missing save");
  check(!SavedState.validStars("x".repeat(64)),"corrupt stars");
  check(SavedState.validStars("01".repeat(32)),"valid stars");
  comboTests();undoTests();resultTests();feedbackTests();balance97();missionTests();campaignTests();
  System.out.println("Campaign checks passed");
 }
 static void missionTests(){
  Mission m=new Mission(1);Engine e=new Engine();
  check(!m.complete(e),"initial mission");e.score=500;check(m.complete(e),"score goal");
  m=new Mission(2);e.lines=3;check(m.complete(e),"line goal");
  m=new Mission(3);e.lines=5;e.moves=20;check(m.complete(e),"last move wins");
  e.lines=4;check(m.failed(e),"move limit");
  m=new Mission(4);m.setup(e);check(e.stars[3][3],"star seeded");
  for(int i=0;i<8;i++)e.board[3][i]=1;e.clear();check(!e.stars[3][3],"star cleared");
  check(!m.complete(e),"remaining star");
  for(int i=0;i<8;i++)e.board[5][i]=1;e.clear();check(m.complete(e),"star mission done");
  Engine obstacle=new Engine();new Mission(7).setup(obstacle);
  check(obstacle.board[3][3]!=0,"obstacle seeded");
  check(!obstacle.fits(new int[][]{{0,0}},3,3),"obstacle blocks placement");
  check(new Mission(8).limit()==25,"final challenge limit");
 }
 static void campaignTests(){
  check(Mission.COUNT==100,"100 stages");
  check(new Mission(101).stage==100,"upper bound");
  check(new Mission(0).stage==1,"lower bound");
  check(new Mission(8).nextStage()==9,"continue old finale");
  check(new Mission(100).nextStage()==1,"campaign wrap");
  for(int stage=9;stage<=100;stage++)checkStage(stage);
 }
 static void balance97(){
  Mission m=new Mission(97);Engine e=new Engine();
  check(m.scoreGoal()==2800,"97 reduced goal");check(m.limit()==50,"97 extra moves");
  e.score=2799;check(!m.complete(e),"97 below goal");
  e.moves=50;check(m.failed(e),"97 limit fails");
  e.score=2800;check(m.complete(e)&&!m.failed(e),"97 last turn win");
  check(new Mission(93).scoreGoal()==3950,"93 unchanged");
  check(new Mission(98).limit()==38,"98 unchanged");
 }
 static void feedbackTests(){
  Engine e=new Engine();for(int i=0;i<8;i++){e.board[0][i]=1;e.board[i][0]=2;}
  check(e.clear()==2,"cross clear feedback");int cells=0;
  for(int[] row:e.cleared)for(int color:row)if(color!=0)cells++;
  check(cells==15,"cross particles deduplicated");
  check(e.cleared[0][0]==2,"cleared color preserved");
  check(e.clear()==0&&e.cleared[0][0]==0,"old particles reset");
 }
 static void resultTests(){
  Engine e=new Engine();e.score=123;e.lines=2;e.moves=7;
  check(EndSummary.title(false,new Mission(1),e).equals("게임 종료"),"free end title");
  check(EndSummary.message(false,new Mission(1),e,500).contains("123"),"result score");
  e.moves=20;check(EndSummary.message(true,new Mission(3),e,500).contains("배치 횟수"),"limit reason");
  e.lines=5;check(EndSummary.title(true,new Mission(3),e).equals("미션 성공!"),"success title");
 }
 static void undoTests(){
  Engine e=new Engine();e.score=300;e.moves=4;e.lines=2;e.board[1][2]=3;e.stars[1][2]=true;
  int[] hand={1,-1,4},tones={2,3,4};GameSnapshot snapshot=new GameSnapshot(e,hand,tones);
  e.board[1][2]=0;e.stars[1][2]=false;hand[0]=-1;tones[0]=5;
  Engine restored=snapshot.engine();check(restored.score==300&&restored.moves==4&&restored.lines==2,"undo counters");
  check(restored.board[1][2]==3&&restored.stars[1][2],"undo board stars");
  check(snapshot.hand()[0]==1&&snapshot.tones()[0]==2,"undo independent hand");
 }
 static void comboTests(){
  Engine e=new Engine();int[][] dot={{0,0}};
  for(int i=0;i<7;i++){e.board[0][i]=1;e.board[1][i]=1;}
  e.place(dot,0,7,1);check(e.combo==1&&e.score==110,"first clear combo");
  e.place(dot,1,7,1);check(e.combo==2&&e.score==320,"second clear bonus");
  GameSnapshot snapshot=new GameSnapshot(e,new int[]{0,0,0},new int[]{1,1,1});
  check(snapshot.engine().combo==2,"undo retains combo");
  e.place(dot,2,2,1);check(e.combo==0,"nonclear resets combo");
  e.combo=3;check(!e.place(dot,2,2,1)&&e.combo==3,"invalid preserves combo");
 }
 static void checkStage(int stage){
  Mission m=new Mission(stage);Engine e=new Engine();m.setup(e);
  check(!m.complete(e),"not initially won "+stage);
  check(m.limit()>=25,"reasonable move budget "+stage);
  check(e.canFit(new int[][]{{0,0},{0,1},{1,0},{1,1}}),"initial room "+stage);
  e.moves=m.limit();check(m.failed(e),"limit failure "+stage);
  e.score=m.scoreGoal();e.lines=m.lineGoal();
  for(boolean[] row:e.stars)java.util.Arrays.fill(row,false);
  check(m.complete(e),"goal success "+stage);
  check(!m.failed(e),"last turn success "+stage);
  check(!m.label(e).isEmpty(),"goal label "+stage);
 }
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
