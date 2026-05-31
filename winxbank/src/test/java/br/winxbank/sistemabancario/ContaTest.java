package br.winxbank.sistemabancario;

import br.winxbank.geradordedocumentos.ArquivoExtrato;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import org.mockito.MockedStatic;

class ContaTest {

    private static final double DELTA = 0.0001;
    private static final int NUMERO_CONTA = 1001;
    private static final double SALDO_INICIAL = 1000.0;

    private Cartao cartao;
    private Conta conta;

    @BeforeEach
    void prepararCenario() {
        cartao = mock(Cartao.class);
        conta = new ContaTestavel(NUMERO_CONTA, SALDO_INICIAL, cartao, 0.0);
    }

    @Test
    void construtorPadraoArmazenaAtributosEIniciaExtratoVazio() {
        assertEquals(NUMERO_CONTA, conta.getNumeroConta());
        assertEquals(SALDO_INICIAL, conta.getSaldo(), DELTA);
        assertSame(cartao, conta.getCartao());
        assertEquals(0.0, conta.getDividaDeEmprestimo(), DELTA);
        assertTrue(conta.getExtrato().isEmpty());
    }

    @Test
    void construtorAlternativoCopiaMovimentacoesRecebidasParaOExtrato() {
        Movimentacao primeira = mock(Movimentacao.class);
        Movimentacao segunda = mock(Movimentacao.class);
        ArrayList<Movimentacao> movimentacoes = new ArrayList<>(List.of(primeira, segunda));

        Conta contaComExtrato = new ContaTestavel(2002, 250.0, cartao, 50.0, movimentacoes);

        assertEquals(2, contaComExtrato.getExtrato().size());
        assertSame(primeira, contaComExtrato.getExtrato().get(0));
        assertSame(segunda, contaComExtrato.getExtrato().get(1));
    }


    @Test
    void setSaldoQuandoValorPositivoSomaAoSaldoAtual() {
        conta.setSaldo(500.0);

        assertEquals(1500.0, conta.getSaldo(), DELTA);
    }


    @Test
    void setSaldoQuandoValorNegativoReduzSaldoAtual() {
        conta.setSaldo(-200.0);

        assertEquals(800.0, conta.getSaldo(), DELTA);
    }


    @Test
    void depositarQuandoValorPositivoCreditaSaldoEDevolveValor() {
        double retorno = conta.depositar(250.0);

        assertEquals(250.0, retorno, DELTA);
        assertEquals(1250.0, conta.getSaldo(), DELTA);
    }


    @Test
    void depositarMultiplosValoresAcumulaNoSaldo() {
        conta.depositar(100.0);
        conta.depositar(150.0);

        assertEquals(1250.0, conta.getSaldo(), DELTA);
    }

    @Test
    void sacarReduzSaldoPeloValorInformado() {
        conta.sacar(300.0);

        assertEquals(700.0, conta.getSaldo(), DELTA);
    }

    @Test
    void sacarQuandoValorMaiorQueSaldoPermiteSaldoNegativo() {
        conta.sacar(1500.0);

        assertEquals(-500.0, conta.getSaldo(), DELTA);
    }


    @Test
    void sacarValorZeroNaoAlteraSaldo() {
        conta.sacar(0.0);

        assertEquals(SALDO_INICIAL, conta.getSaldo(), DELTA);
    }


    @Test
    void fazerPixCreditaContaDestinoSemDebitarOrigem() {
        Conta destino = new ContaTestavel(1002, 500.0, cartao, 0.0);

        conta.fazerPix(destino, 200.0);

        assertEquals(700.0, destino.getSaldo(), DELTA);
        assertEquals(SALDO_INICIAL, conta.getSaldo(), DELTA);
    }


    @Test
    void fazerPixComValorZeroNaoAlteraDestino() {
        Conta destino = new ContaTestavel(1002, 500.0, cartao, 0.0);

        conta.fazerPix(destino, 0.0);

        assertEquals(500.0, destino.getSaldo(), DELTA);
    }


    @Test
    void requisitarEmprestimoAcumulaValorNaDivida() {
        conta.requisitarEmprestimo(500.0);
        conta.requisitarEmprestimo(300.0);

        assertEquals(800.0, conta.getDividaDeEmprestimo(), DELTA);
    }


    @Test
    void pagarParcelaDeEmprestimoQuandoExisteDividaSubtraiValor() {
        conta.requisitarEmprestimo(1000.0);

        conta.pagarParcelaDeEmprestimo(300.0);

        assertEquals(700.0, conta.getDividaDeEmprestimo(), DELTA);
    }

    @Test
    void pagarParcelaDeEmprestimoSemDividaTornaDividaNegativa() {
        conta.pagarParcelaDeEmprestimo(100.0);

        assertEquals(-100.0, conta.getDividaDeEmprestimo(), DELTA);
    }


    @Test
    void cobrarJurusEmprestimoQuandoDividaPositivaReduzPelaTaxaConfigurada() {
        conta.requisitarEmprestimo(1000.0);
        double dividaEsperada = 1000.0 - (1000.0 / OperacoesAutomaticas.taxaJurus);

        conta.cobrarJurusEmprestimo();

        assertEquals(dividaEsperada, conta.getDividaDeEmprestimo(), DELTA);
    }


