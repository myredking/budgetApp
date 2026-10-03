from pathlib import Path
import json
import subprocess


WEB_ROOT = Path(__file__).parents[1] / "web"


def read_asset(name: str) -> str:
    return (WEB_ROOT / name).read_text(encoding="utf-8")


def test_plan_history_and_route_edit_controls_are_present() -> None:
    html = read_asset("index.html")

    assert 'id="save-plan-button"' in html
    assert 'id="history-button"' in html
    assert 'id="refresh-plan-button"' in html
    assert 'id="saved-plans-panel"' in html
    assert 'id="alternate-picker"' in html
    assert 'id="no-time-limit"' in html
    assert 'id="share-plan-button"' in html
    assert 'id="nearby-source-status"' in html
    assert 'id="event-source-status"' in html
    assert 'id="nearby-location-button"' in html
    assert 'id="nearby-map-button"' in html
    assert 'id="nearby-map-note"' in html
    assert 'id="nearby-sort"' in html
    assert 'id="nearby-sort-hint"' in html
    assert 'rel="manifest"' in html
    assert 'name="theme-color"' in html
    assert 'name="mobile-web-app-capable"' in html
    assert 'name="apple-mobile-web-app-capable"' in html
    assert 'styles.css?v=19' in html
    assert 'plan_logic.js?v=19' in html
    assert 'app.js?v=19' in html
    assert "navigator.serviceWorker" in read_asset("app.js")


def test_plan_history_and_route_edit_behaviors_are_wired() -> None:
    javascript = read_asset("app.js")
    logic = read_asset("plan_logic.js")

    assert "localStorage" in javascript
    assert "savePlan" in javascript
    assert "loadSavedPlan" in javascript
    assert "refreshPlan" in javascript
    assert "replacePlace" in javascript
    assert "data-place-change-index" in javascript
    assert "loadSharedPlan" in javascript
    assert "sharePlan" in javascript
    assert "getDataSourceLabel" in javascript
    assert "place-map-link" in javascript
    assert "nearby-location-button" in javascript
    assert "currentLocation" in javascript
    assert "map-user-point" in javascript
    assert "sortNearbyItems" in javascript
    assert "getTransportMinutes" in javascript
    assert 'function applyPlanSnapshot' in javascript
    assert 'function applyPlanSnapshot(plan, message) {\n  const formData = plan.form;\n  currentLocation = null;' in javascript
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

    assert "const CACHE_NAME = 'courseon-shell-v8'" in service_worker
    assert "cache.put" in service_worker
    assert "/api/" in service_worker
