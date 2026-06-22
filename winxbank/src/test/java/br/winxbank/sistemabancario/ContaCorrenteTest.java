package br.winxbank.sistemabancario;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ContaCorrenteTest {

    private ContaCorrente conta;
    private CartaoCredito cartaoCreditoMock;
    private Cartao cartaoDebitoMock;

    @BeforeEach
    void setUp() {
        cartaoCreditoMock = mock(CartaoCredito.class);
        cartaoDebitoMock = mock(Cartao.class);

        conta = new ContaCorrente(
                123,
                1000.0,
                cartaoDebitoMock,
                0.0,
                cartaoCreditoMock
        );
    }

    @Test
    void devePagarFaturaCorretamente() {
        conta.pagarFatura(200);

        assertEquals(800.0, conta.getSaldo());
        verify(cartaoCreditoMock).setFatura(-200);
    }

    @Test
    void deveDescontarTaxa() {
        double saldoInicial = conta.getSaldo();

        conta.descontarTaxa();

        assertTrue(conta.getSaldo() < saldoInicial);
    }

    @Test
    void deveChamarMovimentacaoBancariaAoDescontarTaxa() {
        ContaCorrente spyConta = spy(conta);

        spyConta.descontarTaxa();

        verify(spyConta).movimentacaoBancaria(anyDouble());
    }

    @Test
    void deveRetornarTipoContaCorrente() {
        assertEquals("Corrente", conta.getTipoDaConta());
    }

    @Test
    void deveRetornarCartaoCredito() {
        assertEquals(cartaoCreditoMock, conta.getCartaoCredito());
    }

    @Test
    void deveComprarNoDebitoComConfirmacao() {

        conta.comprar(200, 1, 1);

        verify(cartaoDebitoMock, times(1))
                .debitar(conta, 200);

        verify(cartaoCreditoMock, never())
                .creditar(anyDouble());
    }

    @Test
    void naoDeveComprarNoDebitoSemConfirmacao() {

        conta.comprar(200, 1, 0);

        verify(cartaoDebitoMock, never())
                .debitar(any(), anyDouble());

        verify(cartaoCreditoMock, never())
                .creditar(anyDouble());
    }

    @Test
    void deveComprarNoCreditoComConfirmacao() {

        conta.comprar(300, 2, 1);

        verify(cartaoCreditoMock, times(1))
                .creditar(300);

        verify(cartaoDebitoMock, never())
                .debitar(any(), anyDouble());
    }

    @Test
    void naoDeveComprarNoCreditoSemConfirmacao() {

        conta.comprar(300, 2, 0);

        verify(cartaoCreditoMock, never())
                .creditar(anyDouble());

        verify(cartaoDebitoMock, never())
                .debitar(any(), anyDouble());
    }

    @Test
    void naoDeveFazerNadaQuandoFormaPagamentoForInvalida() {

        conta.comprar(100, 99, 1);

        verify(cartaoDebitoMock, never())
                .debitar(any(), anyDouble());

        verify(cartaoCreditoMock, never())
                .creditar(anyDouble());
    }

    @Test
    void naoDeveFazerNadaQuandoFormaPagamentoForInvalidaESemConfirmacao() {

        conta.comprar(100, 99, 0);

        verify(cartaoDebitoMock, never())
                .debitar(any(), anyDouble());

        verify(cartaoCreditoMock, never())
                .creditar(anyDouble());
    }
}
