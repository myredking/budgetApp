"""Small API proxy and static server for the trip planner web app."""

from __future__ import annotations

import json
import ipaddress
import mimetypes
import os
import re
import time
from dataclasses import dataclass
from datetime import date, datetime
from html import unescape
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from typing import Any, Callable, Generic, TypeVar
from urllib.parse import parse_qs, parse_qsl, unquote, urlparse

import requests
from dotenv import load_dotenv


WEB_ROOT = Path(__file__).resolve().parent
NAVER_LOCAL_URL = "https://naverapihub.apigw.ntruss.com/search/v1/local"
NAVER_REVERSE_GEOCODE_URL = "https://naveropenapi.apigw.ntruss.com/map-reversegeocode/v2/gc"
TOUR_FESTIVAL_URL = "https://apis.data.go.kr/B551011/KorService2/searchFestival2"
TOUR_AREA_CODES = {
    "seoul": "1", "incheon": "2", "daejeon": "3", "daegu": "4", "gwangju": "5",
    "busan": "6", "ulsan": "7", "sejong": "8", "gyeonggi": "31", "gangwon": "32",
    "chungbuk": "33", "chungnam": "34", "gyeongbuk": "35", "gyeongnam": "36",
    "jeonbuk": "37", "jeonnam": "38", "jeju": "39",
    "suwon": "31", "seongnam": "31", "goyang": "31", "yongin": "31", "hwaseong": "31",
    "pyeongtaek": "31", "anyang": "31", "bucheon": "31", "namyangju": "31", "paju": "31",
    "chuncheon": "32", "wonju": "32", "gangneung": "32", "sokcho": "32", "donghae": "32", "samcheok": "32",
    "cheongju": "33", "chungju": "33", "jecheon": "33", "cheonan": "34", "asan": "34", "gongju": "34",
    "boryeong": "34", "seosan": "34", "dangjin": "34", "jeonju": "37", "gunsan": "37", "iksan": "37",
    "namwon": "37", "mokpo": "38", "yeosu": "38", "suncheon": "38", "naju": "38", "gwangyang": "38",
    "pohang": "35", "gyeongju": "35", "gumi": "35", "andong": "35", "gimcheon": "35", "yeongju": "35",
    "changwon": "36", "jinju": "36", "tongyeong": "36", "gimhae": "36", "geoje": "36", "yangsan": "36",
    "seoul-gangnam": "1", "seoul-seongsu": "1", "seoul-hongdae": "1", "seoul-jongno": "1", "seoul-jamsil": "1",
    "busan-haeundae": "6", "busan-gwangan": "6", "busan-seomyeon": "6", "busan-nampo": "6",
    "daegu-suseong": "4", "daegu-dongseongno": "4", "daegu-daemyeong": "4",
    "incheon-songdo": "2", "incheon-guwol": "2", "incheon-gaehangro": "2",
    "daejeon-dunsan": "3", "daejeon-soje": "3", "daejeon-yuseong": "3",
    "jeju-aewol": "39", "jeju-jungmun": "39", "jeju-seogwipo": "39",
}
load_dotenv()

CacheValue = TypeVar("CacheValue")


class DailyApiBudget:
    """Limit outbound API attempts to a conservative daily free-tier budget."""

    def __init__(self, limits: dict[str, int]) -> None:
        self._limits = {service: max(limit, 0) for service, limit in limits.items()}
        self._counts: dict[str, tuple[str, int]] = {}

    def try_consume(self, service: str) -> bool:
        """Consume one request slot and report whether the slot was available."""
        today = date.today().isoformat()
        count_day, count = self._counts.get(service, (today, 0))
        if count_day != today:
            count = 0
        limit = self._limits.get(service, 0)
        if count >= limit:
            self._counts[service] = (today, count)
            return False
        self._counts[service] = (today, count + 1)
        return True


