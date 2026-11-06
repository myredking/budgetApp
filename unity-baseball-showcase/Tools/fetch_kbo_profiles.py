"""Fetch public search profiles, without inventing season stats or ratings."""
from __future__ import annotations

import argparse
from datetime import date
import json
from pathlib import Path
import time

from bs4 import BeautifulSoup
import requests

from kbo_roster_import import club_rows, collect_team

URL = "https://www.koreabaseball.com/Player/Search.aspx"


def payload(html: str, team: str, target: str = "") -> dict[str, str]:
    soup = BeautifulSoup(html, "html.parser")
    data = {node["name"]: node.get("value", "") for node in soup.select("input[name]")
            if node.get("type") not in ("submit", "image", "button")}
    for select in soup.select("select[name]"):
        data[select["name"]] = team if select["name"].endswith("$ddlTeam") else ""
    data["__EVENTTARGET"] = target
    data["__EVENTARGUMENT"] = ""
    if not target:
        button = soup.select_one("input[name$='$btnSearch']")
        data[button["name"]] = button["value"]
    return data


def download_team(session: requests.Session, team: str) -> list[dict]:
    response = session.get(URL, timeout=30)
    response.raise_for_status()
    response = session.post(URL, data=payload(response.text, team), timeout=30)
    response.raise_for_status()
    current = response.text

    def load_next(target: str) -> str:
        nonlocal current
        time.sleep(.6)
        response = session.post(URL, data=payload(current, team, target), timeout=30)
        response.raise_for_status()
        current = response.text
        return current

    return collect_team(current, load_next)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--retrieved-on", required=True, type=date.fromisoformat)
    args = parser.parse_args()
    session = requests.Session()
    response = session.get(URL, timeout=30)
    response.raise_for_status()
    soup = BeautifulSoup(response.text, "html.parser")
    teams = [(node["value"], node.get_text(strip=True)) for node in soup.select("select[name$='$ddlTeam'] option[value]") if node["value"]]
    if len(teams) != 10:
        raise ValueError("Expected ten current clubs")
    players, counts = [], {}
    for team, name in teams:
        rows = club_rows(download_team(session, team),name)
        players.extend(rows)
        counts[name] = len(rows)
        print(name, len(rows), flush=True)
        time.sleep(.6)
    if len({row["kbo_id"] for row in players}) != len(players):
        raise ValueError("Duplicate player across clubs")
    snapshot = {"retrieved_on": str(args.retrieved_on), "source": URL, "scope": "Current player-search profiles, not final 2026 season roster or season stats", "affiliation_source": "https://m.heroesbaseball.co.kr/mobile/heroes/introduce/introduce.do", "team_counts": counts, "players": players}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    temporary = args.output.with_suffix(".pending.json")
    temporary.write_text(json.dumps(snapshot, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    temporary.replace(args.output)


if __name__ == "__main__":
    main()
