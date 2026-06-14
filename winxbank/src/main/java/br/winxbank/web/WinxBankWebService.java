package br.winxbank.web;

import br.winxbank.exception.*;
import br.winxbank.repository.ArquivoBanco;
import br.winxbank.repository.ArquivoDeClientes;
import br.winxbank.repository.ArquivoDeMesAtual;
import br.winxbank.sistemabancario.*;
import br.winxbank.sistemaclientes.Cliente;
import br.winxbank.sistemaclientes.ClienteWinx;
import br.winxbank.sistemaclientes.RegistroDeClientes;
import br.winxbank.tempo.Ano;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Map;

/**
 * Camada de aplicação usada apenas pela versão web.
 * A lógica de domínio continua nas classes originais do projeto.
 */
public class WinxBankWebService {

    private static final DecimalFormat MOEDA = new DecimalFormat("0.00");
    private Cliente clienteAtual = new Cliente();
    private boolean inicializado = false;
    private String ultimaMensagem = "Bem-vindo ao WinxBank Web.";
    private String ultimoDetalhe = "";

    public synchronized void inicializar() {
        if (inicializado) {
            return;
        }
        ArquivoDeClientes.getInstancia().readjason();
        ArquivoDeMesAtual.getInstancia().lerMesAtual();
        if (Ano.getInstancia().getMesAtual() == null || Ano.getInstancia().getMesAtual().isBlank()) {
            Ano.getInstancia().setMesAtual("Janeiro");
        }
        try {
            ArquivoBanco.getInstancia().construirBanco();
        } catch (IOException | ClassNotFoundException e) {
            ultimaMensagem = "Banco iniciado sem dados anteriores.";
        }
        inicializado = true;
    }

    public synchronized void executar(String acao, Map<String, String> form) {
        inicializar();
        ultimoDetalhe = "";
        try {
            Ano.getInstancia().fazerMesPassar();
            switch (acao) {
                case "cadastrar" -> cadastrarCliente(form);
                case "login" -> login(form);
                case "abrirConta" -> abrirConta(form);
                case "fecharConta" -> fecharConta(form);
                case "apagarUsuario" -> apagarUsuario();
                case "depositar" -> depositar(form);
                case "comprar" -> comprar(form);
                case "pix" -> pix(form);
                case "sacar" -> sacar(form);
                case "pagarFatura" -> pagarFatura(form);
                case "ajustarLimite" -> ajustarLimite(form);
                case "pagarParcela" -> pagarParcela(form);
                case "requisitarEmprestimo" -> requisitarEmprestimo(form);
                case "converterPontos" -> converterPontos(form);
                case "gerarExtrato" -> gerarExtrato(form);
                case "gerarInforme" -> gerarInforme(form);
                case "exibirClientes" -> exibirClientes();
                case "limparClientes" -> limparClientes();
                case "dadosBanco" -> dadosBanco();
                case "logout" -> logout();
                default -> ultimaMensagem = "Opcao invalida. Escolha uma operação do menu web.";
            }
        } catch (RuntimeException | IOException | InterruptedException e) {
            ultimaMensagem = mensagemErro(e);
        } finally {
            persistir();
        }
    }

    public synchronized String renderizarPagina() {
        inicializar();
        boolean logado = clienteAtual.getCpf() != null;
        return """
                <!DOCTYPE html>
                <html lang="pt-BR">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>WinxBank Web</title>
                    <style>
                """
                + estilosUx()
                + """
                    </style>
                </head>
                <body>
                """
                + (logado ? paginaDashboardUx() : paginaAutenticacaoUx())
                + scriptsUx()
                + """
                </body>
                </html>
                """;
    }