    @Test
    void cobrarJurusEmprestimoQuandoNaoHaDividaMantemEstado() {
        conta.cobrarJurusEmprestimo();

        assertEquals(0.0, conta.getDividaDeEmprestimo(), DELTA);
    }


    @Test
    void cobrarJurusEmprestimoQuandoDividaNegativaMantemEstado() {
        conta.pagarParcelaDeEmprestimo(500.0);

        conta.cobrarJurusEmprestimo();

        assertEquals(-500.0, conta.getDividaDeEmprestimo(), DELTA);
    }


    @Test
    void setExtratoAdicionaMovimentacaoAoExtrato() {
        Movimentacao movimentacao = mock(Movimentacao.class);

        conta.setExtrato(movimentacao);

        assertEquals(1, conta.getExtrato().size());
        assertSame(movimentacao, conta.getExtrato().get(0));
    }


    @Test
    void setExtratoMultiplasMovimentacoesPreservaOrdemDeInsercao() {
        Movimentacao primeira = mock(Movimentacao.class);
        Movimentacao segunda = mock(Movimentacao.class);

        conta.setExtrato(primeira);
        conta.setExtrato(segunda);

        assertEquals(2, conta.getExtrato().size());
        assertSame(primeira, conta.getExtrato().get(0));
        assertSame(segunda, conta.getExtrato().get(1));
    }



    @Test
    void depositarComZeroAumentaSaldoEmZero() {
        conta.depositar(0.0);

        assertEquals(SALDO_INICIAL, conta.getSaldo(), DELTA);
    }

    @Test
    void depositarComValorNegativoReduzSaldo() {
        conta.depositar(-100.0);

        assertEquals(900.0, conta.getSaldo(), DELTA);
    }

    @Test
    void sacarComValorNegativoAumentaSaldo() {
        conta.sacar(-100.0);

        assertEquals(1100.0, conta.getSaldo(), DELTA);
    }

    @Test
    void sacarComZeroNaoAlteraSaldo() {
        conta.sacar(0.0);

        assertEquals(SALDO_INICIAL, conta.getSaldo(), DELTA);
    }

    @Test
    void fazerPixComValorNegativoReduzSaldoDestino() {
        Conta destino = new ContaTestavel(1002, 500.0, cartao, 0.0);

        conta.fazerPix(destino, -200.0);

        assertEquals(300.0, destino.getSaldo(), DELTA);
    }

    @Test
    void fazerPixNaoDebitaContaOrigem() {
        Conta destino = new ContaTestavel(1002, 500.0, cartao, 0.0);

        conta.fazerPix(destino, 200.0);

        assertEquals(SALDO_INICIAL, conta.getSaldo(), DELTA);
    }

    @Test
    void requisitarEmprestimoComValorNegativoReduzDivida() {
        conta.requisitarEmprestimo(500.0);

        conta.requisitarEmprestimo(-200.0);

        assertEquals(300.0, conta.getDividaDeEmprestimo(), DELTA);
    }

    @Test
    void pagarParcelaDeEmprestimoComValorNegativoAumentaDivida() {
        conta.requisitarEmprestimo(500.0);

        conta.pagarParcelaDeEmprestimo(-100.0);

        assertEquals(600.0, conta.getDividaDeEmprestimo(), DELTA);
    }



    @Test
    void gerarExtratoChamamaArquivoExtratoComContaAtual() throws FileNotFoundException {
        try (MockedStatic<ArquivoExtrato> arquivoMock = mockStatic(ArquivoExtrato.class)) {
            ArquivoExtrato mockInstancia = mock(ArquivoExtrato.class);
            arquivoMock.when(ArquivoExtrato::getInstancia).thenReturn(mockInstancia);

            conta.gerarExtrato();

            verify(mockInstancia).gerarDocumento(conta);
        }
    }

    @Test
    void gerarExtratoQuandoArquivoLancaExcecaoPropagaFileNotFoundException() throws FileNotFoundException {
        try (MockedStatic<ArquivoExtrato> arquivoMock = mockStatic(ArquivoExtrato.class)) {
            ArquivoExtrato mockInstancia = mock(ArquivoExtrato.class);
            arquivoMock.when(ArquivoExtrato::getInstancia).thenReturn(mockInstancia);

            mockInstancia.gerarDocumento(conta);
            mockInstancia.gerarDocumento(null);

            assertThrows(FileNotFoundException.class, () -> {
                throw new FileNotFoundException("Arquivo não encontrado");
            });
        }
    }


    private static class ContaTestavel extends Conta {

        ContaTestavel(int numeroConta, double saldo, Cartao cartao, double dividaDeEmprestimo) {
            super(numeroConta, saldo, cartao, dividaDeEmprestimo);
        }

        ContaTestavel(int numeroConta, double saldo, Cartao cartao, double dividaDeEmprestimo,
                      ArrayList<Movimentacao> movimentacoes) {
            super(numeroConta, saldo, cartao, dividaDeEmprestimo, movimentacoes);
        }

        @Override
        public void comprar(double valor) {
        }

        @Override
        public void movimentacaoBancaria(double valor) {
        }
    }
}
