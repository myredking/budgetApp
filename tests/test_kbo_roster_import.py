"""Official search-table parsing; no network access in tests."""
import importlib.util
from pathlib import Path

import pytest

MODULE = Path(__file__).resolve().parents[1] / "unity-baseball-showcase/Tools/kbo_roster_import.py"


def importer() -> object:
    spec = importlib.util.spec_from_file_location("kbo_roster_import", MODULE)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def test_player_row_uses_official_id_and_physical_profile() -> None:
    html = """<table><tbody><tr><td>5</td><td><a href='/Record/Player/HitterDetail/Basic.aspx?playerId=52605'>김도영</a></td>
    <td>KIA</td><td>내야수</td><td>2003-10-02</td><td>183cm, 85kg</td><td>학교</td></tr></tbody></table>"""
    rows = importer().parse_players(html)
    assert rows[0]["kbo_id"] == 52605
    assert rows[0]["name"] == "김도영"
    assert rows[0]["height_cm"] == 183
    assert rows[0]["weight_kg"] == 85
    assert rows[0]["source"].startswith("https://www.koreabaseball.com/Record/Player/")


def test_missing_results_are_empty_but_duplicate_ids_are_rejected() -> None:
    module = importer()
    assert module.parse_players("<table><tbody></tbody></table>") == []
    row = "<tr><td>1</td><td><a href='/Record/Player/PitcherDetail/Basic.aspx?playerId=1'>선수</a></td><td>KIA</td><td>투수</td><td>2000-01-01</td><td>190cm,100kg</td><td>학교</td></tr>"
    with pytest.raises(ValueError, match="Duplicate"):
        module.parse_players("<table><tbody>" + row + row + "</tbody></table>")


def test_count_and_pager_are_read_from_observed_markup() -> None:
    module = importer()
    html = """<span>검색결과 : 1,021 건</span><div class='paging'>
    <a href="javascript:__doPostBack('pager$btnNo2','')">2</a>
    <a href="javascript:__doPostBack('pager$btnNext','')"></a></div>"""
    assert module.parse_total(html) == 1021
    assert module.next_page_target(html, 1) == "pager$btnNo2"
    assert module.next_page_target(html, 5) == "pager$btnNext"
    with pytest.raises(ValueError):
        module.parse_total("unrelated page")


def player_page(player_id: int, following: bool) -> str:
    pager = "<div class='paging'><a href=\"javascript:__doPostBack('pager$btnNo2','')\">2</a></div>" if following else ""
    return f"<span>검색결과 : 2 건</span><table><tbody><tr><td>1</td><td><a href='/Record/Player/PitcherDetail/Basic.aspx?playerId={player_id}'>선수</a></td><td>KIA</td><td>투수</td><td>2000-01-01</td><td>190cm,100kg</td><td>학교</td></tr></tbody></table>{pager}"


def test_collection_requires_complete_unique_pages() -> None:
    module = importer()
    rows = module.collect_team(player_page(1, True), lambda target: player_page(2, False))
    assert [row["kbo_id"] for row in rows] == [1, 2]
    with pytest.raises(ValueError, match="Repeated"):
        module.collect_team(player_page(1, True), lambda target: player_page(1, False))


def test_futures_affiliation_preserves_official_team_label() -> None:
    module = importer()
    source = [{"kbo_id": 1, "team": "고양"}]
    rows = module.club_rows(source, "키움")
    assert rows[0]["team"] == "고양"
    assert rows[0]["club"] == "키움"
    assert "club" not in source[0]
    with pytest.raises(ValueError):
        module.club_rows(source, "LG")