class ApiResponseCache(Generic[CacheValue]):
    """Keep repeated searches local so they do not consume API quota."""

    def __init__(
        self,
        ttl_seconds: int,
        clock: Callable[[], float] = time.monotonic,
    ) -> None:
        self._ttl_seconds = max(ttl_seconds, 1)
        self._clock = clock
        self._entries: dict[str, tuple[float, CacheValue]] = {}

    def get(self, key: str) -> CacheValue | None:
        """Return a non-expired cached response, if present."""
        entry = self._entries.get(key)
        if entry is None:
            return None
        expires_at, value = entry
        if expires_at <= self._clock():
            self._entries.pop(key, None)
            return None
        return value

    def put(self, key: str, value: CacheValue) -> None:
        """Store a response until the configured TTL elapses."""
        self._entries[key] = (self._clock() + self._ttl_seconds, value)


def configured_limit(name: str, fallback: int) -> int:
    """Read a positive daily request limit from the environment."""
    try:
        return max(int(os.getenv(name, str(fallback))), 0)
    except ValueError:
        return fallback


def configured_host() -> str:
    """Return the interface used for local and LAN browser access."""
    return os.getenv("WEB_HOST", "0.0.0.0").strip() or "0.0.0.0"


API_BUDGET = DailyApiBudget(
    {
        "naver": configured_limit("NAVER_DAILY_LIMIT", 500),
        "tour": configured_limit("TOUR_DAILY_LIMIT", 100),
        "maps": configured_limit("MAP_DAILY_LIMIT", 100),
        "partner": configured_limit("PARTNER_DAILY_LIMIT", 30),
    }
)
API_CACHE: ApiResponseCache[Any] = ApiResponseCache(
    configured_limit("API_CACHE_TTL_SECONDS", 600)
)


@dataclass(frozen=True)
class ApiConfig:
    """Credentials used by the server-side API proxy."""

    naver_client_id: str
    naver_client_secret: str
    tour_service_key: str
    map_api_key_id: str
    map_api_key: str
    partner_feed_url: str = ""
    myrealtrip_api_url: str = ""
    myrealtrip_api_key: str = ""
    klook_api_url: str = ""
    klook_api_key: str = ""
    partner_feed_allowed_hosts: str = ""

    @classmethod
    def from_env(cls) -> "ApiConfig":
        """Build configuration from environment variables."""
        load_dotenv()
        return cls(
            naver_client_id=os.getenv("NAVER_CLIENT_ID", ""),
            naver_client_secret=os.getenv("NAVER_CLIENT_SECRET", ""),
            tour_service_key=os.getenv("TOUR_API_SERVICE_KEY", ""),
            map_api_key_id=os.getenv("NAVER_MAP_API_KEY_ID", ""),
            map_api_key=os.getenv("NAVER_MAP_API_KEY", ""),
            partner_feed_url=os.getenv("TRAVEL_PARTNER_FEED_URL", ""),
            partner_feed_allowed_hosts=os.getenv("PARTNER_FEED_ALLOWED_HOSTS", ""),
            myrealtrip_api_url=os.getenv("MYREALTRIP_API_URL", ""),
            myrealtrip_api_key=os.getenv("MYREALTRIP_API_KEY", ""),
            klook_api_url=os.getenv("KLOOK_API_URL", ""),
            klook_api_key=os.getenv("KLOOK_API_KEY", ""),
        )

    @property
    def has_naver_credentials(self) -> bool:
        """Return whether Naver API credentials are configured."""
        return bool(self.naver_client_id and self.naver_client_secret)

    @property
    def has_tour_credentials(self) -> bool:
        """Return whether the tourism API credential is configured."""
        return bool(self.tour_service_key)

    @property
    def has_map_credentials(self) -> bool:
        """Return whether Naver Maps credentials are configured."""
        return bool(self.map_api_key_id and self.map_api_key)


def public_config(config: ApiConfig) -> dict[str, str]:
    """Return browser-safe configuration without exposing secret keys."""
    return {"map_client_id": config.map_api_key_id}


def tour_area_code(destination: str) -> str:
    """Return the Korea Tourism area code for a destination slug."""
    return TOUR_AREA_CODES.get(destination, "")


def strip_markup(value: str) -> str:
    """Remove HTML markup and decode entities from an API string."""
    plain_text = re.sub(r"<[^>]*>", "", value)
    return unescape(plain_text).strip()


def optional_text(value: Any) -> str:
    """Convert optional provider fields without exposing Python null text."""
    return "" if value is None else str(value)


