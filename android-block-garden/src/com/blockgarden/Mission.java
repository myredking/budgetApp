package com.blockgarden;
public final class Mission {
 public static final int COUNT=8;
 public final int stage;
 public Mission(int stage){this.stage=Math.max(1,Math.min(COUNT,stage));}
 public void setup(Engine e){
  if(stage<4)return;
  e.stars[3][3]=true;e.stars[5][5]=true;
  if(stage>=6){e.stars[1][6]=true;e.stars[6][1]=true;}
  if(stage>=7)seedObstacles(e);
 }
 private void seedObstacles(Engine e){for(int r=0;r<8;r++)for(int c=0;c<8;c++)if(e.stars[r][c])e.board[r][c]=4;}
 public int limit(){if(stage==8)return 25;return stage==3?20:(stage>=5?35:0);}
 public boolean complete(Engine e){
  if(stage==1)return e.score>=500;
  if(stage==2)return e.lines>=3;
  if(stage==3)return e.lines>=5;
  return e.starCount()==0;
 }
 public boolean failed(Engine e){return limit()>0&&e.moves>=limit()&&!complete(e);}
 public String label(Engine e){
  if(stage==1)return "500점 달성 · "+e.score+" / 500";
  if(stage==2)return "3줄 제거 · "+e.lines+" / 3";
  if(stage==3)return "20회 안에 5줄 · "+e.lines+" / 5 · 남은 횟수 "+Math.max(0,20-e.moves);
  String goal=(stage>=7?"장애물 제거 · 남은 칸 ":"별 칸 제거 · 남은 별 ")+e.starCount();
  return limit()==0?goal:goal+" · 남은 횟수 "+Math.max(0,limit()-e.moves);
 }
}
