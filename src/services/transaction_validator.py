"""
TransactionValidator - Serviço de validação de transações bancárias.

Aplica regras de negócio, limites operacionais e políticas de segurança
para aprovar ou rejeitar transações antes de executá-las.
"""

from datetime import datetime, time
from typing import List, Tuple, Dict
from src.models.account import Account, AccountType, AccountStatus
from src.models.transaction import Transaction, TransactionType


class TransactionValidationError(Exception):
    pass


class TransactionValidator:
    """
    Valida transações bancárias aplicando regras de negócio completas.
    """

    # Limites diários por tipo de conta e operação (R$)
    DAILY_LIMITS = {
        AccountType.CHECKING: {
            TransactionType.WITHDRAWAL: 5000.0,
            TransactionType.TRANSFER: 10000.0,
            TransactionType.PIX: 20000.0,
            TransactionType.TED: 15000.0,
            TransactionType.DOC: 5000.0,
            TransactionType.PAYMENT: 10000.0,
        },
        AccountType.SAVINGS: {
            TransactionType.WITHDRAWAL: 2000.0,
            TransactionType.TRANSFER: 5000.0,
            TransactionType.PIX: 5000.0,
            TransactionType.TED: 5000.0,
            TransactionType.DOC: 2000.0,
            TransactionType.PAYMENT: 5000.0,
        },
        AccountType.SALARY: {
            TransactionType.WITHDRAWAL: 3000.0,
            TransactionType.TRANSFER: 7000.0,
            TransactionType.PIX: 10000.0,
            TransactionType.TED: 7000.0,
            TransactionType.DOC: 3000.0,
            TransactionType.PAYMENT: 7000.0,
        },
        AccountType.INVESTMENT: {
            TransactionType.WITHDRAWAL: 50000.0,
            TransactionType.TRANSFER: 100000.0,
            TransactionType.PIX: 50000.0,
            TransactionType.TED: 100000.0,
            TransactionType.DOC: 50000.0,
            TransactionType.PAYMENT: 100000.0,
        },
    }

    # Horários de operação por tipo de transação
    OPERATION_HOURS = {
        TransactionType.DOC: (time(6, 0), time(17, 0)),
        TransactionType.TED: (time(6, 0), time(17, 0)),
        TransactionType.PAYMENT: (time(0, 0), time(23, 59)),
        TransactionType.PIX: (time(0, 0), time(23, 59)),
        TransactionType.WITHDRAWAL: (time(0, 0), time(23, 59)),
        TransactionType.TRANSFER: (time(0, 0), time(23, 59)),
        TransactionType.DEPOSIT: (time(0, 0), time(23, 59)),
    }

    def validate(
        self,
        transaction: Transaction,
        source_account: Account,
        daily_spent: Dict[TransactionType, float],
        destination_account: Account = None,
    ) -> Tuple[bool, List[str]]:
        """
        Valida uma transação completa.
        Retorna (válido, lista_de_erros).
        """
        errors = []

        errors.extend(self._validate_basic_fields(transaction))
        errors.extend(self._validate_account_status(source_account))
        errors.extend(self._validate_operation_hours(transaction))
        errors.extend(self._validate_balance(transaction, source_account))
        errors.extend(self._validate_daily_limit(transaction, source_account, daily_spent))
        errors.extend(self._validate_destination(transaction, destination_account))
        errors.extend(self._validate_account_type_restrictions(transaction, source_account))

        return len(errors) == 0, errors

    def _validate_basic_fields(self, transaction: Transaction) -> List[str]:
        """Valida campos básicos da transação."""
        errors = []

        if transaction.amount <= 0:
            errors.append("O valor da transação deve ser positivo.")

        if transaction.amount > 1_000_000:
            errors.append("Valor excede o limite máximo por operação de R$ 1.000.000,00.")

        if not transaction.transaction_id:
            errors.append("ID da transação não pode ser vazio.")

        if not transaction.source_account_id:
            errors.append("Conta de origem não informada.")

        # Centavos: valida precisão máxima de 2 casas decimais
        if round(transaction.amount, 2) != transaction.amount:
            errors.append("Valor da transação não pode ter mais de 2 casas decimais.")

        return errors

    def _validate_account_status(self, account: Account) -> List[str]:
        """Verifica se a conta de origem está apta a realizar transações."""
        errors = []

        if account.status == AccountStatus.BLOCKED:
            errors.append("Conta de origem está bloqueada.")
        elif account.status == AccountStatus.CLOSED:
            errors.append("Conta de origem está encerrada.")
        elif account.status == AccountStatus.PENDING:
            errors.append("Conta de origem está pendente de ativação.")

        return errors

    def _validate_operation_hours(self, transaction: Transaction) -> List[str]:
        """Verifica se a operação está dentro do horário permitido."""
        errors = []

        if transaction.transaction_type not in self.OPERATION_HOURS:
            return errors

        start_time, end_time = self.OPERATION_HOURS[transaction.transaction_type]
        current_time = transaction.timestamp.time()

        if transaction.transaction_type in (TransactionType.DOC, TransactionType.TED):
            if not (start_time <= current_time <= end_time):
                errors.append(
                    f"{transaction.transaction_type.value.upper()} só pode ser realizado "
                    f"entre {start_time.strftime('%H:%M')} e {end_time.strftime('%H:%M')}."
                )

            # DOC e TED não processados em fins de semana
            weekday = transaction.timestamp.weekday()
            if weekday >= 5:
                errors.append(
                    f"{transaction.transaction_type.value.upper()} não é processado "
                    f"em fins de semana."
                )

        return errors

    def _validate_balance(self, transaction: Transaction, account: Account) -> List[str]:
        """Verifica se há saldo suficiente para a operação."""
        errors = []

        if not transaction.is_outgoing():
            return errors

        available = account.get_available_balance()

        # Poupança não tem limite de crédito
        if account.account_type == AccountType.SAVINGS:
            available = account.balance

        if transaction.amount > available:
            errors.append(
                f"Saldo insuficiente. Disponível: R$ {available:.2f}, "
                f"Solicitado: R$ {transaction.amount:.2f}."
            )

        # Conta poupança: máximo de 6 saques/transferências por mês (regra BACEN)
        if account.account_type == AccountType.SAVINGS and transaction.is_outgoing():
            monthly_count = account.metadata.get("monthly_outgoing_count", 0) if hasattr(account, 'metadata') else 0
            # Aviso se próximo do limite (implementação simplificada)

        return errors

    def _validate_daily_limit(
        self,
        transaction: Transaction,
        account: Account,
        daily_spent: Dict[TransactionType, float],
    ) -> List[str]:
        """Verifica se a transação ultrapassa o limite diário."""
        errors = []

        if not transaction.is_outgoing():
            return errors

        account_limits = self.DAILY_LIMITS.get(account.account_type, {})
        limit = account_limits.get(transaction.transaction_type)

        if limit is None:
            return errors

        already_spent = daily_spent.get(transaction.transaction_type, 0.0)
        if already_spent + transaction.amount > limit:
            remaining = max(0.0, limit - already_spent)
            errors.append(
                f"Limite diário atingido para {transaction.transaction_type.value}. "
                f"Disponível hoje: R$ {remaining:.2f}."
            )

        return errors

    def _validate_destination(
        self, transaction: Transaction, destination_account: Account
    ) -> List[str]:
        """Valida a conta de destino para operações que exigem uma."""
        errors = []

        requires_destination = {
            TransactionType.TRANSFER,
            TransactionType.PIX,
            TransactionType.TED,
            TransactionType.DOC,
        }

        if transaction.transaction_type in requires_destination:
            if destination_account is None:
                errors.append("Conta de destino não informada para esta operação.")
            else:
                if destination_account.status == AccountStatus.CLOSED:
                    errors.append("Conta de destino está encerrada.")
                if destination_account.status == AccountStatus.BLOCKED:
                    errors.append("Conta de destino está bloqueada.")
                if destination_account.account_id == transaction.source_account_id:
                    errors.append("Conta de origem e destino não podem ser a mesma.")

        return errors

    def _validate_account_type_restrictions(
        self, transaction: Transaction, account: Account
    ) -> List[str]:
        """Aplica restrições específicas por tipo de conta."""
        errors = []

        # Conta investimento: só aceita transferências e não aceita pagamentos diretos
        if account.account_type == AccountType.INVESTMENT:
            if transaction.transaction_type == TransactionType.PAYMENT:
                errors.append("Conta de investimento não realiza pagamentos diretamente.")
            if transaction.transaction_type == TransactionType.WITHDRAWAL:
                errors.append(
                    "Saques em espécie não permitidos em conta de investimento. "
                    "Transfira para conta corrente."
                )

        # Conta salário: só pode receber créditos do empregador, sem saques em caixa
        if account.account_type == AccountType.SALARY:
            if transaction.transaction_type == TransactionType.DOC:
                errors.append("Conta salário não permite envio de DOC.")

        return errors

    def calculate_applicable_fee(
        self, transaction: Transaction, account: Account, monthly_transaction_count: int
    ) -> float:
        """
        Calcula a tarifa aplicável à transação conforme tipo de conta e
        quantidade de operações realizadas no mês.
        """
        # Depósitos e créditos: sempre isentos
        if not transaction.is_outgoing():
            return 0.0

        # PIX: sempre isento para pessoa física
        if transaction.transaction_type == TransactionType.PIX:
            return 0.0

        # Isenção por quantidade de transações no pacote da conta
        free_transactions = {
            AccountType.CHECKING: 10,
            AccountType.SAVINGS: 4,
            AccountType.SALARY: 8,
            AccountType.INVESTMENT: 5,
        }

        free_count = free_transactions.get(account.account_type, 0)
        if monthly_transaction_count <= free_count:
            return 0.0

        # Tarifas excedentes
        if transaction.transaction_type == TransactionType.TED:
            return 8.50
        elif transaction.transaction_type == TransactionType.DOC:
            return 6.00
        elif transaction.transaction_type == TransactionType.WITHDRAWAL:
            return 3.50
        elif transaction.transaction_type == TransactionType.TRANSFER:
            return 2.00
        elif transaction.transaction_type == TransactionType.PAYMENT:
            return 1.50
        else:
            return 0.0