def first_optional_text(*values: Any) -> str:
    """Return the first populated value from provider field aliases."""
    for value in values:
        text = optional_text(value)
        if text:
            return text
    return ""


def normalize_naver_item(item: dict[str, Any]) -> dict[str, str]:
    """Convert a Naver local-search item into the app's place shape."""
    return {
        "title": strip_markup(str(item.get("title", ""))),
        "category": strip_markup(str(item.get("category", ""))),
        "description": strip_markup(str(item.get("description", ""))),
        "address": str(item.get("address", "")),
        "road_address": str(item.get("roadAddress", "")),
        "mapx": str(item.get("mapx", "")),
        "mapy": str(item.get("mapy", "")),
        "link": str(item.get("link", "")),
        "rating": optional_text(item.get("rating", "")),
        "reviews": first_optional_text(item.get("reviewCount"), item.get("reviews")),
        "opening_hours": first_optional_text(item.get("openingHours"), item.get("opening_hours")),
        "parking": first_optional_text(item.get("parking"), item.get("parkingInfo")),
        "image": first_optional_text(item.get("image"), item.get("imageUrl"), item.get("image_url")),
        "source": "naver",
    }


def normalize_tour_event(item: dict[str, Any]) -> dict[str, str]:
    """Convert a TourAPI festival item into the app's event shape."""
    return {
        "title": str(item.get("title", "")),
        "address": str(item.get("addr1", "")),
        "start_date": str(item.get("eventstartdate", "")),
        "end_date": str(item.get("eventenddate", "")),
        "place": str(item.get("eventplace", "")),
        "image": str(item.get("firstimage", "")),
        "content_id": str(item.get("contentid", "")),
        "source": "tour_api",
    }


SENSITIVE_URL_KEYS = {"api_key", "apikey", "token", "secret", "access_token", "client_secret", "authorization"}


def has_sensitive_query(parsed: Any) -> bool:
    """Detect credentials embedded in a URL query string."""
    query_keys = {key.lower() for key, _ in parse_qsl(parsed.query, keep_blank_values=True)}
    return bool(query_keys.intersection(SENSITIVE_URL_KEYS))


def is_public_hostname(hostname: str) -> bool:
    """Reject local names and IP literals before making an outbound request."""
    normalized = hostname.lower()
    if not normalized or normalized in {"localhost", "localhost.localdomain"} or normalized.endswith(".local"):
        return False
    try:
        ipaddress.ip_address(normalized)
    except ValueError:
        return True
    return False


def safe_https_url(value: Any) -> str:
    """Keep only HTTPS links supplied by a configured partner feed."""
    candidate = optional_text(value).strip()
    try:
        parsed = urlparse(candidate)
        hostname = parsed.hostname or ""
        has_credentials = bool(parsed.username or parsed.password)
    except ValueError:
        return ""
    if parsed.scheme != "https" or not parsed.netloc or has_credentials:
        return ""
    if has_credentials or has_sensitive_query(parsed) or not is_public_hostname(hostname):
        return ""
    return candidate


def host_matches(hostname: str, hosts: set[str] | tuple[str, ...]) -> bool:
    """Match an exact host or a subdomain against an allowlist."""
    return any(hostname == host or hostname.endswith(f".{host}") for host in hosts)


def configured_partner_hosts(value: str) -> set[str]:
    """Parse the explicit host allowlist used by a legacy partner feed."""
    return {host.strip().lower().lstrip(".") for host in value.split(",") if host.strip()}


def safe_provider_url(
    value: Any,
    provider: str,
    allowed_hosts: set[str] | None = None,
) -> str:
    """Allow provider API calls only to the provider's official domain."""
    candidate = safe_https_url(value)
    if not candidate:
        return ""
    hostname = (urlparse(candidate).hostname or "").lower()
    suffixes = {
        "마이리얼트립": ("myrealtrip.com",),
        "Klook": ("klook.com",),
    }.get(provider, ())
    if provider == "공식 제휴사" and allowed_hosts is not None and not host_matches(hostname, allowed_hosts):
        return ""
    if suffixes and not host_matches(hostname, suffixes):
        return ""
    return candidate


