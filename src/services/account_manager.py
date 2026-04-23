"""
AccountManager - Serviço de gerenciamento de contas bancárias.

Lida com abertura, encerramento, bloqueio, operações e regras
de negócio das contas no banco.
"""

from datetime import datetime, date, timedelta
from typing import List, Dict, Tuple, Optional
from src.models.account import Account, AccountType, AccountStatus
from src.models.customer import Customer, CustomerStatus
from src.models.transaction import Transaction, TransactionType, TransactionStatus


class AccountManagerError(Exception):
    pass


class AccountManager:


    
    MIN_BALANCE = {
        AccountType.CHECKING: 0.0,
        AccountType.SAVINGS: 0.0,
        AccountType.SALARY: 0.0,
        AccountType.INVESTMENT: 1000.0,
    }

   
    MAINTENANCE_FEE = {
        AccountType.CHECKING: 25.90,
        AccountType.SAVINGS: 0.0,
        AccountType.SALARY: 0.0,
        AccountType.INVESTMENT: 0.0,
    }

    
    CREDIT_LIMIT_FACTOR = {
        AccountType.CHECKING: 1.5,   
        AccountType.SAVINGS: 0.0,
        AccountType.SALARY: 1.0,
        AccountType.INVESTMENT: 0.0,
    }

    def open_account(
        self,
        customer: Customer,
        account_type: AccountType,
        initial_deposit: float = 0.0,
        account_id: Optional[str] = None,
    ) -> Tuple[Account, List[str]]:
        """
        Abre uma nova conta para o cliente.
        Valida elegibilidade e aplica regras de negócio.
        Retorna (conta, lista_de_avisos).
        """
        warnings = []

        # Validações de elegibilidade
        if customer.status == CustomerStatus.BLACKLISTED:
            raise AccountManagerError("Cliente negativado não pode abrir conta.")

        if customer.status == CustomerStatus.INACTIVE:
            raise AccountManagerError("Cliente inativo. Reative o cadastro primeiro.")

        if customer.get_age() < 18:
            raise AccountManagerError("Abertura de conta restrita a maiores de 18 anos.")

        # Conta salário: exige vínculo empregatício
        if account_type == AccountType.SALARY:
            if customer.monthly_income <= 0:
                raise AccountManagerError(
                    "Conta salário exige comprovação de renda. Informe renda mensal."
                )

        # Conta investimento: saldo mínimo inicial
        if account_type == AccountType.INVESTMENT:
            min_initial = self.MIN_BALANCE[AccountType.INVESTMENT]
            if initial_deposit < min_initial:
                raise AccountManagerError(
                    f"Conta de investimento requer depósito inicial mínimo de R$ {min_initial:.2f}."
                )

        # Avisos relevantes
        if account_type == AccountType.CHECKING and customer.credit_score < 500:
            warnings.append(
                "Score de crédito baixo. Limite de crédito não disponível inicialmente."
            )

        if initial_deposit < 0:
            raise AccountManagerError("Depósito inicial não pode ser negativo.")

        # Gera ID da conta
        if account_id is None:
            account_id = f"{customer.customer_id}-{account_type.value[:3].upper()}-{datetime.now().strftime('%Y%m%d%H%M%S')}"

        # Calcula limite de crédito inicial
        credit_limit = self._calculate_initial_credit_limit(customer, account_type)

        account = Account(
            account_id=account_id,
            customer_id=customer.customer_id,
            account_type=account_type,
            balance=initial_deposit,
            credit_limit=credit_limit,
            status=AccountStatus.ACTIVE,
        )

        return account, warnings

    def _calculate_initial_credit_limit(
        self, customer: Customer, account_type: AccountType
    ) -> float:
        """
        Define o limite de crédito inicial baseado no perfil do cliente.
        """
        factor = self.CREDIT_LIMIT_FACTOR.get(account_type, 0.0)

        if factor == 0.0:
            return 0.0

        base_limit = customer.monthly_income * factor

        # Ajuste por score de crédito
        if customer.credit_score >= 800:
            score_multiplier = 1.5
        elif customer.credit_score >= 700:
            score_multiplier = 1.2
        elif customer.credit_score >= 600:
            score_multiplier = 1.0
        elif customer.credit_score >= 500:
            score_multiplier = 0.7
        else:
            score_multiplier = 0.0  # Sem limite

        return round(base_limit * score_multiplier, 2)

    def close_account(
        self, account: Account, reason: str, force: bool = False
    ) -> Dict:
        """
        Encerra uma conta bancária.
        Verifica pendências antes de permitir o encerramento.
        
        """
        issues = []

        if account.status == AccountStatus.CLOSED:
            raise AccountManagerError("Conta já está encerrada.")

        # Verifica saldo residual
        if account.balance > 0:
            issues.append(f"Conta possui saldo de R$ {account.balance:.2f} a ser transferido.")

        if account.balance < 0:
            issues.append(f"Conta possui débito de R$ {abs(account.balance):.2f}")

        # Verifica crédito utilizado
        credit_used = account.credit_limit - account.get_available_balance() if account.credit_limit > 0 else 0
        if credit_used > 0:
            issues.append(f"Limite de crédito utilizado: R$ {credit_used:.2f} a pagar.")

        if issues and not force:
            raise AccountManagerError(
                f"Não é possível encerrar a conta. Pendências: {'; '.join(issues)}"
            )

        account.status = AccountStatus.CLOSED
        account.last_updated = datetime.now()

        return {
            "account_id": account.account_id,
            "status": "closed",
            "reason": reason,
            "closed_at": account.last_updated.isoformat(),
            "pending_issues": issues,
        }

    def block_account(
        self, account: Account, reason: str, temporary: bool = True
    ) -> Dict:
        """Bloqueia uma conta (temporária ou permanentemente)."""
        if account.status == AccountStatus.CLOSED:
            raise AccountManagerError("Não é possível bloquear conta encerrada.")

        if account.status == AccountStatus.BLOCKED:
            raise AccountManagerError("Conta já está bloqueada.")

        account.status = AccountStatus.BLOCKED
        account.last_updated = datetime.now()

        return {
            "account_id": account.account_id,
            "status": "blocked",
            "reason": reason,
            "temporary": temporary,
            "blocked_at": account.last_updated.isoformat(),
        }

    def unblock_account(self, account: Account, authorization_code: str) -> Dict:
        """Desbloqueia uma conta com código de autorização."""
        if account.status != AccountStatus.BLOCKED:
            raise AccountManagerError("Conta não está bloqueada.")

        if not authorization_code or len(authorization_code) < 6:
            raise AccountManagerError("Código de autorização inválido.")

        account.status = AccountStatus.ACTIVE
        account.last_updated = datetime.now()

        return {
            "account_id": account.account_id,
            "status": "active",
            "unblocked_at": account.last_updated.isoformat(),
        }

    def apply_maintenance_fee(self, account: Account, month: int, year: int) -> float:
        """
        Aplica a tarifa de manutenção mensal quando aplicável.
        Isenta clientes com saldo médio acima de um threshold.
        """
        fee = self.MAINTENANCE_FEE.get(account.account_type, 0.0)

        if fee == 0.0:
            return 0.0

        if account.status != AccountStatus.ACTIVE:
            return 0.0

        # Isenção para conta corrente com saldo médio alto
        if account.account_type == AccountType.CHECKING:
            issuance_threshold = 3000.0
            if account.balance >= issuance_threshold:
                return 0.0

        # Aplica a tarifa
        if account.balance >= fee:
            account.balance -= fee
        else:
            # Usa limite de crédito se necessário
            available = account.get_available_balance()
            if available >= fee:
                account.balance -= fee
            else:
                # Cobra o que puder e registra pendência
                partial = available
                account.balance -= partial
                return partial  # Cobrou apenas parcialmente

        account.last_updated = datetime.now()
        return fee

    def update_credit_limit(
        self,
        account: Account,
        customer: Customer,
        new_score: int,
        requested_increase: Optional[float] = None,
    ) -> Dict:
        """
        Reavalia e atualiza o limite de crédito baseado no novo score.
        """
        if account.account_type not in (AccountType.CHECKING, AccountType.SALARY):
            raise AccountManagerError(
                "Limite de crédito disponível apenas para conta corrente e salário."
            )

        if account.status != AccountStatus.ACTIVE:
            raise AccountManagerError("Conta deve estar ativa para reavaliação de limite.")

        old_limit = account.credit_limit
        customer.credit_score = new_score
        new_limit = self._calculate_initial_credit_limit(customer, account.account_type)

        # Se há solicitação de aumento, tenta acomodar
        if requested_increase and requested_increase > new_limit:
            # Aprovação parcial: máximo calculado
            approved_limit = new_limit
            decision = "partial_approval"
        elif requested_increase and requested_increase <= new_limit:
            approved_limit = requested_increase
            decision = "approved"
        else:
            approved_limit = new_limit
            decision = "automatic_update"

        # Nunca reduz limite abaixo do utilizado
        credit_used = max(0, -account.balance) if account.balance < 0 else 0
        if approved_limit < credit_used:
            approved_limit = credit_used

        account.credit_limit = approved_limit
        account.last_updated = datetime.now()

        return {
            "account_id": account.account_id,
            "old_limit": old_limit,
            "new_limit": approved_limit,
            "decision": decision,
            "new_score": new_score,
            "updated_at": account.last_updated.isoformat(),
        }

    def get_account_statement(
        self,
        account: Account,
        transactions: List[Transaction],
        start_date: date,
        end_date: date,
    ) -> Dict:
        """
        Gera extrato da conta em um período.
        """
        if start_date > end_date:
            raise AccountManagerError("Data inicial não pode ser maior que a final.")

        period_days = (end_date - start_date).days
        if period_days > 365:
            raise AccountManagerError("Período máximo para extrato é 365 dias.")

        period_transactions = [
            t for t in transactions
            if start_date <= t.timestamp.date() <= end_date
            and t.source_account_id == account.account_id
            and t.status == TransactionStatus.COMPLETED
        ]

        total_credits = sum(
            t.amount for t in period_transactions if not t.is_outgoing()
        )
        total_debits = sum(
            t.amount for t in period_transactions if t.is_outgoing()
        )

        return {
            "account_id": account.account_id,
            "period": {
                "start": start_date.isoformat(),
                "end": end_date.isoformat(),
                "days": period_days,
            },
            "current_balance": account.balance,
            "available_balance": account.get_available_balance(),
            "total_credits": round(total_credits, 2),
            "total_debits": round(total_debits, 2),
            "net_movement": round(total_credits - total_debits, 2),
            "transaction_count": len(period_transactions),
            "transactions": [t.to_dict() for t in sorted(
                period_transactions, key=lambda x: x.timestamp, reverse=True
            )],
        }
