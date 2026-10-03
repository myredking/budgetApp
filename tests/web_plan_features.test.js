const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const logic = require('../web/plan_logic.js');

const appSource = fs.readFileSync(path.join(__dirname, '..', 'web', 'app.js'), 'utf8');
const htmlSource = fs.readFileSync(path.join(__dirname, '..', 'web', 'index.html'), 'utf8');

test('시간 제한 없음은 여유 있는 하루 범위를 만든다', () => {
  const range = logic.getDailyTimeRange('', '', true);

  assert.deepEqual(range, { startMinutes: 480, endMinutes: 1320, hours: 14, unlimited: true });
  assert.equal(logic.getTimeRangeLabel(range, '', ''), '시간 제한 없음');
  assert.deepEqual(logic.buildTimeSlots(4, range), ['08:00', '11:30', '15:00', '18:30']);
});

test('지정 시간이 없으면 잘못된 범위로 처리한다', () => {
  assert.equal(logic.getDailyTimeRange('', '20:00', false), null);
  assert.equal(logic.getDailyTimeRange('20:00', '10:00', false), null);
});

test('저장 계획 갱신과 삭제는 같은 계획을 중복 생성하지 않는다', () => {
  const first = { id: 'plan-1', savedAt: 'old', updatedAt: 'old' };
  const updated = { id: 'plan-1', savedAt: 'new', updatedAt: 'new' };

  const replaced = logic.upsertPlan([first], updated, 20);
  assert.equal(replaced.length, 1);
  assert.deepEqual(replaced[0], updated);
  assert.deepEqual(logic.removePlan(replaced, 'plan-1'), []);
});

