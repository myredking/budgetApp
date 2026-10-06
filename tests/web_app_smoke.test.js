const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const logic = require('../web/plan_logic.js');

class FakeElement {
  constructor(tagName = 'div') {
    this.tagName = tagName;
    this.children = [];
    this.listeners = {};
    this.dataset = {};
    this.style = {};
    this.className = '';
    this.textContent = '';
    this.hidden = false;
    this.disabled = false;
    this.value = '';
    this.options = [];
    this.attributes = {};
    this.classList = {
      values: new Set(),
      add: (...names) => names.forEach((name) => this.classList.values.add(name)),
      remove: (...names) => names.forEach((name) => this.classList.values.delete(name)),
      toggle: (name, force) => {
        const next = force === undefined ? !this.classList.values.has(name) : force;
        if (next) this.classList.values.add(name);
        else this.classList.values.delete(name);
        return next;
      },
      contains: (name) => this.classList.values.has(name),
    };
  }

  append(...items) {
    this.children.push(...items.filter(Boolean));
  }

  replaceChildren(...items) {
    this.children = items.filter(Boolean);
  }

  addEventListener(type, handler) {
    this.listeners[type] = [...(this.listeners[type] || []), handler];
  }

  dispatchEvent(event) {
    const nextEvent = typeof event === 'string' ? { type: event, target: this } : event;
    return Promise.all((this.listeners[nextEvent.type] || []).map((handler) => handler(nextEvent)));
  }

  closest(selector) {
    if (selector === '[data-today-destination]' && this.dataset.todayDestination) return this;
    if (selector === '[data-place-change-index]' && this.dataset.placeChangeIndex) return this;
    if (selector === '[data-plan-action]' && this.dataset.planAction) return this;
    return null;
  }

  querySelector(selector) {
    const valueMatch = selector.match(/^option\[value="([^"]+)"\]$/);
    if (valueMatch) {
      return this.children.find((node) => node.tagName === 'option' && node.value === valueMatch[1]) || null;
    }
    return this.find((node) => node.tagName === selector);
  }

  setAttribute(name, value) {
    this.attributes[name] = String(value);
    this[name] = String(value);
  }

  removeAttribute(name) {
    delete this.attributes[name];
    delete this[name];
  }

  getAttribute(name) {
    return this.attributes[name] ?? null;
  }

  scrollIntoView() {}

  find(predicate) {
    for (const child of this.children) {
      if (child instanceof FakeElement && predicate(child)) return child;
      if (child instanceof FakeElement) {
        const match = child.find(predicate);
        if (match) return match;
      }
    }
    return null;
  }
}

function buildElements() {
  const ids = [
    'planner-form', 'destination', 'theme', 'trip-date', 'trip-end-date', 'start-time', 'end-time',
    'no-time-limit', 'people', 'transport', 'budget', 'location-status', 'itinerary',
    'budget-fit-button', 'save-plan-button', 'history-button', 'refresh-plan-button', 'saved-plans-panel',
    'saved-plans-list', 'alternate-picker', 'alternate-list', 'close-history-button', 'close-alternate-button',
    'nearby-list', 'nearby-title', 'nearby-source-status', 'event-source-status', 'data-mode-pill',
    'today-list', 'today-label', 'event-list', 'event-heading', 'food-search', 'food-query', 'food-type',
    'map-points', 'map-footer-text', 'map-destination', 'nearby-map-note', 'nearby-location-button',
    'nearby-map-button', 'nearby-sort', 'nearby-sort-hint', 'naver-map', 'map-fallback', 'map-provider-status',
    'integration-title', 'integration-copy', 'integration-status', 'result-title', 'result-subtitle',
    'average-rating', 'route-heading', 'route-count', 'estimated-total', 'daily-budget', 'budget-message',
    'planner-grid', 'result-section', 'discovery-section', 'events-section', 'recommendation-map-card',
    'locate-button', 'food-search-button', 'share-plan-button',
  ];
  const elements = new Map(ids.map((id) => [`#${id}`, new FakeElement('div')]));
  elements.get('#saved-plans-panel').hidden = true;
  elements.get('#destination').value = 'nationwide';
  elements.get('#theme').value = 'auto';
  elements.get('#start-time').value = '10:00';
  elements.get('#end-time').value = '20:00';
  elements.get('#people').value = '2';
  elements.get('#transport').value = 'public';
  elements.get('#no-time-limit').checked = false;
  elements.get('#food-query').value = '';
  elements.get('#food-type').value = '';
  elements.get('#budget').value = '';
  elements.get('#nearby-sort').value = 'recommended';
  elements.get('#nearby-sort').children = ['recommended', 'rating', 'distance'].map((value) => {
    const option = new FakeElement('option');
    option.value = value;
    option.textContent = value;
    return option;
  });
  elements.get('#destination').options = ['nationwide', 'seoul', 'busan'].map((value) => ({ value }));
  elements.get('#theme').options = ['auto', 'date', 'healing', 'food', 'culture', 'nature', 'night'].map((value) => ({ value }));
  elements.filterButtons = ['all', 'restaurant', 'cafe', 'attraction'].map((value, index) => {
    const button = new FakeElement('button');
    button.dataset.nearbyFilter = value;
    button.className = index === 0 ? 'filter-chip active' : 'filter-chip';
    button.setAttribute('aria-pressed', String(index === 0));
    return button;
  });
  elements.quickTabs = ['planner-grid', 'result-section', 'discovery-section', 'events-section', 'saved-plans-panel'].map((target, index) => {
    const link = new FakeElement('a');
    link.dataset.quickNav = target;
    link.className = index === 0 ? 'quick-tab is-active' : 'quick-tab';
    if (index === 0) link.setAttribute('aria-current', 'page');
    return link;
  });
  return elements;
}

