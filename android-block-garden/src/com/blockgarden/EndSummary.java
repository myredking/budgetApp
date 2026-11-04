package com.blockgarden;
public final class EndSummary {
 public static String title(boolean missionMode,Mission mission,Engine engine){
  if(!missionMode)return "게임 종료";
  return mission.complete(engine)?"미션 성공!":"미션 실패";
 }
 public static String message(boolean missionMode,Mission mission,Engine engine,int best){
  String reason="더 이상 놓을 수 있는 블록이 없어요.";
  if(missionMode)reason=missionReason(mission,engine);
  return reason+"\n\n점수  "+engine.score+"\n최고 기록  "+best+"\n제거한 줄  "+engine.lines+"\n배치 횟수  "+engine.moves;
 }
 private static String missionReason(Mission mission,Engine engine){
  if(mission.complete(engine))return mission.stage+"단계 목표를 달성했어요!";
  String reason=mission.failed(engine)?"배치 횟수를 모두 사용했어요.":"더 이상 놓을 수 있는 블록이 없어요.";
  return reason+"\n"+mission.label(engine);
 }
}
