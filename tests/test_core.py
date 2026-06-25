from budget.core import add_transaction


def test_add_transaction_increases_length() -> None:
    transactions = [{"date": "2026-01-01", "type": "income", "amount": 1000}]
    transaction = {
        "date": "2026-01-02",
        "type": "expense",
        "amount": -200,
    }

    result = add_transaction(transactions, transaction)

    assert len(result) == len(transactions) + 1