def safe_partner_link(
    value: Any,
    provider: str,
    allowed_hosts: set[str] | None = None,
) -> str:
    """Keep provider links on the provider domain when the source is known."""
    if provider in {"마이리얼트립", "Klook", "공식 제휴사"}:
        return safe_provider_url(value, provider, allowed_hosts)
    return safe_https_url(value)


def normalize_partner_item(
    item: dict[str, Any],
    provider_hint: str = "",
    allowed_hosts: set[str] | None = None,
) -> dict[str, Any]:
    """Convert a partner product into the safe comparison-card shape."""
    raw_price = first_optional_text(
        item.get("price"), item.get("sale_price"), item.get("salePrice"),
        item.get("total_price"), item.get("totalPrice"), item.get("amount"),
    ).replace(",", "")
    try:
        price = max(float(raw_price or 0), 0)
    except (TypeError, ValueError):
        price = 0.0
    return {
        "provider": first_optional_text(item.get("provider"), item.get("merchant"), provider_hint, "공식 제휴사"),
        "title": first_optional_text(item.get("title"), item.get("name"), item.get("product_name"), item.get("productName"), item.get("displayName")),
        "category": first_optional_text(item.get("category"), item.get("type"), "국내 여행 상품"),
        "price": price,
        "currency": first_optional_text(item.get("currency"), "KRW"),
        "url": safe_partner_link(first_optional_text(item.get("affiliate_url"), item.get("affiliateUrl"), item.get("deeplink"), item.get("booking_url"), item.get("url"), item.get("link")), provider_hint, allowed_hosts),
        "image": safe_partner_link(first_optional_text(item.get("image"), item.get("image_url"), item.get("imageUrl")), provider_hint, allowed_hosts),
        "recommended": bool(item.get("recommended", False)),
        "source": "partner_feed",
    }


def normalize_myrealtrip_item(item: dict[str, Any]) -> dict[str, Any]:
    """Normalize a MyRealTrip marketing-partner response item."""
    return normalize_partner_item(item, "마이리얼트립")


def normalize_klook_item(item: dict[str, Any]) -> dict[str, Any]:
    """Normalize a Klook affiliate response item."""
    return normalize_partner_item(item, "Klook")


def partner_response_items(payload: Any) -> list[dict[str, Any]]:
    """Read product arrays from common official-feed response envelopes."""
    if isinstance(payload, list):
        return [item for item in payload if isinstance(item, dict)]
    if not isinstance(payload, dict):
        return []
    for key in ("items", "products", "results", "deals"):
        items = payload.get(key)
        if isinstance(items, list):
            return [item for item in items if isinstance(item, dict)]
    return []


def normalized_partner_items(payload: Any) -> list[dict[str, Any]]:
    """Normalize, validate, and rank official partner products."""
    items = [normalize_partner_item(item) for item in partner_response_items(payload)]
    valid_items = [item for item in items if item["title"] and item["price"] > 0]
    return sorted(valid_items, key=lambda item: (not item["recommended"], item["price"]))


def normalized_provider_items(
    payload: Any,
    provider_hint: str,
    allowed_hosts: set[str] | None = None,
) -> list[dict[str, Any]]:
    """Normalize products while applying the configured provider label."""
    normalizer = normalize_partner_item
    if provider_hint == "마이리얼트립":
        normalizer = normalize_myrealtrip_item
    elif provider_hint == "Klook":
        normalizer = normalize_klook_item
    if provider_hint in {"마이리얼트립", "Klook"}:
        items = [normalizer(item) for item in partner_response_items(payload)]
    else:
        items = [
            normalize_partner_item(item, provider_hint, allowed_hosts)
            for item in partner_response_items(payload)
        ]
    valid_items = [item for item in items if item["title"] and item["price"] > 0]
    return sorted(valid_items, key=lambda item: (not item["recommended"], item["price"]))


def partner_headers(provider: str, api_key: str) -> dict[str, str]:
    """Build common authorization headers without exposing keys to clients."""
    headers = {"Accept": "application/json"}
    if api_key and provider == "마이리얼트립":
        headers["Authorization"] = f"Bearer {api_key}"
    if api_key and provider == "Klook":
        headers["X-API-Key"] = api_key
    return headers


