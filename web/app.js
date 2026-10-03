const DESTINATIONS = {
  seoul: {
    label: '서울',
    area: '성수 · 서울숲',
    places: [
      { time: '10:30', category: '브런치', title: '성수 브런치 스튜디오', duration: '1시간 20분', distance: '도보 6분', rating: 4.8, reviews: '1,240', review: '분위기와 메뉴 구성이 좋아요', cost: 22000 },
      { time: '12:20', category: '전시', title: '오늘의 작은 전시관', duration: '1시간 30분', distance: '도보 11분', rating: 4.7, reviews: '856', review: '사진 남기기 좋은 공간이에요', cost: 12000 },
      { time: '14:20', category: '산책', title: '서울숲 느린 산책길', duration: '1시간', distance: '도보 8분', rating: 4.9, reviews: '2,018', review: '잠깐 쉬어가기 좋은 코스예요', cost: 0 },
      { time: '16:00', category: '카페', title: '서울숲 커피 라운지', duration: '1시간 20분', distance: '도보 9분', rating: 4.6, reviews: '974', review: '조용하고 오래 머물기 좋아요', cost: 9000 },
      { time: '18:00', category: '저녁', title: '성수 저녁 식탁', duration: '1시간 40분', distance: '도보 12분', rating: 4.7, reviews: '1,106', review: '예약하면 기다림이 적어요', cost: 36000 },
      { time: '20:00', category: '야경', title: '한강 야경 포인트', duration: '50분', distance: '차량 15분', rating: 4.8, reviews: '3,402', review: '하루를 마무리하기 좋은 전망이에요', cost: 0 },
    ],
  },
  busan: {
    label: '부산',
    area: '광안리 · 민락',
    places: [
      { time: '10:30', category: '아침', title: '광안리 아침 식탁', duration: '1시간 10분', distance: '도보 5분', rating: 4.7, reviews: '912', review: '바다를 보며 가볍게 시작해요', cost: 18000 },
      { time: '12:00', category: '산책', title: '광안리 해변 산책', duration: '1시간 20분', distance: '도보 7분', rating: 4.9, reviews: '4,108', review: '날씨 좋은 날 가장 예쁜 구간이에요', cost: 0 },
      { time: '14:00', category: '체험', title: '민락 아트 스페이스', duration: '1시간 30분', distance: '차량 12분', rating: 4.6, reviews: '688', review: '지역 작가 전시를 만날 수 있어요', cost: 10000 },
      { time: '16:00', category: '카페', title: '바다 앞 로스터리', duration: '1시간 20분', distance: '도보 4분', rating: 4.8, reviews: '1,532', review: '노을 시간대 창가 자리가 좋아요', cost: 10000 },
      { time: '18:00', category: '저녁', title: '민락 저녁 포장마차', duration: '1시간 40분', distance: '도보 9분', rating: 4.7, reviews: '2,204', review: '부산다운 저녁을 즐기기 좋아요', cost: 34000 },
      { time: '20:00', category: '야경', title: '광안대교 야경 포인트', duration: '50분', distance: '도보 10분', rating: 4.9, reviews: '5,016', review: '밤 산책의 마지막 장소로 추천해요', cost: 0 },
    ],
  },
  jeju: {
    label: '제주',
    area: '애월 · 곽지',
    places: [
      { time: '10:00', category: '아침', title: '애월 아침 식당', duration: '1시간 20분', distance: '차량 8분', rating: 4.8, reviews: '1,011', review: '제주 재료로 든든하게 시작해요', cost: 20000 },
      { time: '12:00', category: '해변', title: '곽지 바다 산책', duration: '1시간 10분', distance: '차량 15분', rating: 4.9, reviews: '3,280', review: '바람이 좋은 날 걷기 좋아요', cost: 0 },
      { time: '14:00', category: '카페', title: '애월 오션뷰 카페', duration: '1시간 30분', distance: '차량 9분', rating: 4.7, reviews: '2,401', review: '창가에서 쉬어가기 좋아요', cost: 11000 },
      { time: '16:00', category: '자연', title: '곶자왈 짧은 숲길', duration: '1시간 20분', distance: '차량 25분', rating: 4.8, reviews: '1,722', review: '무리 없이 자연을 만나는 길이에요', cost: 5000 },
      { time: '18:30', category: '저녁', title: '제주 저녁 식탁', duration: '1시간 40분', distance: '차량 12분', rating: 4.7, reviews: '1,305', review: '하루 끝에 천천히 먹기 좋아요', cost: 39000 },
      { time: '20:30', category: '밤바다', title: '애월 밤바다 포인트', duration: '45분', distance: '차량 10분', rating: 4.9, reviews: '2,908', review: '조용한 밤 산책으로 마무리해요', cost: 0 },
    ],
  },
};

const CITY_PROFILES = {
  seoul: { label: '서울', area: '성수 · 서울숲' }, busan: { label: '부산', area: '광안리 · 민락' },
  daegu: { label: '대구', area: '수성못 · 동성로' }, incheon: { label: '인천', area: '개항로 · 월미도' },
  gwangju: { label: '광주', area: '양림동 · 동명동' }, daejeon: { label: '대전', area: '엑스포 · 소제동' },
  ulsan: { label: '울산', area: '태화강 · 장생포' }, sejong: { label: '세종', area: '호수공원 · 국립수목원' },
  jeju: { label: '제주', area: '애월 · 곽지' }, suwon: { label: '수원', area: '행궁동 · 화성행궁' },
  seongnam: { label: '성남', area: '판교 · 율동공원' }, goyang: { label: '고양', area: '행주산성 · 일산호수공원' },
  yongin: { label: '용인', area: '한국민속촌 · 호수공원' }, hwaseong: { label: '화성', area: '제부도 · 동탄호수공원' },
  pyeongtaek: { label: '평택', area: '소사벌 · 진위천' }, anyang: { label: '안양', area: '평촌 · 안양예술공원' },
  bucheon: { label: '부천', area: '상동호수공원 · 도당공원' }, namyangju: { label: '남양주', area: '물의정원 · 북한강' },
  paju: { label: '파주', area: '헤이리 · 임진각' }, chuncheon: { label: '춘천', area: '의암호 · 명동' },
  wonju: { label: '원주', area: '뮤지엄산 · 소금산' }, gangneung: { label: '강릉', area: '안목 · 경포' },
  sokcho: { label: '속초', area: '청초호 · 외옹치' }, donghae: { label: '동해', area: '묵호 · 도째비골' },
  samcheok: { label: '삼척', area: '해변 · 장호항' }, cheongju: { label: '청주', area: '문화제조창 · 상당산성' },
  chungju: { label: '충주', area: '탄금대 · 중앙탑' }, jecheon: { label: '제천', area: '의림지 · 청풍호' },
  cheonan: { label: '천안', area: '독립기념관 · 신부동' }, asan: { label: '아산', area: '외암민속마을 · 지중해마을' },
  gongju: { label: '공주', area: '공산성 · 제민천' }, boryeong: { label: '보령', area: '대천해수욕장 · 성주산' },
  seosan: { label: '서산', area: '해미읍성 · 간월암' }, dangjin: { label: '당진', area: '삽교호 · 면천읍성' },
  jeonju: { label: '전주', area: '한옥마을 · 객리단길' }, gunsan: { label: '군산', area: '근대문화거리 · 선유도' },
  iksan: { label: '익산', area: '왕궁리 · 중앙동' }, namwon: { label: '남원', area: '광한루 · 요천' },
  mokpo: { label: '목포', area: '근대역사관 · 갓바위' }, yeosu: { label: '여수', area: '이순신광장 · 돌산' },
  suncheon: { label: '순천', area: '국가정원 · 와온해변' }, naju: { label: '나주', area: '빛가람 · 영산강' },
  gwangyang: { label: '광양', area: '매화마을 · 백운산' }, pohang: { label: '포항', area: '영일대 · 스페이스워크' },
  gyeongju: { label: '경주', area: '황리단길 · 대릉원' }, gumi: { label: '구미', area: '금오산 · 낙동강' },
  andong: { label: '안동', area: '하회마을 · 월영교' }, gimcheon: { label: '김천', area: '직지사 · 연화지' },
  yeongju: { label: '영주', area: '부석사 · 무섬마을' }, changwon: { label: '창원', area: '용지호수 · 마산' },
  jinju: { label: '진주', area: '진주성 · 남강' }, tongyeong: { label: '통영', area: '동피랑 · 미륵산' },
  gimhae: { label: '김해', area: '봉리단길 · 수로왕릉' }, geoje: { label: '거제', area: '바람의언덕 · 학동' },
  yangsan: { label: '양산', area: '통도사 · 황산공원' },
};

const GENERIC_PLACE_TEMPLATES = [
  { category: '브런치', title: '로컬 아침 식탁', duration: '1시간 20분', distance: '도보 8분', rating: 4.7, reviews: '데모', review: '지역 재료로 가볍게 시작해요', cost: 18000 },
  { category: '문화', title: '오늘의 문화 공간', duration: '1시간 30분', distance: '도보 12분', rating: 4.6, reviews: '데모', review: '날씨와 상관없이 즐기기 좋아요', cost: 10000 },
  { category: '산책', title: '느린 산책길', duration: '1시간', distance: '도보 10분', rating: 4.8, reviews: '데모', review: '지역의 분위기를 천천히 느껴요', cost: 0 },
  { category: '카페', title: '로컬 로스터리', duration: '1시간 20분', distance: '도보 7분', rating: 4.6, reviews: '데모', review: '잠깐 쉬어가기 좋은 공간이에요', cost: 9000 },
  { category: '저녁', title: '오늘의 저녁 식탁', duration: '1시간 40분', distance: '도보 15분', rating: 4.7, reviews: '데모', review: '하루를 마무리하기 좋은 메뉴예요', cost: 32000 },
  { category: '전망', title: '지역 야경 포인트', duration: '50분', distance: '차량 15분', rating: 4.8, reviews: '데모', review: '여행의 마지막 장면으로 추천해요', cost: 0 },
];

