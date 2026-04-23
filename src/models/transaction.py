from dataclasses import dataclass, field
from datetime import datetime
from enum import Enum
from typing import Optional


class TransactionType(Enum):
    DEPOSIT = "deposit"
    WITHDRAWAL = "withdrawal"
    TRANSFER = "transfer"
    PAYMENT = "payment"
    PIX = "pix"
    TED = "ted"
    DOC = "doc"
    FEE = "fee"
    INTEREST = "interest"
    REVERSAL = "reversal"


class TransactionStatus(Enum):
    PENDING = "pending"
    COMPLETED = "completed"
    FAILED = "failed"
    CANCELLED = "cancelled"
    REVERSED = "reversed"
    UNDER_REVIEW = "under_review"


@dataclass
class Transaction:
    transaction_id: str
    source_account_id: str
    transaction_type: TransactionType
    amount: float
    description: str = ""
    destination_account_id: Optional[str] = None
    status: TransactionStatus = TransactionStatus.PENDING
    timestamp: datetime = field(default_factory=datetime.now)
    metadata: dict = field(default_factory=dict)

    def is_outgoing(self) -> bool:
        return self.transaction_type in [
            TransactionType.WITHDRAWAL,
            TransactionType.TRANSFER,
            TransactionType.PAYMENT,
            TransactionType.PIX,
            TransactionType.TED,
            TransactionType.DOC,
        ]

    def is_completed(self) -> bool:
        return self.status == TransactionStatus.COMPLETED

    def to_dict(self) -> dict:
        return {
            "transaction_id": self.transaction_id,
            "source_account_id": self.source_account_id,
            "destination_account_id": self.destination_account_id,
            "transaction_type": self.transaction_type.value,
            "amount": self.amount,
            "description": self.description,
            "status": self.status.value,
            "timestamp": self.timestamp.isoformat(),
            "metadata": self.metadata,
        }
