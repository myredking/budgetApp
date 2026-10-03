(function (root, factory) {
  const api = factory();
  if (typeof module !== 'undefined' && module.exports) {
    module.exports = api;
  }
  if (root) {
    root.TravelPlanLogic = api;
  }
})(typeof window !== 'undefined' ? window : globalThis, function () {
  function parseTimeMinutes(value) {
    if (typeof value !== 'string' || !/^\d{2}:\d{2}$/.test(value)) return null;
    const [hour, minute] = value.split(':').map(Number);
    return hour < 24 && minute < 60 ? hour * 60 + minute : null;
  }

  function getDailyTimeRange(startValue, endValue, noTimeLimit) {
    if (noTimeLimit) {
      return { startMinutes: 480, endMinutes: 1320, hours: 14, unlimited: true };
    }
    const startMinutes = parseTimeMinutes(startValue);
    const endMinutes = parseTimeMinutes(endValue);
    if (startMinutes === null || endMinutes === null || endMinutes <= startMinutes) return null;
    return { startMinutes, endMinutes, hours: (endMinutes - startMinutes) / 60, unlimited: false };
  }

  function getTimeRangeLabel(timeRange, startValue, endValue) {
    return timeRange.unlimited ? '시간 제한 없음' : `${startValue}~${endValue}`;
  }

  function formatTime(minutes) {
    return `${String(Math.floor(minutes / 60)).padStart(2, '0')}:${String(minutes % 60).padStart(2, '0')}`;
  }

  function buildTimeSlots(count, timeRange) {
    if (count <= 0) return [];
    const interval = (timeRange.endMinutes - timeRange.startMinutes) / count;
    return Array.from({ length: count }, (_, index) => formatTime(Math.floor(timeRange.startMinutes + interval * index)));
  }

  function upsertPlan(plans, snapshot, limit) {
    return [snapshot, ...plans.filter((plan) => plan.id !== snapshot.id)].slice(0, limit);
  }

  function removePlan(plans, id) {
    return plans.filter((plan) => plan.id !== id);
  }

  function createShareHash(snapshot) {
    return `#plan=${encodeURIComponent(JSON.stringify(snapshot))}`;
  }

  function parseShareHash(hash) {
    if (typeof hash !== 'string' || !hash.startsWith('#plan=')) return null;
    try {
      return JSON.parse(decodeURIComponent(hash.slice('#plan='.length)));
    } catch (error) {
      return null;
    }
  }

  function createNaverSearchUrl(title) {
    return `https://map.naver.com/p/search/${encodeURIComponent(title)}`;
  }

  function createEventSearchUrl(title, place) {
    const query = [title, place].filter((value) => typeof value === 'string' && value.trim()).join(' ');
    return createNaverSearchUrl(query || '국내 행사');
  }

  function isValidCalendarDate(value) {
    if (typeof value !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return false;
    const [year, month, day] = value.split('-').map(Number);
    const date = new Date(year, month - 1, day);
    return date.getFullYear() === year && date.getMonth() === month - 1 && date.getDate() === day;
  }

  function getDayDateLabel(startDate, day) {
    if (!isValidCalendarDate(startDate) || !Number.isInteger(day) || day < 1) return '';
    const date = new Date(`${startDate}T00:00:00`);
    if (Number.isNaN(date.getTime())) return '';
    date.setDate(date.getDate() + day - 1);
    return `${date.getMonth() + 1}월 ${date.getDate()}일`;
  }

  function getDataSourceLabel(source, kind) {
    if (source === 'naver' && kind === 'nearby') return '네이버 실시간';
    if (source === 'naver_empty' && kind === 'nearby') return '네이버 검색 결과 없음';
    if (source === 'loading' && kind === 'nearby') return '실시간 검색 중';
    if (source === 'naver_error' && kind === 'nearby') return '네이버 조회 실패';
    if (source === 'tour_api' && kind === 'events') return '관광공사 실시간';
    if (source === 'loading' && kind === 'events') return '행사 조회 중';
    if (source === 'tour_error' && kind === 'events') return '관광공사 조회 실패';
    if (source === 'tour_empty' && kind === 'events') return '행사 없음';
    return '데모 모드';
  }

  function getIntegrationStatus(hasMapSdk, nearbySource, eventSource) {
    const hasNearbyLiveData = nearbySource === 'naver';
    const hasEventLiveData = eventSource === 'tour_api';
    const hasLiveData = hasNearbyLiveData || hasEventLiveData;
    const hasSearchFailure = [
      'naver_error', 'tour_error', 'naver_empty', 'tour_empty',
    ].includes(nearbySource) || [
      'naver_error', 'tour_error', 'naver_empty', 'tour_empty',
    ].includes(eventSource);
    const nearbyIsLoading = nearbySource === 'loading';
    const eventIsLoading = eventSource === 'loading';
    if (nearbyIsLoading || eventIsLoading) {
      if (nearbyIsLoading && eventIsLoading) {
        return hasMapSdk
          ? {
            title: '네이버 지도를 준비하고 실시간 추천을 확인하고 있어요.',
            message: '지도와 장소·행사 검색을 불러오는 중이에요.',
            status: '지도·검색 확인 중',
          }
          : {
            title: '실시간 추천 데이터를 확인하고 있어요.',
            message: '장소·행사 검색 결과를 불러오는 중이에요.',
            status: '확인 중',
          };
      }
      const loadingKind = nearbyIsLoading ? '장소' : '행사';
      const readyKind = nearbyIsLoading ? '행사' : '장소';
      const readySource = nearbyIsLoading ? eventSource : nearbySource;
      const readyMessage = readySource === 'naver' || readySource === 'tour_api'
        ? `${readyKind} 추천은 확인됐어요.`
        : readySource === 'demo'
          ? `${readyKind} 추천은 예시 데이터예요.`
          : readySource === 'naver_empty' || readySource === 'tour_empty'
            ? `${readyKind} 검색 결과가 없어요.`
            : `${readyKind} 검색에 문제가 있어요.`;
      return hasMapSdk
        ? {
          title: `네이버 지도와 ${loadingKind} 추천을 확인하고 있어요.`,
          message: `지도와 ${loadingKind} 검색 결과를 불러오는 중이에요. ${readyMessage}`,
          status: `지도·${loadingKind} 확인 중`,
        }
        : {
          title: `${loadingKind} 추천을 확인하고 있어요.`,
          message: `${loadingKind} 검색 결과를 불러오는 중이에요. ${readyMessage}`,
          status: `${loadingKind} 확인 중`,
        };
    }
    if (hasMapSdk && hasNearbyLiveData && hasEventLiveData) {
      return {
        title: '네이버 지도·지역 검색이 연결되어 있어요.',
        message: '실시간 검색 결과와 장소 위치를 지도에서 확인할 수 있어요.',
        status: '지도 연결됨',
      };
    }
    if (hasMapSdk && hasLiveData) {
      const liveKind = hasNearbyLiveData ? '장소' : '행사';
      return {
        title: '네이버 지도와 일부 실시간 추천이 연결되어 있어요.',
        message: `지도와 ${liveKind} 검색은 사용할 수 있어요. 다른 추천 영역은 예시 데이터로 보여드려요.`,
        status: '지도·검색 일부 연결',
      };
    }
    if (hasMapSdk) {
      return {
        title: '네이버 지도는 연결되어 있어요.',
        message: hasLiveData
          ? '지도는 사용할 수 있고 일부 실시간 추천도 연결됐어요.'
          : hasSearchFailure
            ? '지도는 사용할 수 있지만 실시간 검색 결과를 확인하지 못했어요.'
            : '지도는 사용할 수 있지만 장소·행사 검색은 예시 추천을 보여드리고 있어요.',
        status: '지도만 연결됨',
      };
    }
    if (hasNearbyLiveData && hasEventLiveData) {
      return {
        title: '실시간 추천 데이터가 연결되어 있어요.',
        message: '장소·행사 검색은 연결됐어요. 지도 키를 연결하면 실제 지도와 마커도 표시돼요.',
        status: '검색 연결됨',
      };
    }
    if (hasLiveData) {
      const liveKind = hasNearbyLiveData ? '장소' : '행사';
      return {
        title: '일부 실시간 추천 데이터가 연결되어 있어요.',
        message: `${liveKind} 검색은 연결됐어요. 다른 추천 영역은 예시 데이터로 보여드려요.`,
        status: '검색 일부 연결',
      };
    }
    return {
      title: '추천 데이터 연결을 확인해 주세요.',
      message: '현재는 예시 추천을 보여드리고 있어요.',
      status: '데모 모드',
    };
  }

  function getNearbySearchContext(destination, destinationLabel, currentAddress, filter) {
    const categoryLabels = { restaurant: '식당', cafe: '카페', attraction: '관광지' };
    const address = typeof currentAddress === 'string' ? currentAddress.trim() : '';
    const isNationwide = destination === 'nationwide' && !address;
    if (isNationwide && filter && filter !== 'all') {
      return { query: '대한민국', category: categoryLabels[filter] || '관광지' };
    }
    return {
      query: address || (isNationwide ? '대한민국 관광지' : destinationLabel),
      category: categoryLabels[filter] || (isNationwide ? '관광지' : '추천 장소'),
    };
  }

  function isValidLocation(location) {
    if (!location || !Number.isFinite(location.latitude) || !Number.isFinite(location.longitude)) return false;
    return location.latitude >= -90 && location.latitude <= 90
      && location.longitude >= -180 && location.longitude <= 180;
  }

  function distanceMeters(value) {
    if (Number.isFinite(value)) return value;
    if (typeof value !== 'string') return Number.POSITIVE_INFINITY;
    const match = value.replace(',', '.').match(/([0-9.]+)\s*(km|m)/i);
    if (!match) return Number.POSITIVE_INFINITY;
    const amount = Number(match[1]);
    return match[2].toLowerCase() === 'km' ? amount * 1000 : amount;
  }

  function sortNearbyItems(items, sort) {
    return items
      .map((place, index) => ({ place, index }))
      .sort((left, right) => {
        if (sort === 'rating') {
          const rating = (place) => place.rating === null || place.rating === undefined || place.rating === ''
            ? -Infinity
            : Number.isFinite(Number(place.rating)) ? Number(place.rating) : -Infinity;
          return rating(right.place) - rating(left.place);
        }
        if (sort === 'distance') {
          const leftDistance = left.place.distance ?? left.place.distance_meters;
          const rightDistance = right.place.distance ?? right.place.distance_meters;
          return distanceMeters(leftDistance) - distanceMeters(rightDistance);
        }
        return left.index - right.index;
      })
      .map(({ place }) => place);
  }

  function getTransportMinutes(transport) {
    return { public: 20, car: 15, rental: 10 }[transport] || 20;
  }

  function parseEventDate(value) {
    if (typeof value !== 'string') return null;
    const normalized = value.replaceAll('-', '');
    if (!/^\d{8}$/.test(normalized)) return null;
    const dateValue = `${normalized.slice(0, 4)}-${normalized.slice(4, 6)}-${normalized.slice(6)}`;
    if (!isValidCalendarDate(dateValue)) return null;
    const date = new Date(`${dateValue}T00:00:00`);
    return Number.isNaN(date.getTime()) ? null : date;
  }

  function formatEventDateRange(startValue, endValue) {
    const startDate = parseEventDate(startValue);
    const endDate = parseEventDate(endValue);
    if (!startDate || !endDate || startDate > endDate) return '';
    const formatDate = (date, includeYear) => `${includeYear ? `${date.getFullYear()}년 ` : ''}${date.getMonth() + 1}월 ${date.getDate()}일`;
    const includeYear = startDate.getFullYear() !== endDate.getFullYear();
    const startLabel = formatDate(startDate, includeYear);
    const endLabel = formatDate(endDate, includeYear);
    return startLabel === endLabel ? startLabel : `${startLabel} ~ ${endLabel}`;
  }

  function getEventResponseState(payload) {
    if (!payload || payload.source !== 'tour_api' || !Array.isArray(payload.items)) return 'error';
    return payload.items.length > 0 ? 'live' : 'empty';
  }

  function getEventResponseAction(payload) {
    const state = getEventResponseState(payload);
    return { state, items: state === 'live' ? payload.items : [] };
  }

  function applyEventResponse(payload, renderers) {
    const action = getEventResponseAction(payload);
    if (action.state === 'live') renderers.live(action.items);
    if (action.state === 'empty') renderers.empty();
    if (action.state === 'error') renderers.error();
    return action;
  }

  function isCurrentEventRequest(requestId, latestRequestId) {
    return requestId === latestRequestId;
  }

  function getEventEmptyState(scope, dateLabel) {
    return `${scope} · ${dateLabel}에는 확인된 행사가 없어요.`;
  }

  return {
    parseTimeMinutes,
    getDailyTimeRange,
    getTimeRangeLabel,
    buildTimeSlots,
    upsertPlan,
    removePlan,
    createShareHash,
    parseShareHash,
    createNaverSearchUrl,
    createEventSearchUrl,
    getDayDateLabel,
    getDataSourceLabel,
    getIntegrationStatus,
    getNearbySearchContext,
    isValidLocation,
    sortNearbyItems,
    getTransportMinutes,
    formatEventDateRange,
    getEventResponseState,
    getEventResponseAction,
    applyEventResponse,
    isCurrentEventRequest,
    getEventEmptyState,
  };
});