const NATIONAL_ROUTE = {
  label: '전국',
  area: '전국 · 오늘의 추천',
  places: [
    { time: '09:30', category: '역사', title: '경주 황리단길 아침 산책', duration: '1시간 30분', distance: '추천 이동', rating: 4.8, reviews: '데모', review: '가을의 골목과 유적을 함께 만나요', cost: 5000 },
    { time: '12:00', category: '바다', title: '강릉 안목 바다 카페', duration: '1시간 30분', distance: '추천 이동', rating: 4.7, reviews: '데모', review: '파도 보며 쉬어가기 좋은 코스예요', cost: 12000 },
    { time: '14:30', category: '골목', title: '전주 한옥마을 한 바퀴', duration: '2시간', distance: '추천 이동', rating: 4.9, reviews: '데모', review: '먹거리와 풍경을 한 번에 즐겨요', cost: 10000 },
    { time: '17:00', category: '노을', title: '여수 돌산 노을 포인트', duration: '1시간', distance: '추천 이동', rating: 4.8, reviews: '데모', review: '오늘 하루의 마무리 장소로 추천해요', cost: 0 },
    { time: '19:00', category: '야경', title: '부산 광안리 밤 산책', duration: '1시간', distance: '추천 이동', rating: 4.9, reviews: '데모', review: '도시와 바다를 함께 보는 저녁이에요', cost: 0 },
    { time: '20:30', category: '자연', title: '제주 곶자왈 짧은 숲길', duration: '1시간 20분', distance: '추천 이동', rating: 4.8, reviews: '데모', review: '조용한 자연을 찾는 날에 좋아요', cost: 5000 },
  ],
};

const NEARBY_TEMPLATES = [
  { type: 'restaurant', foodType: '한식', label: '한식', title: '오늘의 로컬 밥상', rating: 4.8, reviews: '1,240', distance: '650m', reason: '지금 식사하기 좋은 곳', menu: '제육·찌개·비빔밥' },
  { type: 'restaurant', foodType: '중식', label: '중식', title: '동네 중화요리집', rating: 4.7, reviews: '856', distance: '820m', reason: '빠르게 식사하기 좋은 곳', menu: '짜장·짬뽕·딤섬' },
  { type: 'restaurant', foodType: '일식', label: '일식', title: '오늘의 사시미 식당', rating: 4.8, reviews: '974', distance: '1.1km', reason: '가볍고 깔끔한 메뉴', menu: '초밥·우동·사시미' },
  { type: 'restaurant', foodType: '양식', label: '양식', title: '로컬 파스타 키친', rating: 4.6, reviews: '612', distance: '1.3km', reason: '분위기 있는 저녁 장소', menu: '파스타·스테이크·샐러드' },
  { type: 'cafe', foodType: '카페·디저트', label: '카페', title: '동네 로스터리', rating: 4.7, reviews: '856', distance: '820m', reason: '조용히 쉬어가기 좋은 곳', menu: '커피·케이크·차' },
  { type: 'attraction', label: '볼거리', title: '가까운 산책 명소', rating: 4.9, reviews: '2,018', distance: '1.2km', reason: '다음 코스로 이어가기 좋은 곳' },
];

const TODAY_RECOMMENDATIONS = [
  { destination: 'gyeongju', region: '경주', title: '황리단길과 대릉원', tag: '역사·산책', reason: '짧은 시간에도 여행 기분을 내기 좋아요.' },
  { destination: 'gangneung', region: '강릉', title: '안목해변과 초당동', tag: '바다·카페', reason: '카페와 해변을 한 동선으로 묶을 수 있어요.' },
  { destination: 'jeonju', region: '전주', title: '한옥마을과 객리단길', tag: '골목·미식', reason: '식사와 산책을 함께 즐기기 좋은 조합이에요.' },
];

const EVENT_SAMPLES = [
  { region: '전국', title: '오늘의 지역 문화행사', type: '공연·전시', date: '오늘 확인', note: '관광공사 행사정보에서 실시간 조회 예정' },
  { region: '내 주변', title: '주말 로컬 마켓·플리마켓', type: '마켓', date: '이번 주말', note: '현재 위치와 날짜로 필터링 예정' },
  { region: '전국', title: '계절 축제와 야간 개장', type: '축제', date: '날짜 맞춤', note: '지역·기간·운영시간을 반영 예정' },
];

const THEME_PROFILES = {
  date: { label: '데이트·감성', categories: ['브런치', '카페', '전시', '산책', '저녁', '야경'] },
  food: { label: '미식 여행', categories: ['아침', '브런치', '저녁', '골목', '카페'] },
  nature: { label: '자연·힐링', categories: ['자연', '산책', '해변', '바다', '밤바다', '전망'] },
  culture: { label: '역사·문화', categories: ['역사', '문화', '전시', '체험', '골목'] },
  cafe: { label: '카페·사진', categories: ['카페', '브런치', '전망', '해변', '전시'] },
  activity: { label: '액티비티', categories: ['체험', '자연', '해변', '산책'] },
  family: { label: '가족 여행', categories: ['체험', '자연', '문화', '산책', '전망'] },
  night: { label: '야경·야간', categories: ['야경', '밤바다', '저녁', '카페', '전망'] },
};

const SEASON_PROFILES = {
  spring: { label: '봄꽃·산책', categories: ['산책', '자연', '카페', '전망', '해변'] },
  summer: { label: '여름 바다·야외', categories: ['해변', '바다', '밤바다', '산책', '카페'] },
  autumn: { label: '가을 감성', categories: ['산책', '전망', '문화', '카페', '역사'] },
  winter: { label: '겨울 실내·미식', categories: ['문화', '전시', '카페', '저녁', '브런치'] },
};

const TRANSPORT_COST = { public: 8000, car: 14000, rental: 26000 };
const TRANSPORT_LABEL = { public: '대중교통', car: '자가용', rental: '렌터카' };
const MAX_TRIP_DAYS = 30;
const planLogic = window.TravelPlanLogic;
const form = document.querySelector('#planner-form');
const destinationInput = document.querySelector('#destination');
const themeInput = document.querySelector('#theme');
const dateInput = document.querySelector('#trip-date');
const endDateInput = document.querySelector('#trip-end-date');
const startTimeInput = document.querySelector('#start-time');
const endTimeInput = document.querySelector('#end-time');
const noTimeLimitInput = document.querySelector('#no-time-limit');
const peopleInput = document.querySelector('#people');
const transportInput = document.querySelector('#transport');
const budgetInput = document.querySelector('#budget');
const locationStatus = document.querySelector('#location-status');
const itinerary = document.querySelector('#itinerary');
const budgetFitButton = document.querySelector('#budget-fit-button');
const savePlanButton = document.querySelector('#save-plan-button');
const historyButton = document.querySelector('#history-button');
const refreshPlanButton = document.querySelector('#refresh-plan-button');
const savedPlansPanel = document.querySelector('#saved-plans-panel');
const savedPlansList = document.querySelector('#saved-plans-list');
const alternatePicker = document.querySelector('#alternate-picker');
const alternateList = document.querySelector('#alternate-list');
const closeHistoryButton = document.querySelector('#close-history-button');
const closeAlternateButton = document.querySelector('#close-alternate-button');
const nearbyList = document.querySelector('#nearby-list');
const nearbyTitle = document.querySelector('#nearby-title');
const nearbySourceStatus = document.querySelector('#nearby-source-status');
const eventSourceStatus = document.querySelector('#event-source-status');
const dataModePill = document.querySelector('#data-mode-pill');
const todayList = document.querySelector('#today-list');
const eventList = document.querySelector('#event-list');
const eventHeading = document.querySelector('#event-heading');
const foodSearch = document.querySelector('#food-search');
const foodQueryInput = document.querySelector('#food-query');
const foodTypeInput = document.querySelector('#food-type');
const mapPoints = document.querySelector('#map-points');
const mapFooterText = document.querySelector('#map-footer-text');
const mapDestination = document.querySelector('#map-destination');
const nearbyMapNote = document.querySelector('#nearby-map-note');
const nearbyLocationButton = document.querySelector('#nearby-location-button');
const nearbyMapButton = document.querySelector('#nearby-map-button');
const nearbySortInput = document.querySelector('#nearby-sort');
const nearbySortHint = document.querySelector('#nearby-sort-hint');
const naverMapCanvas = document.querySelector('#naver-map');
const mapFallback = document.querySelector('#map-fallback');
const mapProviderStatus = document.querySelector('#map-provider-status');
const integrationTitle = document.querySelector('#integration-title');
const integrationCopy = document.querySelector('#integration-copy');
const integrationStatus = document.querySelector('#integration-status');
let currentPlaces = [];
let currentNearbyItems = [];
let currentNearbySource = 'demo';
let currentEventSource = 'demo';
let currentLocation = null;
let nearbyFilter = 'all';
let nearbySort = 'recommended';
let foodQuery = '';
let foodType = '';
let locationMode = 'selected';
let currentAddress = '';
let nearbyRequestId = 0;
let eventsRequestId = 0;
let naverMapInstance = null;
let naverMapMarkers = [];
let naverMapUserMarker = null;
const PLAN_STORAGE_KEY = 'travel-planner-plans-v1';
let savedPlans = [];
let currentPlanId = null;
let routeOverride = null;
let planGeneration = 0;
let replacementIndex = null;

function isSafeExternalUrl(value) {
  try {
    return new URL(value).protocol === 'https:';
  } catch (error) {
    return false;
  }
}

function hasNumericRating(value) {
  return value !== null && value !== undefined && value !== '' && Number.isFinite(Number(value));
}

function appendNaverSearchLink(container, title, className = '') {
  const link = document.createElement('a');
  link.className = className;
  link.href = planLogic.createNaverSearchUrl(title);
  link.target = '_blank';
  link.rel = 'noreferrer';
  link.textContent = '네이버 지도에서 보기 ↗';
  container.append(link);
}

function appendPlaceLink(container, place, className = '') {
  if (place.live && isSafeExternalUrl(place.link)) {
    const link = document.createElement('a');
    link.className = className;
    link.href = place.link;
    link.target = '_blank';
    link.rel = 'noreferrer';
    link.textContent = '상세 정보에서 확인 ↗';
    container.append(link);
    return;
  }
  appendNaverSearchLink(container, place.title, className);
}

function getDestinationData(destination) {
  if (destination === 'nationwide') {
    return NATIONAL_ROUTE;
  }
  const profile = CITY_PROFILES[destination] || CITY_PROFILES.seoul;
  if (DESTINATIONS[destination]) {
    return { ...DESTINATIONS[destination], label: profile.label, area: profile.area };
  }
  const times = ['10:30', '12:20', '14:20', '16:00', '18:00', '20:00'];
  const places = GENERIC_PLACE_TEMPLATES.map((place, index) => ({
    ...place,
    time: times[index],
    title: `${profile.label} ${place.title}`,
  }));
  return { ...profile, places };
}

