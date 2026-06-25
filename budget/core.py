"""Core business logic for the budget CLI app."""


def add_transaction(transactions: list[dict], transaction: dict) -> list[dict]:
    """Add a transaction to the collection and return the updated list."""
    pass


def get_balance(transactions: list[dict]) -> int:
    """Return the current balance from the given transactions."""
    pass


def filter_by_category(transactions: list[dict], category: str) -> list[dict]:
    """Return transactions that match the requested category."""
    pass


def monthly_summary(transactions: list[dict]) -> dict[str, dict[str, int]]:
    """Return monthly income, expense, and net totals."""
    pass