    private String estilosUx() {
        return """
                :root {
                    --bg: #eff6ff;
                    --bg-2: #f8fbff;
                    --surface: #ffffff;
                    --surface-soft: #f5f9ff;
                    --ink: #172033;
                    --muted: #6b7a90;
                    --line: #dce9f7;
                    --primary: #4aa3f5;
                    --primary-2: #1676d2;
                    --primary-3: #0f4ea6;
                    --success: #27a95f;
                    --warning: #f5a524;
                    --danger: #d9304f;
                    --shadow: 0 26px 70px rgba(15, 78, 166, .16);
                    --soft-shadow: 0 16px 38px rgba(15, 78, 166, .10);
                    --radius-lg: 30px;
                    --radius-md: 20px;
                    --radius-sm: 14px;
                }
                * { box-sizing: border-box; }
                html { scroll-behavior: smooth; }
                body {
                    margin: 0;
                    min-width: 1100px;
                    font-family: Inter, Segoe UI, Arial, Helvetica, sans-serif;
                    color: var(--ink);
                    background:
                        radial-gradient(circle at 0 0, rgba(74,163,245,.34), transparent 380px),
                        radial-gradient(circle at 95% 8%, rgba(39,169,95,.10), transparent 260px),
                        linear-gradient(135deg, #f8fbff 0%, var(--bg) 52%, #ffffff 100%);
                }
                a { color: inherit; }
                .ux-shell { min-height: 100vh; }
                .auth-page {
                    min-height: 100vh;
                    display: grid;
                    grid-template-columns: 1.05fr .95fr;
                    align-items: center;
                    gap: 52px;
                    max-width: 1280px;
                    margin: 0 auto;
                    padding: 48px 42px;
                }
                .auth-hero {
                    position: relative;
                    min-height: 650px;
                    border-radius: 42px;
                    padding: 48px;
                    overflow: hidden;
                    color: #fff;
                    background: linear-gradient(145deg, #63b6ff 0%, #2083df 52%, #0d58b3 100%);
                    box-shadow: var(--shadow);
                }
                .auth-hero::before {
                    content: "";
                    position: absolute;
                    width: 620px;
                    height: 620px;
                    border-radius: 50%;
                    background: rgba(255,255,255,.18);
                    left: -260px;
                    top: -260px;
                }
                .auth-hero::after {
                    content: "";
                    position: absolute;
                    width: 360px;
                    height: 360px;
                    border-radius: 50%;
                    background: rgba(255,255,255,.12);
                    right: -120px;
                    bottom: -120px;
                }
                .brand-compact { position: relative; z-index: 1; display: flex; align-items: center; gap: 14px; }
                .brand-mark {
                    width: 52px;
                    height: 52px;
                    border-radius: 18px;
                    display: grid;
                    place-items: center;
                    font-weight: 950;
                    font-size: 25px;
                    color: var(--primary-3);
                    background: #fff;
                    box-shadow: 0 20px 42px rgba(0,0,0,.16);
                }
                .brand-compact h1, .app-brand h1 { margin: 0; letter-spacing: -.04em; }
                .brand-compact span { display: block; margin-top: 3px; color: rgba(255,255,255,.78); font-size: 13px; }
                .auth-copy { position: relative; z-index: 1; max-width: 590px; margin-top: 96px; }
                .kicker {
                    display: inline-flex;
                    align-items: center;
                    padding: 8px 12px;
                    border: 1px solid rgba(255,255,255,.36);
                    border-radius: 999px;
                    background: rgba(255,255,255,.14);
                    font-weight: 900;
                    font-size: 13px;
                    margin-bottom: 22px;
                }
                .auth-copy h2 { margin: 0 0 18px; font-size: 56px; line-height: .98; letter-spacing: -.065em; }
                .auth-copy p { margin: 0; max-width: 480px; font-size: 17px; line-height: 1.65; color: rgba(255,255,255,.90); }
                .ux-preview {
                    position: relative;
                    z-index: 1;
                    width: 88%;
                    margin-top: 46px;
                    border-radius: 28px;
                    background: rgba(255,255,255,.94);
                    color: var(--ink);
                    border: 1px solid rgba(255,255,255,.62);
                    box-shadow: 0 28px 74px rgba(0,0,0,.20);
                    overflow: hidden;
                }
                .preview-top { height: 44px; display: flex; align-items: center; gap: 8px; padding: 0 18px; border-bottom: 1px solid var(--line); background: #f8fbff; }
                .dot { width: 10px; height: 10px; border-radius: 999px; background: #bdd0e5; }
                .preview-body { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; padding: 18px; }
                .mini-card { border-radius: 20px; padding: 16px; background: #fff; border: 1px solid var(--line); box-shadow: var(--soft-shadow); }
                .mini-card.dark { color: #fff; background: linear-gradient(145deg, #1f2937, #101827); border: 0; }
                .mini-card small { color: var(--muted); display: block; margin-bottom: 6px; font-weight: 850; }
                .mini-card.dark small { color: rgba(255,255,255,.72); }
                .mini-card strong { display: block; font-size: 26px; letter-spacing: -.04em; }
                .mini-line { height: 8px; border-radius: 999px; background: #edf4fb; margin-top: 10px; overflow: hidden; }
                .mini-line::after { content: ""; display: block; width: 72%; height: 100%; background: linear-gradient(90deg, var(--primary), var(--success)); border-radius: inherit; }
                .auth-card {
                    width: 100%;
                    border-radius: 34px;
                    background: rgba(255,255,255,.96);
                    border: 1px solid rgba(220,233,247,.9);
                    box-shadow: var(--shadow);
                    padding: 32px;
                }
                .auth-card h2 { margin: 0; font-size: 30px; letter-spacing: -.04em; }
                .auth-subtitle { margin: 8px 0 24px; color: var(--muted); line-height: 1.5; }
                .auth-tabs { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; padding: 6px; background: #edf5ff; border-radius: 18px; margin-bottom: 18px; }
                .auth-tab {
                    border: 0;
                    margin: 0;
                    border-radius: 14px;
                    padding: 12px;
                    font-weight: 950;
                    cursor: pointer;
                    color: var(--primary-3);
                    background: transparent;
                    box-shadow: none;
                }
                .auth-tab.is-active { background: #fff; box-shadow: 0 10px 24px rgba(15,78,166,.12); }
                .auth-panel { display: none; }
                .auth-panel.is-active { display: block; }
                .auth-panel .card { padding: 0; border: 0; box-shadow: none; background: transparent; border-radius: 0; }
                .auth-panel .card h2, .auth-panel .voltar-home { display: none; }
                .status-message {
                    border-radius: 18px;
                    padding: 14px 16px;
                    margin-bottom: 18px;
                    background: #f0f9ff;
                    border: 1px solid #d7ecff;
                    color: #175a9d;
                    font-size: 14px;
                    line-height: 1.45;
                }
                .app-header {
                    position: sticky;
                    top: 0;
                    z-index: 50;
                    background: rgba(255,255,255,.88);
                    backdrop-filter: blur(18px);
                    border-bottom: 1px solid rgba(220,233,247,.88);
                    box-shadow: 0 12px 30px rgba(15,78,166,.08);
                }
                .header-inner {
                    max-width: 1360px;
                    margin: 0 auto;
                    min-height: 82px;
                    padding: 0 32px;
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                    gap: 20px;
                }
                .app-brand { display: flex; align-items: center; gap: 14px; }
                .app-brand .brand-mark { color: #fff; background: linear-gradient(145deg, var(--primary), var(--primary-3)); box-shadow: 0 16px 30px rgba(22,118,210,.25); }
                .app-brand span { display: block; margin-top: 3px; color: var(--muted); font-size: 13px; }
                .header-nav { display: flex; align-items: center; gap: 8px; }
                .header-nav a {
                    text-decoration: none;
                    color: var(--primary-3);
                    font-size: 13px;
                    font-weight: 900;
                    padding: 10px 13px;
                    border-radius: 999px;
                    border: 1px solid transparent;
                }
                .header-nav a:hover, .header-nav a:focus { background: #edf6ff; border-color: #d7ebff; outline: none; }
                .app-shell {
                    max-width: 1360px;
                    margin: 0 auto;
                    padding: 28px 32px 56px;
                    display: grid;
                    grid-template-columns: 340px 1fr;
                    gap: 24px;
                }
                .sidebar { position: sticky; top: 110px; align-self: start; display: grid; gap: 16px; }
                .sidebar-card, .dashboard-card, .operation-card, .card {
                    background: rgba(255,255,255,.96);
                    border: 1px solid var(--line);
                    border-radius: var(--radius-lg);
                    box-shadow: var(--soft-shadow);
                }
                .sidebar-card { padding: 20px; }
                .sidebar-card h2 { margin: 0 0 12px; font-size: 18px; letter-spacing: -.03em; }
                .status {
                    border-radius: 20px;
                    padding: 15px;
                    background: linear-gradient(135deg, #ecfff4, #f8fffb);
                    border: 1px solid #ccefdc;
                    color: #1f6c42;
                    line-height: 1.45;
                }
                .status strong { display: block; margin-bottom: 4px; }
                .account-total { border-radius: 24px; padding: 20px; color: #fff; background: linear-gradient(145deg, #1f2937, #101827); margin: 12px 0; }
                .account-total small { display: block; color: rgba(255,255,255,.72); margin-bottom: 7px; }
                .account-total strong { font-size: 30px; letter-spacing: -.05em; }
                .quick-meta { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin: 12px 0; }
                .quick-meta div { border-radius: 16px; background: #f7fbff; border: 1px solid var(--line); padding: 11px; }
                .quick-meta span { display: block; color: var(--muted); font-size: 12px; }
                .quick-meta strong { display: block; margin-top: 4px; color: var(--ink); overflow-wrap: anywhere; }
                .badge { display: inline-block; border-radius: 999px; padding: 6px 10px; background: #edf7ff; color: var(--primary-3); font-size: 12px; font-weight: 900; }
                .operation-menu { display: grid; gap: 14px; }
                .menu-group h3 { margin: 0 0 8px; color: var(--muted); text-transform: uppercase; letter-spacing: .08em; font-size: 11px; }
                .menu-group a {
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                    gap: 12px;
                    text-decoration: none;
                    color: #334155;
                    padding: 11px 12px;
                    border-radius: 15px;
                    font-size: 13px;
                    font-weight: 900;
                    border: 1px solid transparent;
                }
                .menu-group a::after { content: "›"; color: #93a6bd; font-size: 18px; }
                .menu-group a:hover, .menu-group a.is-active { background: #edf6ff; color: var(--primary-3); border-color: #d7ebff; }
                .main-area { min-width: 0; }
                .dashboard-grid { display: grid; grid-template-columns: 1.15fr .85fr; gap: 18px; margin-bottom: 18px; }
                .dashboard-card { padding: 24px; }
                .dashboard-card h2, .operation-card h2, .card h2 { margin: 0 0 12px; letter-spacing: -.04em; }
                .welcome-card {
                    color: #fff;
                    min-height: 250px;
                    background:
                        radial-gradient(circle at 92% 8%, rgba(255,255,255,.22), transparent 220px),
                        linear-gradient(135deg, var(--primary), var(--primary-3));
                    border: 0;
                    overflow: hidden;
                    position: relative;
                }
                .welcome-card h2 { font-size: 38px; max-width: 560px; }
                .welcome-card p { color: rgba(255,255,255,.88); line-height: 1.6; max-width: 610px; }
                .quick-actions { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-top: 22px; }
                .quick-actions a {
                    text-decoration: none;
                    background: rgba(255,255,255,.16);
                    border: 1px solid rgba(255,255,255,.28);
                    color: #fff;
                    border-radius: 18px;
                    padding: 14px;
                    font-weight: 950;
                }
                .metrics { display: grid; gap: 14px; }
                .metric { border-radius: 22px; padding: 18px; background: #fff; border: 1px solid var(--line); box-shadow: var(--soft-shadow); }
                .metric span { color: var(--muted); font-size: 13px; font-weight: 850; }
                .metric strong { display: block; margin-top: 6px; font-size: 28px; letter-spacing: -.05em; }
                .operation-card { padding: 24px; }
                .operation-intro { color: var(--muted); line-height: 1.55; max-width: 760px; }
                .operation-panels { margin-top: 18px; }
                .operation-panels .card { display: none; padding: 24px; max-width: 760px; }
                .operation-panels .card.is-active { display: block; animation: rise .16s ease-out; }
                .operation-panels .card h2 { font-size: 26px; }
                .voltar-home { display: none; }
                label { display: block; margin: 13px 0 6px; color: #344256; font-size: 13px; font-weight: 900; }
                input, select {
                    width: 100%;
                    border: 1px solid #d6e4f2;
                    border-radius: 15px;
                    padding: 13px 14px;
                    background: #fbfdff;
                    color: var(--ink);
                    font-size: 14px;
                    outline: none;
                }
                input:focus, select:focus { background: #fff; border-color: var(--primary); box-shadow: 0 0 0 4px rgba(74,163,245,.16); }
                button {
                    margin-top: 16px;
                    width: 100%;
                    border: 0;
                    border-radius: 16px;
                    padding: 14px 16px;
                    color: #fff;
                    background: linear-gradient(135deg, var(--primary), var(--primary-2));
                    font-weight: 950;
                    cursor: pointer;
                    box-shadow: 0 14px 24px rgba(22,118,210,.22);
                }
                button:hover { filter: brightness(.97); }
                button.secondary, .secondary { background: #334155; }
                button.danger, .danger { background: linear-gradient(135deg, #ff6276, #d9304f); }
                .info { color: var(--muted); line-height: 1.5; font-size: 13px; }
                table { width: 100%; border-collapse: collapse; margin-top: 12px; font-size: 13px; overflow: hidden; border-radius: 14px; }
                th, td { padding: 10px; border-bottom: 1px solid var(--line); text-align: left; vertical-align: top; }
                th { background: #f7fbff; color: var(--muted); font-weight: 950; }
                .detalhe { white-space: pre-wrap; background: #101827; color: #f8fafc; border-radius: 18px; padding: 15px; max-height: 280px; overflow: auto; font-family: Consolas, monospace; font-size: 13px; line-height: 1.55; }
                .hidden { display: none !important; }
                @keyframes rise { from { transform: translateY(5px); opacity: .76; } to { transform: translateY(0); opacity: 1; } }
                @media (min-width: 1420px) { .app-shell, .header-inner { max-width: 1460px; } }
                """;
    }

