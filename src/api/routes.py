import os
import sys
import uuid
from datetime import date, datetime
from functools import wraps

from flask import (Flask, jsonify, redirect, render_template,
                   request, session, url_for, flash)
from flask_cors import CORS

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(__file__))))

from src.models.account import Account, AccountType, AccountStatus
from src.models.customer import Customer, CustomerStatus
from src.models.transaction import Transaction, TransactionType, TransactionStatus
from src.models.loan import LoanType, AmortizationSystem
from src.services.loan_calculator import LoanCalculator
from src.services.transaction_validator import TransactionValidator
from src.services.fraud_detector import FraudDetector
from src.services.credit_score_calculator import CreditScoreCalculator
from src.services.investment_calculator import InvestmentCalculator, InvestmentType
from src.services.account_manager import AccountManager
from src.database.db import init_db, autenticar, registrar_log, listar_usuarios, listar_logs

_template_dir = os.path.join(os.path.dirname(os.path.dirname(__file__)), 'templates')
app = Flask(__name__, template_folder=_template_dir)
app.secret_key = 'novobanco-secret-key-2024'
CORS(app)

init_db()

loan_calc       = LoanCalculator()
tx_validator    = TransactionValidator()
fraud_detector  = FraudDetector()
score_calc      = CreditScoreCalculator()
investment_calc = InvestmentCalculator()
account_manager = AccountManager()


def login_required(f):
    @wraps(f)
    def decorated(*args, **kwargs):
        if 'username' not in session:
            return redirect(url_for('login'))
        return f(*args, **kwargs)
    return decorated


def admin_required(f):
    @wraps(f)
    def decorated(*args, **kwargs):
        if 'username' not in session:
            return redirect(url_for('login'))
        if session.get('perfil') != 'admin':
            flash('Acesso restrito a administradores.', 'danger')
            return redirect(url_for('home'))
        return f(*args, **kwargs)
    return decorated


@app.route('/login', methods=['GET', 'POST'])
def login():
    if 'username' in session:
        return redirect(url_for('home'))
    erro = None
    if request.method == 'POST':
        username = request.form.get('username', '').strip()
        senha    = request.form.get('senha', '')
        ip       = request.remote_addr
        user = autenticar(username, senha)
        if user:
            session['username'] = user['username']
            session['nome']     = user['nome_completo']
            session['perfil']   = user['perfil']
            session['user_id']  = user['id']
            registrar_log(user['id'], username, 'login', ip, sucesso=1)
            return redirect(url_for('home'))
        else:
            registrar_log(None, username, 'login_falhou', ip, sucesso=0)
            erro = 'Usuário ou senha incorretos.'
    return render_template('login.html', erro=erro)


@app.route('/logout')
def logout():
    registrar_log(session.get('user_id'), session.get('username'), 'logout', request.remote_addr)
    session.clear()
    return redirect(url_for('login'))


@app.route('/')
@login_required
def home():
    return render_template('home.html', active_page='home')


@app.route('/emprestimos')
@login_required
def emprestimos():
    return render_template('emprestimos.html', active_page='emprestimos')


@app.route('/investimentos')
@login_required
def investimentos():
    return render_template('investimentos.html', active_page='investimentos')


@app.route('/contas')
@login_required
def contas():
    return render_template('contas.html', active_page='contas')


@app.route('/risco')
@login_required
def risco():
    return render_template('risco.html', active_page='risco')


@app.route('/admin')
@admin_required
def admin():
    return render_template('admin.html', active_page='admin',
                           usuarios=listar_usuarios(), logs=listar_logs(30))


@app.route('/health')
def health():
    return jsonify({'status': 'ok', 'service': 'NovoBanco Investimentos Investimentos API', 'version': '1.0.0'})


