from pathlib import Path
import json
import subprocess


WEB_ROOT = Path(__file__).parents[1] / "web"


def read_asset(name: str) -> str:
    return (WEB_ROOT / name).read_text(encoding="utf-8")


def test_plan_archive_and_edit_controls_are_present() -> None:
    html = read_asset("index.html")

    assert 'id="save-plan-button"' in html
    assert 'id="history-button"' in html
    assert 'id="refresh-plan-button"' in html
    assert 'id="saved-plans-panel"' in html
    assert 'id="alternate-picker"' in html
    assert 'id="no-time-limit"' in html
    assert 'id="share-plan-button"' in html


def test_route_place_review_controls_are_present() -> None:
    html = read_asset("index.html")

    for element_id in (
        "place-review-panel",
        "place-review-title",
        "place-review-list",
        "place-review-form",
        "place-review-rating",
        "place-review-text",
    ):
        assert f'id="{element_id}"' in html


def test_community_controls_are_present() -> None:
    html = read_asset("index.html")

    for element_id in (
        "community-section",
        "community-search",
        "community-region-filter",
        "community-theme-filter",
        "community-date-filter",
        "community-post-list",
        "open-community-form-button",
        "community-form-panel",
        "community-post-form",
        "community-post-title",
        "community-post-submit",
        "share-plan-community-button",
        "community-chat-panel",
        "community-chat-form",
        "community-chat-input",
    ):
        assert f'id="{element_id}"' in html


def test_price_comparison_controls_are_present() -> None:
    html = read_asset("index.html")
    javascript = read_asset("app.js")

    for element_id in (
        "price-comparison-section",
        "price-comparison-list",
        "price-comparison-status",
        "price-comparison-refresh",
    ):
        assert f'id="{element_id}"' in html
    assert "loadPriceComparisons" in javascript
    assert "/api/price-comparison" in javascript


def test_quick_navigation_tabs_link_to_each_major_feature() -> None:
    html = read_asset("index.html")

    assert 'id="quick-navigation"' in html
    for target in (
        "planner-grid",
        "result-section",
        "community-section",
        "discovery-section",
        "events-section",
        "saved-plans-panel",
    ):
        assert f'data-quick-nav="{target}"' in html


def test_live_discovery_controls_are_present() -> None:
    html = read_asset("index.html")

    assert 'id="nearby-source-status"' in html
    assert 'id="event-source-status"' in html
    assert 'id="nearby-location-button"' in html
    assert 'id="nearby-map-button"' in html
    assert 'id="nearby-map-note"' in html
    assert 'id="nearby-sort"' in html
    assert 'id="nearby-sort-hint"' in html


def test_location_query_controls_are_present() -> None:
    html = read_asset("index.html")

    assert 'id="location-query"' in html
    assert 'placeholder="예: 강남구, 성수동, 홍대입구역, 경복궁"' in html
    assert 'label="서울 구·동·상권"' in html


def test_site_metadata_and_asset_versions_are_present() -> None:
    html = read_asset("index.html")

    assert 'rel="manifest"' in html
    assert 'name="theme-color"' in html
    assert 'name="mobile-web-app-capable"' in html
    assert 'name="apple-mobile-web-app-capable"' in html
    assert 'styles.css?v=30' in html
    assert 'plan_logic.js?v=30' in html
    assert 'app.js?v=30' in html
    assert "navigator.serviceWorker" in read_asset("app.js")


def test_premium_travel_visuals_are_present() -> None:
    html = read_asset("index.html")
    styles = read_asset("styles.css")

    assert 'srcset="hero-travel.webp"' in html
    assert 'src="hero-travel.png"' in html
    assert (WEB_ROOT / "hero-travel.png").exists()
    assert (WEB_ROOT / "hero-travel.webp").exists()
    assert ".intro-visual" in styles
    assert "@keyframes courseon-rise" in styles
    assert "prefers-reduced-motion: reduce" in styles


def assert_photo_asset_is_synced(asset: str, javascript: str, service_worker: str) -> None:
    assert asset in javascript
    assert asset in service_worker
    assert (WEB_ROOT / asset).exists()
    assert (WEB_ROOT.parent / "travel-site" / "public" / asset).exists()


def test_travel_cards_have_related_photo_assets_and_fallbacks() -> None:
    javascript = read_asset("app.js")
    service_worker = read_asset("sw.js")

    assert_photo_asset_is_synced("travel-food.webp", javascript, service_worker)
    assert_photo_asset_is_synced("travel-culture.webp", javascript, service_worker)
    assert_photo_asset_is_synced("travel-nature.webp", javascript, service_worker)
    assert "createPhotoElement" in javascript
    assert "getPlaceImage" in javascript
    assert "getEventImage" in javascript
    assert "getCommunityImage" in javascript
    assert "appendPhotoIfAvailable" in javascript
    assert "if (!photo?.src) return null" in javascript


def test_travel_photos_use_square_cards() -> None:
    styles = read_asset("styles.css")

    assert "aspect-ratio: 1 / 1" in styles
    assert ".recommendation-photo" in styles
    assert ".community-post-photo" in styles


