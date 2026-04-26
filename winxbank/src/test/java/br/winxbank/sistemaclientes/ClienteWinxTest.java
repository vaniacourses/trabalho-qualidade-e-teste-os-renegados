package br.winxbank.sistemaclientes;

import br.winxbank.exception.NotEnaughPurchasePoints;
import br.winxbank.sistemabancario.Cartao;
import br.winxbank.sistemabancario.CartaoCredito;
import br.winxbank.sistemabancario.ContaCorrente;
import br.winxbank.sistemabancario.Movimentacao;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClienteWinxTest {

    private static ContaCorrente criarContaCorrente(double saldoInicial) {
        Cartao debito = new Cartao(1, 1);
        CartaoCredito credito = new CartaoCredito(2, 2);
        return new ContaCorrente(1001, saldoInicial, debito, 0.0, credito);
    }

    @Test
    void construtorPadraoArmazenaDadosDoClienteWinx() {
        ClienteWinx cliente = new ClienteWinx("ANA", "123", 7);

        assertEquals("ANA", cliente.getNome());
        assertEquals("123", cliente.getCpf());
        assertEquals(7, cliente.getPontosDeCompra());
        assertEquals(0, cliente.getContas().size());
    }

    @Test
    void construtorAlternativoCopiaDadosBasicosDoClienteExistente() {
        ClienteWinx original = new ClienteWinx("ANA", "123", 7);
        original.setContas(criarContaCorrente(0.0));

        ClienteWinx copiado = new ClienteWinx(original);

        assertEquals("ANA", copiado.getNome());
        assertEquals("123", copiado.getCpf());
        assertEquals(7, copiado.getPontosDeCompra());
        // construtor alternativo atual não copia contas
        assertEquals(0, copiado.getContas().size());
    }

    @Test
    void obterPontosDeCompraIncrementaUmPontoPorChamada() {
        ClienteWinx cliente = new ClienteWinx("ANA", "123", 2);

        cliente.obterPontosDeCompra();
        cliente.obterPontosDeCompra();

        assertEquals(4, cliente.getPontosDeCompra());
    }

    @Test
    void converterPontosEmSaldoQuandoPoliticaRejeitaLancaExcecaoENaoAudita() {
        PoliticaDeConversaoDePontos politica = mock(PoliticaDeConversaoDePontos.class);
        AuditoriaDeConversaoDePontos auditoria = mock(AuditoriaDeConversaoDePontos.class);

        doThrow(new NotEnaughPurchasePoints()).when(politica).validarConversao(5);

        ClienteWinx cliente = new ClienteWinx("ANA", "123", 5, politica, auditoria);
        ContaCorrente conta = criarContaCorrente(0.0);

        assertThrows(NotEnaughPurchasePoints.class, () -> cliente.converterPontosEmSaldo(conta));

        verify(politica).validarConversao(5);
        verify(politica, never()).converter(any(Integer.class));
        verify(auditoria, never()).registrarConversao(any(), any(), any(Integer.class), any(Double.class), any());
        // pontos não mudam se conversão falhar na validação
        assertEquals(5, cliente.getPontosDeCompra());
    }

    @Test
    void converterPontosEmSaldoUsaPoliticaERegistraAuditoria() {
        PoliticaDeConversaoDePontos politica = mock(PoliticaDeConversaoDePontos.class);
        AuditoriaDeConversaoDePontos auditoria = mock(AuditoriaDeConversaoDePontos.class);

        when(politica.converter(10)).thenReturn(30.0);

        ClienteWinx cliente = new ClienteWinx("ANA", "123", 10, politica, auditoria);
        ContaCorrente conta = criarContaCorrente(0.0);
        int extratoAntes = conta.getExtrato().size();

        cliente.converterPontosEmSaldo(conta);

        verify(politica).validarConversao(10);
        verify(politica).converter(10);

        assertEquals(30.0, conta.getSaldo(), 0.0001);
        assertEquals(extratoAntes + 1, conta.getExtrato().size());

        Movimentacao ultima = conta.getExtrato().get(conta.getExtrato().size() - 1);
        assertSame(Movimentacao.TipoDaMovimentacao.ENTRADA, ultima.getTipoDaMovimentacao());

        verify(auditoria).registrarConversao(eq(cliente), eq(conta), eq(10), eq(30.0), any(Movimentacao.class));
        assertEquals(0, cliente.getPontosDeCompra());
    }
}