    private String paginaAutenticacaoUx() {
        return """
                <main class="ux-shell auth-page" id="auth-page" data-testid="auth-page">
                    <section class="auth-hero" aria-label="Apresentação do WinxBank">
                        <div class="brand-compact">
                            <div class="brand-mark" aria-hidden="true">B</div>
                            <div>
                                <h1>WinxBank</h1>
                                <span>Banco web local em Java</span>
                            </div>
                        </div>
                        <div class="auth-copy">
                            <span class="kicker">Experiência desktop</span>
                            <h2>Entre para acessar a sua conta.</h2>
                            <p>Realize o login ou abra já a sua conta!</p>
                        </div>
                        <div class="ux-preview" aria-hidden="true">
                            <div class="preview-top"><span class="dot"></span><span class="dot"></span><span class="dot"></span></div>
                            <div class="preview-body">
                                <div class="mini-card dark"><small>Saldo total</small><strong>R$ 5.634,12</strong><div class="mini-line"></div></div>
                                <div class="mini-card"><small>Operações</small><strong>1 por vez</strong><div class="mini-line"></div></div>
                                <div class="mini-card"><small>Navegação</small><strong>Interativa</strong></div>
                                <div class="mini-card"><small>Rendimentio</small><strong>105% CDI</strong></div>
                            </div>
                        </div>
                    </section>
                    <section class="auth-card" aria-label="Login e cadastro">
                        <h2>Acesse o WinxBank</h2>
                        <p class="auth-subtitle">Faça login com CPF existente ou cadastre um novo cliente para começar.</p>
                        <div class="status-message" id="mensagem-status"><strong>Status: </strong> """
                + esc(ultimaMensagem)
                + """
                        </div>
                        <div class="auth-tabs" role="tablist" aria-label="Escolha entre login e cadastro">
                            <button class="auth-tab is-active" id="tab-login" type="button" data-auth-target="auth-login-panel">Login</button>
                            <button class="auth-tab" id="tab-cadastro" type="button" data-auth-target="auth-cadastro-panel">Cadastro</button>
                        </div>
                        <div id="auth-login-panel" class="auth-panel is-active" data-testid="auth-login-panel">
                """
                + formLogin()
                + """
                        </div>
                        <div id="auth-cadastro-panel" class="auth-panel" data-testid="auth-cadastro-panel">
                """
                + formCadastrar()
                + """
                        </div>
                    </section>
                </main>
                """;
    }

