from web.server import (
    ApiConfig,
    DailyApiBudget,
    ApiResponseCache,
    bounded_param,
    extract_reverse_address,
    fetch_naver_places,
    normalize_naver_item,
    normalize_tour_event,
    normalized_date,
    normalized_date_range,
    public_config,
    configured_host,
    tour_area_code,
)


def test_daily_api_budget_blocks_requests_after_the_configured_limit() -> None:
    budget = DailyApiBudget({"naver": 1})

    assert budget.try_consume("naver") is True
    assert budget.try_consume("naver") is False


def test_configured_host_allows_lan_access_and_rejects_blank_override(monkeypatch) -> None:
    monkeypatch.setenv("WEB_HOST", "0.0.0.0")
    assert configured_host() == "0.0.0.0"

    monkeypatch.setenv("WEB_HOST", "   ")
    assert configured_host() == "0.0.0.0"


def test_naver_places_cache_prevents_repeated_paid_api_calls(monkeypatch) -> None:
    calls = []

    class FakeResponse:
        def raise_for_status(self) -> None:
            return None

        def json(self) -> dict:
            return {"items": [{"title": "성수 카페"}]}

    def fake_get(*args, **kwargs):
        calls.append((args, kwargs))
        return FakeResponse()

    monkeypatch.setattr("web.server.requests.get", fake_get)
    config = ApiConfig("client-id", "client-secret", "", "", "")
    budget = DailyApiBudget({"naver": 1})
    cache = ApiResponseCache(ttl_seconds=600, clock=lambda: 100.0)

    first = fetch_naver_places(config, "성수", "카페", cache=cache, budget=budget)
    second = fetch_naver_places(config, "성수", "카페", cache=cache, budget=budget)

    assert first == second
    assert len(calls) == 1


def test_naver_local_search_uses_current_api_hub_contract(monkeypatch) -> None:
    calls = {}

    class FakeResponse:
        def raise_for_status(self) -> None:
            return None

        def json(self) -> dict:
            return {"items": [{"title": "성수 카페", "mapx": "1271234567", "mapy": "375123456"}]}

    def fake_get(url, headers, params, timeout):
        calls.update(url=url, headers=headers, params=params, timeout=timeout)
        return FakeResponse()

    monkeypatch.setattr("web.server.requests.get", fake_get)
    config = ApiConfig("client-id", "client-secret", "", "", "")

    items, source = fetch_naver_places(config, "성수", "카페")

    assert source == "naver"
    assert items[0]["title"] == "성수 카페"
    assert calls["url"] == "https://naverapihub.apigw.ntruss.com/search/v1/local"
    assert calls["headers"] == {
        "X-NCP-APIGW-API-KEY-ID": "client-id",
        "X-NCP-APIGW-API-KEY": "client-secret",
    }
    assert calls["params"] == {"query": "성수 카페", "display": 5, "sort": "comment", "format": "json"}


def test_naver_local_search_retries_a_broad_location_without_category(monkeypatch) -> None:
    queries = []

    class FakeResponse:
        def __init__(self, items):
            self.items = items

        def raise_for_status(self) -> None:
            return None

        def json(self) -> dict:
            return {"items": self.items}

    def fake_get(url, headers, params, timeout):
        queries.append(params["query"])
        items = [] if params["query"] == "성수동 추천 장소" else [{"title": "성수동 장소"}]
        return FakeResponse(items)

    monkeypatch.setattr("web.server.requests.get", fake_get)
    config = ApiConfig("client-id", "client-secret", "", "", "")
    budget = DailyApiBudget({"naver": 2})
    cache = ApiResponseCache(ttl_seconds=600, clock=lambda: 100.0)

    items, source = fetch_naver_places(config, "성수동", "추천 장소", cache=cache, budget=budget)

    assert source == "naver"
    assert items[0]["title"] == "성수동 장소"
    assert queries == ["성수동 추천 장소", "성수동"]


