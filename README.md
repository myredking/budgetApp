# budgetApp

가계부 앱 샘플 프로젝트와 Suno 발매 패키지 도구를 포함합니다.

## Suno → DistroKid 발매 패키지

Suno에서 다운로드한 음원, 커버 이미지, 가사를 준비한 뒤 메타데이터 JSON과 함께 실행합니다.

```powershell
python -m budget.suno_release `
  --metadata examples/suno_release_metadata.json `
  --audio path/to/track.wav `
  --artwork path/to/cover.jpg `
  --lyrics path/to/lyrics.txt `
  --output outputs/releases
```

도구는 다음을 자동으로 처리합니다.

- DistroKid 허용 음원 형식, 1GB 크기·5시간 미만 길이, 커버 이미지 검수
- Suno Pro/Premier 권리 기록과 실제 증빙 파일 확인 게이트
- DistroKid AI 크레딧과 업로드용 메타데이터 생성
- 음원·커버·가사·권리 기록·업로드 체크리스트를 한 폴더에 정리

최종 로그인과 DistroKid 제출은 사용자가 체크리스트를 검토한 뒤 직접 진행합니다.
예시 메타데이터의 `songwriter_name`, `rights_confirmed`, `rights_evidence_path`,
`artwork_reviewed`는 실제 확인을 마친 뒤 알맞게 바꿔야 합니다.
`audio_scope=all`이면 `artist_persona_confirmed`도 `true`로 바꿔야 합니다.
음원을 처음부터 끝까지 재생 확인한 뒤 `audio_reviewed`를 `true`로 바꿔야 합니다.
가사가 없는 곡은
`instrumental`을 `true`로 설정해야 합니다.

`rights_evidence_path`는 실제로 존재하는 Suno 구독·권리 증빙 파일 경로여야 합니다.
압축 음원(예: MP3/M4A/FLAC)은 컨테이너 구조를 검사하고, 최종 제출 전 체크리스트에서
직접 재생해 끝까지 정상인지 확인합니다.

## Step 1: Suno 음원 생성 및 Google Sheets 기록

### 1. 설치

```powershell
python -m pip install -r requirements.txt
```

### 2. Google Sheets 준비

1. Google Cloud 프로젝트를 만들고 Google Sheets API를 활성화합니다.
2. 서비스 계정을 만든 뒤 JSON 키를 `secrets/google-service-account.json`으로 저장합니다.
3. Google Sheets의 `Tracks` 시트를 만들고 아래 헤더를 1행에 넣습니다.

   `Track_ID | Title | Genre | Audio_URL | Cover_URL | Lyrics | Status | Created_At`

4. 서비스 계정 이메일을 해당 스프레드시트에 편집자로 공유합니다.
5. `.env.example`을 `.env`로 복사하고 API 키, 스프레드시트 ID를 입력합니다.

### 3. 실행

```powershell
python -m budget.suno_ingest `
  --title "Midnight Signal" `
  --genre "Electronic" `
  --prompt "A nocturnal electronic track with warm synths and emotional vocals" `
  --output downloads/suno
