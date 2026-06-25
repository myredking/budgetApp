# budgetApp Agent Guide

## Project
This repository is a CSV-based budget app CLI project. It reads transaction CSV files under `data/` and supports adding transactions, calculating balances, filtering by category, and monthly summaries.

## Coding Rules
- Every function and method must include type hints.
- Keep each function at 50 lines or fewer.
- Prefer small, easy-to-read units of logic.

## TDD Rules
- Write tests before implementation.
- Confirm the test fails before starting the implementation.
- When changing existing behavior, add or update tests first.

## Quality Rules
- Keep cyclomatic complexity at 10 or below.
- If complexity grows, split the function or use early returns.

## Quality Review Rules
- Before committing, always run quality review with the `qa_engineer` subagent.
- Fix QA findings before committing.
- The QA prompt lives at `agents/qa_engineer.md`.

## Test Commands
- `pytest`
- `radon cc`

## Commit Rules
- When one feature is complete, commit it and push it right away.
- Keep each commit to one feature.
