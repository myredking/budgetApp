from pathlib import Path
import json

PREFABS = ('Batter.prefab', 'Pitcher.prefab', 'Stadium.prefab', 'Bat.prefab', 'Ball.prefab')

def inspect_assets(project: Path) -> dict[str, object]:
    art = project / 'Assets/Showcase/Art'
    missing = [name for name in PREFABS if not (art / name).is_file()]
    report_path = project / 'Reports/editor-validation.json'
    validated = False
    if report_path.is_file():
        try:
            report = json.loads(report_path.read_text(encoding='utf-8-sig'))
            validated = isinstance(report, dict) and all(report.get(key) is True for key in ('passed', 'visual_review_passed', 'motion_review_passed'))
        except (ValueError, OSError):
            validated = False
    return {'ready': not missing and validated, 'missing': missing,
            'editor_and_visual_review_passed': validated,
            'status': 'ready_for_quality_shot' if not missing and validated else 'not_ready'}

if __name__ == '__main__':
    print(json.dumps(inspect_assets(Path(__file__).resolve().parents[1]), ensure_ascii=False, indent=2))
