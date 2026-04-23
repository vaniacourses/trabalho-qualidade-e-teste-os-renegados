"""
LoanCalculator - Serviço de cálculo de empréstimos.

Suporta múltiplos sistemas de amortização (PRICE, SAC, SAM),
tipos de empréstimo e cálculo de juros compostos.
"""

from datetime import date, timedelta
from typing import List, Dict, Tuple, Optional
from src.models.loan import Loan, LoanType, LoanInstallment, AmortizationSystem


class LoanCalculatorError(Exception):
    pass


class LoanCalculator:
    """
    Calcula empréstimos bancários com suporte a múltiplos sistemas
    de amortização e tipos de produto.

    """

    # Taxas base mensais por tipo de empréstimo (ao mês)
    BASE_RATES = {
        LoanType.PERSONAL: 0.0399,
        LoanType.MORTGAGE: 0.0089,
        LoanType.AUTO: 0.0189,
        LoanType.STUDENT: 0.0129,
        LoanType.BUSINESS: 0.0259,
        LoanType.PAYROLL_DEDUCTIBLE: 0.0179,
    }

    # Limites de prazo (meses) por tipo
    TERM_LIMITS = {
        LoanType.PERSONAL: (6, 60),
        LoanType.MORTGAGE: (60, 360),
        LoanType.AUTO: (12, 72),
        LoanType.STUDENT: (12, 120),
        LoanType.BUSINESS: (12, 84),
        LoanType.PAYROLL_DEDUCTIBLE: (12, 96),
    }

    # Valor máximo por tipo (como múltiplo da renda mensal)
    MAX_INCOME_MULTIPLIER = {
        LoanType.PERSONAL: 10,
        LoanType.MORTGAGE: 80,
        LoanType.AUTO: 30,
        LoanType.STUDENT: 20,
        LoanType.BUSINESS: 40,
        LoanType.PAYROLL_DEDUCTIBLE: 24,
    }

    def calculate_interest_rate(
        self,
        loan_type: LoanType,
        credit_score: int,
        term_months: int,
        monthly_income: float,
        amount: float,
    ) -> float:
        """
        Calcula a taxa de juros personalizada com base no perfil do cliente.
        Leva em conta score de crédito, prazo, renda e valor solicitado.
        """
        base_rate = self.BASE_RATES[loan_type]

        # Ajuste por score de crédito
        if credit_score >= 900:
            score_adjustment = -0.005
        elif credit_score >= 800:
            score_adjustment = -0.003
        elif credit_score >= 700:
            score_adjustment = 0.0
        elif credit_score >= 600:
            score_adjustment = 0.005
        elif credit_score >= 500:
            score_adjustment = 0.010
        elif credit_score >= 400:
            score_adjustment = 0.020
        else:
            score_adjustment = 0.035

        # Ajuste por prazo
        min_term, max_term = self.TERM_LIMITS[loan_type]
        term_ratio = (term_months - min_term) / (max_term - min_term) if max_term != min_term else 0
        if term_ratio > 0.75:
            term_adjustment = 0.005
        elif term_ratio > 0.5:
            term_adjustment = 0.002
        else:
            term_adjustment = 0.0

        # Ajuste por comprometimento de renda
        income_ratio = amount / (monthly_income * 12) if monthly_income > 0 else 1.0
        if income_ratio > 0.5:
            income_adjustment = 0.008
        elif income_ratio > 0.3:
            income_adjustment = 0.004
        else:
            income_adjustment = 0.0

        # Tipos com taxa fixa não recebem ajuste de prazo
        if loan_type in (LoanType.PAYROLL_DEDUCTIBLE, LoanType.STUDENT):
            term_adjustment = 0.0

        final_rate = base_rate + score_adjustment + term_adjustment + income_adjustment
        return max(final_rate, 0.005)  # taxa mínima de 0.5% ao mês

    def validate_loan_request(
        self,
        loan_type: LoanType,
        amount: float,
        term_months: int,
        monthly_income: float,
        credit_score: int,
        age: int,
    ) -> Tuple[bool, List[str]]:
        """
        Valida um pedido de empréstimo contra as regras de negócio.
        Retorna (aprovado, lista_de_erros).
        """
        errors = []

        # Validações de valor
        if amount <= 0:
            errors.append("Valor do empréstimo deve ser positivo.")

        if monthly_income <= 0:
            errors.append("Renda mensal deve ser positiva.")

        # Validação de prazo
        if loan_type in self.TERM_LIMITS:
            min_term, max_term = self.TERM_LIMITS[loan_type]
            if term_months < min_term:
                errors.append(f"Prazo mínimo para {loan_type.value} é {min_term} meses.")
            if term_months > max_term:
                errors.append(f"Prazo máximo para {loan_type.value} é {max_term} meses.")

        # Validação de score mínimo por produto
        if loan_type == LoanType.MORTGAGE and credit_score < 600:
            errors.append("Score mínimo para financiamento imobiliário é 600.")
        elif loan_type == LoanType.BUSINESS and credit_score < 550:
            errors.append("Score mínimo para empréstimo empresarial é 550.")
        elif credit_score < 300:
            errors.append("Score de crédito insuficiente para qualquer produto.")

        # Validação de idade
        if age < 18:
            errors.append("Cliente deve ter pelo menos 18 anos.")
        if loan_type == LoanType.MORTGAGE and age > 70:
            errors.append("Financiamento imobiliário não disponível para clientes acima de 70 anos.")
        if loan_type == LoanType.PAYROLL_DEDUCTIBLE and age < 21:
            errors.append("Crédito consignado disponível apenas para maiores de 21 anos.")

        # Validação de comprometimento de renda
        if monthly_income > 0 and amount > 0:
            max_multiplier = self.MAX_INCOME_MULTIPLIER.get(loan_type, 10)
            if amount > monthly_income * max_multiplier:
                errors.append(
                    f"Valor solicitado excede o limite de {max_multiplier}x a renda mensal "
                    f"para {loan_type.value}."
                )

        return len(errors) == 0, errors

    def calculate_price_installments(
        self, principal: float, monthly_rate: float, term_months: int, start_date: date
    ) -> List[LoanInstallment]:
        """
        Calcula a tabela de amortização pelo sistema PRICE (parcelas iguais).
        PMT = PV * [i*(1+i)^n] / [(1+i)^n - 1]
        """
        if monthly_rate == 0:
            installment_amount = principal / term_months
        else:
            factor = (1 + monthly_rate) ** term_months
            installment_amount = principal * (monthly_rate * factor) / (factor - 1)

        installments = []
        balance = principal

        for i in range(1, term_months + 1):
            interest = balance * monthly_rate
            principal_payment = installment_amount - interest
            balance -= principal_payment
            due_date = start_date + timedelta(days=30 * i)

            installments.append(LoanInstallment(
                installment_number=i,
                due_date=due_date,
                principal=principal_payment,
                interest=interest,
                total=installment_amount,
            ))

        return installments

    def calculate_sac_installments(
        self, principal: float, monthly_rate: float, term_months: int, start_date: date
    ) -> List[LoanInstallment]:
        """
        Calcula a tabela de amortização pelo sistema SAC (amortização constante).
        A amortização é fixa; os juros diminuem conforme o saldo devedor cai.
        """
        principal_payment = principal / term_months
        installments = []
        balance = principal

        for i in range(1, term_months + 1):
            interest = balance * monthly_rate
            total = principal_payment + interest
            balance -= principal_payment
            due_date = start_date + timedelta(days=30 * i)

            installments.append(LoanInstallment(
                installment_number=i,
                due_date=due_date,
                principal=principal_payment,
                interest=interest,
                total=total,
            ))

        return installments

    def calculate_sam_installments(
        self, principal: float, monthly_rate: float, term_months: int, start_date: date
    ) -> List[LoanInstallment]:
        """
        Sistema SAM: média aritmética entre PRICE e SAC.
        """
        price = self.calculate_price_installments(principal, monthly_rate, term_months, start_date)
        sac = self.calculate_sac_installments(principal, monthly_rate, term_months, start_date)
        installments = []

        for p, s in zip(price, sac):
            total = (p.total + s.total) / 2
            interest = (p.interest + s.interest) / 2
            principal_payment = total - interest
            installments.append(LoanInstallment(
                installment_number=p.installment_number,
                due_date=p.due_date,
                principal=principal_payment,
                interest=interest,
                total=total,
            ))

        return installments

    def generate_installments(
        self,
        principal: float,
        monthly_rate: float,
        term_months: int,
        amortization_system: AmortizationSystem,
        start_date: Optional[date] = None,
    ) -> List[LoanInstallment]:
        """Gera parcelas conforme o sistema de amortização escolhido."""
        if start_date is None:
            start_date = date.today()

        if principal <= 0:
            raise LoanCalculatorError("Principal deve ser maior que zero.")
        if term_months <= 0:
            raise LoanCalculatorError("Prazo deve ser maior que zero.")
        if monthly_rate < 0:
            raise LoanCalculatorError("Taxa de juros não pode ser negativa.")

        if amortization_system == AmortizationSystem.PRICE:
            return self.calculate_price_installments(principal, monthly_rate, term_months, start_date)
        elif amortization_system == AmortizationSystem.SAC:
            return self.calculate_sac_installments(principal, monthly_rate, term_months, start_date)
        elif amortization_system == AmortizationSystem.SAM:
            return self.calculate_sam_installments(principal, monthly_rate, term_months, start_date)
        else:
            raise LoanCalculatorError(f"Sistema de amortização desconhecido: {amortization_system}")

    def calculate_early_payoff(
        self, loan: Loan, payoff_date: date, discount_rate: float = 0.005
    ) -> Dict:
        """
        Calcula o valor para quitação antecipada.
        Aplica desconto sobre os juros futuros conforme política do banco.
        """
        if loan.status.value not in ("active",):
            raise LoanCalculatorError("Quitação antecipada disponível apenas para empréstimos ativos.")

        remaining = [i for i in loan.installments if not i.paid]
        if not remaining:
            return {"payoff_amount": 0.0, "savings": 0.0, "remaining_installments": 0}

        total_remaining = sum(i.total for i in remaining)
        future_interest = sum(i.interest for i in remaining)

        # Desconto maior para quitação mais rápida
        remaining_count = len(remaining)
        if remaining_count > 24:
            applied_discount = discount_rate * 1.5
        elif remaining_count > 12:
            applied_discount = discount_rate * 1.2
        else:
            applied_discount = discount_rate

        discount_amount = future_interest * applied_discount * remaining_count
        payoff_amount = total_remaining - discount_amount

        return {
            "payoff_amount": round(payoff_amount, 2),
            "total_without_discount": round(total_remaining, 2),
            "discount_amount": round(discount_amount, 2),
            "savings": round(discount_amount, 2),
            "remaining_installments": remaining_count,
        }

    def calculate_effective_annual_rate(self, monthly_rate: float) -> float:
        """Converte taxa mensal para taxa efetiva anual (CET)."""
        return (1 + monthly_rate) ** 12 - 1