function getSeasonKey(dateValue) {
  const month = Number(dateValue.slice(5, 7));
  if (month >= 3 && month <= 5) return 'spring';
  if (month >= 6 && month <= 8) return 'summer';
  if (month >= 9 && month <= 11) return 'autumn';
  return 'winter';
}

function getDailyTimeRange() {
  return planLogic.getDailyTimeRange(startTimeInput.value, endTimeInput.value, noTimeLimitInput.checked);
}

function getTimeRangeLabel(timeRange) {
  return planLogic.getTimeRangeLabel(timeRange, startTimeInput.value, endTimeInput.value);
}

function syncTimeLimitInputs() {
  const disabled = noTimeLimitInput.checked;
  if (disabled) {
    startTimeInput.dataset.previousValue = startTimeInput.value || '10:00';
    endTimeInput.dataset.previousValue = endTimeInput.value || '20:00';
    startTimeInput.value = '';
    endTimeInput.value = '';
  } else {
    startTimeInput.value = startTimeInput.dataset.previousValue || '10:00';
    endTimeInput.value = endTimeInput.dataset.previousValue || '20:00';
  }
  startTimeInput.disabled = disabled;
  endTimeInput.disabled = disabled;
}

function getThemePlan(theme, dateValue, timeRange) {
  const seasonProfile = SEASON_PROFILES[getSeasonKey(dateValue)];
  const timeProfile = timeRange.startMinutes >= 16 * 60
    ? THEME_PROFILES.night
    : timeRange.hours <= 4
      ? THEME_PROFILES.date
      : seasonProfile;
  const profile = theme === 'auto' ? timeProfile : THEME_PROFILES[theme] || seasonProfile;
  const timeLabel = timeRange.unlimited
    ? '시간 제한 없음 · 여유로운 하루'
    : timeRange.hours <= 4
    ? '짧은 시간 집중 코스'
      : timeRange.startMinutes >= 16 * 60
      ? '야간 중심 코스'
      : timeRange.startMinutes < 9 * 60
        ? '아침부터 여유로운 코스'
        : '낮 시간 중심 코스';
  return { ...profile, seasonLabel: seasonProfile.label, timeLabel };
}

function getNearbyRecommendations(destination) {
  const destinationData = getDestinationData(destination);
  const subject = locationMode === 'current' ? '현재 위치' : destinationData.label;
  return NEARBY_TEMPLATES.map((place) => ({
    ...place,
    title: `${subject} ${place.title}`,
  }));
}

function getTripDays() {
  const start = new Date(`${dateInput.value}T00:00:00`);
  const end = new Date(`${endDateInput.value}T00:00:00`);
  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime()) || end < start) {
    return 0;
  }
  const days = Math.floor((end - start) / 86400000) + 1;
  return days <= MAX_TRIP_DAYS ? days : 0;
}

function addDaysToDateValue(value, days) {
  const date = new Date(`${value}T00:00:00`);
  if (Number.isNaN(date.getTime())) return '';
  date.setDate(date.getDate() + days);
  return [date.getFullYear(), date.getMonth() + 1, date.getDate()]
    .map((part, index) => index === 0 ? String(part) : String(part).padStart(2, '0'))
    .join('-');
}

function updateEndDateBounds() {
  endDateInput.min = dateInput.value;
  endDateInput.max = addDaysToDateValue(dateInput.value, MAX_TRIP_DAYS - 1);
  if (!endDateInput.value || endDateInput.value < dateInput.value) {
    endDateInput.value = dateInput.value;
  }
  if (endDateInput.max && endDateInput.value > endDateInput.max) {
    endDateInput.value = endDateInput.max;
  }
}

function formatDateRange() {
  const start = new Date(`${dateInput.value}T00:00:00`);
  const end = new Date(`${endDateInput.value}T00:00:00`);
  const format = { month: 'long', day: 'numeric' };
  if (dateInput.value === endDateInput.value) {
    return start.toLocaleDateString('ko-KR', format);
  }
  return `${start.toLocaleDateString('ko-KR', format)} ~ ${end.toLocaleDateString('ko-KR', format)}`;
}

function focusRecommendation(index) {
  const recommendation = nearbyList.children[index];
  recommendation?.scrollIntoView({ behavior: 'smooth', block: 'center' });
  recommendation?.classList.add('recommendation-highlight');
  window.setTimeout(() => recommendation?.classList.remove('recommendation-highlight'), 1200);
}

function updateDataSourceStatus() {
  nearbySourceStatus.textContent = planLogic.getDataSourceLabel(currentNearbySource, 'nearby');
  eventSourceStatus.textContent = planLogic.getDataSourceLabel(currentEventSource, 'events');
  updateIntegrationStatus();
  const isLive = currentNearbySource === 'naver' || currentEventSource === 'tour_api';
  const isLoading = currentNearbySource === 'loading' || currentEventSource === 'loading';
  const hasNearbyError = currentNearbySource === 'naver_error';
  const hasEventError = currentEventSource === 'tour_error';
  dataModePill.textContent = isLoading
    ? '실시간 데이터 확인 중…'
    : hasNearbyError
      ? '실시간 검색 실패 · 예시 추천 표시'
      : hasEventError
        ? '실시간 행사 조회 실패 · 예시 행사 표시'
      : currentNearbySource === 'naver_empty'
        ? '검색 결과 없음 · 조건을 바꿔보세요'
        : currentEventSource === 'tour_empty'
          ? '행사 없음 · 날짜/지역을 바꿔보세요'
        : isLive ? '실시간 데이터 연결됨' : '데모 데이터 · 출처별 상태 표시';
}

function updateIntegrationStatus() {
  const status = planLogic.getIntegrationStatus(Boolean(window.naver?.maps), currentNearbySource, currentEventSource);
  integrationTitle.textContent = status.title;
  integrationCopy.textContent = status.message;
  integrationStatus.textContent = status.status;
}

function updateNearbyMapContext(items) {
  const subject = locationMode === 'current'
    ? currentAddress || '내 주변'
    : getDestinationData(destinationInput.value).label;
  mapDestination.textContent = subject;
  nearbyMapNote.textContent = `${subject} 추천 ${items.length}곳이 지도에 번호로 표시돼요. 거리·평점·영업시간·주차 상태를 함께 확인해요.`;
}

function parseNaverCoordinate(place) {
  const rawLongitude = Number(place.mapx);
  const rawLatitude = Number(place.mapy);
  if (!Number.isFinite(rawLongitude) || !Number.isFinite(rawLatitude)) {
    return null;
  }
  const longitude = rawLongitude / 10000000;
  const latitude = rawLatitude / 10000000;
  if (latitude < 30 || latitude > 45 || longitude < 120 || longitude > 135) {
    return null;
  }
  return { latitude, longitude };
}

function getNaverCoordinates(items) {
  return items.map((place, index) => ({ place, index, position: parseNaverCoordinate(place) }))
    .filter((item) => item.position);
}

function clearNaverMapMarkers() {
  naverMapMarkers.forEach((marker) => marker.setMap(null));
  naverMapMarkers = [];
  if (naverMapUserMarker) {
    naverMapUserMarker.setMap(null);
    naverMapUserMarker = null;
  }
}

function showMapFallback() {
  naverMapCanvas.hidden = true;
  mapFallback.hidden = false;
  mapProviderStatus.textContent = window.naver?.maps
    ? '미리보기 · 좌표가 있는 검색 결과에서 실제 지도 표시'
    : '미리보기 · 지도 키 설정 후 실제 지도';
}

function renderNaverMap(items) {
  const coordinates = getNaverCoordinates(items);
  const hasCurrentLocation = planLogic.isValidLocation(currentLocation);
  if (!window.naver?.maps || (!coordinates.length && !hasCurrentLocation)) {
    return false;
  }
  const firstPosition = coordinates[0]?.position || currentLocation;
  naverMapCanvas.hidden = false;
  mapFallback.hidden = true;
  if (!naverMapInstance) {
    naverMapInstance = new naver.maps.Map(naverMapCanvas, {
      center: new naver.maps.LatLng(firstPosition.latitude, firstPosition.longitude),
      zoom: 15,
      zoomControl: true,
      mapTypeControl: false,
    });
  }
  clearNaverMapMarkers();
  const mapPositions = coordinates.map((item) => item.position);
  if (hasCurrentLocation) mapPositions.push(currentLocation);
  const center = mapPositions.reduce(
    (total, position) => ({
      latitude: total.latitude + position.latitude,
      longitude: total.longitude + position.longitude,
    }),
    { latitude: 0, longitude: 0 },
  );
  naverMapInstance.setCenter(new naver.maps.LatLng(
    center.latitude / mapPositions.length,
    center.longitude / mapPositions.length,
  ));
  naverMapInstance.setZoom(coordinates.length <= 1 ? 16 : 14);
  naverMapMarkers = coordinates.map(({ place, index, position }) => {
    const marker = new naver.maps.Marker({
      map: naverMapInstance,
      position: new naver.maps.LatLng(position.latitude, position.longitude),
      title: place.title,
    });
    naver.maps.Event.addListener(marker, 'click', () => focusRecommendation(index));
    return marker;
  });
  if (hasCurrentLocation) {
    naverMapUserMarker = new naver.maps.Marker({
      map: naverMapInstance,
      position: new naver.maps.LatLng(currentLocation.latitude, currentLocation.longitude),
      title: '현재 위치',
    });
  }
  mapProviderStatus.textContent = '실제 네이버 지도';
  return true;
}

function getStaticMapPositions(count) {
  const positions = [[22, 30], [52, 35], [72, 50], [43, 63], [29, 73], [67, 72]];
  return Array.from({ length: count }, (_, index) => positions[index % positions.length]);
}

function getCoordinateMapPositions(items) {
  const coordinates = items.map((place) => ({ x: Number(place.mapx), y: Number(place.mapy) }));
  const valid = coordinates.filter(({ x, y }) => Number.isFinite(x) && Number.isFinite(y));
  if (valid.length < 2) {
    return null;
  }
  const xValues = valid.map(({ x }) => x);
  const yValues = valid.map(({ y }) => y);
  const minX = Math.min(...xValues);
  const maxX = Math.max(...xValues);
  const minY = Math.min(...yValues);
  const maxY = Math.max(...yValues);
  const xRange = maxX - minX || 1;
  const yRange = maxY - minY || 1;
  return coordinates.map(({ x, y }, index) => Number.isFinite(x) && Number.isFinite(y)
    ? [18 + ((x - minX) / xRange) * 64, 20 + (1 - (y - minY) / yRange) * 62]
    : getStaticMapPositions(items.length)[index]);
}