def test_plan_storage_and_edit_behaviors_are_wired() -> None:
    javascript = read_asset("app.js")

    assert "localStorage" in javascript
    assert "savePlan" in javascript
    assert "loadSavedPlan" in javascript
    assert "refreshPlan" in javascript
    assert "replacePlace" in javascript
    assert "data-place-change-index" in javascript
    assert "loadSharedPlan" in javascript
    assert "sharePlan" in javascript


def test_route_map_and_review_behaviors_are_wired() -> None:
    javascript = read_asset("app.js")

    assert "selectItineraryPlace" in javascript
    assert "placeReviews" in javascript


def test_community_behaviors_are_wired() -> None:
    javascript = read_asset("app.js")

    assert "COMMUNITY_STORAGE_KEY" in javascript
    assert "renderCommunityPosts" in javascript
    assert "toggleCommunityJoin" in javascript
    assert "shareCurrentPlanToCommunity" in javascript
    assert "COMMUNITY_CHAT_STORAGE_KEY" in javascript
    assert "renderCommunityChat" in javascript
    assert "sendCommunityChat" in javascript
    assert "communityDateFilter" in javascript


def test_quick_navigation_updates_active_section_and_opens_saved_plans() -> None:
    javascript = read_asset("app.js")

    assert "quickNavigationLinks" in javascript
    assert "setupQuickNavigation" in javascript
    assert "showSavedPlansPanel" in javascript
    assert "IntersectionObserver" in javascript


def test_plan_integration_behaviors_are_wired() -> None:
    javascript = read_asset("app.js")

    assert "getDataSourceLabel" in javascript
    assert "place-map-link" in javascript
    assert "nearby-location-button" in javascript
    assert "currentLocation" in javascript
    assert "map-user-point" in javascript
    assert "sortNearbyItems" in javascript
    assert "getTransportMinutes" in javascript
    assert 'function applyPlanSnapshot' in javascript
    assert 'function applyPlanSnapshot(plan, message) {\n  const formData = plan.form;\n  currentLocation = null;' in javascript


def test_location_query_search_behaviors_are_wired() -> None:
    javascript = read_asset("app.js")

    assert "locationQueryInput" in javascript
    assert "getSelectedLocationLabel" in javascript
    assert "locationQuery" in javascript
    assert "getNearbySearchContext(" in javascript


def test_time_limit_behavior_is_wired() -> None:
    logic = read_asset("plan_logic.js")

    assert "시간 제한 없음" in logic


def test_web_plan_runtime_behavior() -> None:
    test_files = [
        Path(__file__).with_name("web_plan_features.test.js"),
        Path(__file__).with_name("web_app_smoke.test.js"),
    ]
    result = subprocess.run(
        ["node", "--test", *(str(test_file) for test_file in test_files)],
        capture_output=True,
        text=True,
        encoding="utf-8",
        check=False,
    )

    assert result.returncode == 0, f"{result.stdout}\n{result.stderr}"


def test_web_manifest_describes_installable_travel_service() -> None:
    manifest = json.loads(read_asset("manifest.webmanifest"))

    assert manifest["name"] == "코스온 | 국내 여행 코스 플래너"
    assert manifest["short_name"] == "코스온"
    assert manifest["display"] == "standalone"
    assert manifest["start_url"] == "/"
    assert manifest["icons"][0]["src"] == "icon.svg"
    assert (WEB_ROOT / "icon.svg").exists()


def test_brand_copy_reflects_live_integrations() -> None:
    html = read_asset("index.html")
    javascript = read_asset("app.js")

    assert "코스온" in html
    assert "API 연결 예정" not in html
    assert "실제 지도 연동 준비 중" not in html
    assert "네이버 연동 후 실시간으로 바뀌어요" not in javascript


def test_service_worker_caches_the_planner_shell_without_caching_api_responses() -> None:
    service_worker = read_asset("sw.js")

    assert "const CACHE_NAME = 'courseon-shell-v18'" in service_worker
    assert "'./hero-travel.png'" in service_worker
    assert "cache.put" in service_worker
    assert "/api/" in service_worker


def test_map_card_keeps_the_map_footer_visible() -> None:
    styles = read_asset("styles.css")

    assert ".map-card { display: flex; flex-direction: column; overflow: hidden; }" in styles
    assert ".map-surface { background: #e7f0ec; flex: 1 1 auto; height: auto;" in styles
    assert "@media (max-width: 800px)" in styles
    assert ".map-card { min-height: 330px; }.map-surface { min-height: 330px; }" in styles


def test_public_visual_asset_matches_the_source_asset() -> None:
    public_root = WEB_ROOT.parent / "travel-site" / "public"
    source_asset = WEB_ROOT / "hero-travel.png"
    public_asset = public_root / "hero-travel.png"
    source_webp = WEB_ROOT / "hero-travel.webp"
    public_webp = public_root / "hero-travel.webp"

    assert source_asset.exists()
    assert public_asset.exists()
    assert source_asset.read_bytes() == public_asset.read_bytes()
    assert source_webp.exists()
    assert public_webp.exists()
    assert source_webp.read_bytes() == public_webp.read_bytes()
