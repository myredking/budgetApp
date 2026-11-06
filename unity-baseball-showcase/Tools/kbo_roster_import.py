"""Read factual player-search rows; ratings and season results are separate data."""
from __future__ import annotations

import re
from collections.abc import Callable
from urllib.parse import parse_qs, urljoin, urlparse

from bs4 import BeautifulSoup

BASE = "https://www.koreabaseball.com"


def parse_players(html: str) -> list[dict]:
    result = []
    seen = set()
    for row in BeautifulSoup(html, "html.parser").select("table tbody tr"):
        anchor = row.select_one("a[href*='playerId=']")
        if anchor is None:
            continue
        cells = [cell.get_text(" ", strip=True) for cell in row.select("td")]
        if len(cells) != 7:
            raise ValueError("Unexpected player table columns")
        source = urljoin(BASE, anchor["href"])
        if urlparse(source).netloc != "www.koreabaseball.com":
            raise ValueError("Unexpected profile host")
        player_id = int(parse_qs(urlparse(source).query)["playerId"][0])
        if player_id in seen:
            raise ValueError("Duplicate player ID")
        seen.add(player_id)
        body = re.search(r"(\d+)\s*cm\s*,?\s*(\d+)\s*kg", cells[5])
        result.append({"kbo_id": player_id, "name": cells[1], "team": cells[2],
            "position": cells[3], "number": cells[0], "height_cm": int(body[1]) if body else None,
            "weight_kg": int(body[2]) if body else None, "source": source})
    return result


def parse_total(html: str) -> int:
    text = BeautifulSoup(html, "html.parser").get_text(" ", strip=True)
    count = re.search(r"검색결과\s*:\s*([\d,]+)\s*건", text)
    if count is None:
        raise ValueError("Missing result count")
    return int(count[1].replace(",", ""))


def next_page_target(html: str, current_page: int) -> str:
    fallback = ""
    for anchor in BeautifulSoup(html, "html.parser").select(".paging a"):
        target = re.search(r"__doPostBack\('([^']+)'", anchor.get("href", ""))
        if target is None:
            continue
        if anchor.get_text(strip=True) == str(current_page + 1):
            return target[1]
        if target[1].endswith("btnNext"):
            fallback = target[1]
    if not fallback:
        raise ValueError("Missing next page")
    return fallback


def collect_team(html: str, load_next: Callable[[str], str]) -> list[dict]:
    total = parse_total(html)
    rows = parse_players(html)
    seen = {row["kbo_id"] for row in rows}
    page = 1
    while len(rows) < total:
        if page > 50:
            raise ValueError("Too many pages")
        html = load_next(next_page_target(html, page))
        following = parse_players(html)
        if parse_total(html) != total or not following:
            raise ValueError("Result count changed or page empty")
        for row in following:
            if row["kbo_id"] in seen:
                raise ValueError("Repeated player across pages")
            seen.add(row["kbo_id"])
            rows.append(row)
        page += 1
    if len(rows) != total:
        raise ValueError("Incomplete roster")
    return rows


def club_rows(rows: list[dict], club: str) -> list[dict]:
    allowed = {club}
    if club == "키움":
        allowed.add("고양")
    if any(row["team"] not in allowed for row in rows):
        raise ValueError("Unexpected team filter result")
    return [{**row, "club": club} for row in rows]
