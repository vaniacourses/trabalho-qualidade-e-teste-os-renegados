package br.winxbank.sistemabancario;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;

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

        assertEquals(800, conta.getSaldo());
        verify(cartaoCreditoMock).setFatura(-200);
    }

    @Test
    void deveComprarNoDebitoComConfirmacao() {
        // Simula entrada:
        // 1 → débito
        // 1 → confirmar
        String input = "1\n1\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        conta.comprar(200);

        verify(cartaoDebitoMock).debitar(conta, 200);
    }

    @Test
    void naoDeveComprarNoDebitoSeNaoConfirmar() {
        // 1 → débito
        // 2 → não confirmar
        String input = "1\n2\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        conta.comprar(200);

        verify(cartaoDebitoMock, never()).debitar(any(), anyDouble());
    }

    @Test
    void deveComprarNoCreditoComConfirmacao() {
        // 2 → crédito
        // 1 → confirmar
        String input = "2\n1\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        conta.comprar(300);

        verify(cartaoCreditoMock).creditar(300);
    }

    @Test
    void naoDeveComprarNoCreditoSeNaoConfirmar() {
        // 2 → crédito
        // 2 → não confirmar
        String input = "2\n2\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        conta.comprar(300);

        verify(cartaoCreditoMock, never()).creditar(anyDouble());
    }

    @Test
    void naoDeveFazerNadaSeOpcaoInvalida() {
        // 3 → opção inválida
        String input = "3\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        conta.comprar(100);

        verify(cartaoDebitoMock, never()).debitar(any(), anyDouble());
        verify(cartaoCreditoMock, never()).creditar(anyDouble());
    }
}