function createHarness({ hash = '', nearbyItems = [], failLocation = false, locationAddress = '서울특별시 서울시' } = {}) {
  const elements = buildElements();
  let eventCalls = 0;
  let failEvents = false;
  const nearbyRequests = [];
  const eventRequests = [];
  const navigator = { serviceWorker: { register: async () => {} } };
  const document = {
    head: new FakeElement('head'),
    querySelector: (selector) => elements.get(selector) || new FakeElement(),
      querySelectorAll: (selector) => {
        if (selector === '[data-nearby-filter]') return elements.filterButtons;
        if (selector === '[data-quick-nav]') return elements.quickTabs;
        return [];
      },
    createElement: (tagName) => new FakeElement(tagName),
    createTextNode: (text) => ({ textContent: text }),
  };
  const fetch = async (request) => {
    const url = String(request);
    if (url.includes('/api/config')) return { json: async () => ({ map_client_id: '' }) };
    if (url.includes('/api/nearby')) {
      nearbyRequests.push(url);
      return { json: async () => ({ source: 'naver', items: nearbyItems }) };
    }
    if (url.includes('/api/location')) {
      if (failLocation) throw new Error('location unavailable');
      return { json: async () => ({ address: locationAddress }) };
    }
    if (url.includes('/api/events')) {
      eventCalls += 1;
      eventRequests.push(url);
      if (failEvents) throw new Error('events unavailable');
      return { json: async () => ({ source: 'tour_api', items: [{ title: '강남페스티벌', address: '서울 강남구', start_date: '20261003', end_date: '20261005', place: '코엑스' }] }) };
    }
    return { json: async () => ({}) };
  };
  const context = {
    console,
    URL,
    URLSearchParams,
    fetch,
    document,
    window: null,
    navigator,
    localStorage: { getItem: () => null, setItem: () => {} },
    setTimeout,
    Event: class { constructor(type) { this.type = type; this.target = null; } },
    location: { hash, href: `https://courseon.test/${hash}`, pathname: '/', search: '' },
    history: { replaceState: () => {} },
  };
  context.window = context;
  context.TravelPlanLogic = logic;
  vm.runInNewContext(fs.readFileSync(path.join(__dirname, '..', 'web', 'app.js'), 'utf8'), context);
  return {
    elements,
    navigator,
    setFailEvents: (value) => { failEvents = value; },
    getEventCalls: () => eventCalls,
    getEventRequests: () => eventRequests,
    getNearbyRequests: () => nearbyRequests,
    filterButtons: elements.filterButtons,
    quickTabs: elements.quickTabs,
  };
}

function textFrom(node) {
  if (!(node instanceof FakeElement)) return '';
  return [node.textContent || '', ...node.children.map(textFrom)].join(' ');
}

async function settle() {
  await new Promise((resolve) => setImmediate(resolve));
  await new Promise((resolve) => setImmediate(resolve));
}

test('행사 카드의 링크와 조회 실패 재시도 동작을 실제 DOM으로 확인한다', async () => {
  const harness = createHarness();
  await settle();
  const eventList = harness.elements.get('#event-list');
  const link = eventList.find((node) => node.tagName === 'a');
  assert.ok(link);
  assert.equal(link.textContent, '행사 위치·상세 보기 ↗');
  assert.match(link.href, /%EA%B0%95%EB%82%A8%ED%8E%98%EC%8A%A4%ED%8B%B0%EB%B2%8C/);

  harness.setFailEvents(true);
  await harness.elements.get('#destination').dispatchEvent({ type: 'change', target: harness.elements.get('#destination') });
  await settle();
  const retry = eventList.find((node) => node.tagName === 'button' && node.textContent === '다시 확인');
  assert.ok(retry);
  assert.ok(eventList.find((node) => node.tagName === 'strong' && node.textContent === '오늘의 지역 문화행사'));
  const callsBeforeRetry = harness.getEventCalls();
  await retry.dispatchEvent({ type: 'click', target: retry });
  await settle();
  assert.equal(harness.getEventCalls(), callsBeforeRetry + 1);
});

