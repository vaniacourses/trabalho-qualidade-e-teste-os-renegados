# Banking System — Sistema Bancário para Testes

Sistema bancário desenvolvido em Python com Flask, projetado para aplicação das técnicas de **Qualidade e Teste de Software**.

---

## Estrutura do Projeto

```
banking_system/
├── src/
│   ├── models/
│   │   ├── account.py         # Modelo de conta bancária
│   │   ├── customer.py        # Modelo de cliente
│   │   ├── transaction.py     # Modelo de transação
│   │   └── loan.py            # Modelo de empréstimo e parcelas
│   ├── services/              # ← Classes com alta complexidade (alvos dos testes)
│   │   ├── loan_calculator.py         # Cálculo de empréstimos (PRICE, SAC, SAM)
│   │   ├── transaction_validator.py   # Validação de transações bancárias
│   │   ├── fraud_detector.py          # Detecção de fraudes por regras
│   │   ├── credit_score_calculator.py # Cálculo de score de crédito
│   │   ├── investment_calculator.py   # Simulação de renda fixa
│   │   └── account_manager.py         # Gerenciamento de contas
│   └── api/
│       └── routes.py          # API REST (Flask)
├── tests/
│   ├── unit/
│   │   ├── test_loan_calculator.py
│   │   ├── test_transaction_validator.py
│   │   ├── test_fraud_detector.py
│   │   └── test_investment_and_account.py
│   └── integration/
│       └── test_integration.py
├── requirements.txt
├── pytest.ini
└── conftest.py
```

---

## Classes de Serviço — Mapeamento por Membro do Grupo

| Membro | Classe Principal | Arquivo |
|--------|-----------------|---------|
| Membro 1 | `LoanCalculator` | `src/services/loan_calculator.py` |
| Membro 2 | `TransactionValidator` | `src/services/transaction_validator.py` |
| Membro 3 | `FraudDetector` | `src/services/fraud_detector.py` |
| Membro 4 | `CreditScoreCalculator` | `src/services/credit_score_calculator.py` |
| Membro 5 | `InvestmentCalculator` | `src/services/investment_calculator.py` |
| Membro 6 | `AccountManager` | `src/services/account_manager.py` |

---

## Funcionalidades do Sistema

### LoanCalculator
- Cálculo de taxa de juros personalizada por perfil de cliente
- Validação de pedidos de empréstimo (score, prazo, renda, idade)
- Geração de tabelas de amortização: PRICE, SAC e SAM
- Cálculo de quitação antecipada com desconto
- Conversão de taxa mensal para CET (taxa efetiva anual)

### TransactionValidator
- Validação de campos básicos (valor, ID)
- Verificação de status da conta (bloqueada, encerrada, pendente)
- Controle de horários de operação (DOC/TED não processados em fins de semana)
- Validação de saldo e limite de crédito
- Controle de limites diários por tipo e conta
- Validação de conta de destino
- Restrições por tipo de conta
- Cálculo de tarifas por pacote

### FraudDetector
- Detecção de valor incomum vs. histórico e renda
- Velocity check (muitas transações em pouco tempo)
- Padrões de horário incomum
- Anomalia geográfica (país estrangeiro, VPN, novo dispositivo)
- Novo beneficiário com alto valor
- Padrão de valores redondos (possível bot)
- Conta recém-criada com saídas altas
- Fracionamento (structuring) próximo ao limite COAF

### CreditScoreCalculator
- Histórico de pagamentos (35%)
- Utilização de crédito (30%)
- Idade do histórico (15%)
- Mix de produtos (10%)
- Novas consultas (10%)
- Estabilidade de renda e registros negativos
- Simulação de melhoria de score por ações corretivas

### InvestmentCalculator
- CDB pós-fixado indexado ao CDI
- LCI/LCA isentos de IR
- Poupança (regra nova: 0,5% a.m. + TR ou 70% da Selic + TR)
- Tesouro Direto: Selic, IPCA+, Prefixado
- Tributação completa: IOF regressivo + IR regressivo
- Taxa de custódia B3
- Comparativo ranqueado de investimentos

### AccountManager
- Abertura de conta com validação de elegibilidade
- Cálculo de limite de crédito inicial por score
- Encerramento com verificação de pendências
- Bloqueio/desbloqueio de conta
- Aplicação de tarifa de manutenção mensal (com isenções)
- Reavaliação de limite de crédito
- Geração de extrato por período

---

## Instalação e Execução

```bash
# Instalar dependências
pip install -r requirements.txt

# Executar todos os testes
python -m pytest tests/ -v

# Executar com cobertura
python -m pytest tests/ --cov=src --cov-report=term-missing

# Executar apenas testes unitários
python -m pytest tests/unit/ -v

# Executar apenas testes de integração
python -m pytest tests/integration/ -v

# Iniciar a API
python src/api/routes.py
```

---

## Exemplos de Uso da API

### Simular empréstimo
```bash
curl -X POST http://localhost:5000/loans/simulate \
  -H "Content-Type: application/json" \
  -d '{
    "loan_type": "personal",
    "amount": 20000,
    "term_months": 24,
    "credit_score": 700,
    "monthly_income": 8000,
    "age": 35,
    "amortization_system": "price"
  }'
```

### Calcular score de crédito
```bash
curl -X POST http://localhost:5000/credit/score \
  -H "Content-Type: application/json" \
  -d '{
    "payment_history": {"total_payments": 60, "on_time": 58, "late_30_days": 2,
                        "late_60_days": 0, "late_90_plus_days": 0, "defaults": 0},
    "debt_info": {"total_credit_limit": 15000, "total_used": 4000},
    "credit_history": {"oldest_account_months": 72, "average_account_age_months": 48},
    "credit_mix": {"credit_card": true, "personal_loan": true, "mortgage": false,
                   "auto_loan": false, "payroll_deductible": false},
    "inquiries": {"last_12_months": 3, "last_3_months": 1},
    "income_info": {"employment_years": 4, "income_type": "clt", "monthly_variation_pct": 5},
    "negative_records": {"active_negatives": 0, "total_negative_value": 0}
  }'
```

### Comparar investimentos
```bash
curl -X POST http://localhost:5000/investments/compare \
  -H "Content-Type: application/json" \
  -d '{
    "principal": 10000,
    "days": 365,
    "options": [
      {"type": "cdb", "cdi_pct": 110, "label": "CDB Premium"},
      {"type": "lci", "cdi_pct": 92, "label": "LCI 92%"},
      {"type": "poupanca", "label": "Poupança"},
      {"type": "tesouro_selic", "label": "Tesouro Selic"}
    ]
  }'
```

---

## Ferramentas Sugeridas para o Trabalho

| Atividade | Ferramenta |
|-----------|-----------|
| Testes unitários | pytest |
| Cobertura de código | pytest-cov |
| Mutação | mutmut |
| Testes de sistema | Selenium |
| Inspeção de código | SonarQube / Pylint |
| Bug tracker | GitHub Issues |
| Gestão de casos de teste | TestLink |