function renderMapMarkers(items, sourceLabel = '데모 추천 장소') {
  if (renderNaverMap(items)) {
    mapFooterText.textContent = `${items.length}곳 · 실제 네이버 지도 · 번호를 누르면 목록으로 이동해요`;
    return;
  }
  showMapFallback();
  const positions = getCoordinateMapPositions(items) || getStaticMapPositions(items.length);
  mapPoints.replaceChildren();
  items.slice(0, positions.length).forEach((place, index) => {
    const marker = document.createElement('button');
    marker.type = 'button';
    marker.className = 'map-point map-point-dynamic';
    marker.style.left = `${positions[index][0]}%`;
    marker.style.top = `${positions[index][1]}%`;
    marker.title = place.title;
    marker.setAttribute('aria-label', `${index + 1}번 ${place.title}`);
    marker.addEventListener('click', () => focusRecommendation(index));
    const number = document.createElement('span');
    number.textContent = String(index + 1);
    marker.append(number);
    mapPoints.append(marker);
  });
  if (planLogic.isValidLocation(currentLocation)) {
    const userMarker = document.createElement('span');
    userMarker.className = 'map-user-point';
    userMarker.style.left = '50%';
    userMarker.style.top = '50%';
    userMarker.title = '현재 위치';
    userMarker.setAttribute('aria-label', '현재 위치');
    userMarker.textContent = '⌖';
    mapPoints.append(userMarker);
  }
  mapFooterText.textContent = items.length
    ? `${items.length}곳 · ${sourceLabel} · 번호를 누르면 목록으로 이동해요`
    : planLogic.isValidLocation(currentLocation) ? '현재 위치를 기준으로 추천 장소를 준비 중이에요' : '조건에 맞는 추천 장소가 없어요';
}

function renderNearby() {
  nearbyRequestId += 1;
  const filteredRecommendations = getNearbyRecommendations(destinationInput.value)
    .filter((place) => nearbyFilter === 'all' || place.type === nearbyFilter)
    .filter((place) => !foodType || place.foodType === foodType)
    .filter((place) => !foodQuery || `${place.title} ${place.menu || ''}`.includes(foodQuery));
  const recommendations = planLogic.sortNearbyItems(filteredRecommendations, nearbySort);
  currentNearbyItems = recommendations;
  currentNearbySource = 'demo';
  setNearbySortAvailability(false, true);
  updateDataSourceStatus();
  updateNearbyMapContext(recommendations);
  foodSearch.hidden = nearbyFilter !== 'restaurant';
  nearbyTitle.textContent = locationMode === 'current'
    ? '현재 위치 주변 추천'
    : destinationInput.value === 'nationwide'
      ? '오늘 전국 추천 장소'
      : `${getDestinationData(destinationInput.value).label} 주변 추천`;
  nearbyList.replaceChildren();
  recommendations.forEach((place) => {
    const article = document.createElement('article');
    article.className = 'recommendation-item';
    const icon = document.createElement('span');
    icon.className = 'recommendation-icon';
    icon.textContent = place.type === 'restaurant' ? '餐' : place.type === 'cafe' ? '☕' : '✦';
    const content = document.createElement('div');
    const title = document.createElement('strong');
    title.textContent = place.title;
    const meta = document.createElement('p');
    meta.textContent = `${place.label} · ${place.distance} · ${place.reason}`;
    const detail = document.createElement('small');
    detail.textContent = `${place.menu || '산책·전시·체험'} · ★ ${place.rating} (${place.reviews}) · 데모 리뷰`;
    content.append(title, meta, detail);
    appendNaverSearchLink(content, place.title);
    article.append(icon, content);
    nearbyList.append(article);
  });
  renderMapMarkers(recommendations);
}

function createLiveNearbyCard(place) {
  const article = document.createElement('article');
  article.className = 'recommendation-item';
  const icon = document.createElement('span');
  icon.className = 'recommendation-icon';
  icon.textContent = 'N';
  const content = document.createElement('div');
  const title = document.createElement('strong');
  title.textContent = place.title;
  const meta = document.createElement('p');
  meta.textContent = `${place.category || '장소'} · ${place.road_address || place.address}`;
  const source = document.createElement('small');
  const searchLabel = [foodType, foodQuery].filter(Boolean).join(' · ');
  const ratingText = hasNumericRating(place.rating)
    ? `평점 ${Number(place.rating).toFixed(1)}`
    : '평점 상세 확인';
  const distanceText = place.distance ? `거리 ${place.distance}` : '거리 계산 대기';
  source.textContent = [
    '네이버 지역 검색 결과',
    searchLabel,
    ratingText,
    distanceText,
  ].filter(Boolean).join(' · ');
  const info = document.createElement('div');
  info.className = 'nearby-place-info';
  appendNearbyInfo(info, '거리', place.distance || '내 주변으로 갱신하면 계산');
  appendNearbyInfo(info, '평점', hasNumericRating(place.rating)
    ? `${Number(place.rating).toFixed(1)}점${place.reviews ? ` · ${place.reviews}개 리뷰` : ''}`
    : '네이버 상세 확인');
  appendNearbyInfo(info, '영업시간', place.opening_hours || '네이버 상세 확인');
  appendNearbyInfo(info, '주차', place.parking || '네이버 상세 확인');
  content.append(title, meta, source, info);
  if (place.link && isSafeExternalUrl(place.link)) {
    const link = document.createElement('a');
    link.href = place.link;
    link.target = '_blank';
    link.rel = 'noreferrer';
    link.textContent = '상세 보기 ↗';
    content.append(link);
  } else {
    appendNaverSearchLink(content, place.title);
  }
  article.append(icon, content);
  return article;
}

function appendNearbyInfo(container, label, value) {
  const item = document.createElement('span');
  item.textContent = `${label} ${value}`;
  container.append(item);
}

function renderLiveNearby(items) {
  const liveItems = items.map((place) => {
    const meters = getDistanceFromCurrentLocation(place);
    return meters === null ? place : { ...place, distance_meters: meters, distance: formatDistance(meters) };
  });
  const hasDistance = liveItems.some((place) => Number.isFinite(place.distance_meters));
  const hasRating = liveItems.some((place) => hasNumericRating(place.rating));
  setNearbySortAvailability(true, hasDistance, hasRating);
  const sortedItems = planLogic.sortNearbyItems(liveItems, nearbySort);
  currentNearbyItems = sortedItems;
  currentNearbySource = 'naver';
  updateDataSourceStatus();
  updateNearbyMapContext(sortedItems);
  nearbyList.replaceChildren();
  renderMapMarkers(sortedItems, '네이버 검색 결과');
  sortedItems.forEach((place) => nearbyList.append(createLiveNearbyCard(place)));
}

function setNearbySortAvailability(live, hasDistance, hasRating = false) {
  const ratingOption = nearbySortInput.querySelector('option[value="rating"]');
  const distanceOption = nearbySortInput.querySelector('option[value="distance"]');
  const ratingAvailable = !live || hasRating;
  const distanceAvailable = !live || hasDistance;
  if (ratingOption) {
    ratingOption.disabled = !ratingAvailable;
    ratingOption.textContent = ratingAvailable ? '평점순' : '평점순 (상세 평점 없음)';
  }
  if (distanceOption) {
    distanceOption.disabled = !distanceAvailable;
    distanceOption.textContent = distanceAvailable ? '거리순' : '거리순 (내 위치 필요)';
  }
  const unavailableHints = [];
  if (!ratingAvailable) unavailableHints.push('평점순은 상세 평점이 있을 때 사용 가능');
  if (!distanceAvailable) unavailableHints.push('거리순은 내 위치 필요');
  nearbySortHint.textContent = live
    ? unavailableHints.join(' · ') || '네이버 검색 결과를 기준으로 표시해요.'
    : '';
  if (live && ((nearbySort === 'rating' && !hasRating) || (nearbySort === 'distance' && !hasDistance))) {
    nearbySort = 'recommended';
    nearbySortInput.value = nearbySort;
  }
}

function getDistanceFromCurrentLocation(place) {
  if (!planLogic.isValidLocation(currentLocation)) return null;
  const position = parseNaverCoordinate(place);
  if (!position) return null;
  const toRadians = (value) => value * Math.PI / 180;
  const latitudeDelta = toRadians(position.latitude - currentLocation.latitude);
  const longitudeDelta = toRadians(position.longitude - currentLocation.longitude);
  const latitude = toRadians(currentLocation.latitude);
  const targetLatitude = toRadians(position.latitude);
  const haversine = Math.sin(latitudeDelta / 2) ** 2
    + Math.cos(latitude) * Math.cos(targetLatitude) * Math.sin(longitudeDelta / 2) ** 2;
  return 6371000 * 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
}

function formatDistance(meters) {
  return meters < 1000 ? `${Math.round(meters)}m` : `${(meters / 1000).toFixed(1)}km`;
}

function renderNoLiveNearby() {
  currentNearbyItems = [];
  currentNearbySource = 'naver_empty';
  setNearbySortAvailability(true, false);
  updateDataSourceStatus();
  updateNearbyMapContext([]);
  nearbyMapNote.textContent = '조건에 맞는 네이버 장소가 없어 지도에 표시할 결과가 없어요.';
  renderMapMarkers([], '네이버 검색 결과');
  nearbyList.replaceChildren();
  const article = document.createElement('article');
  article.className = 'recommendation-empty';
  const title = document.createElement('strong');
  title.textContent = '실시간 추천 결과가 없어요';
  const message = document.createElement('p');
  const filterLabel = nearbyFilter === 'restaurant' ? (foodType || '식당') : nearbyFilter === 'cafe' ? '카페' : nearbyFilter === 'attraction' ? '볼거리' : '추천 장소';
  message.textContent = `${filterLabel} 조건에 맞는 네이버 장소를 찾지 못했어요. 지역을 선택하거나 검색 조건을 바꿔보세요.`;
  article.append(title, message);
  nearbyList.append(article);
}

function renderNearbyLiveError() {
  currentNearbySource = 'naver_error';
  updateDataSourceStatus();
  nearbyMapNote.textContent = '실시간 검색에 실패해 예시 추천을 보여드려요.';
}

