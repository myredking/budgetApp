import runpy
from pathlib import Path

import pytest

TOOL = Path(__file__).resolve().parents[1] / "tools/extract_ui_strings.py"


def test_candidate_scan_excludes_comments_concatenation_and_tags() -> None:
    tool = runpy.run_path(str(TOOL))
    source = ('// x.setText("주석");\n'
              'x.setTitle("출항 준비");\n'
              'x.setMessage("항구 "+port);\n'
              'x.setTag("저장");')
    assert [entry["literal"] for entry in tool["candidates"](source)] == ['"출항 준비"']


def test_only_reviewed_occurrences_are_replaced() -> None:
    tool = runpy.run_path(str(TOOL))
    source = 'x.setTitle("출항 준비"); x.setText("확인");'
    selected = {'"출항 준비"': "voyage_prepare"}
    output, additions = tool["convert"](source, selected, "activity.")
    assert output == 'x.setTitle(activity.getString(R.string.voyage_prepare)); x.setText("확인");'
    assert additions == {"voyage_prepare": "출항 준비"}


def test_xml_preserves_newlines_quotes_and_percent_signs() -> None:
    tool = runpy.run_path(str(TOOL))
    source = r'x.setMessage("한 줄\n\"인용\" 100%");'
    output, additions = tool["convert"](source, {r'"한 줄\n\"인용\" 100%"': "sample"}, "")
    xml = tool["merge_strings"]("<resources />", additions)
    assert 'getString(R.string.sample)' in output
    assert 'formatted="false"' in xml
    assert r'\n\"인용\" 100%' in xml


def test_stale_selection_and_conflicting_key_are_rejected() -> None:
    tool = runpy.run_path(str(TOOL))
    with pytest.raises(ValueError):
        tool["convert"]('x.setText("다른 문구");', {'"확인"': "confirm"}, "")
    with pytest.raises(ValueError):
        tool["merge_strings"]('<resources><string name="confirm">기존</string></resources>', {"confirm": "확인"})
