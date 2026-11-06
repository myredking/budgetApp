# KBO 선수 프로필 자료

`kbo-player-profiles-2026-11-06.json`은 2026-11-06 [KBO 선수 조회](https://www.koreabaseball.com/Player/Search.aspx)의 구단별 검색 결과 1,043건입니다. 검색 페이지의 전체 건수와 실제 수집 건수를 비교하고, 페이지 간·구단 간 선수 ID 중복을 거절합니다.

| 구단 필터 | 조회 건수 |
|---|---:|
| KT | 101 |
| 삼성 | 111 |
| KIA | 109 |
| LG | 104 |
| 두산 | 95 |
| SSG | 98 |
| 롯데 | 125 |
| NC | 103 |
| 한화 | 96 |
| 키움 | 101 |

공식 선수 ID·이름·등번호·조회 팀·포지션·신장·체중·개별 프로필 URL을 보관합니다. 생년월일·학교·사진은 이 자료에 수집하지 않았습니다. 체격을 파싱하지 못한 5건은 `null`, 포지션이 없는 1건은 빈 문자열로 유지하며 수치를 만들어 넣지 않습니다.

`team`은 원래 조회 표기를 보존하고 `club`은 선택한 구단 필터입니다. 키움 필터에는 고양 선수 28건이 포함됩니다. 고양과 키움의 관계는 [히어로즈 공식 구단 소개](https://m.heroesbaseball.co.kr/mobile/heroes/introduce/introduce.do)로 확인했습니다.

이 자료는 **현재 선수 검색 명단**입니다. 2026 시즌 최종 등록 명단·1군 엔트리·전 선수 시즌 성적을 뜻하지 않습니다. 게임용 능력치나 신체·얼굴 모델도 포함하지 않습니다. Unity에서 현재 선택 가능한 카드는 기존 33장으로, 이 자료는 확장 준비용이며 이번 APK에 새 카드 1,043장을 추가한 것이 아닙니다.

재수집에는 `Tools/requirements-data.txt`를 설치하고 다음처럼 실행합니다. 날짜는 실제 조회일을 명시합니다.

```powershell
python unity-baseball-showcase/Tools/fetch_kbo_profiles.py --output unity-baseball-showcase/Data/kbo-player-profiles-2026-11-06.json --retrieved-on 2026-11-06
```

구단별 페이지 수집을 순차 실행하며 페이지 전환과 구단 수집 사이에 0.6초를 둡니다. 전체 검증이 성공한 뒤에만 최종 파일을 바꿉니다. 네트워크 없는 파서·페이지 완전성·소속 표기 검사는 `python -m pytest tests/test_kbo_roster_import.py -q`로 실행합니다.
