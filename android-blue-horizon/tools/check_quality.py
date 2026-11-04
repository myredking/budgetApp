"""Check Java and Python function size/complexity for this feature only."""
from pathlib import Path
import ast
import lizard
from radon.complexity import cc_visit

PROJECT = Path(__file__).resolve().parents[1]


def java_issues() -> tuple[list[str], int, int]:
    issues: list[str] = []
    count, maximum = 0, 0
    paths = list((PROJECT / "src").rglob("*.java")) + list((PROJECT / "tests").glob("*.java"))
    for path in paths:
        analysis = lizard.analyze_file(str(path))
        for function in analysis.function_list:
            count += 1
            maximum = max(maximum, function.cyclomatic_complexity)
            length = function.end_line - function.start_line + 1
            if length > 50 or function.cyclomatic_complexity > 10:
                issues.append(f"{path.name}:{function.start_line} {function.name}: {length} lines, CC {function.cyclomatic_complexity}")
    return issues, count, maximum


def python_issues() -> list[str]:
    issues: list[str] = []
    for path in (PROJECT / "tools").glob("*.py"):
        source = path.read_text(encoding="utf-8")
        for node in ast.walk(ast.parse(source)):
            if not isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef)):
                continue
            if node.end_lineno - node.lineno + 1 > 50:
                issues.append(f"{path.name}:{node.lineno} function exceeds 50 lines")
            if node.returns is None or any(arg.annotation is None for arg in node.args.args):
                issues.append(f"{path.name}:{node.lineno} missing type hints")
        for block in cc_visit(source):
            if block.complexity > 10:
                issues.append(f"{path.name}:{block.lineno} CC {block.complexity}")
    return issues


def main() -> None:
    issues, count, maximum = java_issues()
    issues.extend(python_issues())
    print(f"Java: {count} functions checked, maximum CC {maximum}")
    for issue in issues:
        print(issue)
    if issues:
        raise SystemExit(1)
    print("PASS: function length <= 50, complexity <= 10, Python type hints present")


if __name__ == "__main__":
    main()
