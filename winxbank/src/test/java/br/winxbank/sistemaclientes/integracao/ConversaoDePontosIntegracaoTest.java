package br.winxbank.sistemaclientes.integracao;

import br.winxbank.exception.NotEnaughPurchasePoints;
import br.winxbank.sistemabancario.Cartao;
import br.winxbank.sistemabancario.CartaoCredito;
import br.winxbank.sistemabancario.Conta;
import br.winxbank.sistemabancario.ContaCorrente;
import br.winxbank.sistemabancario.ContaPoupanca;
import br.winxbank.sistemabancario.Movimentacao;
import br.winxbank.sistemaclientes.AuditoriaDeConversaoDePontos;
import br.winxbank.sistemaclientes.ClienteWinx;
import br.winxbank.sistemaclientes.PoliticaDeConversaoPadrao;
import br.winxbank.tempo.Ano;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes de INTEGRAÇÃO das três classes da Entrega 1 trabalhando juntas, SEM mocks:
 *   ClienteWinx + PoliticaDeConversaoPadrao (regra real) + Movimentacao (entidade real) + Conta (real).
 *
 * Diferente dos testes unitários (que isolam dependências com mocks), aqui validamos
 * a colaboração real entre os objetos: a política padrão calcula o valor, o ClienteWinx
 * aplica na conta, gera uma Movimentacao real e a auditoria recebe os dados consolidados.
 *
 * Uma auditoria "espiã" simples (sem framework de mock) registra as chamadas para
 * confirmar o contrato de integração ponta a ponta.
 */
class ConversaoDePontosIntegracaoTest {

    private static final double DELTA = 0.0001;
    private static final String MES_FIXO = "Abril";

    private Field instanciaAno;

    /**
     * Auditoria de verdade (não é mock): guarda o que foi auditado para inspeção.
     */
    private static class AuditoriaEspia implements AuditoriaDeConversaoDePontos {
        int chamadas = 0;
        ClienteWinx ultimoCliente;
        Conta ultimaConta;
        int ultimosPontos;
        double ultimoValor;
        Movimentacao ultimaMovimentacao;

        @Override
        public void registrarConversao(ClienteWinx cliente, Conta conta, int pontosConvertidos,
                                       double valorConvertido, Movimentacao movimentacaoGerada) {
            chamadas++;
            ultimoCliente = cliente;
            ultimaConta = conta;
            ultimosPontos = pontosConvertidos;
            ultimoValor = valorConvertido;
            ultimaMovimentacao = movimentacaoGerada;
        }
    }

    @BeforeEach
    void preparar() throws Exception {
        // Movimentacao usa o singleton Ano; fixamos o mês para resultados determinísticos.
        instanciaAno = Ano.class.getDeclaredField("instancia");
        instanciaAno.setAccessible(true);
        instanciaAno.set(null, null);
        Ano.getInstancia().setMesAtual(MES_FIXO);
    }

    @AfterEach
    void limpar() throws Exception {
        instanciaAno.set(null, null);
    }

    private static ContaCorrente criarContaCorrente(double saldoInicial) {
        Cartao debito = new Cartao(1, 1);
        CartaoCredito credito = new CartaoCredito(2, 2);
        return new ContaCorrente(1001, saldoInicial, debito, 0.0, credito);
    }

    private static ContaPoupanca criarContaPoupanca(double saldoInicial) {
        Cartao debito = new Cartao(3, 3);
        return new ContaPoupanca(2002, saldoInicial, debito, 0.0);
    }

    @Test
    // Integração feliz: política padrão (10 pts -> 30) aplicada numa conta corrente real,
    // com movimentação real adicionada ao extrato e auditoria real acionada.
    void conversaoComPoliticaPadraoRealAplicaSaldoGeraMovimentacaoEAudita() {
        AuditoriaEspia auditoria = new AuditoriaEspia();
        ClienteWinx cliente = new ClienteWinx("ANA", "123", 10, new PoliticaDeConversaoPadrao(), auditoria);
        ContaCorrente conta = criarContaCorrente(0.0);

        cliente.converterPontosEmSaldo(conta);

        // PoliticaDeConversaoPadrao: 10 pontos * 3 = 30
        assertEquals(30.0, conta.getSaldo(), DELTA);
        assertEquals(0, cliente.getPontosDeCompra());

        // Movimentacao real foi criada e adicionada ao extrato com o mês fixado.
        Movimentacao ultima = conta.getExtrato().get(conta.getExtrato().size() - 1);
        assertSame(Movimentacao.TipoDaMovimentacao.ENTRADA, ultima.getTipoDaMovimentacao());
        assertEquals(30.0, ultima.getDinheiroMovimentado(), DELTA);
        assertEquals(MES_FIXO, ultima.getMesAtual());

        // Auditoria real recebeu os dados consolidados da operação.
        assertEquals(1, auditoria.chamadas);
        assertSame(cliente, auditoria.ultimoCliente);
        assertSame(conta, auditoria.ultimaConta);
        assertEquals(10, auditoria.ultimosPontos);
        assertEquals(30.0, auditoria.ultimoValor, DELTA);
        assertSame(ultima, auditoria.ultimaMovimentacao);
    }

