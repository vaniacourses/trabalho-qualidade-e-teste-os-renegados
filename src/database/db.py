"""
Gerenciamento do banco de dados SQLite.
Usuários, sessões e log de acessos.
"""

import sqlite3
import hashlib
import os
from datetime import datetime

DB_PATH = os.path.join(os.path.dirname(__file__), "novobanco.db")


def get_connection():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn


def init_db():
    """Inicializa o banco e cria as tabelas se não existirem."""
    conn = get_connection()
    cur = conn.cursor()

    cur.execute("""
        CREATE TABLE IF NOT EXISTS usuarios (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            username TEXT UNIQUE NOT NULL,
            nome_completo TEXT NOT NULL,
            email TEXT NOT NULL,
            senha_hash TEXT NOT NULL,
            perfil TEXT NOT NULL DEFAULT 'usuario',
            ativo INTEGER NOT NULL DEFAULT 1,
            criado_em TEXT NOT NULL,
            ultimo_acesso TEXT
        )
    """)

    cur.execute("""
        CREATE TABLE IF NOT EXISTS log_acessos (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            usuario_id INTEGER,
            username TEXT,
            acao TEXT,
            ip TEXT,
            timestamp TEXT NOT NULL,
            sucesso INTEGER NOT NULL DEFAULT 1
        )
    """)

    conn.commit()
    _seed_users(cur, conn)
    conn.close()


def hash_senha(senha: str) -> str:
    return hashlib.sha256(senha.encode()).hexdigest()


def _seed_users(cur, conn):
    """Cria os usuários iniciais se ainda não existirem."""
    usuarios = [
        ("admin",    "Administrador Sistema", "admin@novobanco.com.br",    "Admin@2024",   "admin"),
        ("joao.silva","João Silva",            "joao.silva@novobanco.com.br","Joao@2024",   "usuario"),
        ("maria.souza","Maria Souza",          "maria.souza@novobanco.com.br","Maria@2024", "usuario"),
    ]
    for username, nome, email, senha, perfil in usuarios:
        cur.execute("SELECT id FROM usuarios WHERE username = ?", (username,))
        if not cur.fetchone():
            cur.execute("""
                INSERT INTO usuarios (username, nome_completo, email, senha_hash, perfil, ativo, criado_em)
                VALUES (?, ?, ?, ?, ?, 1, ?)
            """, (username, nome, email, hash_senha(senha), perfil, datetime.now().isoformat()))
    conn.commit()


def autenticar(username: str, senha: str):
    """Verifica credenciais. Retorna o usuário (Row) ou None."""
    conn = get_connection()
    cur = conn.cursor()
    cur.execute("""
        SELECT * FROM usuarios
        WHERE username = ? AND senha_hash = ? AND ativo = 1
    """, (username, hash_senha(senha)))
    user = cur.fetchone()
    if user:
        cur.execute("""
            UPDATE usuarios SET ultimo_acesso = ? WHERE id = ?
        """, (datetime.now().isoformat(), user["id"]))
        conn.commit()
    conn.close()
    return user


def registrar_log(usuario_id, username, acao, ip, sucesso=1):
    conn = get_connection()
    conn.execute("""
        INSERT INTO log_acessos (usuario_id, username, acao, ip, timestamp, sucesso)
        VALUES (?, ?, ?, ?, ?, ?)
    """, (usuario_id, username, acao, ip, datetime.now().isoformat(), sucesso))
    conn.commit()
    conn.close()


def listar_usuarios():
    conn = get_connection()
    users = conn.execute("""
        SELECT id, username, nome_completo, email, perfil, ativo, criado_em, ultimo_acesso
        FROM usuarios ORDER BY perfil, username
    """).fetchall()
    conn.close()
    return [dict(u) for u in users]


def listar_logs(limit=50):
    conn = get_connection()
    logs = conn.execute("""
        SELECT * FROM log_acessos ORDER BY timestamp DESC LIMIT ?
    """, (limit,)).fetchall()
    conn.close()
    return [dict(l) for l in logs]
