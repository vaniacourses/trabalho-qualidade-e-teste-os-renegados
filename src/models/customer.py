from dataclasses import dataclass, field
from datetime import datetime, date
from enum import Enum
from typing import Optional


class CustomerStatus(Enum):
    ACTIVE = "active"
    INACTIVE = "inactive"
    BLACKLISTED = "blacklisted"
    PENDING_VERIFICATION = "pending_verification"


@dataclass
class Customer:
    customer_id: str
    name: str
    cpf: str
    email: str
    phone: str
    birth_date: date
    monthly_income: float
    status: CustomerStatus = CustomerStatus.ACTIVE
    credit_score: int = 500
    address: Optional[str] = None
    created_at: datetime = field(default_factory=datetime.now)

    def get_age(self) -> int:
        today = date.today()
        return (today - self.birth_date).days // 365

    def is_active(self) -> bool:
        return self.status == CustomerStatus.ACTIVE

    def to_dict(self) -> dict:
        return {
            "customer_id": self.customer_id,
            "name": self.name,
            "cpf": self.cpf,
            "email": self.email,
            "phone": self.phone,
            "birth_date": self.birth_date.isoformat(),
            "monthly_income": self.monthly_income,
            "status": self.status.value,
            "credit_score": self.credit_score,
            "age": self.get_age(),
        }