def fetch_partner_endpoint(
    feed_url: str,
    start_date: str,
    end_date: str,
    destination: str,
    people: str,
    theme: str,
    provider_hint: str = "",
    api_key: str = "",
    allowed_hosts: set[str] | None = None,
    cache: ApiResponseCache[tuple[list[dict[str, Any]], str]] | None = None,
    budget: DailyApiBudget | None = None,
) -> tuple[list[dict[str, Any]], str]:
    """Fetch and normalize one configured official travel product feed."""
    safe_url = safe_provider_url(feed_url, provider_hint, allowed_hosts)
    if not safe_url:
        return [], "not_configured"
    active_cache = cache or API_CACHE
    active_budget = budget or API_BUDGET
    cache_key = f"partner:{provider_hint}:{safe_url}:{start_date}:{end_date}:{destination}:{people}:{theme}"
    cached = active_cache.get(cache_key)
    if cached is not None:
        return cached
    if not active_budget.try_consume("partner"):
        return [], "quota"
    response = requests.get(
        safe_url,
        params={"start_date": start_date, "end_date": end_date, "destination": destination, "people": people, "theme": theme},
        headers=partner_headers(provider_hint, api_key),
        timeout=8,
    )
    response.raise_for_status()
    items = normalized_provider_items(response.json(), provider_hint, allowed_hosts)
    result = items, "partner_feed" if items else "partner_empty"
    active_cache.put(cache_key, result)
    return result


def fetch_partner_feed(
    config: ApiConfig,
    start_date: str,
    end_date: str,
    destination: str,
    people: str,
    theme: str,
    cache: ApiResponseCache[tuple[list[dict[str, Any]], str]] | None = None,
    budget: DailyApiBudget | None = None,
) -> tuple[list[dict[str, Any]], str]:
    """Fetch the legacy single official travel product feed."""
    return fetch_partner_endpoint(
        config.partner_feed_url, start_date, end_date, destination, people, theme,
        provider_hint="공식 제휴사",
        allowed_hosts=configured_partner_hosts(config.partner_feed_allowed_hosts),
        cache=cache, budget=budget,
    )


def fetch_partner_feeds(
    config: ApiConfig,
    start_date: str,
    end_date: str,
    destination: str,
    people: str,
    theme: str,
    cache: ApiResponseCache[tuple[list[dict[str, Any]], str]] | None = None,
    budget: DailyApiBudget | None = None,
) -> tuple[list[dict[str, Any]], str, list[str], list[str]]:
    """Merge the configured MyRealTrip, Klook, and legacy partner feeds."""
    feeds = [
        ("마이리얼트립", config.myrealtrip_api_url, config.myrealtrip_api_key, None),
        ("Klook", config.klook_api_url, config.klook_api_key, None),
        ("공식 제휴사", config.partner_feed_url, "", configured_partner_hosts(config.partner_feed_allowed_hosts)),
    ]
    configured = [(label, url, key, hosts) for label, url, key, hosts in feeds if safe_provider_url(url, label, hosts)]
    if not configured:
        return [], "not_configured", [], []
    items: list[dict[str, Any]] = []
    providers: list[str] = []
    failed_providers: list[str] = []
    failed = False
    for label, url, key, hosts in configured:
        try:
            result, source = fetch_partner_endpoint(url, start_date, end_date, destination, people, theme, label, key, hosts, cache, budget)
        except requests.RequestException:
            failed = True
            failed_providers.append(label)
            continue
        if source == "partner_feed":
            providers.append(label)
        elif source == "quota":
            failed = True
            failed_providers.append(label)
        items.extend(result)
    items.sort(key=lambda item: (not item["recommended"], item["price"]))
    if items:
        return items, "partner_feed", providers, failed_providers
    return [], "partner_error" if failed else "partner_empty", providers, failed_providers


def extract_reverse_address(payload: dict[str, Any]) -> str:
    """Extract a readable city and district from reverse-geocoding JSON."""
    results = payload.get("results", [])
    if not isinstance(results, list) or not results:
        return ""
    region = results[0].get("region", {})
    names = [
        region.get(area, {}).get("name", "")
        for area in ("area1", "area2", "area3")
    ]
    return " ".join(name for name in names if name)