test('전국 후보 화면은 실제 도시 동선과 다른 안내를 보여준다', async () => {
  const harness = createHarness();
  await settle();
  assert.equal(harness.elements.get('#nearby-title').textContent, '오늘 전국 추천 장소');
  assert.match(harness.elements.get('#result-subtitle').textContent, /전국 후보예요/);
  assert.match(harness.elements.get('#route-heading').textContent, /오늘의 전국 후보/);
  assert.match(textFrom(harness.elements.get('#itinerary')), /도시 선택 후 이동 계산/);
});

test('실시간 주변 데이터에 평점·현재 위치가 없으면 정렬 조건을 설명하고 잠근다', async () => {
  const harness = createHarness({ nearbyItems: [{ title: '평점 없는 장소', category: '관광지' }] });
  await settle();
  const sort = harness.elements.get('#nearby-sort');
  const rating = sort.children.find((option) => option.value === 'rating');
  const distance = sort.children.find((option) => option.value === 'distance');
  assert.equal(rating.disabled, true);
  assert.equal(distance.disabled, true);
  assert.match(rating.textContent, /상세 평점 없음/);
  assert.match(distance.textContent, /내 위치 필요/);
  assert.match(harness.elements.get('#nearby-sort-hint').textContent, /거리순은 내 위치 필요/);
});

test('오늘 추천 버튼을 누르면 목적지와 생성된 코스가 실제로 바뀐다', async () => {
  const harness = createHarness();
  await settle();
  const todayList = harness.elements.get('#today-list');
  const button = todayList.find((node) => node.tagName === 'button');
  assert.ok(button);
  await todayList.dispatchEvent({ type: 'click', target: button });
  await settle();
  assert.equal(harness.elements.get('#destination').value, 'gyeongju');
  assert.match(harness.elements.get('#result-title').textContent, /경주/);
});

test('현재 위치 확인에 실패하면 이전 주변 추천을 선택 지역 기준으로 되돌린다', async () => {
  const harness = createHarness();
  harness.elements.get('#destination').value = 'seoul';
  await settle();
  harness.navigator.geolocation = {
    getCurrentPosition: (success) => success({ coords: { latitude: 37.5665, longitude: 126.978 } }),
  };
  await harness.elements.get('#locate-button').dispatchEvent({ type: 'click', target: harness.elements.get('#locate-button') });
  await settle();
  assert.equal(harness.elements.get('#nearby-title').textContent, '현재 위치 주변 추천');

  harness.navigator.geolocation = { getCurrentPosition: (_, failure) => failure() };
  await harness.elements.get('#locate-button').dispatchEvent({ type: 'click', target: harness.elements.get('#locate-button') });
  await settle();
  assert.equal(harness.elements.get('#nearby-title').textContent, '서울 주변 추천');
  assert.match(harness.elements.get('#location-status').textContent, /여행지를 직접 선택/);
  assert.equal(harness.elements.get('#map-points').find((node) => node.className === 'map-user-point'), null);
  assert.match(harness.getNearbyRequests().at(-1), /query=%EC%84%9C%EC%9A%B8/);
});

test('빠른 이동 탭은 저장 계획을 열고 닫을 때 활성 상태를 갱신한다', async () => {
  const harness = createHarness();
  await settle();
  const savedTab = harness.quickTabs.at(-1);
  await savedTab.dispatchEvent({ type: 'click', target: savedTab, preventDefault: () => {} });
  await settle();
  assert.equal(harness.elements.get('#saved-plans-panel').hidden, false);
  assert.equal(savedTab.getAttribute('aria-current'), 'page');

  await harness.elements.get('#history-button').dispatchEvent({ type: 'click', target: harness.elements.get('#history-button') });
  assert.equal(harness.elements.get('#saved-plans-panel').hidden, true);
  assert.equal(savedTab.getAttribute('aria-current'), null);
  assert.equal(harness.quickTabs[1].getAttribute('aria-current'), 'page');
});

test('위치 기능을 지원하지 않아도 선택한 지역 기준으로 주변 추천을 복구한다', async () => {
  const harness = createHarness();
  harness.elements.get('#destination').value = 'busan';
  await settle();
  await harness.elements.get('#locate-button').dispatchEvent({ type: 'click', target: harness.elements.get('#locate-button') });
  await settle();
  assert.equal(harness.elements.get('#nearby-title').textContent, '부산 주변 추천');
  assert.match(harness.elements.get('#location-status').textContent, /위치 확인을 지원하지 않아요/);
  assert.match(harness.getNearbyRequests().at(-1), /query=%EB%B6%80%EC%82%B0/);
});