test('여행 계획을 공유 해시로 직렬화하고 다시 복원한다', () => {
  const snapshot = {
    title: '가을 감성 · 서울숲',
    form: { destination: 'seoul', theme: 'date', noTimeLimit: true },
    places: [{ title: '서울숲 산책', category: '산책' }],
  };

  const hash = logic.createShareHash(snapshot);
  assert.match(hash, /^#plan=/);
  assert.deepEqual(logic.parseShareHash(hash), snapshot);
});

test('잘못된 공유 해시는 안전하게 무시한다', () => {
  assert.equal(logic.parseShareHash('#other=value'), null);
  assert.equal(logic.parseShareHash('#plan=%E0%A4%A'), null);
});

test('추천 장소 이름으로 네이버 지도 검색 링크를 만든다', () => {
  assert.equal(
    logic.createNaverSearchUrl('성수 브런치 스튜디오'),
    'https://map.naver.com/p/search/%EC%84%B1%EC%88%98%20%EB%B8%8C%EB%9F%B0%EC%B9%98%20%EC%8A%A4%ED%8A%9C%EB%94%94%EC%98%A4',
  );
});

test('행사 이름과 장소로 네이버 지도 상세 검색 링크를 만든다', () => {
  assert.equal(
    logic.createEventSearchUrl('강남페스티벌', '서울 강남구'),
    'https://map.naver.com/p/search/%EA%B0%95%EB%82%A8%ED%8E%98%EC%8A%A4%ED%8B%B0%EB%B2%8C%20%EC%84%9C%EC%9A%B8%20%EA%B0%95%EB%82%A8%EA%B5%AC',
  );
});

test('여행 시작일 기준으로 일정 날짜를 계산한다', () => {
  assert.equal(logic.getDayDateLabel('2026-10-02', 1), '10월 2일');
  assert.equal(logic.getDayDateLabel('2026-10-02', 3), '10월 4일');
  assert.equal(logic.getDayDateLabel('2026-02-31', 1), '');
});

test('데이터 출처에 맞는 상태 문구를 표시한다', () => {
  assert.equal(logic.getDataSourceLabel('naver', 'nearby'), '네이버 실시간');
  assert.equal(logic.getDataSourceLabel('naver_empty', 'nearby'), '네이버 검색 결과 없음');
  assert.equal(logic.getDataSourceLabel('loading', 'nearby'), '실시간 검색 중');
  assert.equal(logic.getDataSourceLabel('naver_error', 'nearby'), '네이버 조회 실패');
  assert.equal(logic.getDataSourceLabel('tour_api', 'events'), '관광공사 실시간');
  assert.equal(logic.getDataSourceLabel('loading', 'events'), '행사 조회 중');
  assert.equal(logic.getDataSourceLabel('tour_error', 'events'), '관광공사 조회 실패');
  assert.equal(logic.getDataSourceLabel('tour_empty', 'events'), '행사 없음');
  assert.equal(logic.getDataSourceLabel('demo', 'nearby'), '데모 모드');
});

test('검색 API와 지도 SDK 연결 상태를 구분한다', () => {
  assert.deepEqual(logic.getIntegrationStatus(true, 'naver', 'tour_api'), {
    title: '네이버 지도·지역 검색이 연결되어 있어요.',
    message: '실시간 검색 결과와 장소 위치를 지도에서 확인할 수 있어요.',
    status: '지도 연결됨',
  });
  assert.deepEqual(logic.getIntegrationStatus(false, 'naver', 'tour_api'), {
    title: '실시간 추천 데이터가 연결되어 있어요.',
    message: '장소·행사 검색은 연결됐어요. 지도 키를 연결하면 실제 지도와 마커도 표시돼요.',
    status: '검색 연결됨',
  });
  assert.deepEqual(logic.getIntegrationStatus(false, 'naver', 'demo'), {
    title: '일부 실시간 추천 데이터가 연결되어 있어요.',
    message: '장소 검색은 연결됐어요. 다른 추천 영역은 예시 데이터로 보여드려요.',
    status: '검색 일부 연결',
  });
  assert.deepEqual(logic.getIntegrationStatus(false, 'demo', 'tour_api'), {
    title: '일부 실시간 추천 데이터가 연결되어 있어요.',
    message: '행사 검색은 연결됐어요. 다른 추천 영역은 예시 데이터로 보여드려요.',
    status: '검색 일부 연결',
  });
  assert.deepEqual(logic.getIntegrationStatus(false, 'demo', 'demo'), {
    title: '추천 데이터 연결을 확인해 주세요.',
    message: '현재는 예시 추천을 보여드리고 있어요.',
    status: '데모 모드',
  });
  assert.deepEqual(logic.getIntegrationStatus(true, 'demo', 'demo'), {
    title: '네이버 지도는 연결되어 있어요.',
    message: '지도는 사용할 수 있지만 장소·행사 검색은 예시 추천을 보여드리고 있어요.',
    status: '지도만 연결됨',
  });
  assert.deepEqual(logic.getIntegrationStatus(true, 'naver_error', 'tour_empty'), {
    title: '네이버 지도는 연결되어 있어요.',
    message: '지도는 사용할 수 있지만 실시간 검색 결과를 확인하지 못했어요.',
    status: '지도만 연결됨',
  });
  assert.deepEqual(logic.getIntegrationStatus(true, 'naver', 'demo'), {
    title: '네이버 지도와 일부 실시간 추천이 연결되어 있어요.',
    message: '지도와 장소 검색은 사용할 수 있어요. 다른 추천 영역은 예시 데이터로 보여드려요.',
    status: '지도·검색 일부 연결',
  });
  assert.deepEqual(logic.getIntegrationStatus(true, 'demo', 'tour_api'), {
    title: '네이버 지도와 일부 실시간 추천이 연결되어 있어요.',
    message: '지도와 행사 검색은 사용할 수 있어요. 다른 추천 영역은 예시 데이터로 보여드려요.',
    status: '지도·검색 일부 연결',
  });
  assert.deepEqual(logic.getIntegrationStatus(false, 'loading', 'loading'), {
    title: '실시간 추천 데이터를 확인하고 있어요.',
    message: '장소·행사 검색 결과를 불러오는 중이에요.',
    status: '확인 중',
  });
  assert.deepEqual(logic.getIntegrationStatus(true, 'loading', 'loading'), {
    title: '네이버 지도를 준비하고 실시간 추천을 확인하고 있어요.',
    message: '지도와 장소·행사 검색을 불러오는 중이에요.',
    status: '지도·검색 확인 중',
  });
  assert.deepEqual(logic.getIntegrationStatus(false, 'naver', 'loading'), {
    title: '행사 추천을 확인하고 있어요.',
    message: '행사 검색 결과를 불러오는 중이에요. 장소 추천은 확인됐어요.',
    status: '행사 확인 중',
  });
  assert.deepEqual(logic.getIntegrationStatus(true, 'loading', 'tour_api'), {
    title: '네이버 지도와 장소 추천을 확인하고 있어요.',
    message: '지도와 장소 검색 결과를 불러오는 중이에요. 행사 추천은 확인됐어요.',
    status: '지도·장소 확인 중',
  });
  assert.deepEqual(logic.getIntegrationStatus(false, 'loading', 'tour_error'), {
    title: '장소 추천을 확인하고 있어요.',
    message: '장소 검색 결과를 불러오는 중이에요. 행사 검색에 문제가 있어요.',
    status: '장소 확인 중',
  });
  assert.deepEqual(logic.getIntegrationStatus(true, 'naver_empty', 'loading'), {
    title: '네이버 지도와 행사 추천을 확인하고 있어요.',
    message: '지도와 행사 검색 결과를 불러오는 중이에요. 장소 검색 결과가 없어요.',
    status: '지도·행사 확인 중',
  });
});

test('행사 API의 로딩·실패·응답 오류를 구분한다', () => {
  assert.match(appSource, /currentEventSource = 'loading'/);
  assert.match(appSource, /currentEventSource = 'tour_error'/);
  assert.match(appSource, /currentEventSource = 'tour_empty'/);
  assert.match(appSource, /applyEventResponse\(payload/);
  assert.match(appSource, /실시간 행사 조회 실패 · 예시 행사 표시/);
  assert.match(appSource, /행사 위치·상세 보기/);
  assert.match(appSource, /행사 조회에 실패했어요/);
});

test('오늘의 전국 추천을 선택하면 해당 지역 코스로 바로 전환한다', () => {
  assert.match(appSource, /data-today-destination/);
  assert.match(appSource, /today-plan-button/);
  assert.match(appSource, /destinationInput\.value = button\.dataset\.todayDestination/);
});

test('전국 추천과 실제 도시 동선을 구분해서 안내한다', () => {
  assert.match(appSource, /오늘 전국 추천 장소/);
  assert.match(appSource, /전국 후보예요/);
  assert.match(appSource, /도시 선택 후 이동 계산/);
});

test('평점·거리 정렬이 잠길 때 사용자에게 이유를 안내한다', () => {
  assert.match(appSource, /nearby-sort-hint/);
  assert.match(appSource, /평점순 \(상세 평점 없음\)/);
  assert.match(appSource, /거리순 \(내 위치 필요\)/);
});

test('새 배포본은 버전이 붙은 정적 파일을 요청한다', () => {
  assert.match(htmlSource, /styles\.css\?v=19/);
  assert.match(htmlSource, /plan_logic\.js\?v=19/);
  assert.match(htmlSource, /app\.js\?v=19/);
  assert.match(appSource, /register\('\.\/sw\.js\?v=19'\)/);
});

test('여행지 변경은 메인 맞춤 코스를 즉시 다시 렌더링한다', () => {
  const handler = appSource.match(
    /destinationInput\.addEventListener\('change', \(\) => \{([\s\S]*?)\n\}\);/,
  );

  assert.ok(handler);
  assert.match(handler[1], /renderPlan\(\);/);
});

test('현재 위치 좌표를 안전하게 검증한다', () => {
  assert.equal(logic.isValidLocation({ latitude: 37.5665, longitude: 126.978 }), true);
  assert.equal(logic.isValidLocation({ latitude: 95, longitude: 126.978 }), false);
  assert.equal(logic.isValidLocation(null), false);
});

test('주변 장소를 평점순과 거리순으로 정렬한다', () => {
  const places = [
    { title: '먼 곳', rating: 4.9, distance: '1.2km' },
    { title: '가까운 곳', rating: 4.6, distance: '350m' },
  ];

  assert.deepEqual(logic.sortNearbyItems(places, 'rating').map((place) => place.title), ['먼 곳', '가까운 곳']);
  assert.deepEqual(logic.sortNearbyItems(places, 'distance').map((place) => place.title), ['가까운 곳', '먼 곳']);
  assert.deepEqual(
    logic.sortNearbyItems([
      { title: '평점 없는 곳', rating: null },
      { title: '평점 있는 곳', rating: 4.2 },
    ], 'rating').map((place) => place.title),
    ['평점 있는 곳', '평점 없는 곳'],
  );
  assert.deepEqual(
    logic.sortNearbyItems([
      { title: '먼 곳', distance_meters: 1200 },
      { title: '가까운 곳', distance_meters: 350 },
    ], 'distance').map((place) => place.title),
    ['가까운 곳', '먼 곳'],
  );
});

test('이동수단별 예상 이동시간을 계산한다', () => {
  assert.equal(logic.getTransportMinutes('public'), 20);
  assert.equal(logic.getTransportMinutes('car'), 15);
  assert.equal(logic.getTransportMinutes('rental'), 10);
  assert.equal(logic.getTransportMinutes('unknown'), 20);
});

test('행사 날짜를 읽기 쉬운 한국어 범위로 표시한다', () => {
  assert.equal(logic.formatEventDateRange('20261002', '20261002'), '10월 2일');
  assert.equal(logic.formatEventDateRange('20261002', '20261004'), '10월 2일 ~ 10월 4일');
  assert.equal(logic.formatEventDateRange('invalid', '20261004'), '');
  assert.equal(logic.formatEventDateRange('20260230', '20260301'), '');
  assert.equal(logic.formatEventDateRange('20261004', '20261002'), '');
});

test('행사 API 응답을 실시간·없음·오류 상태로 분류한다', () => {
  const liveItems = [{ title: '축제' }];
  const render = (payload) => {
    const calls = [];
    const action = logic.applyEventResponse(payload, {
      live: (items) => calls.push(['live', items]),
      empty: () => calls.push(['empty']),
      error: () => calls.push(['error']),
    });
    return { action, calls };
  };

  assert.deepEqual(render({ source: 'tour_api', items: liveItems }), {
    action: { state: 'live', items: liveItems },
    calls: [['live', liveItems]],
  });
  assert.deepEqual(render({ source: 'tour_api', items: [] }), {
    action: { state: 'empty', items: [] },
    calls: [['empty']],
  });
  assert.deepEqual(render({ source: 'tour_api', items: null }), {
    action: { state: 'error', items: [] },
    calls: [['error']],
  });
  assert.deepEqual(render({ source: 'demo', items: [] }), {
    action: { state: 'error', items: [] },
    calls: [['error']],
  });
});

test('오래된 행사 응답은 최신 요청으로 인정하지 않는다', () => {
  assert.equal(logic.isCurrentEventRequest(4, 4), true);
  assert.equal(logic.isCurrentEventRequest(3, 4), false);
});

test('공개용 Site 파일은 원본 웹 파일과 동기화된다', () => {
  const publicAppSource = fs.readFileSync(path.join(__dirname, '..', 'travel-site', 'public', 'app.js'), 'utf8');
  const publicLogicSource = fs.readFileSync(path.join(__dirname, '..', 'travel-site', 'public', 'plan_logic.js'), 'utf8');
  const sourceWorker = fs.readFileSync(path.join(__dirname, '..', 'web', 'sw.js'), 'utf8');
  const publicWorker = fs.readFileSync(path.join(__dirname, '..', 'travel-site', 'public', 'sw.js'), 'utf8');
  assert.equal(publicAppSource, appSource);
  assert.equal(publicLogicSource, fs.readFileSync(path.join(__dirname, '..', 'web', 'plan_logic.js'), 'utf8'));
  assert.equal(publicWorker, sourceWorker);
});

test('서비스워커 캐시 버전은 새 배포본으로 갱신된다', () => {
  const sourceWorker = fs.readFileSync(path.join(__dirname, '..', 'web', 'sw.js'), 'utf8');
      assert.match(sourceWorker, /CACHE_NAME = 'courseon-shell-v8'/);
  assert.match(sourceWorker, /ASSET_VERSION = '19'/);
  assert.match(sourceWorker, /`\.\/app\.js\?v=\$\{ASSET_VERSION\}`/);
  assert.match(sourceWorker, /caches\.match\(`\.\/index\.html\?v=\$\{ASSET_VERSION\}`\)/);
});

test('행사 없음 상태는 데모 추천과 구분되는 안내를 만든다', () => {
  assert.equal(
    logic.getEventEmptyState('서울', '10월 3일'),
    '서울 · 10월 3일에는 확인된 행사가 없어요.',
  );
});

test('전국 주변 추천은 좌표가 있는 실제 관광지 검색으로 시작한다', () => {
  assert.deepEqual(
    logic.getNearbySearchContext('nationwide', '전국', '', 'all'),
    { query: '대한민국 관광지', category: '관광지' },
  );
  assert.deepEqual(
    logic.getNearbySearchContext('seoul', '서울', '', 'restaurant'),
    { query: '서울', category: '식당' },
  );
  assert.deepEqual(
    logic.getNearbySearchContext('nationwide', '전국', '', 'restaurant'),
    { query: '대한민국', category: '식당' },
  );
  assert.deepEqual(
    logic.getNearbySearchContext('nationwide', '전국', '', 'cafe'),
    { query: '대한민국', category: '카페' },
  );
});

test('네이버 결과가 없을 때는 데모 카드 대신 빈 결과 안내를 표시한다', () => {
  assert.match(appSource, /function renderNoLiveNearby\(\)/);
  assert.match(appSource, /payload\.source === 'naver'[\s\S]*renderNoLiveNearby\(\)/);
  assert.match(appSource, /nationwideCategoryQuery/);
});

test('실시간 주변 검색의 로딩·실패 상태를 사용자에게 알린다', () => {
  assert.match(appSource, /currentNearbySource = 'loading'/);
  assert.match(appSource, /currentNearbySource = 'naver_error'/);
  assert.match(appSource, /실시간 검색에 실패해 예시 추천을 보여드려요/);
  assert.match(appSource, /주변 장소를 불러오는 중이에요/);
  assert.match(appSource, /Array\.isArray\(payload\.items\)/);
  assert.doesNotMatch(appSource, /네이버 연동 후 실시간으로 바뀌어요/);
});

test('실시간 주변 장소를 일정 변경 후보로 연결한다', () => {
  assert.match(appSource, /function createLiveAlternative\(/);
  assert.match(appSource, /currentNearbySource === 'naver'/);
  assert.match(appSource, /place\.live/);
  assert.match(appSource, /평점 확인/);
  assert.match(appSource, /filter\(\(place\) => hasNumericRating\(place\.rating\)\)/);
});