def reverse_geocode(
    config: ApiConfig,
    latitude: float,
    longitude: float,
    cache: ApiResponseCache[tuple[str, str]] | None = None,
    budget: DailyApiBudget | None = None,
) -> tuple[str, str]:
    """Convert browser coordinates to an address with Naver Maps."""
    if not config.has_map_credentials:
        return "", "demo"
    active_cache = cache or API_CACHE
    active_budget = budget or API_BUDGET
    cache_key = f"reverse:{latitude:.5f}:{longitude:.5f}"
    cached = active_cache.get(cache_key)
    if cached is not None:
        return cached
    if not active_budget.try_consume("maps"):
        return "", "quota"
    response = requests.get(
        NAVER_REVERSE_GEOCODE_URL,
        headers={
            "x-ncp-apigw-api-key-id": config.map_api_key_id,
            "x-ncp-apigw-api-key": config.map_api_key,
        },
        params={
            "request": "coordsToaddr",
            "coords": f"{longitude},{latitude}",
            "sourcecrs": "epsg:4326",
            "orders": "admcode,legalcode,addr,roadaddr",
            "output": "json",
        },
        timeout=8,
    )
    response.raise_for_status()
    result = extract_reverse_address(response.json()), "naver_maps"
    active_cache.put(cache_key, result)
    return result


def naver_response_items(payload: Any) -> list[dict[str, Any]]:
    """Read dictionary items from a Naver search response."""
    if not isinstance(payload, dict):
        return []
    items = payload.get("items", [])
    if not isinstance(items, list):
        return []
    return [item for item in items if isinstance(item, dict)]


def fetch_naver_query(config: ApiConfig, search_query: str) -> list[dict[str, Any]]:
    """Fetch one local-search query from Naver and return raw items."""
    response = requests.get(
        NAVER_LOCAL_URL,
        headers={
            "X-NCP-APIGW-API-KEY-ID": config.naver_client_id,
            "X-NCP-APIGW-API-KEY": config.naver_client_secret,
        },
        params={"query": search_query, "display": 5, "sort": "comment", "format": "json"},
        timeout=8,
    )
    response.raise_for_status()
    return naver_response_items(response.json())


def fetch_naver_query_variants(
    config: ApiConfig,
    queries: list[str],
    budget: DailyApiBudget,
) -> tuple[list[dict[str, Any]], str]:
    """Try the primary query and a broad fallback within the daily budget."""
    for search_query in queries:
        if not budget.try_consume("naver"):
            return [], "quota"
        items = fetch_naver_query(config, search_query)
        if items:
            return items, "naver"
    return [], "naver"


def build_naver_query_variants(query: str, category: str) -> list[str]:
    """Build a precise query followed by a broader location fallback."""
    search_query = " ".join(part for part in (query, category) if part).strip()
    queries = [search_query]
    broad_query = query.strip()
    if category and broad_query and broad_query != search_query:
        queries.append(broad_query)
    return queries


def store_naver_result(
    items: list[dict[str, Any]],
    source: str,
    cache: ApiResponseCache[tuple[list[dict[str, str]], str]],
    cache_key: str,
) -> tuple[list[dict[str, str]], str]:
    """Normalize and cache successful Naver results without caching quota errors."""
    if source == "quota":
        return [], source
    result = [normalize_naver_item(item) for item in items], source
    cache.put(cache_key, result)
    return result


def fetch_naver_places(
    config: ApiConfig,
    query: str,
    category: str,
    cache: ApiResponseCache[tuple[list[dict[str, str]], str]] | None = None,
    budget: DailyApiBudget | None = None,
) -> tuple[list[dict[str, str]], str]:
    """Fetch nearby-style place results through Naver's local search API."""
    if not config.has_naver_credentials:
        return [], "demo"
    active_cache = cache or API_CACHE
    active_budget = budget or API_BUDGET
    search_query = " ".join(part for part in (query, category) if part).strip()
    cache_key = f"local:{search_query}"
    cached = active_cache.get(cache_key)
    if cached is not None:
        return cached
    search_queries = build_naver_query_variants(query, category)
    items, source = fetch_naver_query_variants(config, search_queries, active_budget)
    return store_naver_result(items, source, active_cache, cache_key)