    @Test
    // Integração no valor-limite da regra real: exatamente o mínimo (10) deve converter.
    void conversaoNoLimiteMinimoDaPoliticaPadraoRealConverte() {
        AuditoriaEspia auditoria = new AuditoriaEspia();
        ClienteWinx cliente = new ClienteWinx("ANA", "123",
                PoliticaDeConversaoPadrao.MINIMO_PONTOS, new PoliticaDeConversaoPadrao(), auditoria);
        ContaCorrente conta = criarContaCorrente(0.0);

        cliente.converterPontosEmSaldo(conta);

        assertEquals(30.0, conta.getSaldo(), DELTA);
        assertEquals(1, auditoria.chamadas);
    }

    @Test
    // Integração no caminho de rejeição: 1 ponto abaixo do mínimo, política real lança exceção,
    // a conta NÃO é alterada e a auditoria NÃO é acionada.
    void conversaoAbaixoDoMinimoDaPoliticaPadraoRealLancaExcecaoENaoAlteraConta() {
        AuditoriaEspia auditoria = new AuditoriaEspia();
        ClienteWinx cliente = new ClienteWinx("ANA", "123",
                PoliticaDeConversaoPadrao.MINIMO_PONTOS - 1, new PoliticaDeConversaoPadrao(), auditoria);
        ContaCorrente conta = criarContaCorrente(50.0);

        assertThrows(NotEnaughPurchasePoints.class, () -> cliente.converterPontosEmSaldo(conta));

        assertEquals(50.0, conta.getSaldo(), DELTA);
        assertEquals(PoliticaDeConversaoPadrao.MINIMO_PONTOS - 1, cliente.getPontosDeCompra());
        assertEquals(0, auditoria.chamadas);
        assertTrue(conta.getExtrato().isEmpty());
    }

    @Test
    // Integração com conta poupança real: a conversão também funciona somando ao saldo existente.
    void conversaoEmContaPoupancaRealSomaAoSaldoExistente() {
        AuditoriaEspia auditoria = new AuditoriaEspia();
        ClienteWinx cliente = new ClienteWinx("ANA", "123", 20, new PoliticaDeConversaoPadrao(), auditoria);
        ContaPoupanca conta = criarContaPoupanca(100.0);

        cliente.converterPontosEmSaldo(conta);

        // 20 pontos * 3 = 60, somado aos 100 já existentes.
        assertEquals(160.0, conta.getSaldo(), DELTA);
        assertEquals(60.0, auditoria.ultimoValor, DELTA);
        assertEquals(20, auditoria.ultimosPontos);
    }

    @Test
    // Integração com fluxo completo do cliente: acumula pontos via obterPontosDeCompra e converte.
    void clienteAcumulaPontosComObterPontosEConverteComPoliticaReal() {
        AuditoriaEspia auditoria = new AuditoriaEspia();
        ClienteWinx cliente = new ClienteWinx("ANA", "123", 8, new PoliticaDeConversaoPadrao(), auditoria);
        ContaCorrente conta = criarContaCorrente(0.0);

        // 8 -> 9 -> 10 (atinge o mínimo da política real).
        cliente.obterPontosDeCompra();
        cliente.obterPontosDeCompra();

        cliente.converterPontosEmSaldo(conta);

        assertEquals(30.0, conta.getSaldo(), DELTA);
        assertEquals(0, cliente.getPontosDeCompra());
        assertEquals(1, auditoria.chamadas);
    }

    @Test
    // Integração: o cliente pode ter várias contas; a conversão afeta apenas a conta escolhida.
    void conversaoAfetaApenasAContaSelecionadaEntreVariasContasDoCliente() {
        AuditoriaEspia auditoria = new AuditoriaEspia();
        ClienteWinx cliente = new ClienteWinx("ANA", "123", 10, new PoliticaDeConversaoPadrao(), auditoria);
        ContaCorrente contaAlvo = criarContaCorrente(0.0);
        ContaPoupanca outraConta = criarContaPoupanca(0.0);
        cliente.setContas(new ArrayList<>(List.of(contaAlvo, outraConta)));

        cliente.converterPontosEmSaldo(contaAlvo);

        assertEquals(30.0, contaAlvo.getSaldo(), DELTA);
        assertEquals(0.0, outraConta.getSaldo(), DELTA);
        assertTrue(outraConta.getExtrato().isEmpty());
    }
}