    private String paginaDashboardUx() {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                <div class="ux-shell" id="app-page" data-testid="app-page">
                    <header class="app-header">
                        <div class="header-inner">
                            <div class="app-brand">
                                <div class="brand-mark" aria-hidden="true">B</div>
                                <div>
                                    <h1>WinxBank</h1>
                                    <span id="header-cliente">
                """);
        sb.append(esc(clienteAtual.getNome())).append(" · Mês: ").append(esc(Ano.getInstancia().getMesAtual()));
        sb.append("""
                                    </span>
                                </div>
                            </div>
                            <nav class="header-nav" aria-label="Navegação principal">
                                <a id="nav-dashboard" href="#dashboard">Dashboard</a>
                                <a id="nav-contas" href="#sec-abrirConta">Contas</a>
                                <a id="nav-transacoes" href="#sec-depositar">Transações</a>
                                <a id="nav-cartoes" href="#sec-comprar">Cartões</a>
                                <a id="nav-documentos" href="#sec-gerarExtrato">Documentos</a>
                            </nav>
                        </div>
                    </header>
                    <main class="app-shell">
                        <aside class="sidebar" id="sidebar">
                            <section class="sidebar-card" id="painel-status">
                                <div class="status" id="mensagem-status"><strong>Status da operação</strong>
                """);
        sb.append(esc(ultimaMensagem));
        sb.append("""
                                </div>
                            </section>
                            <section class="sidebar-card" id="resumo-cliente">
                """);
        sb.append(renderizarClienteAtual());
        sb.append("""
                            </section>
                            <section class="sidebar-card" aria-label="Menu de operações">
                                <h2>Funcionalidades</h2>
                """);
        sb.append(menuOperacoesUx());
        sb.append("""
                            </section>
                        </aside>
                        <section class="main-area">
                            <section id="dashboard" class="dashboard-view" data-testid="dashboard-view">
                """);
        sb.append(dashboardUx());
        sb.append("""
                            </section>
                            <section class="operation-card" id="operacoes" aria-label="Operação selecionada">
                                <h2 id="titulo-operacao">Escolha uma funcionalidade</h2>
                                <p class="operation-intro">Use o menu lateral ou os atalhos do dashboard para abrir apenas o formulário necessário. Isso deixa a navegação mais parecida com uma área logada de banco digital.</p>
                                <div class="operation-panels" id="operation-panels">
                """);
        sb.append(paineisOperacoesUx());
        sb.append("""
                                </div>
                            </section>
                """);
        sb.append(renderizarDetalhe());
        sb.append("""
                        </section>
                    </main>
                </div>
                """);
        return sb.toString();
    }

    private String dashboardUx() {
        double saldoTotal = 0;
        double dividaTotal = 0;
        for(Conta conta : clienteAtual.getContas()){
            saldoTotal += conta.getSaldo();
            dividaTotal += conta.getDividaDeEmprestimo();
        }
        String tipoCliente = clienteAtual instanceof ClienteWinx ? "ClienteWinx" : "Cliente";
        StringBuilder sb = new StringBuilder();
        sb.append("""
                <div class="dashboard-grid">
                    <article class="dashboard-card welcome-card">
                        <span class="kicker">Área logada</span>
                        <h2>Olá, 
                """);
        sb.append(esc(clienteAtual.getNome()));
        sb.append("""
                        .</h2>
                        <p>Agora a experiência começa pelo resumo da conta. As funcionalidades ficam organizadas por jornada do cliente e não aparecem todas empilhadas na tela.</p>
                        <div class="quick-actions">
                            <a id="quick-pix" href="#sec-pix">Fazer Pix</a>
                            <a id="quick-depositar" href="#sec-depositar">Depositar</a>
                            <a id="quick-extrato" href="#sec-gerarExtrato">Gerar extrato</a>
                        </div>
                    </article>
                    <aside class="metrics" aria-label="Resumo financeiro">
                        <div class="metric"><span>Saldo total</span><strong id="metric-saldo">R$ 
                """);
        sb.append(MOEDA.format(saldoTotal));
        sb.append("""
                        </strong></div>
                        <div class="metric"><span>Dívida de empréstimo</span><strong id="metric-divida">R$ 
                """);
        sb.append(MOEDA.format(dividaTotal));
        sb.append("""
                        </strong></div>
                        <div class="metric"><span>Tipo de cliente</span><strong id="metric-tipo">
                """);
        sb.append(tipoCliente);
        sb.append("""
                        </strong></div>
                    </aside>
                </div>
                <article class="dashboard-card">
                    <h2>Minhas contas</h2>
                """);
        sb.append(renderizarTabelaContas(clienteAtual.getContas()));
        sb.append("""
                </article>
                """);
        return sb.toString();
    }

    private String menuOperacoesUx() {
        return """
                <nav class="operation-menu" id="menu-operacoes" data-testid="menu-operacoes">
                    <div class="menu-group">
                        <h3>Contas</h3>
                        <a class="nav-action" id="nav-abrirConta" href="#sec-abrirConta" data-target="sec-abrirConta">Abrir conta</a>
                        <a class="nav-action" id="nav-fecharConta" href="#sec-fecharConta" data-target="sec-fecharConta">Fechar conta</a>
                    </div>
                    <div class="menu-group">
                        <h3>Transações</h3>
                        <a class="nav-action" id="nav-depositar" href="#sec-depositar" data-target="sec-depositar">Depositar</a>
                        <a class="nav-action" id="nav-pix" href="#sec-pix" data-target="sec-pix">Fazer Pix</a>
                        <a class="nav-action" id="nav-sacar" href="#sec-sacar" data-target="sec-sacar">Sacar</a>
                    </div>
                    <div class="menu-group">
                        <h3>Cartões</h3>
                        <a class="nav-action" id="nav-comprar" href="#sec-comprar" data-target="sec-comprar">Comprar</a>
                        <a class="nav-action" id="nav-pagarFatura" href="#sec-pagarFatura" data-target="sec-pagarFatura">Pagar fatura</a>
                        <a class="nav-action" id="nav-ajustarLimite" href="#sec-ajustarLimite" data-target="sec-ajustarLimite">Ajustar limite</a>
                    </div>
                    <div class="menu-group">
                        <h3>Crédito e benefícios</h3>
                        <a class="nav-action" id="nav-requisitarEmprestimo" href="#sec-requisitarEmprestimo" data-target="sec-requisitarEmprestimo">Requisitar empréstimo</a>
                        <a class="nav-action" id="nav-pagarParcela" href="#sec-pagarParcela" data-target="sec-pagarParcela">Pagar parcela</a>
                        <a class="nav-action" id="nav-converterPontos" href="#sec-converterPontos" data-target="sec-converterPontos">Converter pontos</a>
                    </div>
                    <div class="menu-group">
                        <h3>Documentos</h3>
                        <a class="nav-action" id="nav-gerarExtrato" href="#sec-gerarExtrato" data-target="sec-gerarExtrato">Gerar extrato</a>
                        <a class="nav-action" id="nav-gerarInforme" href="#sec-gerarInforme" data-target="sec-gerarInforme">Gerar informe</a>
                    </div>
                    <div class="menu-group">
                        <h3>Administração</h3>
                        <a class="nav-action" id="nav-exibirClientes" href="#sec-exibirClientes" data-target="sec-exibirClientes">Exibir clientes</a>
                        <a class="nav-action" id="nav-dadosBanco" href="#sec-dadosBanco" data-target="sec-dadosBanco">Dados do banco</a>
                        <a class="nav-action" id="nav-apagarUsuario" href="#sec-apagarUsuario" data-target="sec-apagarUsuario">Apagar usuário</a>
                        <a class="nav-action" id="nav-limparClientes" href="#sec-limparClientes" data-target="sec-limparClientes">Limpar clientes</a>
                    </div>
                </nav>
                """;
    }

    private String paineisOperacoesUx() {
        return formAbrirConta()
                + formFecharConta()
                + formDepositar()
                + formPix()
                + formSacar()
                + formComprar()
                + formPagarFatura()
                + formAjustarLimite()
                + formRequisitarEmprestimo()
                + formPagarParcela()
                + formConverterPontos()
                + formGerarExtrato()
                + formGerarInforme()
                + formExibirClientes()
                + formDadosBanco()
                + formApagarUsuario()
                + formLimparClientes();
    }

    private String scriptsUx() {
        return """
                <script>
                (function(){
                    function showAuth(targetId) {
                        document.querySelectorAll('.auth-panel').forEach(function(panel){ panel.classList.remove('is-active'); });
                        document.querySelectorAll('.auth-tab').forEach(function(tab){ tab.classList.remove('is-active'); });
                        var panel = document.getElementById(targetId);
                        var tab = document.querySelector('[data-auth-target="' + targetId + '"]');
                        if(panel) { panel.classList.add('is-active'); }
                        if(tab) { tab.classList.add('is-active'); }
                    }
                    document.querySelectorAll('[data-auth-target]').forEach(function(tab){
                        tab.addEventListener('click', function(){ showAuth(tab.getAttribute('data-auth-target')); });
                    });
                    if(window.location.hash === '#sec-cadastrar') { showAuth('auth-cadastro-panel'); }

                    function showOperation(targetId) {
                        var dashboard = document.getElementById('dashboard');
                        var operationBox = document.getElementById('operacoes');
                        document.querySelectorAll('.operation-panels .card').forEach(function(card){ card.classList.remove('is-active'); });
                        document.querySelectorAll('.nav-action').forEach(function(nav){ nav.classList.remove('is-active'); });
                        if(!targetId || targetId === 'dashboard') {
                            if(dashboard) { dashboard.classList.remove('hidden'); }
                            if(operationBox) { operationBox.classList.add('hidden'); }
                            return;
                        }
                        var selected = document.getElementById(targetId);
                        if(dashboard) { dashboard.classList.add('hidden'); }
                        if(operationBox) { operationBox.classList.remove('hidden'); }
                        if(selected) {
                            selected.classList.add('is-active');
                            var title = selected.querySelector('h2');
                            var titleTarget = document.getElementById('titulo-operacao');
                            if(title && titleTarget) { titleTarget.textContent = title.textContent; }
                            selected.scrollIntoView({ behavior: 'smooth', block: 'start' });
                        }
                        document.querySelectorAll('[data-target="' + targetId + '"]').forEach(function(nav){ nav.classList.add('is-active'); });
                    }
                    function route() {
                        var hash = window.location.hash ? window.location.hash.substring(1) : 'dashboard';
                        if(hash.indexOf('sec-') === 0) { showOperation(hash); }
                        else { showOperation('dashboard'); }
                    }
                    window.addEventListener('hashchange', route);
                    route();
                })();
                </script>
                """;
    }

    public synchronized String estadoJson() {
        inicializar();
        return "{" +
                "\"mesAtual\":\"" + json(Ano.getInstancia().getMesAtual()) + "\"," +
                "\"clienteLogado\":" + (clienteAtual.getCpf() == null ? "null" : "\"" + json(clienteAtual.getCpf()) + "\"") + "," +
                "\"quantidadeClientes\":" + RegistroDeClientes.getInstancia().getClientes().size() +
                "}";
    }

    private void cadastrarCliente(Map<String, String> form) {
        String nome = texto(form, "nome");
        String cpf = cpfNormalizado(texto(form, "cpf"));
        if (cpf.isBlank()) {
            throw new IllegalArgumentException("CPF obrigatório não preenchido.");
        }
        if (buscarClientePorCpf(cpf) != null) {
            ultimaMensagem = "Usuário não pode ser criado. CPF já existente no registro.";
            return;
        }
        int tipoConta = inteiro(form, "tipoConta");
        double saldoInicial = decimal(form, "saldoInicial");
        Cliente criado = RegistroDeClientes.getInstancia().cadastrarCliente(nome, cpf, tipoConta, saldoInicial);
        if (criado == null) {
            ultimaMensagem = "Usuário não pode ser criado. Verifique o tipo de conta informado.";
            return;
        }
        clienteAtual = copiarClienteParaSessao(criado);
        ultimaMensagem = "Cadastro realizado com sucesso. Você já está logado como " + clienteAtual.getNome() + ".";
        ultimoDetalhe = resumoCliente(clienteAtual);
    }

    private void login(Map<String, String> form) {
        String cpf = texto(form, "cpfLogin");
        Cliente cliente = buscarClientePorCpf(cpf);
        if (cliente == null) {
            ultimaMensagem = "Cliente inexistente. Verifique se o CPF foi cadastrado ou use a aba Cadastro.";
            return;
        }
        clienteAtual = copiarClienteParaSessao(cliente);
        ultimaMensagem = "Login realizado com sucesso para " + clienteAtual.getNome() + ".";
        ultimoDetalhe = resumoCliente(clienteAtual);
    }

    private void abrirConta(Map<String, String> form) throws InterruptedException {
        Cliente cliente = exigirLogin();
        int tipoConta = inteiro(form, "tipoContaAbrir");
        double saldoInicial = decimal(form, "saldoInicialAbrir");
        Conta conta = Banco.getInstancia().abrirNovaConta(tipoConta, saldoInicial);
        if (conta == null) {
            ultimaMensagem = "Tipo de conta inválido. Use 1 para Corrente ou 2 para Poupança.";
            return;
        }
        Movimentacao movimentacao = new Movimentacao(conta.getSaldo(), Movimentacao.TipoDaMovimentacao.ENTRADA);
        conta.setExtrato(movimentacao);
        if(conta.getClass() == ContaPoupanca.class){
            ((ContaPoupanca) conta).setInformeRendimento(movimentacao);
        }
        cliente.setContas(conta);
        if(conta.getSaldo() >= 100000 || (cliente.acessarContas() != null && cliente.acessarContas().getSaldo() >= 100000)){
            ClienteWinx clienteWinx = new ClienteWinx(cliente.getNome(), cliente.getCpf(), 0);
            clienteWinx.setContas(cliente.getContas());
            clienteAtual = clienteWinx;
        }
        RegistroDeClientes.getInstancia().atualizarCliente(clienteAtual);
        ultimaMensagem = "Conta aberta com sucesso.";
        ultimoDetalhe = resumoConta(conta);
    }

    private void fecharConta(Map<String, String> form) throws InterruptedException {
        Cliente cliente = exigirLogin();
        int numeroConta = inteiro(form, "numeroContaFechar");
        Banco.getInstancia().fecharConta(cliente, numeroConta);
        RegistroDeClientes.getInstancia().atualizarCliente(cliente);
        ultimaMensagem = "Conta apagada com sucesso.";
    }

    private void apagarUsuario() {
        Cliente cliente = exigirLogin();
        RegistroDeClientes.getInstancia().removerCliente(cliente);
        clienteAtual = new Cliente();
        ultimaMensagem = "Usuário apagado com sucesso.";
    }

    private void depositar(Map<String, String> form) throws InterruptedException {
        Cliente cliente = exigirLogin();
        Conta conta = selecionarConta(cliente, inteiro(form, "numeroContaDeposito"));
        double valor = decimal(form, "valorDeposito");
        conta.depositar(valor);
        conta.setExtrato(new Movimentacao(valor, Movimentacao.TipoDaMovimentacao.ENTRADA));
        RegistroDeClientes.getInstancia().atualizarCliente(cliente);
        ultimaMensagem = "Depósito realizado com sucesso.";
        ultimoDetalhe = resumoConta(conta);
    }

    private void comprar(Map<String, String> form) throws InterruptedException {
        Cliente cliente = exigirLogin();
        Conta conta = selecionarConta(cliente, inteiro(form, "numeroContaCompra"));
        double valorCompra = decimal(form, "valorCompra");
        int formaPagamento = inteiro(form, "formaPagamentoCompra");
        int confirmar = inteiro(form, "confirmarCartaoCompra");
        if(valorCompra > conta.getSaldo()){
            throw new ValueIsHigherThanBalanceException();
        }
        boolean confirmado;
        if(conta instanceof ContaCorrente contaCorrente){
            confirmado = contaCorrente.comprar(valorCompra, formaPagamento, confirmar);
        } else if(conta instanceof ContaPoupanca contaPoupanca) {
            confirmado = contaPoupanca.comprar(valorCompra, confirmar);
        } else {
            confirmado = false;
        }
        if(!confirmado){
            ultimaMensagem = "Compra cancelada. Confirme com valor 1 no campo de confirmação.";
            return;
        }
        if(cliente instanceof ClienteWinx clienteWinx){
            clienteWinx.obterPontosDeCompra();
        }
        conta.setExtrato(new Movimentacao(valorCompra, Movimentacao.TipoDaMovimentacao.SAIDA));
        RegistroDeClientes.getInstancia().atualizarCliente(cliente);
        ultimaMensagem = "Compra realizada com sucesso.";
        ultimoDetalhe = resumoConta(conta);
    }

    private void pix(Map<String, String> form) throws InterruptedException {
        Cliente cliente = exigirLogin();
        Conta contaOrigem = selecionarConta(cliente, inteiro(form, "numeroContaPixOrigem"));
        String cpfDestino = texto(form, "cpfDestinoPix");
        Cliente clienteDestino = buscarClientePorCpf(cpfDestino);
        if(clienteDestino == null){
            throw new ClientNotFoundException();
        }
        Conta contaDestino = selecionarConta(clienteDestino, inteiro(form, "numeroContaPixDestino"));
        double valor = decimal(form, "valorPix");
        if(valor > contaOrigem.getSaldo()){
            throw new ValueIsHigherThanBalanceException();
        }
        contaOrigem.fazerPix(contaDestino, valor);
        contaOrigem.setExtrato(new Movimentacao(valor, Movimentacao.TipoDaMovimentacao.SAIDA));
        contaDestino.setExtrato(new Movimentacao(valor, Movimentacao.TipoDaMovimentacao.ENTRADA));
        RegistroDeClientes.getInstancia().atualizarCliente(cliente);
        RegistroDeClientes.getInstancia().atualizarCliente(clienteDestino);
        ultimaMensagem = "Pix realizado com sucesso.";
        ultimoDetalhe = "Origem:\n" + resumoConta(contaOrigem) + "\nDestino:\n" + resumoConta(contaDestino);
    }

    private void sacar(Map<String, String> form) throws InterruptedException {
        Cliente cliente = exigirLogin();
        Conta conta = selecionarConta(cliente, inteiro(form, "numeroContaSaque"));
        double valor = decimal(form, "valorSaque");
        if(valor > conta.getSaldo()){
            throw new ValueIsHigherThanBalanceException();
        }
        conta.sacar(valor);
        conta.setExtrato(new Movimentacao(valor, Movimentacao.TipoDaMovimentacao.SAIDA));
        RegistroDeClientes.getInstancia().atualizarCliente(cliente);
        ultimaMensagem = "Saque realizado com sucesso.";
        ultimoDetalhe = resumoConta(conta);
    }

    private void pagarFatura(Map<String, String> form) throws InterruptedException {
        Cliente cliente = exigirLogin();
        ContaCorrente conta = selecionarContaCorrente(cliente, inteiro(form, "numeroContaFatura"));
        double valor = decimal(form, "valorFatura");
        conta.pagarFatura(valor);
        conta.setExtrato(new Movimentacao(valor, Movimentacao.TipoDaMovimentacao.SAIDA));
        RegistroDeClientes.getInstancia().atualizarCliente(cliente);
        ultimaMensagem = "Fatura paga com sucesso.";
        ultimoDetalhe = resumoConta(conta);
    }

    private void ajustarLimite(Map<String, String> form) {
        Cliente cliente = exigirLogin();
        ContaCorrente conta = selecionarContaCorrente(cliente, inteiro(form, "numeroContaLimite"));
        double novoLimite = decimal(form, "novoLimite");
        conta.getCartaoCredito().ajustarLimite(novoLimite);
        ultimaMensagem = "Limite ajustado com sucesso.";
        ultimoDetalhe = resumoConta(conta);
    }

    private void pagarParcela(Map<String, String> form) throws InterruptedException {
        Cliente cliente = exigirLogin();
        Conta conta = selecionarConta(cliente, inteiro(form, "numeroContaParcela"));
        double valor = decimal(form, "valorParcela");
        conta.pagarParcelaDeEmprestimo(valor);
        conta.setExtrato(new Movimentacao(valor, Movimentacao.TipoDaMovimentacao.SAIDA));
        RegistroDeClientes.getInstancia().atualizarCliente(cliente);
        ultimaMensagem = "Parcela de empréstimo paga com sucesso.";
        ultimoDetalhe = resumoConta(conta);
    }

    private void requisitarEmprestimo(Map<String, String> form) throws InterruptedException {
        Cliente cliente = exigirLogin();
        Conta conta = selecionarConta(cliente, inteiro(form, "numeroContaEmprestimo"));
        double valor = decimal(form, "valorEmprestimo");
        conta.requisitarEmprestimo(valor);
        RegistroDeClientes.getInstancia().atualizarCliente(cliente);
        ultimaMensagem = "Empréstimo requisitado com sucesso.";
        ultimoDetalhe = resumoConta(conta);
    }

    private void converterPontos(Map<String, String> form) throws InterruptedException {
        Cliente cliente = exigirLogin();
        if(!(cliente instanceof ClienteWinx clienteWinx)){
            throw new ClientFoundIsNotClientWinxException();
        }
        if(clienteWinx.getPontosDeCompra() == 0){
            throw new NotEnaughPurchasePoints();
        }
        Conta conta = selecionarConta(cliente, inteiro(form, "numeroContaConverter"));
        clienteWinx.converterPontosEmSaldo(conta);
        RegistroDeClientes.getInstancia().atualizarCliente(cliente);
        ultimaMensagem = "Pontos convertidos em saldo com sucesso.";
        ultimoDetalhe = resumoConta(conta);
    }

    private void gerarExtrato(Map<String, String> form) throws IOException {
        Cliente cliente = exigirLogin();
        Conta conta = selecionarConta(cliente, inteiro(form, "numeroContaExtrato"));
        conta.gerarExtrato();
        ultimaMensagem = "Extrato gerado com sucesso no arquivo do projeto.";
        ultimoDetalhe = resumoExtrato(conta);
    }

    private void gerarInforme(Map<String, String> form) throws IOException {
        Cliente cliente = exigirLogin();
        ContaPoupanca conta = selecionarContaPoupanca(cliente, inteiro(form, "numeroContaInforme"));
        conta.gerarInformeRendimento();
        ultimaMensagem = "Informe de rendimento gerado com sucesso no arquivo do projeto.";
        ultimoDetalhe = resumoConta(conta);
    }

    private void exibirClientes() {
        ultimaMensagem = "Lista de clientes exibida.";
        ultimoDetalhe = resumoClientesTexto();
    }

    private void limparClientes() {
        RegistroDeClientes.getInstancia().limparListaDeClientes();
        clienteAtual = new Cliente();
        ultimaMensagem = "Lista de clientes limpa com sucesso.";
    }

    private void dadosBanco() {
        Banco banco = Banco.getInstancia();
        ultimaMensagem = "Dados do banco exibidos.";
        ultimoDetalhe = "Despesas do banco: " + MOEDA.format(banco.getDespesas()) + "\nReceitas do banco: " + MOEDA.format(banco.getReceitas());
    }

    private void logout() {
        clienteAtual = new Cliente();
        ultimaMensagem = "Sessão encerrada na interface web.";
    }

    private Cliente exigirLogin() {
        if(clienteAtual.getCpf() == null){
            throw new YouAreNotLoggedInException();
        }
        return clienteAtual;
    }

    private Conta selecionarConta(Cliente cliente, int numeroConta) {
        Conta conta = cliente.selecionarConta(numeroConta);
        if(conta == null){
            throw new BankAccountNotFoundException();
        }
        return conta;
    }

    private ContaCorrente selecionarContaCorrente(Cliente cliente, int numeroConta) {
        Conta conta = selecionarConta(cliente, numeroConta);
        if(!(conta instanceof ContaCorrente contaCorrente)){
            throw new BankAccountIsNotCurrentAccountException();
        }
        return contaCorrente;
    }

    private Cliente buscarClientePorCpf(String cpfInformado) {
        String cpfBusca = cpfNormalizado(cpfInformado);
        for (Cliente cliente : RegistroDeClientes.getInstancia().getClientes()) {
            if (cpfNormalizado(cliente.getCpf()).equals(cpfBusca)) {
                return cliente;
            }
        }
        return null;
    }

    private String cpfNormalizado(String cpf) {
        if (cpf == null) {
            return "";
        }
        return cpf.replaceAll("\\D", "").trim();
    }

    private ContaPoupanca selecionarContaPoupanca(Cliente cliente, int numeroConta) {
        Conta conta = selecionarConta(cliente, numeroConta);
        if(!(conta instanceof ContaPoupanca contaPoupanca)){
            throw new BankAccountIsNotSavingsAccountException();
        }
        return contaPoupanca;
    }

    private Cliente copiarClienteParaSessao(Cliente cliente) {
        Cliente copia;
        if(cliente.getClass() == ClienteWinx.class){
            copia = new ClienteWinx(cliente);
        }
        else{
            copia = new Cliente(cliente);
        }
        copia.setContas(cliente.getContas());
        return copia;
    }

    private void persistir() {
        try {
            ArquivoDeClientes.getInstancia().escreverJson(RegistroDeClientes.getInstancia().getClientes());
            ArquivoDeMesAtual.getInstancia().escreverMesAtual();
            ArquivoBanco.getInstancia().atualizarArquivo(Banco.getInstancia());
        } catch (Exception e) {
            if (ultimaMensagem == null || ultimaMensagem.isBlank()) {
                ultimaMensagem = "Ação executada, mas ocorreu erro ao persistir os dados: " + mensagemErro(e);
            }
        }
    }

    private int inteiro(Map<String, String> form, String campo) {
        return Integer.parseInt(texto(form, campo));
    }

    private double decimal(Map<String, String> form, String campo) {
        String valor = texto(form, campo).replace(',', '.');
        return Double.parseDouble(valor);
    }

    private String texto(Map<String, String> form, String campo) {
        String valor = form.getOrDefault(campo, "").trim();
        if (valor.isEmpty()) {
            throw new IllegalArgumentException("Campo obrigatório não preenchido: " + campo);
        }
        return valor;
    }

    private String mensagemErro(Exception e) {
        String msg = e.getMessage();
        if(msg == null || msg.isBlank()){
            msg = e.getClass().getSimpleName();
        }
        return msg;
    }

    private String menuNavegacao() {
        String[][] links = {
                {"home", "Home"},
                {"sec-cadastrar", "Criar usuário"},
                {"sec-login", "Login"},
                {"sec-abrirConta", "Abrir conta"},
                {"sec-fecharConta", "Fechar conta"},
                {"sec-apagarUsuario", "Apagar usuário"},
                {"sec-depositar", "Depositar"},
                {"sec-comprar", "Comprar"},
                {"sec-pix", "Pix"},
                {"sec-sacar", "Sacar"},
                {"sec-pagarFatura", "Pagar fatura"},
                {"sec-ajustarLimite", "Ajustar limite"},
                {"sec-pagarParcela", "Pagar parcela"},
                {"sec-requisitarEmprestimo", "Empréstimo"},
                {"sec-converterPontos", "Converter pontos"},
                {"sec-gerarExtrato", "Gerar extrato"},
                {"sec-gerarInforme", "Gerar informe"},
                {"sec-exibirClientes", "Exibir clientes"},
                {"sec-limparClientes", "Limpar clientes"},
                {"sec-dadosBanco", "Dados do banco"}
        };
        StringBuilder sb = new StringBuilder("<nav id=\"menu-principal\" class=\"top-nav\" aria-label=\"Menu principal\">");
        for(String[] link : links){
            String navId = link[0].replace("sec-", "nav-");
            if("home".equals(link[0])){
                navId = "nav-home";
            }
            sb.append("<a id=\"").append(navId).append("\" data-testid=\"").append(navId).append("\" href=\"#").append(link[0]).append("\">")
                    .append(esc(link[1])).append("</a>");
        }
        sb.append("</nav>");
        return sb.toString();
    }

    private String secaoHome() {
        return """
                <section id=\"home\" class=\"home-page\" data-testid=\"home-page\">
                    <div class=\"hero-desktop\">
                        <div class=\"hero-copy\">
                            <span class=\"hero-kicker\">BankApp para desktop</span>
                            <h2>Seu banco em uma tela web moderna.</h2>
                            <p>A experiência agora fica parecida com um app bancário, mas adaptada para computador: menu superior, dashboard, cartões de operação e inputs HTML para automatizar com Selenium.</p>
                            <div class=\"hero-buttons\">
                                <a class=\"hero-button\" id=\"home-cta-cadastrar\" href=\"#sec-cadastrar\">Criar usuário</a>
                                <a class=\"hero-button secondary-link\" id=\"home-cta-login\" href=\"#sec-login\">Fazer login</a>
                            </div>
                        </div>
                        <div class=\"desktop-preview\" aria-hidden=\"true\">
                            <div class=\"browser-window\">
                                <div class=\"browser-top\"><span class=\"dot\"></span><span class=\"dot\"></span><span class=\"dot\"></span><span class=\"address\"></span></div>
                                <div class=\"dashboard-preview\">
                                    <div class=\"preview-card dark\">
                                        <h3>Saldo disponível</h3>
                                        <p class=\"amount\">R$ 5.634,12</p>
                                        <p class=\"green-line\">+12,4% este mês</p>
                                    </div>
                                    <div class=\"preview-card\">
                                        <h3>Orçamento</h3>
                                        <p class=\"amount\">R$ 2.000</p>
                                        <div class=\"mini-chart\"></div>
                                    </div>
                                    <div class=\"preview-card\">
                                        <h3>Transações</h3>
                                        <ul class=\"preview-list\"><li><span>Depósito</span><strong class=\"green-line\">+250</strong></li><li><span>Pix</span><strong>-80</strong></li><li><span>Compra</span><strong>-120</strong></li></ul>
                                    </div>
                                    <div class=\"preview-card\">
                                        <h3>Atalhos</h3>
                                        <ul class=\"preview-list\"><li><span>Pix</span><strong>→</strong></li><li><span>Extrato</span><strong>→</strong></li><li><span>Fatura</span><strong>→</strong></li></ul>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class=\"home-grid\">
                        <a class=\"home-card\" id=\"home-card-cadastro\" href=\"#sec-cadastrar\"><strong>Cadastro e acesso</strong><span>Criar usuário, fazer login e gerenciar o cliente atual.</span></a>
                        <a class=\"home-card\" id=\"home-card-contas\" href=\"#sec-abrirConta\"><strong>Contas</strong><span>Abrir, fechar, depositar, sacar e consultar contas.</span></a>
                        <a class=\"home-card\" id=\"home-card-operacoes\" href=\"#sec-comprar\"><strong>Operações bancárias</strong><span>Comprar, Pix, fatura, limite, empréstimo e parcelas.</span></a>
                        <a class=\"home-card\" id=\"home-card-relatorios\" href=\"#sec-gerarExtrato\"><strong>Relatórios</strong><span>Gerar extrato, informe de rendimento e visualizar dados do banco.</span></a>
                    </div>
                    <p class=\"info\">Mês atual: <strong>"""
                + esc(Ano.getInstancia().getMesAtual())
                + "</strong> · Clientes cadastrados: <strong>"
                + RegistroDeClientes.getInstancia().getClientes().size()
                + "</strong></p></section>";
    }