async function loadLiveNearby() {
  const requestId = ++nearbyRequestId;
  const destinationData = getDestinationData(destinationInput.value);
  const searchContext = planLogic.getNearbySearchContext(
    destinationInput.value,
    destinationData.label,
    currentAddress,
    nearbyFilter,
  );
  const nationwideCategoryQuery = destinationInput.value === 'nationwide'
    && !currentAddress
    && nearbyFilter !== 'all'
    ? '대한민국'
    : searchContext.query;
  const searchTerms = [
    nationwideCategoryQuery,
    nearbyFilter === 'restaurant' ? foodType : '',
    nearbyFilter === 'restaurant' ? foodQuery : '',
  ].filter(Boolean);
  const params = new URLSearchParams({
    query: searchTerms.join(' '),
    category: searchContext.category,
  });
  currentNearbySource = 'loading';
  updateDataSourceStatus();
  try {
    const response = await fetch(`/api/nearby?${params.toString()}`);
    const payload = await response.json();
    if (requestId !== nearbyRequestId) return;
    const items = Array.isArray(payload.items) ? payload.items : null;
    if (payload.source === 'naver' && items) {
      items.length > 0 ? renderLiveNearby(items) : renderNoLiveNearby();
      return;
    }
    renderNearbyLiveError();
  } catch (error) {
    if (requestId !== nearbyRequestId) return;
    renderNearbyLiveError();
  }
}

function loadNaverMapScript(clientId) {
  return new Promise((resolve, reject) => {
    const script = document.createElement('script');
    script.src = `https://oapi.map.naver.com/openapi/v3/maps.js?ncpKeyId=${encodeURIComponent(clientId)}`;
    script.onload = resolve;
    script.onerror = reject;
    document.head.append(script);
  });
}

async function loadNaverMapSdk() {
  try {
    const response = await fetch('/api/config');
    const config = await response.json();
    if (!config.map_client_id) {
      return;
    }
    await loadNaverMapScript(config.map_client_id);
    renderMapMarkers(currentNearbyItems, currentNearbySource === 'naver' ? '네이버 검색 결과' : '데모 추천 장소');
    updateIntegrationStatus();
  } catch (error) {
    showMapFallback();
    updateIntegrationStatus();
  }
}

function registerServiceWorker() {
  if (!('serviceWorker' in navigator)) return;
  navigator.serviceWorker.register('./sw.js?v=20').catch(() => {
    // The planner remains fully usable when service workers are unavailable.
  });
}

function appendEventSearchLink(container, title, place) {
  const link = document.createElement('a');
  link.className = 'event-search-link';
  link.href = planLogic.createEventSearchUrl(title, place);
  link.target = '_blank';
  link.rel = 'noreferrer';
  link.textContent = '행사 위치·상세 보기 ↗';
  container.append(link);
}

function renderTodayRecommendations() {
  todayList.replaceChildren();
  TODAY_RECOMMENDATIONS.forEach((recommendation) => {
    const article = document.createElement('article');
    article.className = 'today-item';
    const content = document.createElement('div');
    const region = document.createElement('span');
    region.textContent = `${recommendation.region} · ${recommendation.tag}`;
    const title = document.createElement('strong');
    title.textContent = recommendation.title;
    const reason = document.createElement('p');
    reason.textContent = recommendation.reason;
    content.append(region, title, reason);
    const button = document.createElement('button');
    button.className = 'today-plan-button';
    button.type = 'button';
    button.dataset.todayDestination = recommendation.destination;
    button.textContent = '이 지역 코스 만들기';
    article.append(content, button);
    todayList.append(article);
  });
}

function renderTodayAndEvents(loadEvents = true) {
  const today = new Date();
  document.querySelector('#today-label').textContent = today.toLocaleDateString('ko-KR', { month: 'numeric', day: 'numeric' });
  renderTodayRecommendations();
  renderDemoEvents();
  if (loadEvents) loadLiveEvents();
}

function refreshLiveEvents() {
  renderDemoEvents();
  loadLiveEvents();
}

function getEventScope() {
  return destinationInput.value === 'nationwide'
    ? '전국'
    : getDestinationData(destinationInput.value).label;
}

function renderDemoEvents() {
  const scope = getEventScope();
  const dateLabel = formatDateRange();
  currentEventSource = 'demo';
  updateDataSourceStatus();
  eventHeading.textContent = scope === '전국' ? '전국 행사를 찾아볼까요?' : `${scope} 주변 행사를 찾아볼까요?`;
  eventList.replaceChildren();
  EVENT_SAMPLES.forEach((event) => {
    const article = document.createElement('article');
    article.className = 'event-item';
    const content = document.createElement('div');
    const region = document.createElement('span');
    region.textContent = `${scope} · ${event.type}`;
    const title = document.createElement('strong');
    title.textContent = event.title;
    const note = document.createElement('p');
    note.textContent = `${scope} · ${dateLabel} 실시간 조회가 되지 않아 예시로 보여드려요.`;
    content.append(region, title, note);
    appendEventSearchLink(content, event.title, scope);
    const period = document.createElement('time');
    period.textContent = dateLabel;
    article.append(content, period);
    eventList.append(article);
  });
}

function renderLiveEvents(items) {
  currentEventSource = 'tour_api';
  updateDataSourceStatus();
  eventList.replaceChildren();
  items.slice(0, 3).forEach((event) => {
    const article = document.createElement('article');
    article.className = 'event-item';
    const content = document.createElement('div');
    const region = document.createElement('span');
    region.textContent = `${event.address || '전국'} · 행사`;
    const title = document.createElement('strong');
    title.textContent = event.title;
    const note = document.createElement('p');
    note.textContent = event.place || '한국관광공사 행사정보';
    content.append(region, title, note);
    appendEventSearchLink(content, event.title, event.place || event.address);
    const period = document.createElement('time');
    period.textContent = planLogic.formatEventDateRange(event.start_date, event.end_date) || '일정 확인';
    article.append(content, period);
    eventList.append(article);
  });
}

function renderNoLiveEvents() {
  const scope = getEventScope();
  const dateLabel = formatDateRange();
  currentEventSource = 'tour_empty';
  updateDataSourceStatus();
  eventList.replaceChildren();
  const article = document.createElement('article');
  article.className = 'event-item event-empty';
  const message = document.createElement('p');
  message.textContent = planLogic.getEventEmptyState(scope, dateLabel);
  const note = document.createElement('small');
  note.textContent = '관광공사 실시간 행사정보에서 해당 기간을 조회했어요.';
  article.append(message, note);
  eventList.append(article);
}

function renderEventLiveError() {
  currentEventSource = 'tour_error';
  updateDataSourceStatus();
  renderDemoEvents();
  currentEventSource = 'tour_error';
  updateDataSourceStatus();
  const article = document.createElement('article');
  article.className = 'event-item event-empty';
  const title = document.createElement('strong');
  title.textContent = '실시간 행사 조회에 실패했어요';
  const message = document.createElement('p');
  message.textContent = '잠시 후 다시 확인하거나, 행사 위치를 지도에서 검색해보세요.';
  const retry = document.createElement('button');
  retry.className = 'event-retry-button';
  retry.type = 'button';
  retry.textContent = '다시 확인';
  retry.addEventListener('click', loadLiveEvents);
  article.append(title, message, retry);
  eventList.append(article);
}

async function loadLiveEvents() {
  const requestId = ++eventsRequestId;
  const startDate = dateInput.value.replaceAll('-', '');
  const endDate = endDateInput.value.replaceAll('-', '');
  const destination = destinationInput.value;
  currentEventSource = 'loading';
  updateDataSourceStatus();
  try {
    const params = new URLSearchParams({ start_date: startDate, end_date: endDate, destination });
    const response = await fetch(`/api/events?${params.toString()}`);
    const payload = await response.json();
    if (!planLogic.isCurrentEventRequest(requestId, eventsRequestId)) return;
    planLogic.applyEventResponse(payload, {
      live: renderLiveEvents,
      empty: renderNoLiveEvents,
      error: renderEventLiveError,
    });
  } catch (error) {
    if (requestId === eventsRequestId) renderEventLiveError();
  }
}

function formatWon(value) {
  return `₩ ${Math.round(value).toLocaleString('ko-KR')}`;
}

function getPlaceCount(days, hours) {
  if (days > 1) return days * 2;
  if (hours <= 4) return 2;
  if (hours <= 7) return 3;
  return 4;
}

function getRouteDistance(transport) {
  const transportKey = ['public', 'car', 'rental'].includes(transport) ? transport : 'public';
  const minutes = planLogic.getTransportMinutes(transportKey);
  return `${TRANSPORT_LABEL[transportKey]} · 예상 이동 ${minutes}분`;
}

function getThemeScore(place, categories) {
  const index = categories.indexOf(place.category);
  return index === -1 ? categories.length + 1 : index;
}

function buildTimeSlots(count, timeRange) {
  return planLogic.buildTimeSlots(count, timeRange);
}

function getSelectedPlaces(destination, days, themePlan, timeRange, transport, generation = 0) {
  const places = getDestinationData(destination).places;
  const placesPerDay = days === 1 ? getPlaceCount(days, timeRange.hours) : 2;
  const requestedCount = days === 1 ? placesPerDay : getPlaceCount(days, timeRange.hours);
  const count = days === 1 ? requestedCount : Math.max(days, Math.min(requestedCount, places.length));
  const sortedPlaces = [...places].sort((left, right) => getThemeScore(left, themePlan.categories) - getThemeScore(right, themePlan.categories));
  const rotation = sortedPlaces.length ? generation % sortedPlaces.length : 0;
  const orderedPlaces = sortedPlaces.slice(rotation).concat(sortedPlaces.slice(0, rotation));
  const timeSlots = buildTimeSlots(Math.min(placesPerDay, count), timeRange);
  const daySlotCounts = Array(days).fill(0);
  return Array.from({ length: count }, (_, index) => {
    const sourcePlace = orderedPlaces[index % orderedPlaces.length];
    const place = index < orderedPlaces.length
      ? sourcePlace
      : { ...sourcePlace, title: `${sourcePlace.title} · 다시 보기`, review: '지역 주변의 다른 장소로 바꿀 수 있어요' };
    const day = days === 1 ? 1 : Math.min(days, Math.floor(index * days / count) + 1);
    const slotIndex = daySlotCounts[day - 1]++;
    return {
      ...place,
      day,
      time: timeSlots[Math.min(slotIndex, timeSlots.length - 1)],
      distance: getRouteDistance(transport),
    };
  });
}

function calculateEstimate(places, days, people, transport) {
  const activityCost = places.reduce((total, place) => total + place.cost * people, 0);
  const movingCost = TRANSPORT_COST[transport] * people * days;
  const stayCost = days > 1 ? 90000 * (days - 1) : 0;
  const subtotal = activityCost + movingCost + stayCost;
  const contingency = Math.ceil(subtotal * 0.1);
  const unknownCosts = places.filter((place) => place.costUnknown).length;
  return { subtotal, contingency, total: subtotal + contingency, unknownCosts };
}