def tour_response_items(payload: Any) -> list[dict[str, Any]]:
    """Read event dictionaries from a TourAPI response envelope."""
    if not isinstance(payload, dict):
        return []
    response_data = payload.get("response", {})
    body = response_data.get("body", {}) if isinstance(response_data, dict) else {}
    item_data = body.get("items", {}) if isinstance(body, dict) else {}
    items = item_data.get("item", []) if isinstance(item_data, dict) else []
    if isinstance(items, dict):
        return [items]
    if not isinstance(items, list):
        return []
    return [item for item in items if isinstance(item, dict)]


def fetch_tour_events(
    config: ApiConfig,
    start_date: str,
    end_date: str,
    area_code: str,
    cache: ApiResponseCache[tuple[list[dict[str, str]], str]] | None = None,
    budget: DailyApiBudget | None = None,
) -> tuple[list[dict[str, str]], str]:
    """Fetch festivals and events through Korea Tourism TourAPI."""
    if not config.has_tour_credentials:
        return [], "demo"
    active_cache = cache or API_CACHE
    active_budget = budget or API_BUDGET
    cache_key = f"events:{start_date}:{end_date}:{area_code}"
    cached = active_cache.get(cache_key)
    if cached is not None:
        return cached
    if not active_budget.try_consume("tour"):
        return [], "quota"
    params = {
        "serviceKey": config.tour_service_key,
        "MobileOS": "ETC",
        "MobileApp": "CourseOn",
        "_type": "json",
        "eventStartDate": start_date,
        "eventEndDate": end_date,
        "numOfRows": 20,
        "pageNo": 1,
    }
    if area_code:
        params["areaCode"] = area_code
    response = requests.get(TOUR_FESTIVAL_URL, params=params, timeout=8)
    response.raise_for_status()
    items = tour_response_items(response.json())
    result = [normalize_tour_event(item) for item in items], "tour_api"
    active_cache.put(cache_key, result)
    return result


def json_bytes(payload: dict[str, Any]) -> bytes:
    """Serialize an API payload as UTF-8 JSON bytes."""
    return json.dumps(payload, ensure_ascii=False).encode("utf-8")


def bounded_param(
    params: dict[str, list[str]],
    name: str,
    max_length: int = 100,
) -> str:
    """Read a trimmed query parameter with a safe length limit."""
    return params.get(name, [""])[0].strip()[:max_length]


def normalized_date(value: str, fallback: str) -> str:
    """Return a valid YYYYMMDD value or the supplied fallback."""
    cleaned = value.replace("-", "")
    try:
        datetime.strptime(cleaned, "%Y%m%d")
    except ValueError:
        return fallback
    return cleaned


def normalized_date_range(
    start_value: str,
    end_value: str,
    fallback: str,
) -> tuple[str, str]:
    """Return a valid, chronological event date range."""
    start_date = normalized_date(start_value, fallback)
    end_date = normalized_date(end_value, start_date)
    return start_date, max(start_date, end_date)


