from budget.core import (
    add_transaction,
    filter_by_category,
    get_balance,
    monthly_summary,
)


def test_add_transaction_increases_length() -> None:
    transactions = [
        {
            "date": "2026-01-05",
            "type": "지출",
            "category": "식비",
            "description": "점심식사",
            "amount": -12000,
            "memo": "",
        }
    ]
    transaction = {
        "date": "2026-01-07",
        "type": "수입",
        "category": "급여",
        "description": "월급",
        "amount": 3500000,
        "memo": "1월급여",
    }

    initial_length = len(transactions)
    result = add_transaction(transactions, transaction)

    assert len(result) == initial_length + 1


def test_add_transaction_preserves_negative_amount_for_expense() -> None:
    transactions = []
    transaction = {
        "date": "2026-01-10",
        "type": "지출",
        "category": "교통",
        "description": "지하철",
        "amount": -1500,
        "memo": "",
    }

    result = add_transaction(transactions, transaction)

    assert result[-1]["amount"] == -1500


def test_add_transaction_preserves_positive_amount_for_income() -> None:
    transactions = []
    transaction = {
        "date": "2026-01-12",
        "type": "기타수입",
        "category": "기타수입",
        "description": "중고 판매",
        "amount": 25000,
        "memo": "중고마켓",
    }

    result = add_transaction(transactions, transaction)

    assert result[-1]["amount"] == 25000


def test_add_transaction_allows_empty_description() -> None:
    transactions = []
    transaction = {
        "date": "2026-01-15",
        "type": "지출",
        "category": "쇼핑",
        "description": "",
        "amount": -45000,
        "memo": "",
    }

    result = add_transaction(transactions, transaction)

    assert result[-1]["description"] == ""


def test_get_balance_returns_sum_of_incomes_and_expenses() -> None:
    transactions = [
        {"amount": -12000},
        {"amount": 3500000},
        {"amount": -1500},
        {"amount": -5800},
        {"amount": -45000},
    ]

    result = get_balance(transactions)

    assert result == 3435700


def test_get_balance_returns_zero_for_empty_list() -> None:
    result = get_balance([])

    assert result == 0.0


def test_get_balance_matches_step2_sample_data() -> None:
    transactions = [
        {"amount": -979796},
        {"amount": -65990},
        {"amount": -80861},
        {"amount": -75010},
        {"amount": -107684},
        {"amount": -326526},
        {"amount": -432554},
        {"amount": -282323},
        {"amount": -659572},
        {"amount": -64470},
        {"amount": 135541},
        {"amount": -90127},
        {"amount": -223148},
        {"amount": -20331},
        {"amount": -103886},
        {"amount": -63306},
        {"amount": -33021},
        {"amount": -651009},
        {"amount": 4358625},
    ]

    result = get_balance(transactions)

    assert result == 234552


def test_filter_by_category_matches_case_insensitively() -> None:
    transactions = [
        {
            "date": "2026-01-04",
            "type": "지출",
            "category": "여행",
            "description": "항공권",
            "amount": -979796,
            "memo": "메모_3",
        },
        {
            "date": "2026-01-05",
            "type": "지출",
            "category": "의료",
            "description": "한의원",
            "amount": -65990,
            "memo": "카드결제",
        },
        {
            "date": "2026-01-15",
            "type": "수입",
            "category": "기타수입",
            "description": "중고 판매",
            "amount": 135541,
            "memo": "",
        },
    ]

    result = filter_by_category(transactions, "여행".upper())

    assert result == [transactions[0]]


def test_filter_by_category_returns_empty_list_for_missing_category() -> None:
    transactions = [
        {
            "date": "2026-01-04",
            "type": "지출",
            "category": "여행",
            "description": "항공권",
            "amount": -979796,
            "memo": "메모_3",
        }
    ]

    result = filter_by_category(transactions, "없는카테고리")

    assert result == []


def test_filter_by_category_returns_independent_list() -> None:
    transactions = [
        {
            "date": "2026-01-04",
            "type": "지출",
            "category": "식비",
            "description": "점심",
            "amount": -12000,
            "memo": "",
        }
    ]

    result = filter_by_category(transactions, "식비")
    result.append(
        {
            "date": "2026-01-05",
            "type": "지출",
            "category": "식비",
            "description": "저녁",
            "amount": -15000,
            "memo": "",
        }
    )

    assert len(transactions) == 1
    assert len(result) == 2


def test_monthly_summary_groups_income_expense_and_net() -> None:
    transactions = [
        {
            "date": "2026-01-04",
            "type": "지출",
            "category": "여행",
            "description": "항공권",
            "amount": -1000,
            "memo": "",
        },
        {
            "date": "2026-01-05",
            "type": "수입",
            "category": "급여",
            "description": "월급",
            "amount": 3000,
            "memo": "",
        },
        {
            "date": "2026-02-01",
            "type": "지출",
            "category": "식비",
            "description": "점심",
            "amount": -500,
            "memo": "",
        },
    ]

    result = monthly_summary(transactions)

    assert result == {
        "2026-01": {"income": 3000, "expense": -1000, "net": 2000},
        "2026-02": {"income": 0, "expense": -500, "net": -500},
    }


def test_monthly_summary_matches_step3_sample_data() -> None:
    transactions = [
        {"date": "2025-01-02", "amount": 405037},
        {"date": "2025-01-03", "amount": -2886860},
        {"date": "2025-02-01", "amount": 12940804},
        {"date": "2025-02-10", "amount": -1832242},
        {"date": "2026-01-15", "amount": 5140637},
        {"date": "2026-01-20", "amount": -1283712},
        {"date": "2026-02-01", "amount": 720569},
        {"date": "2026-02-20", "amount": -4012456},
    ]

    result = monthly_summary(transactions)

    assert result["2025-01"] == {
        "income": 405037,
        "expense": -2886860,
        "net": -2481823,
    }
    assert result["2025-02"] == {
        "income": 12940804,
        "expense": -1832242,
        "net": 11108562,
    }
    assert result["2026-01"] == {
        "income": 5140637,
        "expense": -1283712,
        "net": 3856925,
    }
    assert result["2026-02"] == {
        "income": 720569,
        "expense": -4012456,
        "net": -3291887,
    }