function getBudgetFitPlaces(places, days, people, transport, budget) {
  const adjusted = [...places];
  while (adjusted.length > days) {
    const estimate = calculateEstimate(adjusted, days, people, transport);
    if (estimate.total <= budget) {
      break;
    }
    const dayCounts = adjusted.reduce((counts, place) => {
      counts[place.day] = (counts[place.day] || 0) + 1;
      return counts;
    }, {});
    const removable = adjusted
      .filter((place) => dayCounts[place.day] > 1)
      .sort((left, right) => right.cost - left.cost)[0];
    if (!removable) {
      break;
    }
    adjusted.splice(adjusted.indexOf(removable), 1);
  }
  return adjusted;
}

function renderItinerary(places) {
  itinerary.replaceChildren();
  places.forEach((place, index) => {
    const article = document.createElement('article');
    article.className = 'place-card';
    const time = document.createElement('div');
    time.className = 'place-time';
    const dayLabel = document.createElement('small');
    const dateLabel = planLogic.getDayDateLabel(dateInput.value, place.day);
    dayLabel.textContent = `DAY ${place.day}${dateLabel ? ` · ${dateLabel}` : ''}`;
    time.append(dayLabel);
    time.append(document.createTextNode(place.time));
    const info = document.createElement('div');
    info.className = 'place-info';
    const titleRow = document.createElement('div');
    titleRow.className = 'place-title-row';
    const title = document.createElement('h4');
    title.textContent = place.title;
    const changeButton = document.createElement('button');
    changeButton.className = 'place-change-button';
    changeButton.type = 'button';
    changeButton.dataset.placeChangeIndex = String(index);
    changeButton.textContent = '장소 변경';
    titleRow.append(title, changeButton);
    const meta = document.createElement('p');
    meta.className = 'place-meta';
    const distanceLabel = destinationInput.value === 'nationwide' ? '도시 선택 후 이동 계산' : place.distance;
    meta.textContent = `${place.category} · ${place.duration} · ${distanceLabel}`;
    const review = document.createElement('div');
    review.className = 'place-review';
    review.textContent = place.live
      ? '네이버 실시간 장소 · 상세 링크에서 평점과 영업시간을 확인하세요.'
      : destinationInput.value === 'nationwide'
        ? `“${place.review}” · 전국 후보 · 도시 선택 후 실제 동선으로 계산해요.`
      : `“${place.review}” · 데모 리뷰`;
    info.append(titleRow, meta, review);
    appendPlaceLink(info, place, 'place-map-link');
    const rating = document.createElement('div');
    rating.className = 'place-rating';
    if (hasNumericRating(place.rating)) {
      rating.innerHTML = '<span>★</span> ';
      rating.append(document.createTextNode(`${Number(place.rating).toFixed(1)} `));
      const reviewCount = document.createElement('small');
      reviewCount.textContent = `(${place.reviews})`;
      rating.append(reviewCount);
    } else {
      rating.textContent = '평점 확인';
    }
    article.append(time, info, rating);
    itinerary.append(article);
  });
}

function readSavedPlans() {
  try {
    const stored = JSON.parse(localStorage.getItem(PLAN_STORAGE_KEY) || '[]');
    return Array.isArray(stored) ? stored.map(normalizeSavedPlan).filter(Boolean) : [];
  } catch (error) {
    return [];
  }
}

function persistSavedPlans() {
  try {
    localStorage.setItem(PLAN_STORAGE_KEY, JSON.stringify(savedPlans.slice(0, 20)));
    return true;
  } catch (error) {
    locationStatus.textContent = '브라우저 저장공간을 사용할 수 없어 계획을 저장하지 못했어요.';
    return false;
  }
}

function sanitizeSavedText(value, fallback) {
  return typeof value === 'string' && value.trim() ? value.slice(0, 160) : fallback;
}

function normalizeSavedPlace(place) {
  if (!place || typeof place !== 'object') return null;
  const hasRating = hasNumericRating(place.rating);
  const rating = hasRating ? Number(place.rating) : null;
  return {
    title: sanitizeSavedText(place.title, '추천 장소'),
    category: sanitizeSavedText(place.category, '추천'),
    duration: sanitizeSavedText(place.duration, '체류 시간 확인 필요'),
    distance: sanitizeSavedText(place.distance, '이동 경로 확인'),
    review: sanitizeSavedText(place.review, '여행 취향에 맞는 장소예요'),
    reviews: sanitizeSavedText(place.reviews, place.live ? '네이버 확인' : '데모'),
    rating: hasRating ? Math.min(5, Math.max(0, rating)) : null,
    cost: Number.isFinite(Number(place.cost)) ? Number(place.cost) : 0,
    costUnknown: Boolean(place.costUnknown),
    day: Number.isInteger(place.day) && place.day > 0 ? place.day : 1,
    time: sanitizeSavedText(place.time, '시간 확인'),
    live: Boolean(place.live),
    link: isSafeExternalUrl(place.link) ? place.link : '',
  };
}

function hasOptionValue(select, value) {
  return Array.from(select.options).some((option) => option.value === value);
}

function normalizeSavedForm(form) {
  const validDate = (value) => /^\d{4}-\d{2}-\d{2}$/.test(value);
  const validTime = (value) => /^\d{2}:\d{2}$/.test(value);
  const people = Number(form.people);
  const budget = Number(form.budget);
  const hasBudget = form.budget !== '' && form.budget !== null && form.budget !== undefined;
  return {
    destination: hasOptionValue(destinationInput, form.destination) ? form.destination : 'nationwide',
    theme: hasOptionValue(themeInput, form.theme) ? form.theme : 'auto',
    startDate: validDate(form.startDate) ? form.startDate : dateInput.value,
    endDate: validDate(form.endDate) ? form.endDate : endDateInput.value,
    startTime: validTime(form.startTime) ? form.startTime : startTimeInput.value,
    endTime: validTime(form.endTime) ? form.endTime : endTimeInput.value,
    noTimeLimit: Boolean(form.noTimeLimit),
    people: Number.isInteger(people) && people >= 1 && people <= 12 ? String(people) : peopleInput.value,
    transport: ['public', 'car', 'rental'].includes(form.transport) ? form.transport : 'public',
    budget: hasBudget && Number.isFinite(budget) && budget >= 0 ? String(budget) : '',
  };
}

function normalizeSavedPlan(plan) {
  if (!plan?.form || !Array.isArray(plan.places)) return null;
  const places = plan.places.map(normalizeSavedPlace).filter(Boolean);
  if (!places.length) return null;
  return {
    id: sanitizeSavedText(plan.id, `${Date.now()}-${Math.random()}`),
    title: sanitizeSavedText(plan.title, '저장된 여행 계획'),
    savedAt: sanitizeSavedText(plan.savedAt, new Date().toISOString()),
    updatedAt: sanitizeSavedText(plan.updatedAt, new Date().toISOString()),
    form: normalizeSavedForm(plan.form),
    places,
  };
}

function getPlanFormData() {
  return {
    destination: destinationInput.value,
    theme: themeInput.value,
    startDate: dateInput.value,
    endDate: endDateInput.value,
    startTime: startTimeInput.value,
    endTime: endTimeInput.value,
    noTimeLimit: noTimeLimitInput.checked,
    people: peopleInput.value,
    transport: transportInput.value,
    budget: budgetInput.value,
  };
}

function getPlanSnapshot() {
  if (!currentPlaces.length) return null;
  const now = new Date().toISOString();
  return {
    id: currentPlanId || `${Date.now()}-${Math.random().toString(36).slice(2)}`,
    title: document.querySelector('#result-title').textContent,
    savedAt: now,
    updatedAt: now,
    form: getPlanFormData(),
    places: currentPlaces.map((place) => ({ ...place })),
  };
}

function getSavedPlanScope(plan) {
  return plan.form.destination === 'nationwide'
    ? '전국'
    : getDestinationData(plan.form.destination).label;
}

function formatSavedTime(value) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? '저장 날짜 확인 필요' : date.toLocaleString('ko-KR', { dateStyle: 'medium', timeStyle: 'short' });
}

function renderSavedPlans() {
  savedPlansList.replaceChildren();
  if (!savedPlans.length) {
    const empty = document.createElement('p');
    empty.className = 'empty-plans';
    empty.textContent = '저장된 계획이 아직 없어요. 코스를 만든 뒤 하트 버튼으로 저장해보세요.';
    savedPlansList.append(empty);
    return;
  }
  savedPlans.forEach((plan) => {
    const article = document.createElement('article');
    article.className = 'saved-plan-item';
    const copy = document.createElement('div');
    const title = document.createElement('strong');
    title.textContent = plan.title || '저장된 여행 계획';
    const meta = document.createElement('p');
    meta.textContent = `${getSavedPlanScope(plan)} · ${plan.form.startDate} ~ ${plan.form.endDate} · ${plan.places.length}곳`;
    const saved = document.createElement('small');
    saved.textContent = formatSavedTime(plan.updatedAt || plan.savedAt);
    copy.append(title, meta, saved);
    const actions = document.createElement('div');
    actions.className = 'saved-plan-actions';
    ['load', 'delete'].forEach((action) => {
      const button = document.createElement('button');
      button.className = action === 'load' ? 'text-button text-button-primary' : 'text-button';
      button.type = 'button';
      button.dataset.planAction = action;
      button.dataset.planId = plan.id;
      button.textContent = action === 'load' ? '열기' : '삭제';
      actions.append(button);
    });
    article.append(copy, actions);
    savedPlansList.append(article);
  });
}

function savePlan() {
  const snapshot = getPlanSnapshot();
  if (!snapshot) {
    locationStatus.textContent = '먼저 여행 코스를 만든 뒤 저장해 주세요.';
    return;
  }
  const existing = savedPlans.find((plan) => plan.id === snapshot.id);
  if (existing) snapshot.savedAt = existing.savedAt;
  const previousPlans = savedPlans;
  savedPlans = planLogic.upsertPlan(savedPlans, snapshot, 20);
  if (!persistSavedPlans()) {
    savedPlans = previousPlans;
    renderSavedPlans();
    return;
  }
  currentPlanId = snapshot.id;
  renderSavedPlans();
  savedPlansPanel.hidden = false;
  savedPlansPanel.scrollIntoView({ behavior: 'smooth', block: 'start' });
  locationStatus.textContent = '여행 계획을 저장했어요.';
}

