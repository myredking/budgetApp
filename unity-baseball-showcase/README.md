# Unity 타격 장면 품질 검증 준비

범위는 타자1명·투수1명·경기장1곳입니다. 기존 Android 시제품과 별도 프로젝트입니다.

**현재 상태: 무료 인체·의복·눈과 163개 뼈대를 연결한 공통 선수 원형의 Blender 렌더까지 확인했습니다. 김도영·류현진의 체격/투타를 바탕으로 동작 초안을 추가했습니다. 실제 선수 얼굴과 고유 폼의 정밀 재현·경기장·새 APK는 아직 완성되지 않았습니다.**

## 준비한 것

- 설치된 Unity 6.6 6000.6.8f1 / 공식 템플릿 기준 URP17.6.0 설정 및 Pipeline0.10.0-exp.1 연결
- 실제 인체 프리팹·Animator·모션을 요구하는 에셋 바인딩
- 에셋이 연결된 뒤 타자/투수/구장과 중계 카메라·조명을 배치하는 Editor 도구 초안
- 실제 에셋과 엔진/시각 검증이 없으면 준비 완료로 판정하지 않는 점검 도구
- 공식 MakeHuman 저장소의 CC0 인체 원본·골격·가중치 확보. `asset-acquisition.json`에 고정 버전/해시/출처 기록. 이것은 옷을 입고 모션이 적용된 완성 선수 모델이 아닙니다.

Unity 패키지 복원과 C# 컴파일은 확인했습니다. 엔진에서 FBX 피부 변형 모델·3초 이상 클립·손 뼈대 이동·피부 텍스처·바닥 재질 저장을 검사하는 EditMode 테스트 1건이 통과했습니다. 실제 Play 모드에서 프레임 증가와 모델 동작 변화도 확인했습니다. 기능 검증과 참고 이미지 수준의 품질 통과는 별개입니다.

## 엔진 설치 상태

Unity Hub 설치·로그인·Unity 6.6 (6000.6.8f1) 설치가 완료됐습니다. Android 제작에 필요한 Android Build Support는 아직 설치 목록에 없습니다. 현재 설치된 추가 모듈은 Web입니다.

## 선수별 동작 초안

대상은 KBO 공식 프로필의 김도영(183cm/85kg, 우투우타, 5번)과 류현진(190cm/113kg, 좌투우타, 99번)입니다. `player-profiles.json`에 출처와 확인일을 기록했습니다. 키 비율과 체격 폭을 구분했으며 체중은 신체 시뮬레이션으로 재현한 값이 아닙니다.

기존 인체 원형에 턱·볼 폭, 의상 색과 번호, 우타 스윙·좌완 투구 동작을 손으로 제작했습니다. 선수 얼굴은 아직 공통 원형에 가깝습니다. 관절 각도·타이밍은 사진을 참고한 제작자 추정이며 실제 선수의 모션 캡처/측정값이 아닙니다. 류현진은 과거 폼 참고가 포함돼 정확한 2026년 폼으로 검증하지 않았습니다. 유사도 승인 전까지 고유 폼 재현 완료로 표시하지 않습니다. 글러브도 부피 확인용 초안입니다.

`Tools/build_player_review.py`를 Blender에서 실행하면 97프레임(30fps, 3.2초)의 골격 동작을 굽고 FBX로 내보냅니다. 입력은 로컬의 기존 `GenericAthlete.blend` 또는 제작 캐시 `athlete-uniform.blend`입니다. 출력 원본은 `SourceArt/Players/`, Unity에 넣는 파일은 `Assets/Showcase/Art/Players/`의 FBX입니다. Blender 원본은 Unity 임포터가 별도 Blender 설치를 찾지 않도록 Assets 바깥에 보관합니다.

Unity 메뉴 `DeckBall > Player Study > Build`는 두 선수의 움직이는 모델과 조명·카메라만 갖춘 검토 장면을 만듭니다. 구장이나 플레이 가능한 경기 시스템은 포함하지 않습니다. `Capture`는 현재 장면의 실제 엔진 이미지를 `Reports/player-study-unity.png`에 저장합니다. 게임 완료 여부를 판단하는 기존 품질 게이트는 계속 별도로 유지합니다.

![실제 Unity 선수 검토 화면](Reports/player-study-unity.png)

![실제 Unity 재생 캡처](Reports/player-study-unity-motion.gif)

위 GIF는 실제 Unity Play 모드를 10.26초 동안 29번 캡처해 만든 검토 자료입니다. 캡처 간격이 일정하지 않아 게임의 목표 프레임률을 보여주는 영상은 아닙니다. 프레임 증가 증거는 `Reports/player-runtime-frame-check.json`, 시각별 캡처 기록은 `Reports/player-runtime-capture.json`, Blender의 손 목표 오차는 `Reports/player-motion-validation.json`에 있습니다. 이 검사는 참고 사진 수준의 외형이나 선수별 폼 유사도를 승인하지 않습니다.