```

생성 완료 응답의 모든 클립을 `Pending` 상태로 기록하고, 음원·커버·가사를 `downloads/suno`에 저장합니다. 로컬 다운로드가 실패해도 URL은 시트에 기록하여 후속 재처리가 가능하도록 합니다.

### 4. Suno Pro 다운로드 한도 보호

Suno Pro 기준 월 20곡을 기본 한도로 사용합니다. 동일한 `Track_ID`의 재시도는 다시 차감하지 않으며, 상태는 `.state/suno.downloads.json`에 저장됩니다.

`.env`에서 실제 Suno 결제 갱신일을 설정합니다.

```dotenv
SUNO_MONTHLY_DOWNLOAD_LIMIT=20
SUNO_BILLING_DAY=1
SUNO_QUOTA_STATE_FILE=.state/suno.downloads.json
```

결제 갱신일이 매월 15일이면 `SUNO_BILLING_DAY=15`로 설정합니다. 한도에 도달하면 새 곡 처리를 중단하고, 이미 예약된 `Track_ID`는 실패 후 재실행할 수 있습니다.

이 모듈의 기본 URL은 문서화된 외부 Suno-compatible API 예시입니다. Suno Inc. 공식 API 접근 권한이 있다면 `SUNO_API_BASE_URL`과 응답 어댑터를 해당 공식 계약에 맞춰 설정해야 하며, Suno 웹 쿠키·내부 엔드포인트·스크래핑 방식은 사용하지 않습니다.

## Step 7: n8n + FFmpeg 로컬 실행 환경

앨범 폴더 생성과 MP3 정규화를 n8n에서 실행하려면 self-hosted Docker 환경을 사용합니다. `n8n/Dockerfile`은 n8n 이미지에 FFmpeg를 추가하고, `n8n/docker-compose.yml`은 n8n 데이터와 작업 파일을 지속 저장하도록 구성되어 있습니다.

자세한 설정과 실행 명령은 [n8n/README.md](n8n/README.md)를 참고하세요.

1차 앨범 폴더 생성 워크플로우는 [n8n/workflows/01_album_folder_trigger.json](n8n/workflows/01_album_folder_trigger.json)에서 가져올 수 있습니다.

## Suno 자동화 운영 정책과 앨범 파이프라인

이 저장소의 기본 흐름은 다음과 같습니다.

`Suno 생성/다운로드 → 로컬 sidecar 저장 → 4곡 이상 단위 앨범 후보 → 무료 로컬 커버 생성 → 사람 검토 → DistroKid 패키지 → FFmpeg 영상 → YouTube 비공개 업로드`

### 비용을 낮추는 기본 구성

- 음원·가사·커버·메타데이터·YouTube 업로드 상태는 로컬 디스크에 저장합니다.
- 커버는 외부 이미지 API를 호출하지 않고 Pillow로 결정론적으로 생성합니다. 앨범 제목과 아티스트가 같으면 같은 커버가 재현되며, 다른 앨범과 동일한 커버는 차단합니다.
- 영상은 로컬 FFmpeg로 커버 한 장과 앨범 전체 음원을 합칩니다. 영상 변환 SaaS나 스토리지를 사용하지 않습니다.
- YouTube는 공식 Data API의 무료 OAuth 업로드만 사용합니다. 첫 OAuth 인증 후 토큰을 로컬에 보관하고, 파일 해시로 중복 업로드를 막습니다. API 쿼터를 아끼기 위해 검색 API는 사용하지 않습니다.
- DistroKid 최종 제출은 자동화하지 않습니다. 공식 업로드 계약이 없는 화면 조작 자동화는 계정·정책 리스크가 있으므로, 검수된 앨범 폴더를 사람이 로그인해 업로드합니다.

### 1. Suno 다운로드 결과를 앨범 후보로 모으기

`suno_ingest`로 받은 각 곡은 `downloads/suno`에 음원·커버·가사와 함께 `*.track.json` sidecar를 남깁니다. sidecar는 나중에 원본 URL, Track_ID, 생성/다운로드 시각을 확인할 수 있게 합니다.

먼저 [examples/suno_automation_defaults.json](examples/suno_automation_defaults.json)을 복사해 실제 아티스트·법적 작곡가명·Suno 증빙 경로를 입력합니다. `rights_confirmed`, `artwork_reviewed`, `audio_reviewed`, `artist_persona_confirmed`는 실제 확인 전까지 `false`로 유지합니다.

자동 실행 전에 다음 사전 점검을 실행합니다. 이 명령은 폴더를 만들거나 API를 호출하지 않습니다.

```powershell
python -m budget.suno_pipeline `
  --input downloads/suno `
  --output outputs/albums `
  --metadata-defaults config/suno_automation_defaults.json `
  --preflight-only
```

`--preflight-only`는 점검만 하고 종료합니다. `--preflight`를 빌드·영상·업로드 옵션과 함께 쓰면 점검을 통과한 뒤에만 다음 단계가 실행됩니다. FFmpeg가 필요한 영상 작업에서는 FFmpeg 설치 여부를, 실제 YouTube 업로드에서는 OAuth 파일 준비 여부를 함께 확인하며 점검이 실패하면 n8n도 해당 실행을 중단합니다.

```powershell
python -m budget.suno_pipeline `
  --input downloads/suno `
  --output outputs/albums `
  --metadata-defaults config/suno_automation_defaults.json
```

