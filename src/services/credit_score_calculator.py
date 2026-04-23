"""
CreditScoreCalculator - Serviço de cálculo e atualização de score de crédito.

Implementa um modelo simplificado similar ao Serasa/SPC para calcular
o score de crédito de clientes baseado em múltiplos fatores.
"""

from datetime import datetime, date, timedelta
from typing import List, Dict, Tuple, Optional


class ScoreFactor:
    def __init__(self, name: str, weight: float, score: int, description: str):
        self.name = name
        self.weight = weight
        self.score = score
        self.description = description

    def to_dict(self) -> dict:
        return {
            "name": self.name,
            "weight": self.weight,
            "score": self.score,
            "description": self.description,
        }


class CreditScoreCalculator:
    """
    Calcula o score de crédito de um cliente baseado em seu histórico financeiro.
    Score varia de 0 a 1000 (similar ao modelo Serasa Experian).

    """

    # Faixas de score e classificação
    SCORE_RANGES = [
        (900, 1000, "Excelente", "Risco muito baixo"),
        (800, 899, "Bom", "Risco baixo"),
        (700, 799, "Regular", "Risco médio-baixo"),
        (600, 699, "Abaixo do regular", "Risco médio"),
        (500, 599, "Ruim", "Risco médio-alto"),
        (400, 499, "Muito ruim", "Risco alto"),
        (0, 399, "Péssimo", "Risco muito alto"),
    ]

    def calculate_score(self, customer_data: Dict) -> Tuple[int, List[ScoreFactor], str]:
        """
        Calcula o score de crédito completo.
        Retorna (score, fatores_analisados, classificação).
        """
        factors = []

        factors.append(self._evaluate_payment_history(customer_data.get("payment_history", {})))
        factors.append(self._evaluate_debt_utilization(customer_data.get("debt_info", {})))
        factors.append(self._evaluate_credit_age(customer_data.get("credit_history", {})))
        factors.append(self._evaluate_credit_mix(customer_data.get("credit_mix", {})))
        factors.append(self._evaluate_new_credit_inquiries(customer_data.get("inquiries", {})))
        factors.append(self._evaluate_income_stability(customer_data.get("income_info", {})))
        factors.append(self._evaluate_negative_records(customer_data.get("negative_records", {})))

        # Calcula score ponderado (0-1000)
        total_weight = sum(f.weight for f in factors)
        weighted_score = sum(f.score * f.weight for f in factors) / total_weight if total_weight > 0 else 500
        final_score = max(0, min(1000, round(weighted_score)))

        classification = self._classify_score(final_score)
        return final_score, factors, classification

    def _evaluate_payment_history(self, payment_data: Dict) -> ScoreFactor:
        """
        Histórico de pagamentos: 35% do score.
        Verifica atrasos, inadimplências e pagamentos em dia.
        """
        weight = 0.35
        total_payments = payment_data.get("total_payments", 0)
        on_time = payment_data.get("on_time", 0)
        late_30 = payment_data.get("late_30_days", 0)
        late_60 = payment_data.get("late_60_days", 0)
        late_90_plus = payment_data.get("late_90_plus_days", 0)
        defaults = payment_data.get("defaults", 0)

        if total_payments == 0:
            return ScoreFactor("payment_history", weight, 500, "Sem histórico de pagamentos.")

        on_time_rate = on_time / total_payments if total_payments > 0 else 0

        if defaults > 0:
            base_score = 100
            score = max(0, base_score - (defaults * 50))
        elif late_90_plus > 0:
            base_score = 300
            score = max(100, base_score - (late_90_plus * 40))
        elif late_60 > 0:
            base_score = 500
            score = max(200, base_score - (late_60 * 30))
        elif late_30 > 0:
            base_score = 650
            score = max(400, base_score - (late_30 * 20))
        elif on_time_rate >= 0.98:
            score = 950
        elif on_time_rate >= 0.95:
            score = 900
        elif on_time_rate >= 0.90:
            score = 800
        else:
            score = 700

        desc = f"Taxa de pagamentos em dia: {on_time_rate*100:.1f}%."
        return ScoreFactor("payment_history", weight, score, desc)

    def _evaluate_debt_utilization(self, debt_data: Dict) -> ScoreFactor:
        """
        Taxa de utilização de crédito: 30% do score.
        Quanto do limite disponível está sendo usado.
        """
        weight = 0.30
        total_limit = debt_data.get("total_credit_limit", 0)
        total_used = debt_data.get("total_used", 0)

        if total_limit == 0:
            return ScoreFactor("debt_utilization", weight, 500, "Nenhum crédito disponível.")

        utilization = total_used / total_limit

        if utilization <= 0.10:
            score = 950
            desc = f"Excelente utilização: {utilization*100:.0f}% do limite."
        elif utilization <= 0.20:
            score = 900
            desc = f"Boa utilização: {utilization*100:.0f}% do limite."
        elif utilization <= 0.30:
            score = 800
            desc = f"Utilização moderada: {utilization*100:.0f}% do limite."
        elif utilization <= 0.50:
            score = 650
            desc = f"Utilização elevada: {utilization*100:.0f}% do limite."
        elif utilization <= 0.70:
            score = 450
            desc = f"Utilização muito elevada: {utilization*100:.0f}% do limite."
        elif utilization <= 0.90:
            score = 250
            desc = f"Utilização crítica: {utilization*100:.0f}% do limite."
        else:
            score = 100
            desc = f"Limite praticamente esgotado: {utilization*100:.0f}% utilizado."

        return ScoreFactor("debt_utilization", weight, score, desc)

    def _evaluate_credit_age(self, history_data: Dict) -> ScoreFactor:
        """
        Idade do histórico de crédito: 15% do score.
        """
        weight = 0.15
        oldest_account_months = history_data.get("oldest_account_months", 0)
        average_account_age_months = history_data.get("average_account_age_months", 0)

        if oldest_account_months == 0:
            return ScoreFactor("credit_age", weight, 400, "Sem histórico de crédito.")

        # Pontuação base pelo tempo do crédito mais antigo
        if oldest_account_months >= 120:  # 10+ anos
            base = 950
        elif oldest_account_months >= 84:  # 7-10 anos
            base = 850
        elif oldest_account_months >= 60:  # 5-7 anos
            base = 750
        elif oldest_account_months >= 36:  # 3-5 anos
            base = 650
        elif oldest_account_months >= 24:  # 2-3 anos
            base = 550
        elif oldest_account_months >= 12:  # 1-2 anos
            base = 450
        else:
            base = 350

        # Ajuste pela idade média
        if average_account_age_months >= oldest_account_months * 0.7:
            age_bonus = 50
        elif average_account_age_months >= oldest_account_months * 0.5:
            age_bonus = 20
        else:
            age_bonus = 0

        score = min(1000, base + age_bonus)
        desc = f"Conta mais antiga: {oldest_account_months} meses."
        return ScoreFactor("credit_age", weight, score, desc)

    def _evaluate_credit_mix(self, mix_data: Dict) -> ScoreFactor:
        """
        Variedade de crédito: 10% do score.
        """
        weight = 0.10
        has_credit_card = mix_data.get("credit_card", False)
        has_loan = mix_data.get("personal_loan", False)
        has_mortgage = mix_data.get("mortgage", False)
        has_auto = mix_data.get("auto_loan", False)
        has_payroll = mix_data.get("payroll_deductible", False)

        product_count = sum([has_credit_card, has_loan, has_mortgage, has_auto, has_payroll])

        if product_count == 0:
            score = 400
        elif product_count == 1:
            score = 600
        elif product_count == 2:
            score = 750
        elif product_count == 3:
            score = 850
        else:
            score = 950

        # Bônus por ter financiamento imobiliário (sinal de estabilidade)
        if has_mortgage:
            score = min(1000, score + 30)

        desc = f"{product_count} tipo(s) de produto de crédito ativo(s)."
        return ScoreFactor("credit_mix", weight, score, desc)

    def _evaluate_new_credit_inquiries(self, inquiry_data: Dict) -> ScoreFactor:
        """
        Novas consultas de crédito nos últimos 12 meses: 10% do score.
        Muitas consultas indicam busca desesperada por crédito.
        """
        weight = 0.10
        inquiries_12m = inquiry_data.get("last_12_months", 0)
        inquiries_3m = inquiry_data.get("last_3_months", 0)

        if inquiries_3m >= 6:
            score = 200
            desc = f"Muitas consultas recentes: {inquiries_3m} nos últimos 3 meses."
        elif inquiries_3m >= 3:
            score = 400
            desc = f"Consultas elevadas nos últimos 3 meses: {inquiries_3m}."
        elif inquiries_12m >= 10:
            score = 500
            desc = f"Muitas consultas no ano: {inquiries_12m}."
        elif inquiries_12m >= 6:
            score = 650
            desc = f"Consultas moderadas no ano: {inquiries_12m}."
        elif inquiries_12m >= 3:
            score = 800
            desc = f"Poucas consultas: {inquiries_12m} no último ano."
        elif inquiries_12m == 0:
            score = 1000
            desc = "Nenhuma consulta de crédito no último ano."
        else:
            score = 900
            desc = f"Consultas controladas: {inquiries_12m} no último ano."

        return ScoreFactor("new_credit", weight, score, desc)

    def _evaluate_income_stability(self, income_data: Dict) -> ScoreFactor:
        """Estabilidade de renda (fator complementar não presente em todos os modelos)."""
        weight = 0.05
        employment_years = income_data.get("employment_years", 0)
        income_type = income_data.get("income_type", "informal")
        income_variation = income_data.get("monthly_variation_pct", 50)

        if income_type == "public_servant":
            base = 950
        elif income_type == "clt" and employment_years >= 3:
            base = 850
        elif income_type == "clt":
            base = 750
        elif income_type == "self_employed" and employment_years >= 5:
            base = 700
        elif income_type == "self_employed":
            base = 600
        elif income_type == "retired":
            base = 900
        else:
            base = 500

        # Penalidade por alta variação de renda
        if income_variation > 50:
            variation_penalty = 100
        elif income_variation > 30:
            variation_penalty = 50
        else:
            variation_penalty = 0

        score = max(0, base - variation_penalty)
        desc = f"Tipo de renda: {income_type}, {employment_years} ano(s) no emprego atual."
        return ScoreFactor("income_stability", weight, score, desc)

    def _evaluate_negative_records(self, negative_data: Dict) -> ScoreFactor:
        """Registros negativos ativos (SPC/Serasa)."""
        weight = 0.05  # Já coberto indiretamente por payment_history, mas com peso extra
        active_negatives = negative_data.get("active_negatives", 0)
        total_negative_value = negative_data.get("total_negative_value", 0.0)
        oldest_negative_months = negative_data.get("oldest_negative_months", 0)

        if active_negatives == 0:
            return ScoreFactor("negative_records", weight, 1000, "Nenhuma pendência ativa.")

        if total_negative_value > 10000:
            score = 50
        elif total_negative_value > 5000:
            score = 100
        elif total_negative_value > 1000:
            score = 150
        else:
            score = 200

        # Dívidas mais antigas são menos prejudiciais
        if oldest_negative_months > 36:
            score = min(400, score + 100)
        elif oldest_negative_months > 12:
            score = min(300, score + 50)

        desc = f"{active_negatives} pendência(s) ativa(s), total: R$ {total_negative_value:.2f}."
        return ScoreFactor("negative_records", weight, score, desc)

    def _classify_score(self, score: int) -> str:
        """Retorna a classificação textual do score."""
        for min_score, max_score, label, risk in self.SCORE_RANGES:
            if min_score <= score <= max_score:
                return f"{label} ({risk})"
        return "Indefinido"

    def estimate_score_improvement(
        self, current_data: Dict, improvement_actions: List[str]
    ) -> Dict:
        """
        Estima o ganho de score para ações de melhoria.
        Útil para orientar o cliente sobre o que fazer.
        """
        estimates = {}
        current_score, _, _ = self.calculate_score(current_data)

        for action in improvement_actions:
            simulated_data = current_data.copy()

            if action == "pay_all_debts":
                simulated_data["negative_records"] = {"active_negatives": 0, "total_negative_value": 0}
                simulated_data["payment_history"] = {**current_data.get("payment_history", {}), "defaults": 0}
            elif action == "reduce_utilization_30":
                debt_info = simulated_data.get("debt_info", {}).copy()
                limit = debt_info.get("total_credit_limit", 0)
                debt_info["total_used"] = min(debt_info.get("total_used", 0), limit * 0.30)
                simulated_data["debt_info"] = debt_info
            elif action == "no_new_inquiries_12m":
                simulated_data["inquiries"] = {"last_12_months": 0, "last_3_months": 0}
            elif action == "keep_accounts_open":
                history = simulated_data.get("credit_history", {}).copy()
                history["oldest_account_months"] = history.get("oldest_account_months", 0) + 12
                simulated_data["credit_history"] = history

            new_score, _, _ = self.calculate_score(simulated_data)
            estimates[action] = {
                "estimated_gain": new_score - current_score,
                "estimated_score": new_score,
            }

        return estimates
