"""
FraudDetector - Serviço de detecção de fraudes em transações bancárias.

Analisa padrões de comportamento, localização, horários e histórico
para identificar transações suspeitas.
"""

from datetime import datetime, timedelta
from typing import List, Dict, Tuple, Optional
from src.models.transaction import Transaction, TransactionType


class FraudRisk:
    LOW = "low"
    MEDIUM = "medium"
    HIGH = "high"
    CRITICAL = "critical"


class FraudAlert:
    def __init__(self, rule: str, risk_level: str, description: str, score: int):
        self.rule = rule
        self.risk_level = risk_level
        self.description = description
        self.score = score

    def to_dict(self) -> dict:
        return {
            "rule": self.rule,
            "risk_level": self.risk_level,
            "description": self.description,
            "score": self.score,
        }


class FraudDetector:
    """
    Motor de detecção de fraudes baseado em regras e análise de padrões.
    """

    # Pontuação de risco: >= 70 bloqueia, 40-69 coloca em revisão
    BLOCK_THRESHOLD = 70
    REVIEW_THRESHOLD = 40

    # Horários de alto risco (madrugada)
    HIGH_RISK_START = 0
    HIGH_RISK_END = 5

    def analyze(
        self,
        transaction: Transaction,
        transaction_history: List[Transaction],
        customer_profile: Dict,
        device_info: Optional[Dict] = None,
    ) -> Tuple[str, List[FraudAlert], int]:
        """
        Analisa uma transação e retorna (decisão, alertas, score_de_risco).
        Decisão pode ser: 'approve', 'review', 'block'.
        """
        alerts = []
        total_score = 0

        # Executa todas as regras
        alerts.extend(self._check_unusual_amount(transaction, transaction_history, customer_profile))
        alerts.extend(self._check_velocity(transaction, transaction_history))
        alerts.extend(self._check_time_pattern(transaction, transaction_history))
        alerts.extend(self._check_geographic_anomaly(transaction, transaction_history, device_info))
        alerts.extend(self._check_new_beneficiary(transaction, transaction_history))
        alerts.extend(self._check_round_amount_pattern(transaction, transaction_history))
        alerts.extend(self._check_account_age(transaction, customer_profile))
        alerts.extend(self._check_structuring(transaction, transaction_history))

        total_score = sum(a.score for a in alerts)

        if total_score >= self.BLOCK_THRESHOLD:
            decision = "block"
        elif total_score >= self.REVIEW_THRESHOLD:
            decision = "review"
        else:
            decision = "approve"

        return decision, alerts, total_score

    def _check_unusual_amount(
        self,
        transaction: Transaction,
        history: List[Transaction],
        profile: Dict,
    ) -> List[FraudAlert]:
        """Detecta valores incomuns comparados ao histórico e perfil do cliente."""
        alerts = []
        outgoing = [t for t in history if t.is_outgoing() and t.amount > 0]

        if len(outgoing) < 5:
            return alerts  # Histórico insuficiente para comparação

        avg_amount = sum(t.amount for t in outgoing) / len(outgoing)
        max_historical = max(t.amount for t in outgoing)

        # Valor muito acima da média
        if transaction.amount > avg_amount * 5 and transaction.amount > 500:
            alerts.append(FraudAlert(
                rule="UNUSUAL_AMOUNT_AVG",
                risk_level=FraudRisk.HIGH,
                description=f"Valor R$ {transaction.amount:.2f} é {transaction.amount/avg_amount:.1f}x acima da média histórica.",
                score=25,
            ))
        elif transaction.amount > avg_amount * 3 and transaction.amount > 200:
            alerts.append(FraudAlert(
                rule="UNUSUAL_AMOUNT_AVG_MODERATE",
                risk_level=FraudRisk.MEDIUM,
                description=f"Valor acima de 3x a média histórica.",
                score=15,
            ))

        # Maior transação já realizada
        if transaction.amount > max_historical * 1.5:
            alerts.append(FraudAlert(
                rule="EXCEEDS_MAX_HISTORICAL",
                risk_level=FraudRisk.HIGH,
                description="Valor supera em 50% o maior já transacionado.",
                score=20,
            ))

        # Comparação com renda mensal
        monthly_income = profile.get("monthly_income", 0)
        if monthly_income > 0 and transaction.amount > monthly_income * 3:
            alerts.append(FraudAlert(
                rule="EXCEEDS_INCOME_MULTIPLE",
                risk_level=FraudRisk.MEDIUM,
                description="Valor superior a 3x a renda mensal do cliente.",
                score=15,
            ))

        return alerts

    def _check_velocity(
        self, transaction: Transaction, history: List[Transaction]
    ) -> List[FraudAlert]:
        """Detecta muitas transações em curto período (velocity check)."""
        alerts = []
        now = transaction.timestamp
        one_hour_ago = now - timedelta(hours=1)
        ten_min_ago = now - timedelta(minutes=10)

        recent_1h = [t for t in history if t.timestamp >= one_hour_ago and t.is_outgoing()]
        recent_10m = [t for t in history if t.timestamp >= ten_min_ago and t.is_outgoing()]

        # Muitas transações na última hora
        if len(recent_1h) >= 10:
            alerts.append(FraudAlert(
                rule="HIGH_VELOCITY_1H",
                risk_level=FraudRisk.CRITICAL,
                description=f"{len(recent_1h)} transações de saída na última hora.",
                score=40,
            ))
        elif len(recent_1h) >= 5:
            alerts.append(FraudAlert(
                rule="MODERATE_VELOCITY_1H",
                risk_level=FraudRisk.HIGH,
                description=f"{len(recent_1h)} transações de saída na última hora.",
                score=20,
            ))

        # Rajada nos últimos 10 minutos
        if len(recent_10m) >= 4:
            alerts.append(FraudAlert(
                rule="BURST_10MIN",
                risk_level=FraudRisk.CRITICAL,
                description=f"{len(recent_10m)} transações nos últimos 10 minutos.",
                score=35,
            ))
        elif len(recent_10m) >= 2:
            alerts.append(FraudAlert(
                rule="BURST_10MIN_LOW",
                risk_level=FraudRisk.MEDIUM,
                description=f"{len(recent_10m)} transações nos últimos 10 minutos.",
                score=10,
            ))

        return alerts

    def _check_time_pattern(
        self, transaction: Transaction, history: List[Transaction]
    ) -> List[FraudAlert]:
        """Detecta transações em horários incomuns para o perfil do cliente."""
        alerts = []
        hour = transaction.timestamp.hour

        # Horário de madrugada
        if self.HIGH_RISK_START <= hour < self.HIGH_RISK_END:
            # Verifica se cliente tem histórico de transações nesse horário
            historical_night = [
                t for t in history
                if self.HIGH_RISK_START <= t.timestamp.hour < self.HIGH_RISK_END
            ]
            if len(historical_night) == 0 and transaction.is_outgoing():
                alerts.append(FraudAlert(
                    rule="UNUSUAL_HOUR_NO_HISTORY",
                    risk_level=FraudRisk.HIGH,
                    description="Transação de saída na madrugada sem histórico neste horário.",
                    score=25,
                ))
            elif transaction.amount > 1000 and transaction.is_outgoing():
                alerts.append(FraudAlert(
                    rule="HIGH_AMOUNT_NIGHT",
                    risk_level=FraudRisk.MEDIUM,
                    description="Transação de alto valor na madrugada.",
                    score=15,
                ))

        return alerts

    def _check_geographic_anomaly(
        self,
        transaction: Transaction,
        history: List[Transaction],
        device_info: Optional[Dict],
    ) -> List[FraudAlert]:
        """Detecta anomalias geográficas (impossible travel, novo dispositivo)."""
        alerts = []

        if device_info is None:
            return alerts

        current_ip = device_info.get("ip_address", "")
        current_country = device_info.get("country", "BR")
        device_id = device_info.get("device_id", "")
        is_vpn = device_info.get("is_vpn", False)

        # Transação via VPN
        if is_vpn and transaction.is_outgoing() and transaction.amount > 100:
            alerts.append(FraudAlert(
                rule="VPN_DETECTED",
                risk_level=FraudRisk.MEDIUM,
                description="Transação originada via VPN/proxy.",
                score=15,
            ))

        # País diferente do habitual
        if current_country != "BR":
            alerts.append(FraudAlert(
                rule="FOREIGN_COUNTRY",
                risk_level=FraudRisk.HIGH,
                description=f"Acesso originado de {current_country}.",
                score=30,
            ))

        # Dispositivo nunca usado antes
        historical_devices = {
            t.metadata.get("device_id") for t in history if t.metadata.get("device_id")
        }
        if device_id and device_id not in historical_devices and transaction.amount > 500:
            alerts.append(FraudAlert(
                rule="NEW_DEVICE_HIGH_AMOUNT",
                risk_level=FraudRisk.MEDIUM,
                description="Dispositivo nunca utilizado realizando transação de alto valor.",
                score=20,
            ))

        return alerts

    def _check_new_beneficiary(
        self, transaction: Transaction, history: List[Transaction]
    ) -> List[FraudAlert]:
        """Detecta transferências para contas nunca usadas antes."""
        alerts = []

        if not transaction.destination_account_id:
            return alerts

        known_destinations = {
            t.destination_account_id for t in history
            if t.destination_account_id
        }

        if transaction.destination_account_id not in known_destinations:
            if transaction.amount > 5000:
                alerts.append(FraudAlert(
                    rule="NEW_BENEFICIARY_HIGH_AMOUNT",
                    risk_level=FraudRisk.HIGH,
                    description="Alto valor enviado para destinatário nunca utilizado.",
                    score=25,
                ))
            elif transaction.amount > 1000:
                alerts.append(FraudAlert(
                    rule="NEW_BENEFICIARY_MEDIUM_AMOUNT",
                    risk_level=FraudRisk.LOW,
                    description="Destinatário novo para valor moderado.",
                    score=8,
                ))

        return alerts

    def _check_round_amount_pattern(
        self, transaction: Transaction, history: List[Transaction]
    ) -> List[FraudAlert]:
        """
        Detecta padrão de valores redondos repetidos (possível money structuring
        ou uso de bot de saque).
        """
        alerts = []
        recent = [
            t for t in history
            if t.timestamp >= transaction.timestamp - timedelta(hours=24)
            and t.is_outgoing()
        ]

        def is_round(amount: float) -> bool:
            return amount % 100 == 0 or amount % 50 == 0

        round_count = sum(1 for t in recent if is_round(t.amount))

        if len(recent) >= 3 and round_count == len(recent) and is_round(transaction.amount):
            alerts.append(FraudAlert(
                rule="ROUND_AMOUNT_PATTERN",
                risk_level=FraudRisk.MEDIUM,
                description="Padrão de valores redondos repetidos detectado.",
                score=15,
            ))

        return alerts

    def _check_account_age(
        self, transaction: Transaction, profile: Dict
    ) -> List[FraudAlert]:
        """Aplica regras extras para contas recém-criadas."""
        alerts = []
        account_created = profile.get("account_created_at")

        if account_created is None:
            return alerts

        if isinstance(account_created, str):
            account_created = datetime.fromisoformat(account_created)

        account_age_days = (transaction.timestamp - account_created).days

        if account_age_days < 7 and transaction.is_outgoing() and transaction.amount > 1000:
            alerts.append(FraudAlert(
                rule="NEW_ACCOUNT_HIGH_OUTGOING",
                risk_level=FraudRisk.CRITICAL,
                description=f"Conta com {account_age_days} dias realizando saída de alto valor.",
                score=40,
            ))
        elif account_age_days < 30 and transaction.is_outgoing() and transaction.amount > 5000:
            alerts.append(FraudAlert(
                rule="YOUNG_ACCOUNT_HIGH_AMOUNT",
                risk_level=FraudRisk.HIGH,
                description="Conta com menos de 30 dias com transação de alto valor.",
                score=25,
            ))

        return alerts

    def _check_structuring(
        self, transaction: Transaction, history: List[Transaction]
    ) -> List[FraudAlert]:
        """
        Detecta fracionamento de valores para evitar limites (structuring).
        Clássico sinal de lavagem de dinheiro.
        """
        alerts = []
        last_24h = [
            t for t in history
            if t.timestamp >= transaction.timestamp - timedelta(hours=24)
            and t.is_outgoing()
            and t.transaction_type == transaction.transaction_type
        ]

        total_24h = sum(t.amount for t in last_24h) + transaction.amount

        # Soma de transações próxima a limites reportáveis
        if 8000 <= total_24h <= 10000 and len(last_24h) >= 3:
            alerts.append(FraudAlert(
                rule="STRUCTURING_COAF_THRESHOLD",
                risk_level=FraudRisk.CRITICAL,
                description=(
                    f"Soma de {len(last_24h)+1} transações em 24h (R$ {total_24h:.2f}) "
                    f"próxima ao limite de reporte COAF."
                ),
                score=45,
            ))
        elif total_24h > 5000 and len(last_24h) >= 5:
            alerts.append(FraudAlert(
                rule="STRUCTURING_PATTERN",
                risk_level=FraudRisk.HIGH,
                description="Múltiplas transações de saída do mesmo tipo em 24h.",
                score=20,
            ))

        return alerts