    private String renderizarClienteAtual() {
        StringBuilder sb = new StringBuilder();
        sb.append("<section id=\"cliente-atual\"><h2>Minha conta</h2>");
        if(clienteAtual.getCpf() == null){
            sb.append("<div class=\"account-total\"><small>Nenhum cliente logado</small><strong>R$ 0,00</strong></div>");
            sb.append("<p class=\"info\">Faça login para visualizar contas, saldo, extrato e dados do cliente.</p>");
        } else {
            double saldoTotal = 0;
            double dividaTotal = 0;
            for(Conta conta : clienteAtual.getContas()){
                saldoTotal += conta.getSaldo();
                dividaTotal += conta.getDividaDeEmprestimo();
            }
            sb.append("<p><span class=\"badge\">").append(clienteAtual instanceof ClienteWinx ? "ClienteWinx" : "Cliente").append("</span></p>");
            sb.append("<div class=\"account-total\"><small>Saldo total</small><strong>R$ ").append(MOEDA.format(saldoTotal)).append("</strong></div>");
            sb.append("<div class=\"quick-meta\"><div><span>Cliente</span><strong>").append(esc(clienteAtual.getNome())).append("</strong></div><div><span>CPF</span><strong>").append(esc(clienteAtual.getCpf())).append("</strong></div></div>");
            sb.append("<div class=\"quick-meta\"><div><span>Contas</span><strong>").append(clienteAtual.getContas().size()).append("</strong></div><div><span>Dívida</span><strong>R$ ").append(MOEDA.format(dividaTotal)).append("</strong></div></div>");
            if(clienteAtual instanceof ClienteWinx clienteWinx){
                sb.append("<p class=\"info\"><strong>Pontos:</strong> ").append(clienteWinx.getPontosDeCompra()).append("</p>");
            }
            sb.append(renderizarTabelaContas(clienteAtual.getContas()));
            sb.append(formLogout());
        }
        sb.append("</section>");
        return sb.toString();
    }

