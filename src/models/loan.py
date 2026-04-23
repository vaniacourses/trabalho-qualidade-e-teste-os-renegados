from dataclasses import dataclass, field
from datetime import datetime, date
from enum import Enum
from typing import List, Optional


class LoanType(Enum):
    PERSONAL = "personal"
    MORTGAGE = "mortgage"
    AUTO = "auto"
    STUDENT = "student"
    BUSINESS = "business"
    PAYROLL_DEDUCTIBLE = "payroll_deductible"


class LoanStatus(Enum):
    REQUESTED = "requested"
    UNDER_ANALYSIS = "under_analysis"
    APPROVED = "approved"
    REJECTED = "rejected"
    ACTIVE = "active"
    PAID_OFF = "paid_off"
    DEFAULTED = "defaulted"


class AmortizationSystem(Enum):
    PRICE = "price"   # Tabela Price (equal installments)
    SAC = "sac"       # SAC (decreasing installments)
    SAM = "sam"       # SAM (mix of Price and SAC)


@dataclass
class LoanInstallment:
    installment_number: int
    due_date: date
    principal: float
    interest: float
    total: float
    paid: bool = False
    paid_at: Optional[datetime] = None

    def to_dict(self) -> dict:
        return {
            "installment_number": self.installment_number,
            "due_date": self.due_date.isoformat(),
            "principal": round(self.principal, 2),
            "interest": round(self.interest, 2),
            "total": round(self.total, 2),
            "paid": self.paid,
        }


@dataclass
class Loan:
    loan_id: str
    customer_id: str
    account_id: str
    loan_type: LoanType
    principal_amount: float
    interest_rate_monthly: float
    term_months: int
    amortization_system: AmortizationSystem
    status: LoanStatus = LoanStatus.REQUESTED
    installments: List[LoanInstallment] = field(default_factory=list)
    created_at: datetime = field(default_factory=datetime.now)
    approved_at: Optional[datetime] = None
    rejection_reason: Optional[str] = None

    def get_total_paid(self) -> float:
        return sum(i.total for i in self.installments if i.paid)

    def get_remaining_balance(self) -> float:
        return sum(i.principal for i in self.installments if not i.paid)

    def get_paid_installments_count(self) -> int:
        return sum(1 for i in self.installments if i.paid)

    def to_dict(self) -> dict:
        return {
            "loan_id": self.loan_id,
            "customer_id": self.customer_id,
            "account_id": self.account_id,
            "loan_type": self.loan_type.value,
            "principal_amount": self.principal_amount,
            "interest_rate_monthly": self.interest_rate_monthly,
            "term_months": self.term_months,
            "amortization_system": self.amortization_system.value,
            "status": self.status.value,
            "total_paid": self.get_total_paid(),
            "remaining_balance": self.get_remaining_balance(),
            "installments_count": len(self.installments),
            "paid_installments": self.get_paid_installments_count(),
        }