test('현재 위치의 주소 변환에 실패해도 좌표 기반 도시 권역으로 주변 추천을 이어간다', async () => {
  const harness = createHarness({ failLocation: true });
  harness.elements.get('#destination').value = 'seoul';
  await settle();
  harness.navigator.geolocation = {
    getCurrentPosition: (success) => success({ coords: { latitude: 37.5665, longitude: 126.978 } }),
  };
  await harness.elements.get('#locate-button').dispatchEvent({ type: 'click', target: harness.elements.get('#locate-button') });
  await settle();
  assert.equal(harness.elements.get('#nearby-title').textContent, '현재 위치 주변 추천');
  assert.match(harness.elements.get('#location-status').textContent, /서울 인근/);
  assert.ok(harness.elements.get('#map-points').find((node) => node.className === 'map-user-point'));
  assert.match(harness.getNearbyRequests().at(-1), /query=%EC%84%9C%EC%9A%B8/);
});

test('주소 변환 응답이 비어도 좌표 기반 도시 권역으로 주변 추천을 이어간다', async () => {
  const harness = createHarness({ locationAddress: '' });
  harness.elements.get('#destination').value = 'seoul';
  await settle();
  harness.navigator.geolocation = {
    getCurrentPosition: (success) => success({ coords: { latitude: 37.5665, longitude: 126.978 } }),
  };
  await harness.elements.get('#locate-button').dispatchEvent({ type: 'click', target: harness.elements.get('#locate-button') });
  await settle();
  assert.equal(harness.elements.get('#nearby-title').textContent, '현재 위치 주변 추천');
  assert.match(harness.elements.get('#location-status').textContent, /서울 인근/);
  assert.ok(harness.elements.get('#map-points').find((node) => node.className === 'map-user-point'));
  assert.match(harness.getNearbyRequests().at(-1), /query=%EC%84%9C%EC%9A%B8/);
});

test('주변 추천 필터의 선택 상태를 보조기기에 전달한다', async () => {
  const harness = createHarness();
  await settle();
  assert.equal(harness.filterButtons[0].getAttribute('aria-pressed'), 'true');
  await harness.filterButtons[1].dispatchEvent({ type: 'click', target: harness.filterButtons[1] });
  await settle();
  assert.equal(harness.filterButtons[0].getAttribute('aria-pressed'), 'false');
  assert.equal(harness.filterButtons[1].getAttribute('aria-pressed'), 'true');
});

test('공유 계획을 열면 계획의 지역과 날짜로 행사 추천을 다시 조회한다', async () => {
  const shareHash = logic.createShareHash({
    id: 'shared-plan',
    title: '부산 주말 계획',
    savedAt: '2026-10-03T09:00:00.000Z',
    updatedAt: '2026-10-03T09:00:00.000Z',
    form: {
      destination: 'busan',
      theme: 'date',
      startDate: '2026-10-05',
      endDate: '2026-10-05',
      startTime: '10:00',
      endTime: '20:00',
      noTimeLimit: false,
      people: '2',
      transport: 'public',
      budget: '',
    },
    places: [{
      title: '광안리 산책', category: '산책', duration: '1시간', distance: '도보 5분',
      rating: 4.8, reviews: '데모', review: '바다를 보며 걷기 좋아요', cost: 0, day: 1, time: '10:00',
    }],
  });
  const harness = createHarness({ hash: shareHash });
  await settle();
  assert.equal(harness.getEventRequests().length, 1);
  assert.match(harness.getEventRequests().at(-1), /start_date=20261005/);
  assert.match(harness.getEventRequests().at(-1), /destination=busan/);
});

test('실시간 주변 장소 카드에 거리·평점·영업시간·주차 상태를 표시한다', async () => {
  const harness = createHarness({
    nearbyItems: [{
      title: '성수 테스트 식당',
      category: '한식>고기요리',
      address: '서울 성동구',
      road_address: '서울 성동구 연무장길 1',
      mapx: '1269861471',
      mapy: '375611325',
      rating: '4.6',
      reviews: '120',
      opening_hours: '10:00~22:00',
      parking: '주차 가능',
      link: 'https://example.com/place',
    }],
  });
  await settle();
  harness.navigator.geolocation = {
    getCurrentPosition: (success) => success({ coords: { latitude: 37.5665, longitude: 126.978 } }),
  };
  await harness.elements.get('#nearby-location-button').dispatchEvent({ type: 'click', target: harness.elements.get('#nearby-location-button') });
  await settle();
  const cardText = textFrom(harness.elements.get('#nearby-list').children[0]);
  assert.match(cardText, /거리 934m/);
  assert.match(cardText, /평점 4\.6/);
  assert.match(cardText, /영업시간 10:00~22:00/);
  assert.match(cardText, /주차 가능/);
});