@app.route('/loans/simulate', methods=['POST'])
@login_required
def simulate_loan():
    data = request.get_json()
    try:
        loan_type      = LoanType(data['loan_type'])
        amortization   = AmortizationSystem(data.get('amortization_system', 'price'))
        principal      = float(data['amount'])
        term           = int(data['term_months'])
        credit_score   = int(data.get('credit_score', 700))
        monthly_income = float(data.get('monthly_income', 5000))
        age            = int(data.get('age', 35))
        valid, errors  = loan_calc.validate_loan_request(loan_type, principal, term, monthly_income, credit_score, age)
        if not valid:
            return jsonify({'valid': False, 'errors': errors}), 422
        rate         = loan_calc.calculate_interest_rate(loan_type, credit_score, term, monthly_income, principal)
        installments = loan_calc.generate_installments(principal, rate, term, amortization)
        return jsonify({
            'valid': True, 'loan_type': loan_type.value,
            'amortization_system': amortization.value, 'principal': principal,
            'monthly_rate': round(rate, 6),
            'annual_rate': round(loan_calc.calculate_effective_annual_rate(rate), 4),
            'term_months': term,
            'installments': [i.to_dict() for i in installments],
            'total_amount': round(sum(i.total for i in installments), 2),
            'total_interest': round(sum(i.interest for i in installments), 2),
        })
    except (KeyError, ValueError) as e:
        return jsonify({'error': str(e)}), 400


@app.route('/transactions/validate', methods=['POST'])
@login_required
def validate_transaction():
    data = request.get_json()
    try:
        ad = data['source_account']
        account = Account(
            account_id=ad['account_id'], customer_id=ad['customer_id'],
            account_type=AccountType(ad['account_type']),
            balance=float(ad['balance']), credit_limit=float(ad.get('credit_limit', 0)),
            status=AccountStatus(ad.get('status', 'active')),
        )
        tx = Transaction(
            transaction_id=str(uuid.uuid4()), source_account_id=account.account_id,
            transaction_type=TransactionType(data['transaction_type']),
            amount=float(data['amount']), timestamp=datetime.now(),
        )
        daily_spent = {TransactionType(k): float(v) for k, v in data.get('daily_spent', {}).items()}
        valid, errors = tx_validator.validate(tx, account, daily_spent)
        fee = tx_validator.calculate_applicable_fee(tx, account, data.get('monthly_transaction_count', 0))
        return jsonify({'valid': valid, 'errors': errors, 'applicable_fee': fee})
    except (KeyError, ValueError) as e:
        return jsonify({'error': str(e)}), 400


@app.route('/fraud/analyze', methods=['POST'])
@login_required
def analyze_fraud():
    data = request.get_json()
    try:
        tx = Transaction(
            transaction_id=data.get('transaction_id', str(uuid.uuid4())),
            source_account_id=data['source_account_id'],
            transaction_type=TransactionType(data['transaction_type']),
            amount=float(data['amount']),
            destination_account_id=data.get('destination_account_id'),
            timestamp=datetime.now(), metadata=data.get('metadata', {}),
        )
        decision, alerts, score = fraud_detector.analyze(
            tx, [], data.get('customer_profile', {}), data.get('device_info'))
        return jsonify({'decision': decision, 'risk_score': score,
                        'alerts': [a.to_dict() for a in alerts], 'alerts_count': len(alerts)})
    except (KeyError, ValueError) as e:
        return jsonify({'error': str(e)}), 400


@app.route('/credit/score', methods=['POST'])
@login_required
def calculate_credit_score():
    data = request.get_json()
    try:
        score, factors, classification = score_calc.calculate_score(data)
        return jsonify({'score': score, 'classification': classification,
                        'factors': [f.to_dict() for f in factors]})
    except Exception as e:
        return jsonify({'error': str(e)}), 400


@app.route('/investments/compare', methods=['POST'])
@login_required
def compare_investments():
    data = request.get_json()
    try:
        results = investment_calc.compare_investments(
            float(data['principal']), int(data['days']), data['options'])
        return jsonify({'results': results})
    except (KeyError, ValueError) as e:
        return jsonify({'error': str(e)}), 400


@app.route('/accounts/open', methods=['POST'])
@login_required
def open_account():
    data = request.get_json()
    try:
        cd = data['customer']
        customer = Customer(
            customer_id=cd['customer_id'], name=cd['name'], cpf=cd['cpf'],
            email=cd['email'], phone=cd['phone'],
            birth_date=date.fromisoformat(cd['birth_date']),
            monthly_income=float(cd['monthly_income']),
            credit_score=int(cd.get('credit_score', 600)),
            status=CustomerStatus(cd.get('status', 'active')),
        )
        account, warnings = account_manager.open_account(
            customer, AccountType(data['account_type']), float(data.get('initial_deposit', 0)))
        return jsonify({'account': account.to_dict(), 'warnings': warnings}), 201
    except Exception as e:
        return jsonify({'error': str(e)}), 400


if __name__ == '__main__':
    app.run(debug=True, port=5000)