function applyPlanSnapshot(plan, message) {
  const formData = plan.form;
  currentLocation = null;
  locationMode = 'selected';
  currentAddress = '';
  destinationInput.value = formData.destination;
  themeInput.value = formData.theme;
  dateInput.value = formData.startDate;
  endDateInput.value = formData.endDate;
  updateEndDateBounds();
  startTimeInput.value = formData.startTime;
  endTimeInput.value = formData.endTime;
  noTimeLimitInput.checked = formData.noTimeLimit;
  syncTimeLimitInputs();
  peopleInput.value = formData.people;
  transportInput.value = formData.transport;
  budgetInput.value = formData.budget;
  currentPlanId = plan.id;
  routeOverride = plan.places.map((place) => ({ ...place }));
  replacementIndex = null;
  alternatePicker.hidden = true;
  renderPlan({ placesOverride: routeOverride });
  refreshLiveEvents();
  savedPlansPanel.hidden = true;
  document.querySelector('#result-section').scrollIntoView({ behavior: 'smooth', block: 'start' });
  locationStatus.textContent = message;
}

function loadSavedPlan(id) {
  const plan = savedPlans.find((item) => item.id === id);
  if (plan) {
    clearShareHash();
    applyPlanSnapshot(plan, '저장한 여행 계획을 불러왔어요.');
  }
}

function loadSharedPlan() {
  const sharedData = planLogic.parseShareHash(window.location.hash);
  if (!sharedData) return false;
  const plan = normalizeSavedPlan(sharedData);
  if (plan) {
    applyPlanSnapshot(plan, '공유된 여행 계획을 불러왔어요. 저장하면 내 계획에도 추가할 수 있어요.');
    return true;
  } else {
    locationStatus.textContent = '공유 링크의 여행 계획을 확인할 수 없어요.';
  }
  return false;
}

function deleteSavedPlan(id) {
  const previousPlans = savedPlans;
  savedPlans = planLogic.removePlan(savedPlans, id);
  if (!persistSavedPlans()) {
    savedPlans = previousPlans;
    renderSavedPlans();
    return;
  }
  if (currentPlanId === id) currentPlanId = null;
  renderSavedPlans();
}

function getAlternativePlaces(index) {
  const current = currentPlaces[index];
  const timeRange = getDailyTimeRange();
  if (!current || !timeRange) return [];
  const themePlan = getThemePlan(themeInput.value, dateInput.value, timeRange);
  const usedTitles = new Set(currentPlaces
    .filter((place, placeIndex) => placeIndex !== index)
    .map((place) => place.title.replace(/ · 다시 보기$/, '')));
  const curatedAlternatives = getDestinationData(destinationInput.value).places
    .filter((place) => place.title !== current.title && !usedTitles.has(place.title))
    .sort((left, right) => getThemeScore(left, themePlan.categories) - getThemeScore(right, themePlan.categories))
    .slice(0, 5);
  const liveAlternatives = currentNearbySource === 'naver'
    ? currentNearbyItems
      .filter((place) => !usedTitles.has(place.title))
      .slice(0, 5)
      .map((place) => createLiveAlternative(place, current))
    : [];
  return [...liveAlternatives, ...curatedAlternatives].slice(0, 5);
}

function createLiveAlternative(place, previous) {
  return {
    title: place.title,
    category: place.category || '실시간 장소',
    duration: '머무는 시간 확인',
    distance: getRouteDistance(transportInput.value),
    rating: null,
    reviews: '네이버 확인',
    review: '네이버 상세 링크에서 평점·영업시간을 확인하세요.',
    cost: 0,
    costUnknown: true,
    day: previous.day,
    time: previous.time,
    live: true,
    link: place.link || '',
  };
}