    private String renderizarDetalhe() {
        if(ultimoDetalhe == null || ultimoDetalhe.isBlank()){
            return "";
        }
        return "<h2>Detalhe da operação</h2><div id=\"detalhe-operacao\" class=\"detalhe\">" + esc(ultimoDetalhe) + "</div>";
    }

    private String renderizarTabelaContas(ArrayList<Conta> contas) {
        if(contas.isEmpty()){
            return "<p class=\"info\">Este cliente não possui contas cadastradas.</p>";
        }
        StringBuilder sb = new StringBuilder("<table id=\"tabela-contas\"><thead><tr><th>Tipo</th><th>Nº</th><th>Saldo</th><th>Dívida</th></tr></thead><tbody>");
        for(Conta conta : contas){
            sb.append("<tr><td>").append(tipoConta(conta)).append("</td><td>").append(conta.getNumeroConta()).append("</td><td>R$ ").append(MOEDA.format(conta.getSaldo())).append("</td><td>R$ ").append(MOEDA.format(conta.getDividaDeEmprestimo())).append("</td></tr>");
        }
        sb.append("</tbody></table>");
        return sb.toString();
    }

    private String resumoCliente(Cliente cliente) {
        StringBuilder sb = new StringBuilder();
        sb.append(cliente instanceof ClienteWinx ? "ClienteWinx" : "Cliente").append("\n");
        sb.append("Nome: ").append(cliente.getNome()).append("\nCPF: ").append(cliente.getCpf()).append("\n");
        if(cliente instanceof ClienteWinx clienteWinx){
            sb.append("Pontos por compra: ").append(clienteWinx.getPontosDeCompra()).append("\n");
        }
        for(Conta conta : cliente.getContas()){
            sb.append("\n").append(resumoConta(conta));
        }
        return sb.toString();
    }