검토 중 발견해 수정한 항목: 팔 도달 범위를 벗어난 투구 목표, 발목 방향, 배트 길이, 글러브 크기, 모자/머리 간섭, FBX 피부·신발 텍스처 누락, 카메라 뒤쪽 촬영, 바닥 재질 미저장. 기존 장면 변경 전 저장·취소 확인을 적용했고, 테스트는 수정 중인 기존 장면이 있거나 로컬 아트가 없으면 건너뜁니다. 무료 에셋 본체와 Blender 원본은 Git 제외 상태로 이 PC에만 있습니다.


## 이번에 만든 모델 원형

![Blender 모델 검토용 이미지](Reports/generic-athlete-blender-preview.png)

이 이미지는 Blender에서 실제 3D 모델을 렌더한 결과이며 Unity 플레이 화면이 아닙니다. 특정 KBO 선수의 얼굴을 재현하지 않았습니다. 기본 의복을 남색 상의와 흰 바지로 바꾸고 헬멧 원형을 추가했지만 야구복의 단추·팀 문양·봉제선·벨트와 장비 디테일은 미완성입니다. 헬멧 귀 보호대, 장갑, 배트, 구장과 경기 모션도 남아 있습니다. 참고 이미지 수준 검토는 통과하지 않았습니다.

로컬 원본: `SourceArt/Source-GenericAthlete.blend` (텍스처 내장), `Assets/Showcase/Art/Source/GenericAthlete.fbx` (골격 포함 내보내기). 두 파일은 Art 제외 규칙에 따라 Git에 올라가지 않습니다. 아직 Unity 임포트와 FBX 재임포트 시 외형·골격 유지 검증은 하지 않았습니다. 출처·해시는 `asset-acquisition.json`에 기록했습니다.

## 에셋 제작/연결 순서

1. MakeHuman 원본에 피부·눈·야구복을 만들거나 적합한 무료 에셋을 연결합니다.
2. 타격/투구 모션을 리타기팅하고 손-배트 부착, 공의 릴리스/접촉 프레임을 맞춥니다.
3. 실제 구장 모델·재질을 준비하고 아래 프리팹 이름으로 저장합니다.
4. Unity에서 URP 렌더 파이프라인을 구성합니다.
5. `DeckBall > Quality Shot > Create Asset Bindings`를 실행하고 프리팹과 모션을 배정합니다.
6. `Build Visual Review Scene`으로 정적 배치 초안을 만듭니다. 이 도구는 재생 가능한 타격 시스템을 완성하지 않습니다.
7. 실제 엔진 1920×1080 스틸과 10초 동영상을 기준 이미지와 비교합니다. 이 검증을 통과하기 전에는 품질 개선 완료로 부르지 않습니다.

`Assets/Showcase/Art/`에는 Batter.prefab, Pitcher.prefab, Stadium.prefab, Bat.prefab, Ball.prefab을 사용합니다. 에셋 본체는 별도로 보관하며 기본 Git 추적에서 제외했습니다.

## 테스트

```powershell
python -m pytest tests/test_baseball_showcase.py tests/test_player_motion.py -q
python unity-baseball-showcase/Tools/validate_assets.py
```

현재 점검 결과 `not_ready`가 정상입니다.

## 무료 후보 검토

- MakeHuman 코어 그래픽: https://www.makehumancommunity.org/about/license.html — CC0. 인체 원형으로 사용 가능하나 야구복·피부·모션 작업 필요.
- 무료 Animated baseball player: https://sketchfab.com/3d-models/animated-baseball-player-6b39fcafd2204b9fa9317f7b926807df — API 조회1051 vertices /1950 faces. 목표인 실사에 가까운 표현에는 부적합해 채택하지 않았습니다.
- Poly Haven: https://polyhaven.com/ — 환경광·표면 재질 후보. 아직 내려받거나 품질 검증하지 않았습니다.
- Mixamo: https://www.mixamo.com/ — 야구 모션 검토 후보이며 Adobe 로그인과 실제 클립/라이선스 확인이 필요합니다. 아직 확보하지 않았습니다.

유료 상품은 구매하거나 연결하지 않았습니다. 기존 `android-baseball/DeckBall.apk`는 이번 작업에서 변경하지 않았습니다.

엔진에서 실제 시각/모션 검토를 마친 뒤에만 바인딩의 해당 검토 필드를 체크하고 `Write Readiness Report`를 실행합니다. 자동 구조 점검과 두 검토가 모두 통과해야 Python 도구가 준비 완료로 판정합니다. 현재는 셋 모두 미완료입니다.

Unity 테스트는 Test Runner의 EditMode에서 `PlayerStudyTests.ImportedPlayersHaveSkinningAndMovingAnimation`을 실행합니다. 로컬 모델/텍스처와 저장된 기존 장면이 필요합니다. 초기 테스트는 도구 부재로 실패했고, 도구 추가 후 통과했습니다. 피부 텍스처·바닥 저장 회귀 검사도 실패를 확인한 뒤 수정했습니다.
