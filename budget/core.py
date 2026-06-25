"""Core business logic for the budget CLI app."""


def add_transaction(
    transactions: list[dict[str, object]],
    transaction: dict[str, object],
) -> list[dict[str, object]]:
    """Add a transaction to the collection and return the updated list."""
    updated_transactions = transactions.copy()
    updated_transactions.append(transaction)
    return updated_transactions


def get_balance(transactions: list[dict]) -> int:
    """Return the current balance from the given transactions."""
    if not transactions:
        return 0.0
    return sum(transaction["amount"] for transaction in transactions)


def filter_by_category(
    transactions: list[dict[str, object]],
    category: str,
) -> list[dict[str, object]]:
    """Return transactions that match the requested category."""
    normalized_category = category.casefold()
    return [
        transaction
        for transaction in transactions
        if str(transaction.get("category", "")).casefold() == normalized_category
    ]


def monthly_summary(transactions: list[dict]) -> dict[str, dict[str, int]]:
    """Return monthly income, expense, and net totals."""
    pass
