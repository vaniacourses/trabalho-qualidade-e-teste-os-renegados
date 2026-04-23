from dataclasses import dataclass, field
from datetime import datetime
from enum import Enum
from typing import List, Optional


class AccountType(Enum):
    CHECKING = "checking"
    SAVINGS = "savings"
    SALARY = "salary"
    INVESTMENT = "investment"


class AccountStatus(Enum):
    ACTIVE = "active"
    BLOCKED = "blocked"
    CLOSED = "closed"
    PENDING = "pending"


@dataclass
class Account:
    account_id: str
    customer_id: str
    account_type: AccountType
    balance: float = 0.0
    credit_limit: float = 0.0
    status: AccountStatus = AccountStatus.ACTIVE
    created_at: datetime = field(default_factory=datetime.now)
    last_updated: datetime = field(default_factory=datetime.now)
    transactions: List[str] = field(default_factory=list)

    def get_available_balance(self) -> float:
        return self.balance + self.credit_limit

    def is_active(self) -> bool:
        return self.status == AccountStatus.ACTIVE

    def to_dict(self) -> dict:
        return {
            "account_id": self.account_id,
            "customer_id": self.customer_id,
            "account_type": self.account_type.value,
            "balance": self.balance,
            "credit_limit": self.credit_limit,
            "status": self.status.value,
            "created_at": self.created_at.isoformat(),
            "available_balance": self.get_available_balance(),
        }
