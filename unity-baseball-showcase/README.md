# Unity 타격 장면 품질 검증 준비

범위는 타자1명·투수1명·경기장1곳입니다. 기존 Android 시제품과 별도 프로젝트입니다.

**현재 상태: 무료 인체·의복·눈과 163개 뼈대를 연결한 공통 선수 원형의 Blender 렌더까지 확인했습니다. Unity 게임 화면·야구 모션·새 APK는 아직 완성되지 않았습니다.**

## 준비한 것

- Unity6 LTS 6000.0.62f1 / URP17.0.4 프로젝트 설정
- 실제 인체 프리팹·Animator·모션을 요구하는 에셋 바인딩
- 에셋이 연결된 뒤 타자/투수/구장과 중계 카메라·조명을 배치하는 Editor 도구 초안
- 실제 에셋과 엔진/시각 검증이 없으면 준비 완료로 판정하지 않는 점검 도구
- 공식 MakeHuman 저장소의 CC0 인체 원본·골격·가중치 확보. `asset-acquisition.json`에 고정 버전/해시/출처 기록. 이것은 옷을 입고 모션이 적용된 완성 선수 모델이 아닙니다.

**Unity가 없어 C# 컴파일, 패키지 복원, 렌더링과 실제 에셋 임포트는 미검증입니다.** Python 점검 테스트 통과는 Unity 실행 성공을 뜻하지 않습니다.

## 엔진 설치 상태

Unity Hub 설치와 로그인은 완료했습니다. 사용자가 시작한 Unity 6.6 (6000.6.8f1) 설치는 아직 공식 CLI의 설치 완료 목록에 나타나지 않습니다. 기존 준비 설정은 6000.0.62f1용이므로 실제 설치 버전에 맞춰 패키지와 프로젝트 설정을 확인해야 합니다. Android 제작에는 Android Build Support, SDK/NDK/OpenJDK도 필요합니다.

## 이번에 만든 모델 원형

![Blender 모델 검토용 이미지](Reports/generic-athlete-blender-preview.png)

이 이미지는 Blender에서 실제 3D 모델을 렌더한 결과이며 Unity 플레이 화면이 아닙니다. 특정 KBO 선수의 얼굴을 재현하지 않았습니다. 기본 의복을 남색 상의와 흰 바지로 바꾸고 헬멧 원형을 추가했지만 야구복의 단추·팀 문양·봉제선·벨트와 장비 디테일은 미완성입니다. 헬멧 귀 보호대, 장갑, 배트, 구장과 경기 모션도 남아 있습니다. 참고 이미지 수준 검토는 통과하지 않았습니다.

로컬 원본: `Assets/Showcase/Art/Source/GenericAthlete.blend` (텍스처 내장), `GenericAthlete.fbx` (골격 포함 내보내기). 두 파일은 Art 제외 규칙에 따라 Git에 올라가지 않습니다. 아직 Unity 임포트와 FBX 재임포트 시 외형·골격 유지 검증은 하지 않았습니다. 출처·해시는 `asset-acquisition.json`에 기록했습니다.

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
python -m pytest tests/test_baseball_showcase.py -q
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
