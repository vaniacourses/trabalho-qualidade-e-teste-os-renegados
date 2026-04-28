package br.winxbank.sistemabancario;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class ContaCorrenteTest {

    private ContaCorrente conta;
    private CartaoCredito cartaoCreditoMock;
    private Cartao cartaoDebitoMock;

    private final InputStream systemIn = System.in;

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

    @AfterEach
    void restoreSystemInput() {
        System.setIn(systemIn);
    }

    @Test
    void devePagarFaturaCorretamente() {
        conta.pagarFatura(200);

        assertEquals(800, conta.getSaldo());
        verify(cartaoCreditoMock, times(1)).setFatura(-200);
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

        verify(spyConta, times(1)).movimentacaoBancaria(anyDouble());
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
        String input = "1\n1\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        conta.comprar(200);

        verify(cartaoDebitoMock, times(1)).debitar(conta, 200);
        verify(cartaoCreditoMock, never()).creditar(anyDouble());
    }

    @Test
    void naoDeveComprarNoDebitoSeNaoConfirmar() {
        String input = "1\n2\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        conta.comprar(200);

        verify(cartaoDebitoMock, never()).debitar(any(), anyDouble());
    }

    @Test
    void deveComprarNoCreditoComConfirmacao() {
        String input = "2\n1\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        conta.comprar(300);

        verify(cartaoCreditoMock, times(1)).creditar(300);
        verify(cartaoDebitoMock, never()).debitar(any(), anyDouble());
    }

    @Test
    void naoDeveComprarNoCreditoSeNaoConfirmar() {
        String input = "2\n2\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        conta.comprar(300);

        verify(cartaoCreditoMock, never()).creditar(anyDouble());
    }

    @Test
    void naoDeveFazerNadaSeOpcaoInvalida() {
        String input = "3\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        conta.comprar(100);

        verify(cartaoDebitoMock, never()).debitar(any(), anyDouble());
        verify(cartaoCreditoMock, never()).creditar(anyDouble());
    }
}