def test_normalize_naver_item_strips_markup_and_maps_place_fields() -> None:
    result = normalize_naver_item(
        {
            "title": "<b>성수 카페</b>",
            "category": "카페&gt;커피전문점",
            "description": "<b>조용한</b> 공간",
            "address": "서울 성동구",
            "roadAddress": "서울 성동구 연무장길 1",
            "mapx": "1271234567",
            "mapy": "375123456",
            "link": "https://example.com/place",
            "imageUrl": "https://images.example.com/place.jpg",
            "rating": "4.6",
            "reviewCount": "120",
            "openingHours": "10:00~22:00",
            "parking": "주차 가능",
        }
    )

    assert result == {
        "title": "성수 카페",
        "category": "카페>커피전문점",
        "description": "조용한 공간",
        "address": "서울 성동구",
        "road_address": "서울 성동구 연무장길 1",
        "mapx": "1271234567",
        "mapy": "375123456",
        "link": "https://example.com/place",
        "image": "https://images.example.com/place.jpg",
        "rating": "4.6",
        "reviews": "120",
        "opening_hours": "10:00~22:00",
        "parking": "주차 가능",
        "source": "naver",
    }


def test_normalize_naver_item_omits_null_optional_details() -> None:
    result = normalize_naver_item(
        {
            "title": "장소",
            "rating": None,
            "reviewCount": None,
            "reviews": "120",
            "openingHours": None,
            "opening_hours": "10:00~22:00",
            "parking": None,
            "parkingInfo": "주차 가능",
        }
    )

    assert result["rating"] == ""
    assert result["reviews"] == "120"
    assert result["opening_hours"] == "10:00~22:00"
    assert result["parking"] == "주차 가능"


def test_normalize_tour_event_maps_event_schedule_fields() -> None:
    result = normalize_tour_event(
        {
            "title": "가을 문화축제",
            "addr1": "부산 해운대구",
            "eventstartdate": "20261002",
            "eventenddate": "20261004",
            "eventplace": "해운대 광장",
            "firstimage": "https://example.com/event.jpg",
            "contentid": "12345",
        }
    )

    assert result == {
        "title": "가을 문화축제",
        "address": "부산 해운대구",
        "start_date": "20261002",
        "end_date": "20261004",
        "place": "해운대 광장",
        "image": "https://example.com/event.jpg",
        "content_id": "12345",
        "source": "tour_api",
    }


def test_api_config_reports_demo_mode_without_credentials() -> None:
    config = ApiConfig(
        naver_client_id="",
        naver_client_secret="",
        tour_service_key="",
        map_api_key_id="",
        map_api_key="",
    )

    assert config.has_naver_credentials is False
    assert config.has_tour_credentials is False
    assert config.has_map_credentials is False


def test_public_config_exposes_only_the_map_client_id() -> None:
    config = ApiConfig("naver-id", "secret", "tour-key", "map-id", "map-secret")

    assert public_config(config) == {"map_client_id": "map-id"}


def test_tour_area_code_maps_city_selection_to_tourism_region() -> None:
    assert tour_area_code("busan") == "6"
    assert tour_area_code("suwon") == "31"
    assert tour_area_code("seoul-gangnam") == "1"
    assert tour_area_code("busan-haeundae") == "6"
    assert tour_area_code("nationwide") == ""


def test_extract_reverse_address_reads_city_and_district() -> None:
    result = extract_reverse_address(
        {
            "results": [
                {
                    "region": {
                        "area1": {"name": "경기도"},
                        "area2": {"name": "수원시"},
                        "area3": {"name": "팔달구"},
                    }
                }
            ]
        }
    )

    assert result == "경기도 수원시 팔달구"


def test_bounded_param_trims_and_limits_input() -> None:
    result = bounded_param({"query": ["  서울 카페 " + "x" * 120]}, "query", 12)

    assert result == "서울 카페 xxxxxx"


def test_normalized_date_falls_back_for_invalid_input() -> None:
    assert normalized_date("2026-10-02", "20260101") == "20261002"
    assert normalized_date("not-a-date", "20260101") == "20260101"


def test_normalized_date_range_clamps_end_before_start() -> None:
    assert normalized_date_range("2026-10-04", "2026-10-02", "20260101") == ("20261004", "20261004")
