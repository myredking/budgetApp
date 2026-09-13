# Suno 앨범 패키징용 n8n 실행 환경

이 폴더는 Google Sheets 감지, Google Drive 파일 정리, FFmpeg 음원 정규화를 실행할 로컬 n8n 환경입니다. n8n 데이터베이스와 자격 증명은 Docker 볼륨 `n8n_data`에 보존되고, 워크플로우 작업 파일은 호스트의 `n8n/work`와 컨테이너의 `/home/node/.n8n-files`에 연결됩니다.

## 1. 최초 설정

PowerShell에서 이 폴더로 이동한 뒤 예시 환경 파일을 복사합니다.

```powershell
Copy-Item .env.n8n.example .env.n8n
```

`.env.n8n`을 열어 `N8N_ENCRYPTION_KEY`를 32자 이상의 새 랜덤 문자열로 교체합니다. 이 키를 바꾸면 기존 n8n 자격 증명을 복호화할 수 없으므로, 처음 실행하기 전에 확정해야 합니다.

## 2. 실행

```powershell
docker compose up -d --build
```

브라우저에서 [http://localhost:5678](http://localhost:5678)을 열고 최초 관리자 계정을 생성합니다.

## 3. 동작 확인

컨테이너 안에서 FFmpeg가 설치되었는지 확인합니다.

```powershell
docker compose exec n8n ffmpeg -version
```

n8n 상태와 오류를 확인할 때는 다음을 사용합니다.

```powershell
docker compose ps
docker compose logs --follow n8n
```

정상 상태라면 `n8n` 서비스가 `healthy`로 표시되고, `ffmpeg -version`이 버전을 출력합니다.

## 4. n8n 워크플로우에서 사용할 경로

컨테이너 기준 경로를 사용해야 합니다.

| 용도 | 컨테이너 경로 |
| --- | --- |
| 임시 음원·커버·가사 | `/home/node/.n8n-files` |
| 호스트에 보이는 작업 폴더 | `n8n/work` |
| n8n 내부 데이터·자격 증명 | `/home/node/.n8n` |

예를 들어 `Read/Write Files from Disk` 노드와 `Execute Command` 노드에서는 `C:\...` 경로가 아니라 `/home/node/.n8n-files/...`를 사용합니다.

## 5. 보안 및 비용 주의

이 구성은 `Execute Command`를 사용하기 때문에 신뢰할 수 있는 단일 운영자용 로컬 환경을 전제로 합니다. `5678` 포트를 인터넷에 직접 공개하지 말고, 외부 공개가 필요해지면 HTTPS 리버스 프록시, 접근 제어, 별도 실행 워커를 추가로 설계해야 합니다.

`NODES_EXCLUDE: "[]"`는 n8n 2.0의 기본 차단을 해제하는 설정입니다. n8n Cloud에서는 `Execute Command`를 사용할 수 없으므로, 이 단계의 FFmpeg 방식은 self-hosted Docker 환경에서만 사용합니다.

## 6. 1차 워크플로우 가져오기

`workflows/01_album_folder_trigger.json`은 다음 흐름의 시작 템플릿입니다.

`Google Sheets Trigger → Tracks 전체 조회 → 장르·월별 Pending 곡수 계산 → 기존 폴더 검색 → 없을 때만 앨범 폴더 생성`

n8n이 실행된 뒤 다음 순서로 가져옵니다.

1. 우측 상단 메뉴에서 **Import from File**을 선택합니다.
2. `workflows/01_album_folder_trigger.json`을 선택합니다.
3. `YOUR_GOOGLE_SHEET_ID`를 실제 스프레드시트 ID로 바꿉니다.
4. `YOUR_GOOGLE_DRIVE_ROOT_FOLDER_ID`를 앨범 폴더를 만들 부모 폴더 ID로 바꿉니다.
5. Google Sheets Trigger, Read Tracks, Google Drive 노드에 같은 Google OAuth2 자격 증명을 선택합니다.
6. `Find Eligible Albums` Code 노드의 `MIN_TRACKS = 4`를 원하는 앨범 최소 곡 수로 조정합니다.
7. 테스트 실행 후 조건이 충족된 장르에 앨범 폴더가 1개만 생성되는지 확인하고 워크플로우를 활성화합니다.

현재 시트 헤더는 기존 구조를 그대로 사용합니다. 폴더 ID와 패키징 상태를 시트에 기록하는 작업은 다음 단계에서 `Album_ID`, `Package_Status` 컬럼을 추가할 때 연결합니다.

중지:

```powershell
docker compose stop
```

삭제가 아니라 일시 중지이므로 `n8n_data`와 작업 파일은 보존됩니다. 업데이트나 재시작은 다음처럼 실행합니다.

```powershell
docker compose pull
docker compose up -d --build
```

## 7. 검토 완료 앨범 자동 패키징·YouTube 업로드

전체 로컬 파이프라인은 `workflows/02_local_album_pipeline.json`입니다. 15분마다 다음 흐름을 실행합니다.

`생성 큐 확인 → Suno 음원 생성·로컬 저장 → sidecar 스캔 → 4곡 이상 앨범 그룹화 → 검토 통과 후보만 패키징 → FFmpeg 영상 생성 → YouTube 비공개 업로드`

워크플로우는 기본 비활성화이며, 매 실행마다 사전 점검을 먼저 수행합니다. `metadata-defaults`의 권리·재생·커버·아티스트 확인값이 `true`인 곡만 통과합니다. n8n을 시작하기 전에 다음 폴더와 파일을 준비합니다.

```powershell
New-Item -ItemType Directory -Force config,downloads/suno,outputs,secrets,.state
Copy-Item examples/suno_automation_defaults.json config/suno_automation_defaults.json
Copy-Item examples/suno_generation_queue.json config/suno_generation_queue.json
```

`config/suno_automation_defaults.json`에 실제 아티스트명, 법적 작곡가명, Suno Pro/Premier 증빙 경로를 넣고, 모든 곡의 재생·권리·커버 검토를 마친 뒤에만 확인값을 `true`로 바꿉니다. YouTube OAuth는 먼저 호스트에서 CLI로 한 번 인증해 `.state/youtube-token.json`을 만든 뒤 n8n을 활성화하는 편이 안전합니다.

`.env.n8n`에는 `SUNO_API_KEY`와 사용 중인 문서화된 Suno 호환 API의 `SUNO_API_BASE_URL`도 입력합니다. `config/suno_generation_queue.json`에 `job_id`, `title`, `genre`, `prompt`를 넣으면 워크플로우가 생성 결과를 `/workspace/downloads/suno`에 저장합니다. 큐가 비어 있으면 생성 API를 호출하지 않습니다. 생성 단계가 실패하면 같은 `automation-report.json`에 실패 상태와 pending 작업을 남기므로, `&&` 뒤의 앨범 단계가 실행되지 않아도 원인을 확인할 수 있습니다. 웹 쿠키나 브라우저 스크래핑은 사용하지 않습니다.

```powershell
docker compose up -d --build
```

`02_local_album_pipeline.json`을 가져온 뒤 테스트 실행을 하고, 검토된 앨범만 `outputs/albums/<album-id>`에 생성되는지 확인합니다. 워크플로우는 `--non-interactive` 모드라 브라우저 OAuth를 실행하지 않으며, 호스트에서 먼저 만든 유효한 `.state/youtube-token.json`이 없으면 중단됩니다. 이후 활성화하면 새 후보가 생길 때마다 재실행되며, 이미 업로드한 동일 영상은 파일 해시로 건너뜁니다. YouTube 공개 전환과 DistroKid 최종 제출은 여전히 사람이 직접 합니다.

이 Docker 구성은 로컬 단일 운영자용입니다. `Execute Command`를 사용하므로 `5678` 포트를 인터넷에 공개하지 마세요.