class TravelRequestHandler(BaseHTTPRequestHandler):
    """Serve planner assets and API proxy routes."""

    config = ApiConfig.from_env()

    def do_GET(self) -> None:
        """Handle a static asset or API GET request."""
        parsed_url = urlparse(self.path)
        params = parse_qs(parsed_url.query)
        if parsed_url.path == "/api/config":
            self._send_json(public_config(self.config))
            return
        if parsed_url.path == "/api/nearby":
            self._handle_nearby(params)
            return
        if parsed_url.path == "/api/events":
            self._handle_events(params)
            return
        if parsed_url.path == "/api/price-comparison":
            self._handle_price_comparison(params)
            return
        if parsed_url.path == "/api/location":
            self._handle_location(params)
            return
        self._serve_static(parsed_url.path)

    def _handle_nearby(self, params: dict[str, list[str]]) -> None:
        """Return Naver places or a safe demo-mode response."""
        query = bounded_param(params, "query")
        category = bounded_param(params, "category", 30)
        if not query:
            self._send_json({"source": "invalid", "items": [], "demo": True}, status=400)
            return
        try:
            items, source = fetch_naver_places(self.config, query, category)
        except requests.RequestException:
            self._send_json({"source": "fallback", "items": [], "demo": True}, status=502)
            return
        self._send_json(
            {"source": source, "items": items, "demo": source in {"demo", "quota"}}
        )

    def _handle_events(self, params: dict[str, list[str]]) -> None:
        """Return TourAPI events or a safe demo-mode response."""
        today = date.today().strftime("%Y%m%d")
        start_date, end_date = normalized_date_range(
            bounded_param(params, "start_date"),
            bounded_param(params, "end_date"),
            today,
        )
        destination = bounded_param(params, "destination", 30)
        area_code = bounded_param(params, "area_code", 4) or tour_area_code(destination)
        try:
            items, source = fetch_tour_events(self.config, start_date, end_date, area_code)
        except requests.RequestException:
            self._send_json({"source": "fallback", "items": [], "demo": True}, status=502)
            return
        self._send_json(
            {"source": source, "items": items, "demo": source in {"demo", "quota"}}
        )

    def _handle_price_comparison(self, params: dict[str, list[str]]) -> None:
        """Return products from the configured official partner feed only."""
        today = date.today().strftime("%Y%m%d")
        start_date, end_date = normalized_date_range(
            bounded_param(params, "start_date"), bounded_param(params, "end_date"), today
        )
        try:
            items, source, providers, failed_providers = fetch_partner_feeds(
                self.config,
                start_date,
                end_date,
                bounded_param(params, "destination", 40),
                bounded_param(params, "people", 3),
                bounded_param(params, "theme", 20),
            )
        except requests.RequestException:
            self._send_json({"source": "partner_error", "items": [], "providers": [], "failedProviders": [], "demo": True}, status=502)
            return
        self._send_json({"source": source, "items": items, "providers": providers, "failedProviders": failed_providers, "demo": source != "partner_feed"})

    def _handle_location(self, params: dict[str, list[str]]) -> None:
        """Reverse-geocode browser coordinates without exposing map keys."""
        try:
            latitude = float(params.get("lat", [""])[0])
            longitude = float(params.get("lon", [""])[0])
        except ValueError:
            self._send_json({"source": "invalid", "address": ""}, status=400)
            return
        if not -90 <= latitude <= 90 or not -180 <= longitude <= 180:
            self._send_json({"source": "invalid", "address": ""}, status=400)
            return
        try:
            address, source = reverse_geocode(self.config, latitude, longitude)
        except (requests.RequestException, ValueError, TypeError):
            address, source = "", "fallback"
        self._send_json({"source": source, "address": address, "demo": source != "naver_maps"})

    def _serve_static(self, requested_path: str) -> None:
        """Serve a file from the web directory without path traversal."""
        relative_path = unquote(requested_path.lstrip("/")) or "index.html"
        file_path = (WEB_ROOT / relative_path).resolve()
        if WEB_ROOT not in file_path.parents or not file_path.is_file():
            self.send_error(404)
            return
        content_type = mimetypes.guess_type(file_path.name)[0] or "application/octet-stream"
        content = file_path.read_bytes()
        self.send_response(200)
        self.send_header("Content-Type", f"{content_type}; charset=utf-8")
        self.send_header("Content-Length", str(len(content)))
        self.end_headers()
        self.wfile.write(content)

    def _send_json(self, payload: dict[str, Any], status: int = 200) -> None:
        """Write a JSON API response."""
        content = json_bytes(payload)
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(content)))
        self.end_headers()
        self.wfile.write(content)

    def log_message(self, format_string: str, *args: object) -> None:
        """Keep the default access-log format for local development."""
        super().log_message(format_string, *args)


def main() -> None:
    """Start the local planner web server."""
    port = int(os.getenv("WEB_PORT", "8765"))
    host = configured_host()
    server = ThreadingHTTPServer((host, port), TravelRequestHandler)
    print(f"Travel planner running at http://127.0.0.1:{port}")
    if host == "0.0.0.0":
        print(f"LAN access: http://<this-computer-ip>:{port}")
    server.serve_forever()


if __name__ == "__main__":
    main()