function openAlternatePicker(index) {
  const alternatives = getAlternativePlaces(index);
  replacementIndex = index;
  alternateList.replaceChildren();
  if (!alternatives.length) {
    alternateList.textContent = '현재 조건에서 바꿀 수 있는 추천지가 없어요.';
  }
  alternatives.forEach((place) => {
    const button = document.createElement('button');
    button.className = 'alternate-item';
    button.type = 'button';
    const rating = hasNumericRating(place.rating) ? `★ ${Number(place.rating).toFixed(1)}` : '평점 확인';
    const source = place.live ? '네이버 실시간' : rating;
    const title = document.createElement('strong');
    title.textContent = place.title;
    const meta = document.createElement('span');
    meta.textContent = `${place.category} · ${source} · ${place.duration}`;
    button.append(title, meta);
    button.addEventListener('click', () => replacePlace(place));
    alternateList.append(button);
  });
  alternatePicker.hidden = false;
  alternatePicker.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

function replacePlace(alternative) {
  if (replacementIndex === null || !currentPlaces[replacementIndex]) return;
  clearShareHash();
  const previous = currentPlaces[replacementIndex];
  const replacement = {
    ...alternative,
    day: previous.day,
    time: previous.time,
    distance: getRouteDistance(transportInput.value),
  };
  currentPlaces = currentPlaces.map((place, index) => index === replacementIndex ? replacement : place);
  routeOverride = currentPlaces.map((place) => ({ ...place }));
  alternatePicker.hidden = true;
  renderPlan({ placesOverride: routeOverride });
  locationStatus.textContent = '선택한 장소로 코스를 수정했어요. 저장하면 다음에도 그대로 열 수 있어요.';
}

function refreshPlan() {
  clearShareHash();
  routeOverride = null;
  currentPlanId = null;
  replacementIndex = null;
  alternatePicker.hidden = true;
  planGeneration += 1;
  renderPlan();
  locationStatus.textContent = '새로운 여행 코스를 다시 추천했어요.';
}

function startFreshPlan() {
  clearShareHash();
  routeOverride = null;
  currentPlanId = null;
  replacementIndex = null;
  alternatePicker.hidden = true;
}

function clearInvalidPlan() {
  currentPlaces = [];
  locationStatus.textContent = '출발일·도착일, 하루 시간, 인원, 예산을 올바르게 입력해 주세요.';
  document.querySelector('#result-title').textContent = '시간을 확인해 주세요';
  document.querySelector('#result-subtitle').textContent = '하루 시작 시간은 종료 시간보다 앞서야 해요.';
  document.querySelector('#average-rating').textContent = '—';
  itinerary.textContent = '하루 시작·종료 시간을 확인하면 추천 일정을 보여드려요.';
  document.querySelector('#route-heading').textContent = '일정 확인 필요';
  document.querySelector('#route-count').textContent = '일정 확인 필요';
  document.querySelector('#estimated-total').textContent = '—';
  document.querySelector('#daily-budget').textContent = '—';
  document.querySelector('#budget-message').textContent = '시간 범위를 확인해 주세요.';
  document.querySelector('#map-destination').textContent = '일정 확인 필요';
  document.querySelector('.map-label-one').textContent = '시간';
  document.querySelector('.map-label-two').textContent = '확인';
  document.querySelector('.map-label-three').textContent = '필요';
  renderMapMarkers([]);
  document.querySelector('#map-footer-text').textContent = '시간 범위를 확인하면 추천 장소를 표시해요';
  budgetFitButton.hidden = true;
}

function clearShareHash() {
  if (window.location.hash.startsWith('#plan=')) {
    window.history.replaceState(null, '', `${window.location.pathname}${window.location.search}`);
  }
}

function updatePlanSummary(destination, destinationData, days, people, transport, budget, budgetAware, themePlan, places, estimate) {
  const isNationwide = destination === 'nationwide';
  const dateLabel = formatDateRange();
  const timeLabel = getTimeRangeLabel(getDailyTimeRange());
  document.querySelector('#result-title').textContent = isNationwide
    ? `${themePlan.label} · 오늘의 전국 추천`
    : `${themePlan.label} · ${destinationData.label} ${destinationData.area}에서 보내는 ${days === 1 ? '하루' : `${days}일`}`;
  document.querySelector('#result-subtitle').textContent = isNationwide
    ? `${dateLabel} · ${timeLabel} · ${themePlan.seasonLabel} · 전국 후보예요. 도시를 고르면 실제 동선과 예산을 계산해요.`
    : `${dateLabel} · ${timeLabel} · ${themePlan.timeLabel} · ${people}명 · ${TRANSPORT_LABEL[transport]} · 데모 리뷰 데이터`;
  const estimatePrefix = estimate.unknownCosts ? '약 ' : '';
  document.querySelector('#estimated-total').textContent = isNationwide ? '지역 선택 필요' : `${estimatePrefix}${formatWon(estimate.total)}`;
  document.querySelector('#daily-budget').textContent = isNationwide ? '—' : `${estimatePrefix}${formatWon(estimate.total / days / people)}`;
  const ratedPlaces = places.filter((place) => hasNumericRating(place.rating));
  document.querySelector('#average-rating').innerHTML = ratedPlaces.length
    ? `${(ratedPlaces.reduce((total, place) => total + Number(place.rating), 0) / ratedPlaces.length).toFixed(1)} <span>★</span>`
    : '평점 확인';
  document.querySelector('#route-heading').textContent = isNationwide ? `${themePlan.label} · 오늘의 전국 후보` : `${themePlan.label} 동선`;
  document.querySelector('#route-count').textContent = isNationwide ? `${places.length}곳 후보` : `${places.length} places`;
  document.querySelector('#map-destination').textContent = destinationData.label;
  const mapLabels = destinationData.area.split(' · ');
  document.querySelector('.map-label-one').textContent = mapLabels[0] || destinationData.label;
  document.querySelector('.map-label-two').textContent = mapLabels[1] || '오늘 추천';
  document.querySelector('.map-label-three').textContent = isNationwide ? '지역별' : destinationData.label === '제주' ? '곶자왈' : '바다';
  updateBudgetSummary(isNationwide, budget, budgetAware, estimate);
}

function updateBudgetSummary(isNationwide, budget, budgetAware, estimate) {
  const budgetMessage = document.querySelector('#budget-message');
  if (isNationwide) {
    budgetMessage.textContent = '전국 후보는 도시 간 이동을 계산하지 않아요. 도시를 선택하면 예상비용과 예산 잔액을 계산해드려요.';
    budgetMessage.style.color = '';
    budgetFitButton.hidden = true;
  } else if (budget === null) {
    budgetMessage.textContent = estimate.unknownCosts
      ? '실시간 장소 비용은 현장에서 확인해 주세요.'
      : '예산을 입력하면 잔액을 계산해드려요.';
    budgetMessage.style.color = '';
    budgetFitButton.hidden = true;
  } else {
    const difference = budget - estimate.total;
    const estimateNote = estimate.unknownCosts ? ' · 실시간 장소 비용 별도' : '';
    budgetMessage.textContent = difference >= 0
      ? `${formatWon(difference)} 남아요${estimateNote}`
      : `${formatWon(Math.abs(difference))} 초과예요${estimateNote}`;
    budgetMessage.style.color = difference >= 0 ? 'var(--yellow)' : '#ffae9d';
    budgetFitButton.hidden = difference >= 0 || budgetAware;
  }
}

function renderPlan({ budgetAware = false, placesOverride = null } = {}) {
  const destination = destinationInput.value;
  const days = getTripDays();
  const timeRange = getDailyTimeRange();
  const people = Number(peopleInput.value);
  const transport = transportInput.value;
  const budget = budgetInput.value === '' ? null : Number(budgetInput.value);
  if (!Number.isInteger(days) || days < 1 || !timeRange || !Number.isInteger(people) || people < 1 || (budget !== null && (!Number.isFinite(budget) || budget < 0))) {
    clearInvalidPlan();
    return;
  }
  locationStatus.textContent = '';
  const themePlan = getThemePlan(themeInput.value, dateInput.value, timeRange);
  const destinationData = getDestinationData(destination);
  const isNationwide = destination === 'nationwide';
  const candidates = placesOverride?.length
    ? placesOverride
    : getSelectedPlaces(destination, days, themePlan, timeRange, transport, planGeneration);
  const places = budgetAware && budget !== null
    ? getBudgetFitPlaces(candidates, days, people, transport, budget)
    : candidates;
  const estimate = calculateEstimate(places, days, people, transport);
  currentPlaces = places;
  updatePlanSummary(destination, destinationData, days, people, transport, budget, budgetAware, themePlan, places, estimate);
  renderItinerary(places);
  renderNearby();
  loadLiveNearby();
}

function setTodayAsDefault() {
  const today = new Date();
  const year = today.getFullYear();
  const month = String(today.getMonth() + 1).padStart(2, '0');
  const day = String(today.getDate()).padStart(2, '0');
  dateInput.min = `${year}-${month}-${day}`;
  dateInput.value = `${year}-${month}-${day}`;
  endDateInput.value = `${year}-${month}-${day}`;
  updateEndDateBounds();
}

async function resolveCurrentAddress(position) {
  const params = new URLSearchParams({
    lat: String(position.coords.latitude),
    lon: String(position.coords.longitude),
  });
  try {
    const response = await fetch(`/api/location?${params.toString()}`);
    const payload = await response.json();
    if (payload.address) {
      currentAddress = payload.address;
      locationStatus.textContent = `${payload.address} 주변 추천을 준비했어요.`;
      renderNearby();
      loadLiveNearby();
      return;
    }
    useLocationRegionFallback();
  } catch (error) {
    useLocationRegionFallback();
  }
}

function useLocationRegionFallback() {
  const region = planLogic.getLocationRegionHint(currentLocation);
  if (!region) {
    resetLocationRecommendationState('현재 위치 주소를 확인하지 못했어요. 여행지를 직접 선택해 주세요.');
    return;
  }
  currentAddress = region;
  locationStatus.textContent = `${region} 인근에서 현재 위치를 확인했어요. 주변 장소를 불러오는 중이에요.`;
  renderNearby();
  loadLiveNearby();
}

function resetLocationRecommendationState(message) {
  currentLocation = null;
  locationMode = 'selected';
  currentAddress = '';
  locationStatus.textContent = message;
  renderNearby();
  loadLiveNearby();
}

function requestCurrentLocation() {
  locationStatus.textContent = '현재 위치를 확인하고 있어요…';
  if (!navigator.geolocation) {
    resetLocationRecommendationState('이 브라우저에서는 위치 확인을 지원하지 않아요. 여행지를 직접 선택해 주세요.');
    return;
  }
  navigator.geolocation.getCurrentPosition(
    (position) => {
      const location = { latitude: position.coords.latitude, longitude: position.coords.longitude };
      currentLocation = planLogic.isValidLocation(location) ? location : null;
      startFreshPlan();
      locationMode = 'current';
      currentAddress = '';
      locationStatus.textContent = '현재 위치를 확인했어요. 주변 장소를 불러오는 중이에요.';
      renderPlan();
      resolveCurrentAddress(position);
    },
    () => {
      resetLocationRecommendationState('위치 권한을 확인하지 못했어요. 여행지를 직접 선택해도 괜찮아요.');
    },
    { timeout: 8000 },
  );
}

form.addEventListener('submit', (event) => {
  event.preventDefault();
  startFreshPlan();
  locationMode = 'selected';
  currentLocation = null;
  currentAddress = '';
  renderPlan();
  document.querySelector('#result-section').scrollIntoView({ behavior: 'smooth', block: 'start' });
});

document.querySelector('#locate-button').addEventListener('click', requestCurrentLocation);
nearbyLocationButton.addEventListener('click', requestCurrentLocation);
nearbyMapButton.addEventListener('click', () => {
  document.querySelector('#recommendation-map-card').scrollIntoView({ behavior: 'smooth', block: 'center' });
});
budgetFitButton.addEventListener('click', () => {
  clearShareHash();
  renderPlan({ budgetAware: true });
});
savePlanButton.addEventListener('click', savePlan);
historyButton.addEventListener('click', () => {
  savedPlansPanel.hidden = !savedPlansPanel.hidden;
  if (!savedPlansPanel.hidden) {
    renderSavedPlans();
    savedPlansPanel.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
});
refreshPlanButton.addEventListener('click', refreshPlan);
closeHistoryButton.addEventListener('click', () => { savedPlansPanel.hidden = true; });
closeAlternateButton.addEventListener('click', () => { alternatePicker.hidden = true; });
todayList.addEventListener('click', (event) => {
  const button = event.target.closest('[data-today-destination]');
  if (!button) return;
  destinationInput.value = button.dataset.todayDestination;
  destinationInput.dispatchEvent(new Event('change'));
  document.querySelector('#result-section').scrollIntoView({ behavior: 'smooth', block: 'start' });
  locationStatus.textContent = '오늘 추천 지역으로 코스를 다시 만들었어요.';
});
itinerary.addEventListener('click', (event) => {
  const button = event.target.closest('[data-place-change-index]');
  if (button) openAlternatePicker(Number(button.dataset.placeChangeIndex));
});
savedPlansList.addEventListener('click', (event) => {
  const button = event.target.closest('[data-plan-action]');
  if (!button) return;
  if (button.dataset.planAction === 'load') loadSavedPlan(button.dataset.planId);
  if (button.dataset.planAction === 'delete') deleteSavedPlan(button.dataset.planId);
});
destinationInput.addEventListener('change', () => {
  startFreshPlan();
  locationMode = 'selected';
  currentLocation = null;
  currentAddress = '';
  renderPlan();
  renderDemoEvents();
  loadLiveEvents();
});
dateInput.addEventListener('change', () => {
  startFreshPlan();
  updateEndDateBounds();
  renderPlan();
  renderDemoEvents();
  loadLiveEvents();
});
endDateInput.addEventListener('change', () => {
  startFreshPlan();
  if (endDateInput.value < dateInput.value) {
    endDateInput.value = dateInput.value;
  }
  if (endDateInput.max && endDateInput.value > endDateInput.max) {
    endDateInput.value = endDateInput.max;
  }
  renderPlan();
  renderDemoEvents();
  loadLiveEvents();
});
[themeInput, startTimeInput, endTimeInput].forEach((input) => {
  input.addEventListener('change', () => {
    startFreshPlan();
    renderPlan();
    renderDemoEvents();
    loadLiveEvents();
  });
});
noTimeLimitInput.addEventListener('change', () => {
  startFreshPlan();
  syncTimeLimitInputs();
  renderPlan();
  renderDemoEvents();
  loadLiveEvents();
});
foodQueryInput.addEventListener('input', () => {
  foodQuery = foodQueryInput.value.trim();
  renderNearby();
});
foodTypeInput.addEventListener('change', () => {
  foodType = foodTypeInput.value;
  renderNearby();
  loadLiveNearby();
});
nearbySortInput.addEventListener('change', () => {
  nearbySort = nearbySortInput.value;
  renderNearby();
  loadLiveNearby();
});
document.querySelector('#food-search-button').addEventListener('click', () => {
  foodQuery = foodQueryInput.value.trim();
  renderNearby();
  loadLiveNearby();
});
document.querySelectorAll('[data-nearby-filter]').forEach((button) => {
  button.addEventListener('click', () => {
    nearbyFilter = button.dataset.nearbyFilter;
    if (nearbyFilter !== 'restaurant') {
      foodQuery = '';
      foodType = '';
      foodQueryInput.value = '';
      foodTypeInput.value = '';
    }
    document.querySelectorAll('[data-nearby-filter]').forEach((chip) => {
      chip.classList.toggle('active', chip === button);
      chip.setAttribute('aria-pressed', String(chip === button));
    });
    renderNearby();
    loadLiveNearby();
  });
});
async function sharePlan() {
  const snapshot = getPlanSnapshot();
  if (!snapshot) {
    locationStatus.textContent = '먼저 여행 코스를 만든 뒤 공유해 주세요.';
    return;
  }
  const shareUrl = new URL(window.location.href);
  shareUrl.hash = planLogic.createShareHash(snapshot);
  window.history.replaceState(null, '', shareUrl.href);
  const shareData = { title: snapshot.title || '코스온 코스', text: '내 코스온 여행 계획을 확인해보세요.', url: shareUrl.href };
  if (navigator.share) {
    try {
      await navigator.share(shareData);
      locationStatus.textContent = '여행 계획을 공유했어요.';
      return;
    } catch (error) {
      if (error.name === 'AbortError') return;
    }
  }
  try {
    await navigator.clipboard.writeText(shareUrl.href);
    locationStatus.textContent = '공유 링크를 클립보드에 복사했어요.';
  } catch (error) {
    locationStatus.textContent = '공유 링크가 주소창에 준비됐어요. 주소를 복사해 주세요.';
  }
}

document.querySelector('#share-plan-button').addEventListener('click', sharePlan);

setTodayAsDefault();
savedPlans = readSavedPlans();
renderPlan();
renderTodayAndEvents(false);
renderSavedPlans();
if (!loadSharedPlan()) refreshLiveEvents();
loadNaverMapSdk();
registerServiceWorker();
