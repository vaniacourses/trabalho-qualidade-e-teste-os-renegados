package br.winxbank.sistemaclientes;

import br.winxbank.exception.NotEnaughPurchasePoints;
import br.winxbank.sistemabancario.Cartao;
import br.winxbank.sistemabancario.CartaoCredito;
import br.winxbank.sistemabancario.ContaCorrente;
import br.winxbank.sistemabancario.Movimentacao;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
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

    @Test
    void construtorComDependenciasQuandoDependenciasSaoNulasMantemDefaultsESegueFluxoNormalDeConversao() {
        ClienteWinx cliente = new ClienteWinx("ANA", "123", 10, null, null);
        ContaCorrente conta = criarContaCorrente(0.0);

        int extratoAntes = conta.getExtrato().size();
        cliente.converterPontosEmSaldo(conta);

        // regra da politica padrão: 3 reais por ponto (BONUSDECOMPRA)
        assertEquals(30.0, conta.getSaldo(), 0.0001);
        assertEquals(extratoAntes + 1, conta.getExtrato().size());
        assertSame(Movimentacao.TipoDaMovimentacao.ENTRADA,
                conta.getExtrato().get(conta.getExtrato().size() - 1).getTipoDaMovimentacao());
        assertEquals(0, cliente.getPontosDeCompra());
    }

    @Test
    void setPoliticaDeConversaoQuandoParametroNuloNaoDeveSubstituirPoliticaExistente() {
        PoliticaDeConversaoDePontos politica = mock(PoliticaDeConversaoDePontos.class);
        AuditoriaDeConversaoDePontos auditoria = mock(AuditoriaDeConversaoDePontos.class);

        when(politica.converter(10)).thenReturn(123.0);

        ClienteWinx cliente = new ClienteWinx("ANA", "123", 10, politica, auditoria);
        ContaCorrente conta = criarContaCorrente(0.0);

        cliente.setPoliticaDeConversao(null);
        cliente.converterPontosEmSaldo(conta);

        // Se a política tivesse sido trocada, o saldo não seria 123.0
        assertEquals(123.0, conta.getSaldo(), 0.0001);
        verify(politica).validarConversao(10);
        verify(politica).converter(10);
    }

    @Test
    void setAuditoriaDeConversaoQuandoParametroNuloNaoDeveSubstituirAuditoriaExistente() {
        PoliticaDeConversaoDePontos politica = mock(PoliticaDeConversaoDePontos.class);
        AuditoriaDeConversaoDePontos auditoria = mock(AuditoriaDeConversaoDePontos.class);

        when(politica.converter(10)).thenReturn(50.0);

        ClienteWinx cliente = new ClienteWinx("ANA", "123", 10, politica, auditoria);
        ContaCorrente conta = criarContaCorrente(0.0);

        cliente.setAuditoriaDeConversao(null);
        cliente.converterPontosEmSaldo(conta);

        // Comportamento esperado: ainda usa a auditoria original (mock) e NÃO troca por NoOp.
        verify(auditoria).registrarConversao(eq(cliente), eq(conta), eq(10), eq(50.0), any(Movimentacao.class));
    }

    // ==================== Casos adicionais (Entrega 2) ====================

    @Test
    // setPoliticaDeConversao com mock válido DEVE substituir a política usada na conversão.
    // Isola a dependência: o resultado vem inteiramente do mock injetado depois da construção.
    void setPoliticaDeConversaoComMockValidoSubstituiPoliticaUsadaNaConversao() {
        PoliticaDeConversaoDePontos politicaInicial = mock(PoliticaDeConversaoDePontos.class);
        PoliticaDeConversaoDePontos politicaNova = mock(PoliticaDeConversaoDePontos.class);
        AuditoriaDeConversaoDePontos auditoria = mock(AuditoriaDeConversaoDePontos.class);
        when(politicaNova.converter(10)).thenReturn(999.0);

        ClienteWinx cliente = new ClienteWinx("ANA", "123", 10, politicaInicial, auditoria);
        cliente.setPoliticaDeConversao(politicaNova);

        ContaCorrente conta = criarContaCorrente(0.0);
        cliente.converterPontosEmSaldo(conta);

        // Usa a política nova e ignora completamente a inicial.
        assertEquals(999.0, conta.getSaldo(), 0.0001);
        verify(politicaNova).validarConversao(10);
        verify(politicaNova).converter(10);
        verify(politicaInicial, never()).converter(any(Integer.class));
    }

    @Test
    // setAuditoriaDeConversao com mock válido DEVE substituir a auditoria usada na conversão.
    void setAuditoriaDeConversaoComMockValidoSubstituiAuditoriaUsadaNaConversao() {
        PoliticaDeConversaoDePontos politica = mock(PoliticaDeConversaoDePontos.class);
        AuditoriaDeConversaoDePontos auditoriaInicial = mock(AuditoriaDeConversaoDePontos.class);
        AuditoriaDeConversaoDePontos auditoriaNova = mock(AuditoriaDeConversaoDePontos.class);
        when(politica.converter(10)).thenReturn(30.0);

        ClienteWinx cliente = new ClienteWinx("ANA", "123", 10, politica, auditoriaInicial);
        cliente.setAuditoriaDeConversao(auditoriaNova);

        ContaCorrente conta = criarContaCorrente(0.0);
        cliente.converterPontosEmSaldo(conta);

        verify(auditoriaNova).registrarConversao(eq(cliente), eq(conta), eq(10), eq(30.0), any(Movimentacao.class));
        verify(auditoriaInicial, never()).registrarConversao(any(), any(), any(Integer.class), any(Double.class), any());
    }

    @Test
    // O saldo convertido é SOMADO ao saldo já existente na conta (setSaldo acumula).
    // Garante que a conversão não "zera" um saldo pré-existente.
    void converterPontosEmSaldoSomaAoSaldoPreexistenteDaConta() {
        PoliticaDeConversaoDePontos politica = mock(PoliticaDeConversaoDePontos.class);
        AuditoriaDeConversaoDePontos auditoria = mock(AuditoriaDeConversaoDePontos.class);
        when(politica.converter(10)).thenReturn(30.0);

        ClienteWinx cliente = new ClienteWinx("ANA", "123", 10, politica, auditoria);
        ContaCorrente conta = criarContaCorrente(100.0);

        cliente.converterPontosEmSaldo(conta);

        assertEquals(130.0, conta.getSaldo(), 0.0001);
    }

    @Test
    // A ordem das interações importa: valida ANTES de converter.
    // Se a validação falhar, converter() nunca é chamado (já coberto), aqui garantimos a ordem no caminho feliz.
    void converterPontosEmSaldoValidaAntesDeConverter() {
        PoliticaDeConversaoDePontos politica = mock(PoliticaDeConversaoDePontos.class);
        AuditoriaDeConversaoDePontos auditoria = mock(AuditoriaDeConversaoDePontos.class);
        when(politica.converter(10)).thenReturn(30.0);

        ClienteWinx cliente = new ClienteWinx("ANA", "123", 10, politica, auditoria);
        ContaCorrente conta = criarContaCorrente(0.0);

        cliente.converterPontosEmSaldo(conta);

        InOrder ordem = inOrder(politica);
        ordem.verify(politica).validarConversao(10);
        ordem.verify(politica).converter(10);
    }

    @Test
    // A auditoria recebe exatamente os pontos que existiam ANTES da conversão (não o zero pós-conversão).
    void converterPontosEmSaldoAuditaQuantidadeDePontosAnteriorAZeramento() {
        PoliticaDeConversaoDePontos politica = mock(PoliticaDeConversaoDePontos.class);
        AuditoriaDeConversaoDePontos auditoria = mock(AuditoriaDeConversaoDePontos.class);
        when(politica.converter(25)).thenReturn(75.0);

        ClienteWinx cliente = new ClienteWinx("ANA", "123", 25, politica, auditoria);
        ContaCorrente conta = criarContaCorrente(0.0);

        cliente.converterPontosEmSaldo(conta);

        // pontosConvertidos auditado = 25 (valor antes de zerar), não 0.
        verify(auditoria).registrarConversao(eq(cliente), eq(conta), eq(25), eq(75.0), any(Movimentacao.class));
        assertEquals(0, cliente.getPontosDeCompra());
    }

    @Test
    // A movimentação gerada e adicionada ao extrato deve carregar o valor convertido.
    void converterPontosEmSaldoGeraMovimentacaoComValorConvertido() {
        PoliticaDeConversaoDePontos politica = mock(PoliticaDeConversaoDePontos.class);
        AuditoriaDeConversaoDePontos auditoria = mock(AuditoriaDeConversaoDePontos.class);
        when(politica.converter(10)).thenReturn(30.0);

        ClienteWinx cliente = new ClienteWinx("ANA", "123", 10, politica, auditoria);
        ContaCorrente conta = criarContaCorrente(0.0);

        cliente.converterPontosEmSaldo(conta);

        Movimentacao ultima = conta.getExtrato().get(conta.getExtrato().size() - 1);
        assertEquals(30.0, ultima.getDinheiroMovimentado(), 0.0001);
        assertSame(Movimentacao.TipoDaMovimentacao.ENTRADA, ultima.getTipoDaMovimentacao());
    }

    @Test
    // obterPontosDeCompra a partir de zero: cobre o incremento isolado sem estado inicial.
    void obterPontosDeCompraAPartirDeZeroIncrementaCorretamente() {
        ClienteWinx cliente = new ClienteWinx("ANA", "123", 0);

        cliente.obterPontosDeCompra();

        assertEquals(1, cliente.getPontosDeCompra());
    }

    @Test
    // Construtor com dependências válidas usa exatamente as injetadas (não os defaults).
    void construtorComDependenciasValidasUsaAsDependenciasInjetadas() {
        PoliticaDeConversaoDePontos politica = mock(PoliticaDeConversaoDePontos.class);
        AuditoriaDeConversaoDePontos auditoria = mock(AuditoriaDeConversaoDePontos.class);
        when(politica.converter(10)).thenReturn(42.0);

        ClienteWinx cliente = new ClienteWinx("ANA", "123", 10, politica, auditoria);
        ContaCorrente conta = criarContaCorrente(0.0);

        cliente.converterPontosEmSaldo(conta);

        assertEquals(42.0, conta.getSaldo(), 0.0001);
        verify(politica).converter(10);
        verify(auditoria).registrarConversao(eq(cliente), eq(conta), eq(10), eq(42.0), any(Movimentacao.class));
    }

    @Test
    // Construtor alternativo (cópia) com dependências válidas: copia dados e usa as dependências injetadas.
    void construtorCopiaComDependenciasValidasUsaDependenciasInjetadas() {
        ClienteWinx origem = new ClienteWinx("ANA", "123", 10);
        PoliticaDeConversaoDePontos politica = mock(PoliticaDeConversaoDePontos.class);
        AuditoriaDeConversaoDePontos auditoria = mock(AuditoriaDeConversaoDePontos.class);
        when(politica.converter(10)).thenReturn(60.0);

        ClienteWinx copia = new ClienteWinx(origem, politica, auditoria);
        ContaCorrente conta = criarContaCorrente(0.0);
        copia.converterPontosEmSaldo(conta);

        assertEquals("ANA", copia.getNome());
        assertEquals("123", copia.getCpf());
        assertEquals(60.0, conta.getSaldo(), 0.0001);
        verify(auditoria).registrarConversao(eq(copia), eq(conta), eq(10), eq(60.0), any(Movimentacao.class));
    }

    @Test
    // Construtor alternativo (cópia) com dependências NULAS: mantém os defaults (política padrão real).
    // Cobre o ramo "null" dos ifs no construtor de cópia com dependências.
    void construtorCopiaComDependenciasNulasMantemDefaultsESegueFluxoNormal() {
        ClienteWinx origem = new ClienteWinx("ANA", "123", 10);

        ClienteWinx copia = new ClienteWinx(origem, null, null);
        ContaCorrente conta = criarContaCorrente(0.0);
        copia.converterPontosEmSaldo(conta);

        // Política padrão real: 10 * 3 = 30, sem lançar exceção e sem depender de mocks.
        assertEquals(30.0, conta.getSaldo(), 0.0001);
        assertEquals(0, copia.getPontosDeCompra());
    }
}
