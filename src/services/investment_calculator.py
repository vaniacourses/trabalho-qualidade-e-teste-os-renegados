"""
InvestmentCalculator - Serviço de cálculo de investimentos de renda fixa.

Suporta CDB, LCI, LCA, Poupança, Tesouro Direto e fundos DI,
com simulação de imposto de renda e comparativos.
"""

from datetime import date, timedelta
from typing import Dict, List, Tuple, Optional
from enum import Enum


class InvestmentType(Enum):
    CDB = "cdb"
    LCI = "lci"
    LCA = "lca"
    SAVINGS = "poupanca"
    TREASURY_SELIC = "tesouro_selic"
    TREASURY_IPCA = "tesouro_ipca"
    TREASURY_PREFIXED = "tesouro_prefixado"
    DI_FUND = "fundo_di"


class IndexerType(Enum):
    CDI = "cdi"
    SELIC = "selic"
    IPCA = "ipca"
    PREFIXED = "prefixado"
    TR = "tr"


class InvestmentCalculatorError(Exception):
    pass


class InvestmentCalculator:
    """
    Simula e compara investimentos de renda fixa do mercado brasileiro.
    """

    # Alíquotas de IR regressivas (Tabela Regressiva)
    IR_RATES = [
        (0, 180, 0.225),       # até 6 meses: 22,5%
        (181, 360, 0.200),     # 6m a 1 ano: 20%
        (361, 720, 0.175),     # 1 a 2 anos: 17,5%
        (721, float('inf'), 0.150),  # acima de 2 anos: 15%
    ]

    # IOF regressivo (primeiros 30 dias)
    IOF_TABLE = {
        1: 0.96, 2: 0.93, 3: 0.90, 4: 0.86, 5: 0.83, 6: 0.80,
        7: 0.76, 8: 0.73, 9: 0.70, 10: 0.66, 11: 0.63, 12: 0.60,
        13: 0.56, 14: 0.53, 15: 0.50, 16: 0.46, 17: 0.43, 18: 0.40,
        19: 0.36, 20: 0.33, 21: 0.30, 22: 0.26, 23: 0.23, 24: 0.20,
        25: 0.16, 26: 0.13, 27: 0.10, 28: 0.06, 29: 0.03, 30: 0.0,
    }

    def __init__(self, cdi_annual: float = 0.1065, selic_annual: float = 0.1075,
                 ipca_annual: float = 0.0450, tr_monthly: float = 0.0):
        """
        Inicializa com as taxas de mercado.

        Args:
            cdi_annual: CDI anual (ex: 0.1065 = 10,65%)
            selic_annual: Selic anual
            ipca_annual: IPCA anual
            tr_monthly: TR mensal
        """
        self.cdi_annual = cdi_annual
        self.selic_annual = selic_annual
        self.ipca_annual = ipca_annual
        self.tr_monthly = tr_monthly
        self.cdi_daily = (1 + cdi_annual) ** (1 / 252) - 1
        self.selic_daily = (1 + selic_annual) ** (1 / 252) - 1

    def get_ir_rate(self, days: int) -> float:
        """Retorna a alíquota de IR conforme prazo em dias corridos."""
        for min_days, max_days, rate in self.IR_RATES:
            if min_days <= days <= max_days:
                return rate
        return 0.15

    def get_iof_rate(self, days: int) -> float:
        """Retorna a alíquota de IOF conforme dias corridos (primeiros 30 dias)."""
        if days >= 30:
            return 0.0
        return self.IOF_TABLE.get(days, 0.0)

    def calculate_cdb(
        self, principal: float, cdi_percentage: float, days: int
    ) -> Dict:
        """
        Simula um CDB pós-fixado indexado ao CDI.
        """
        if principal <= 0:
            raise InvestmentCalculatorError("Principal deve ser positivo.")
        if cdi_percentage <= 0:
            raise InvestmentCalculatorError("Percentual do CDI deve ser positivo.")
        if days <= 0:
            raise InvestmentCalculatorError("Prazo deve ser positivo.")

        daily_rate = self.cdi_daily * (cdi_percentage / 100)
        gross_amount = principal * (1 + daily_rate) ** days
        gross_yield = gross_amount - principal

        ir_rate = self.get_ir_rate(days)
        iof_rate = self.get_iof_rate(days)

        # IOF incide antes do IR, sobre rendimento bruto
        iof_amount = gross_yield * iof_rate
        yield_after_iof = gross_yield - iof_amount
        ir_amount = yield_after_iof * ir_rate
        net_yield = yield_after_iof - ir_amount
        net_amount = principal + net_yield

        return {
            "investment_type": "CDB",
            "principal": principal,
            "gross_amount": round(gross_amount, 2),
            "gross_yield": round(gross_yield, 2),
            "iof_amount": round(iof_amount, 2),
            "ir_rate": ir_rate,
            "ir_amount": round(ir_amount, 2),
            "net_yield": round(net_yield, 2),
            "net_amount": round(net_amount, 2),
            "net_annual_rate": round(self._to_annual_rate(net_yield / principal, days), 4),
            "days": days,
        }

    def calculate_lci_lca(
        self, principal: float, cdi_percentage: float, days: int,
        investment_type: InvestmentType = InvestmentType.LCI
    ) -> Dict:
        """
        Simula LCI ou LCA (isentos de IR para pessoa física).
        Prazo mínimo: LCI = 90 dias, LCA = 90 dias.
        """
        min_days = 90
        if days < min_days:
            raise InvestmentCalculatorError(
                f"Prazo mínimo para {investment_type.value.upper()} é {min_days} dias."
            )

        daily_rate = self.cdi_daily * (cdi_percentage / 100)
        gross_amount = principal * (1 + daily_rate) ** days
        gross_yield = gross_amount - principal

        # LCI/LCA: isentos de IR e IOF após prazo mínimo
        net_yield = gross_yield
        net_amount = principal + net_yield

        return {
            "investment_type": investment_type.value.upper(),
            "principal": principal,
            "gross_amount": round(gross_amount, 2),
            "gross_yield": round(gross_yield, 2),
            "iof_amount": 0.0,
            "ir_rate": 0.0,
            "ir_amount": 0.0,
            "net_yield": round(net_yield, 2),
            "net_amount": round(net_amount, 2),
            "net_annual_rate": round(self._to_annual_rate(net_yield / principal, days), 4),
            "days": days,
            "tax_exempt": True,
        }

    def calculate_savings(self, principal: float, days: int) -> Dict:
        """
        Simula rendimento da poupança nova (depósitos após Mai/2012).
        Regra: se Selic > 8,5% ao ano: 0,5% ao mês + TR
                se Selic <= 8,5% ao ano: 70% da Selic + TR
        """
        if self.selic_annual > 0.085:
            monthly_rate = 0.005 + self.tr_monthly
        else:
            monthly_rate = (self.selic_annual / 12) * 0.70 + self.tr_monthly

        # Poupança rende apenas a cada 30 dias
        periods = days // 30
        remainder_days = days % 30

        gross_amount = principal * (1 + monthly_rate) ** periods
        # Dias restantes não rendem (só no próximo mês)
        gross_yield = gross_amount - principal
        net_yield = gross_yield  # Poupança é isenta de IR

        return {
            "investment_type": "Poupança",
            "principal": principal,
            "gross_amount": round(gross_amount, 2),
            "gross_yield": round(gross_yield, 2),
            "iof_amount": 0.0,
            "ir_rate": 0.0,
            "ir_amount": 0.0,
            "net_yield": round(net_yield, 2),
            "net_amount": round(net_amount := principal + net_yield, 2),
            "net_annual_rate": round(self._to_annual_rate(net_yield / principal, days) if days > 0 else 0, 4),
            "days": days,
            "tax_exempt": True,
            "complete_periods": periods,
            "remaining_days_not_yielding": remainder_days,
        }

    def calculate_treasury(
        self,
        principal: float,
        days: int,
        treasury_type: InvestmentType,
        contracted_rate: Optional[float] = None,
    ) -> Dict:
        """
        Simula Tesouro Direto (Selic, IPCA+, Prefixado).
        """
        if treasury_type == InvestmentType.TREASURY_SELIC:
            # Tesouro Selic: rende ~100% da Selic
            daily_rate = self.selic_daily
            rate_description = f"Selic ({self.selic_annual*100:.2f}% a.a.)"

        elif treasury_type == InvestmentType.TREASURY_IPCA:
            if contracted_rate is None:
                raise InvestmentCalculatorError("Informe o spread do IPCA+.")
            # IPCA + spread contratado
            effective_annual = (1 + self.ipca_annual) * (1 + contracted_rate) - 1
            daily_rate = (1 + effective_annual) ** (1 / 252) - 1
            rate_description = f"IPCA ({self.ipca_annual*100:.2f}%) + {contracted_rate*100:.2f}%"

        elif treasury_type == InvestmentType.TREASURY_PREFIXED:
            if contracted_rate is None:
                raise InvestmentCalculatorError("Informe a taxa prefixada.")
            daily_rate = (1 + contracted_rate) ** (1 / 252) - 1
            rate_description = f"Prefixado {contracted_rate*100:.2f}% a.a."

        else:
            raise InvestmentCalculatorError("Tipo de Tesouro inválido.")

        gross_amount = principal * (1 + daily_rate) ** days
        gross_yield = gross_amount - principal

        ir_rate = self.get_ir_rate(days)
        iof_rate = self.get_iof_rate(days)

        iof_amount = gross_yield * iof_rate
        yield_after_iof = gross_yield - iof_amount
        ir_amount = yield_after_iof * ir_rate
        net_yield = yield_after_iof - ir_amount
        net_amount = principal + net_yield

        # Taxa de custódia B3: 0,20% ao ano sobre o valor investido
        custody_fee = principal * 0.002 * (days / 365)
        net_amount -= custody_fee
        net_yield -= custody_fee

        return {
            "investment_type": treasury_type.value,
            "rate_description": rate_description,
            "principal": principal,
            "gross_amount": round(gross_amount, 2),
            "gross_yield": round(gross_yield, 2),
            "iof_amount": round(iof_amount, 2),
            "ir_rate": ir_rate,
            "ir_amount": round(ir_amount, 2),
            "custody_fee": round(custody_fee, 2),
            "net_yield": round(net_yield, 2),
            "net_amount": round(net_amount, 2),
            "net_annual_rate": round(self._to_annual_rate(net_yield / principal, days) if principal > 0 else 0, 4),
            "days": days,
        }

    def compare_investments(
        self, principal: float, days: int, options: List[Dict]
    ) -> List[Dict]:
        """
        Compara múltiplos investimentos e os ordena por rentabilidade líquida.
        """
        results = []

        for option in options:
            inv_type = InvestmentType(option["type"])
            try:
                if inv_type == InvestmentType.CDB:
                    result = self.calculate_cdb(principal, option.get("cdi_pct", 100), days)
                elif inv_type in (InvestmentType.LCI, InvestmentType.LCA):
                    result = self.calculate_lci_lca(principal, option.get("cdi_pct", 90), days, inv_type)
                elif inv_type == InvestmentType.SAVINGS:
                    result = self.calculate_savings(principal, days)
                elif inv_type in (InvestmentType.TREASURY_SELIC, InvestmentType.TREASURY_IPCA, InvestmentType.TREASURY_PREFIXED):
                    result = self.calculate_treasury(principal, days, inv_type, option.get("rate"))
                else:
                    continue
                result["label"] = option.get("label", inv_type.value)
                results.append(result)
            except InvestmentCalculatorError:
                continue

        results.sort(key=lambda x: x["net_yield"], reverse=True)
        for i, r in enumerate(results):
            r["rank"] = i + 1

        return results

    def _to_annual_rate(self, total_return: float, days: int) -> float:
        """Converte retorno total em taxa anual efetiva."""
        if days <= 0 or total_return <= -1:
            return 0.0
        return (1 + total_return) ** (365 / days) - 1