    private String resumoConta(Conta conta) {
        StringBuilder sb = new StringBuilder();
        sb.append(tipoConta(conta)).append(" nº ").append(conta.getNumeroConta()).append("\n");
        sb.append("Saldo: R$ ").append(MOEDA.format(conta.getSaldo())).append("\n");
        sb.append("Dívida de empréstimo: R$ ").append(MOEDA.format(conta.getDividaDeEmprestimo())).append("\n");
        sb.append("Cartão débito: ").append(conta.getCartao().getNumero()).append(" | CSV: ").append(conta.getCartao().getCsv()).append("\n");
        if(conta instanceof ContaCorrente contaCorrente){
            sb.append("Cartão crédito: ").append(contaCorrente.getCartaoCredito().getNumero()).append(" | CSV: ").append(contaCorrente.getCartaoCredito().getCsv()).append("\n");
            sb.append("Fatura: R$ ").append(MOEDA.format(contaCorrente.getCartaoCredito().getFatura())).append("\n");
            sb.append("Limite: R$ ").append(MOEDA.format(contaCorrente.getCartaoCredito().getLimite())).append("\n");
        }
        sb.append("Movimentações no extrato: ").append(conta.getExtrato().size());
        return sb.toString();
    }

    private String resumoExtrato(Conta conta) {
        StringBuilder sb = new StringBuilder(resumoConta(conta)).append("\n\nExtrato:\n");
        for(Movimentacao movimentacao : conta.getExtrato()){
            sb.append(movimentacao.getMesAtual()).append(" - ").append(movimentacao.getTipoDaMovimentacao()).append(" - R$ ").append(MOEDA.format(movimentacao.getDinheiroMovimentado())).append("\n");
        }
        return sb.toString();
    }