기본값은 `min_tracks=4`, `max_tracks=12`입니다. 같은 아티스트·장르·생성 월의 곡만 묶고, 4곡 미만 그룹은 발매 폴더를 만들지 않습니다. 결과의 `outputs/albums/covers`와 `outputs/albums/plans`를 확인합니다.

### 2. 사람 검토 후 앨범 패키지와 YouTube 영상 만들기

각 곡을 처음부터 끝까지 재생하고, 권리 증빙·샘플·커버곡 여부·아티스트 페르소나·AI 크레딧을 확인한 뒤 defaults 파일의 관련 확인값을 `true`로 바꿉니다. 커버도 직접 확인해야 합니다. 조건을 충족하지 못한 곡은 자동으로 건너뛰며, `--build`가 발매 파일을 강제로 만들지 않습니다.

```powershell
python -m budget.suno_pipeline `
  --input downloads/suno `
  --output outputs/albums `
  --metadata-defaults config/suno_automation_defaults.json `
  --build `
  --render-video
```

앨범 폴더에는 DistroKid용 `distrokid/upload-checklist.md`, 곡별 음원·가사·메타데이터, `artwork/cover.jpg`, YouTube용 `youtube/metadata.json`·`youtube/concat.txt`·MP4 경로가 생깁니다. FFmpeg가 PATH에 있어야 합니다.

### 3. YouTube 전송 점검과 실제 업로드

먼저 전송 없이 점검합니다.

```powershell
python -m budget.suno_pipeline `
  --input downloads/suno `
  --output outputs/albums `
  --metadata-defaults config/suno_automation_defaults.json `
  --build `
  --render-video `
  --upload-youtube `
  --dry-run
```

실제 업로드 전 Google Cloud에서 YouTube Data API를 켜고 OAuth Desktop Client JSON을 `secrets/youtube-client.json`으로 저장합니다. 첫 실행은 브라우저 인증을 열며, 업로드는 `private`로 고정됩니다.

```powershell
python -m budget.suno_pipeline `
  --input downloads/suno `
  --output outputs/albums `
  --metadata-defaults config/suno_automation_defaults.json `
  --build `
  --render-video `
  --upload-youtube
```

n8n 예약 실행은 브라우저를 열 수 없도록 `--non-interactive` 모드로 고정되어 있습니다. 호스트에서 1회 OAuth 인증을 끝내 `.state/youtube-token.json`을 만든 뒤에만 n8n 워크플로우를 활성화하세요.

YouTube 설명에는 AI 음악 사용 사실을 넣고 API의 `containsSyntheticMedia=true`를 전송합니다. 공개 전환은 YouTube Studio에서 사람이 확인한 뒤 진행합니다.

### 발매 차단 정책

- Suno Basic/무료 플랜 생성물은 상업 배포 후보에서 차단합니다. Pro/Premier에서 생성·다운로드한 곡만 실제 구독 증빙과 함께 통과시킵니다.
- 타인의 가사·샘플·목소리·스타일 모방, 커버곡 무허가, 대량 생성 스팸, 중복 커버는 통과시키지 않습니다.
- DistroKid 커버는 RGB 정사각 JPG(최소 1000×1000, 권장 3000×3000)로 준비하고 URL·QR·플랫폼 로고·가격·무단 사진을 넣지 않습니다.
- `audio_scope=all`인 AI 음원은 DistroKid AI 크레딧과 사람/AI 아티스트 페르소나 선택을 확인해야 합니다.
- YouTube 업로드는 `private`, 구독자 알림 끔, 합성 미디어 표시 켬이 기본입니다. 실제 공개는 별도 사람 승인입니다.

Suno API 또는 호환 제공자를 통한 자동 생성 자체는 서비스별 과금·약관이 다를 수 있습니다. 이 저장소의 무료 구성은 생성 이후의 정리·검수·앨범 묶기·커버·영상·YouTube 업로드에 적용되며, Suno 웹 쿠키·내부 엔드포인트·스크래핑은 사용하지 않습니다.
