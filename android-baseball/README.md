# 덱볼 야구 / DeckBall 0.1.0

안드로이드 8.0 이상용 독립 제작 오프라인 2D 프로토타입입니다.
원작 APK·이미지·음원·선수 DB를 사용하지 않았습니다. 기본 33명은 가상 선수입니다.

## 설치와 플레이

1. `DeckBall.apk`를 휴대폰에 옮겨 파일 앱에서 엽니다.
2. 설치 화면에서 요청하면 해당 파일 앱의 '알 수 없는 앱 설치'를 허용합니다.
3. '선수 덱 / 타순 편집'에서 타자 9명과 투수 1명을 선택합니다.
4. 선수를 눌러 교체하고 ↑로 타순을 바꿉니다. 선수 중복은 차단합니다.
5. 커스텀 선수 만들기로 원하는 이름·구단/연도·포지션·능력치(1~99)를 입력할 수 있습니다.
6. 덱 3개를 저장할 수 있으며, 현재 덱과 커스텀 명단은 앱을 닫아도 보관합니다.
7. 경기 시작 후 수비에서는 구종·코스를 고르고 투구 준비 → 노란 구간에서 릴리스를 누릅니다.
8. 공격에서는 투구 요청 → 노란 구간에서 스윙을 누릅니다. 지켜보기로 공을 보낼 수 있습니다.
9. 감독 모드로 자동 진행하고 언제든 직접 조작으로 전환할 수 있습니다.

3이닝 경기이며 무승부가 가능합니다. 홈런·안타·볼넷·삼진·주자 진루·공수 교대를 지원합니다.
현재는 단순화한 확률 기반 규칙이며 파울·도루·병살·교체 투수·연장전·온라인 경기는 없습니다.
앱을 완전히 종료하거나 홈으로 나가면 진행 중인 경기는 보관하지 않습니다.
실제 KBO 선수 DB, 얼굴, 로고, 3D 경기 및 원작 APK 분석은 포함하지 않습니다.

## 개발 / 검증

Windows와 Python 3.10 이상이 필요합니다.

```powershell
python android-baseball/tools/bootstrap.py
python android-baseball/tools/build.py
python -m pytest tests/test_android_baseball.py -q
python -m radon cc android-baseball/tools tests/test_android_baseball.py -s
python -m lizard android-baseball/src android-baseball/tests -T cyclomatic_complexity=10 -T length=50 -w
```

bootstrap은 Microsoft JDK 17과 Google Android SDK 도구를 사용자 `.codex/android-deckball-tools`에 내려받습니다.
JDK가 이미 있으면 테스트는 JAVA_HOME 또는 PATH도 지원합니다. JDK가 없으면 Android 테스트는 건너뜁니다.
서명 키는 저장소 밖에 생성하는 개발용 키입니다. 배포용 앱 서명 및 Play Store 등록은 별도입니다.
APK 생성 후 apksigner 검증을 수행합니다. APK에 네트워크 권한은 없습니다.
테스트는 JVM에서 덱·득점·타격 및 투구 타이밍을 검증하며 Android UI 실행 검증을 대체하지 않습니다.

## 검증 기록

- 구현 전 테스트 실패 확인, 구현 후 엔진 테스트 통과.
- 커스텀 선수 덱 불러오기 회귀 테스트 실패 확인 후 수정 및 통과.
- QA 검토: 커스텀 덱 복원, 입력 폼 스크롤, 종료 확인 일시정지, JDK 탐색 수정.
- APK v2/v3 서명 검증 및 Android 패키지 정보 검증.
- 실제 Android 기기 및 에뮬레이터 실행은 아직 검증하지 못했습니다.
- 저장소 전체 테스트: tests 범위 288 통과, 기존 여행 웹 런타임 테스트 1건 실패.
- 범위 없는 pytest는 기존 outputs/travel-rea의 외부 패키지 테스트 수집 오류로 중단됩니다.