    private String resumoClientesTexto() {
        StringBuilder sb = new StringBuilder();
        for(Cliente cliente : RegistroDeClientes.getInstancia().getClientes()){
            sb.append(resumoCliente(cliente)).append("\n------------------------------\n");
        }
        if(sb.isEmpty()){
            return "Nenhum cliente cadastrado.";
        }
        return sb.toString();
    }

    private String tipoConta(Conta conta) {
        if(conta instanceof ContaCorrente){
            return "Corrente";
        }
        if(conta instanceof ContaPoupanca){
            return "Poupança";
        }
        return conta.getClass().getSimpleName();
    }

    private String form(String titulo, String acao, String campos, String botao) {
        return "<article class=\"card funcionalidade\" id=\"sec-" + esc(acao) + "\" data-testid=\"sec-" + esc(acao) + "\"><a class=\"voltar-home\" href=\"#home\">↑ Voltar para Home</a><h2>" + esc(titulo) + "</h2><form method=\"post\" action=\"/acao\"><input type=\"hidden\" name=\"acao\" value=\"" + esc(acao) + "\">" + campos + "<button id=\"btn-" + esc(acao) + "\" type=\"submit\">" + esc(botao) + "</button></form></article>";
    }

    private String input(String id, String label, String type, String value) {
        String step = "number".equals(type) ? " step=\"0.01\"" : "";
        return "<label for=\"" + id + "\">" + esc(label) + "</label><input id=\"" + id + "\" name=\"" + id + "\" type=\"" + type + "\" value=\"" + esc(value) + "\"" + step + " required>";
    }

    private String selectTipoConta(String id) {
        return "<label for=\"" + id + "\">Tipo de conta</label><select id=\"" + id + "\" name=\"" + id + "\"><option value=\"1\">1 - Corrente</option><option value=\"2\">2 - Poupança</option></select>";
    }

    private String formCadastrar() {
        return form("1 - Criar usuário", "cadastrar",
                input("nome", "Nome", "text", "") +
                input("cpf", "CPF", "text", "") +
                selectTipoConta("tipoConta") +
                input("saldoInicial", "Saldo inicial", "number", "0"), "Cadastrar");
    }

    private String formLogin() {
        return form("2 - Logar em um usuário", "login", input("cpfLogin", "CPF do usuário", "text", ""), "Entrar");
    }

    private String formAbrirConta() {
        return form("3 - Abrir conta", "abrirConta", selectTipoConta("tipoContaAbrir") + input("saldoInicialAbrir", "Saldo inicial", "number", "0"), "Abrir conta");
    }

    private String formFecharConta() {
        return form("4 - Fechar conta", "fecharConta", input("numeroContaFechar", "Número da conta", "number", ""), "Fechar conta");
    }

    private String formApagarUsuario() {
        return form("5 - Apagar usuário", "apagarUsuario", "<p class=\"info\">Apaga o cliente atualmente logado.</p>", "Apagar usuário").replace("<button", "<button class=\"danger\"");
    }

    private String formDepositar() {
        return form("6 - Depositar", "depositar", input("numeroContaDeposito", "Número da conta", "number", "") + input("valorDeposito", "Valor do depósito", "number", "0"), "Depositar");
    }

    private String formComprar() {
        return form("7 - Comprar", "comprar",
                input("numeroContaCompra", "Número da conta", "number", "") +
                input("valorCompra", "Valor da compra", "number", "0") +
                "<label for=\"formaPagamentoCompra\">Forma de pagamento</label><select id=\"formaPagamentoCompra\" name=\"formaPagamentoCompra\"><option value=\"1\">1 - Débito</option><option value=\"2\">2 - Crédito</option></select>" +
                input("confirmarCartaoCompra", "Confirmar cartão (digite 1)", "number", "1"), "Comprar");
    }

    private String formPix() {
        return form("8 - Fazer Pix", "pix",
                input("numeroContaPixOrigem", "Conta de origem", "number", "") +
                input("cpfDestinoPix", "CPF de destino", "text", "") +
                input("numeroContaPixDestino", "Conta de destino", "number", "") +
                input("valorPix", "Valor do Pix", "number", "0"), "Fazer Pix");
    }

    private String formSacar() {
        return form("9 - Sacar", "sacar", input("numeroContaSaque", "Número da conta", "number", "") + input("valorSaque", "Valor do saque", "number", "0"), "Sacar");
    }

    private String formPagarFatura() {
        return form("10 - Pagar fatura", "pagarFatura", input("numeroContaFatura", "Número da conta corrente", "number", "") + input("valorFatura", "Valor a pagar", "number", "0"), "Pagar fatura");
    }

    private String formAjustarLimite() {
        return form("11 - Ajustar limite", "ajustarLimite", input("numeroContaLimite", "Número da conta corrente", "number", "") + input("novoLimite", "Novo limite", "number", "1000"), "Ajustar limite");
    }

    private String formPagarParcela() {
        return form("12 - Pagar parcela de empréstimo", "pagarParcela", input("numeroContaParcela", "Número da conta", "number", "") + input("valorParcela", "Valor da parcela", "number", "0"), "Pagar parcela");
    }

    private String formRequisitarEmprestimo() {
        return form("13 - Requisitar empréstimo", "requisitarEmprestimo", input("numeroContaEmprestimo", "Número da conta", "number", "") + input("valorEmprestimo", "Valor do empréstimo", "number", "0"), "Requisitar empréstimo");
    }

    private String formConverterPontos() {
        return form("14 - Converter pontos em saldo", "converterPontos", input("numeroContaConverter", "Número da conta", "number", ""), "Converter pontos");
    }

    private String formGerarExtrato() {
        return form("15 - Gerar extrato", "gerarExtrato", input("numeroContaExtrato", "Número da conta", "number", ""), "Gerar extrato");
    }

    private String formGerarInforme() {
        return form("16 - Gerar informe de rendimento", "gerarInforme", input("numeroContaInforme", "Número da conta poupança", "number", ""), "Gerar informe");
    }

    private String formExibirClientes() {
        return form("17 - Exibir clientes", "exibirClientes", "<p class=\"info\">Mostra todos os clientes cadastrados.</p>", "Exibir clientes");
    }

    private String formLimparClientes() {
        return form("18 - Limpar clientes", "limparClientes", "<p class=\"info\">Remove todos os clientes do registro.</p>", "Limpar clientes").replace("<button", "<button class=\"danger\"");
    }

    private String formDadosBanco() {
        return form("19 - Visualizar dados do banco", "dadosBanco", "<p class=\"info\">Exibe receitas e despesas acumuladas.</p>", "Ver dados do banco");
    }

    private String formLogout() {
        return "<form method=\"post\" action=\"/acao\"><input type=\"hidden\" name=\"acao\" value=\"logout\"><button id=\"btn-logout\" class=\"secondary\" type=\"submit\">Sair do cliente atual</button></form>";
    }

    private String esc(String valor) {
        if(valor == null){
            return "";
        }
        return valor.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String json(String valor) {
        if(valor == null){
            return "";
        }
        return valor.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
