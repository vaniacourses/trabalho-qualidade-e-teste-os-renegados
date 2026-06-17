package br.winxbank.sistemabancario;

import br.winxbank.tempo.Ano;
import br.winxbank.tempo.MesAtualProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MovimentacaoTest {

    private static final double DELTA = 0.0001;
    private static final String MES_FIXO = "Abril";

    private Field instanciaAno;

    @BeforeEach
    void preparar() throws Exception {
        instanciaAno = Ano.class.getDeclaredField("instancia");
        instanciaAno.setAccessible(true);
        instanciaAno.set(null, null);
        Ano.getInstancia().setMesAtual(MES_FIXO);
    }

    @AfterEach
    void limpar() throws Exception {
        instanciaAno.set(null, null);
    }

    @Test
    void construtorParaLeituraDeJsonConverteTipoStringEmEnum() {
        Movimentacao movimentacao = new Movimentacao("03/2026", 120.5, "ENTRADA");

        assertEquals("03/2026", movimentacao.getMesAtual());
        assertEquals(120.5, movimentacao.getDinheiroMovimentado(), DELTA);
        assertSame(Movimentacao.TipoDaMovimentacao.ENTRADA, movimentacao.getTipoDaMovimentacao());
    }

    @Test
    void construtorParaLeituraDeJsonComTipoInvalidoLancaErro() {
        assertThrows(IllegalArgumentException.class,
                () -> new Movimentacao("03/2026", 10.0, "NAO_EXISTE"));
    }

    @Test
    void construtorPadraoUsaMesAtualDoAno() {
        Movimentacao movimentacao = new Movimentacao(75.0, Movimentacao.TipoDaMovimentacao.SAIDA);

        assertEquals(MES_FIXO, movimentacao.getMesAtual());
        assertEquals(75.0, movimentacao.getDinheiroMovimentado(), DELTA);
        assertSame(Movimentacao.TipoDaMovimentacao.SAIDA, movimentacao.getTipoDaMovimentacao());
    }

    @Test
    void construtorComProviderUsaMesDoProviderSemDependerDoSingletonAno() {
        MesAtualProvider provider = mock(MesAtualProvider.class);
        when(provider.getMesAtual()).thenReturn("MES_TESTE");

        Movimentacao movimentacao = new Movimentacao(provider, 10.0, Movimentacao.TipoDaMovimentacao.ENTRADA);

        assertEquals("MES_TESTE", movimentacao.getMesAtual());
        assertEquals(10.0, movimentacao.getDinheiroMovimentado(), DELTA);
        assertSame(Movimentacao.TipoDaMovimentacao.ENTRADA, movimentacao.getTipoDaMovimentacao());
    }

    @Test
    void construtorComProviderQuandoProviderENuloUsaMesAtualDoAnoSingleton() {
        Movimentacao movimentacao = new Movimentacao(null, 10.0, Movimentacao.TipoDaMovimentacao.ENTRADA);

        assertEquals(MES_FIXO, movimentacao.getMesAtual());
        assertEquals(10.0, movimentacao.getDinheiroMovimentado(), DELTA);
        assertSame(Movimentacao.TipoDaMovimentacao.ENTRADA, movimentacao.getTipoDaMovimentacao());
    }

    @Test
    void enumDaMovimentacaoPossuiValoresEsperados() {
        assertSame(Movimentacao.TipoDaMovimentacao.ENTRADA, Movimentacao.TipoDaMovimentacao.valueOf("ENTRADA"));
        assertSame(Movimentacao.TipoDaMovimentacao.SAIDA, Movimentacao.TipoDaMovimentacao.valueOf("SAIDA"));
    }

    // ==================== Casos adicionais (Entrega 2) ====================

    @Test
    // Valor zero é um valor de fronteira válido (não há restrição de sinal no construtor).
    void construtorPadraoAceitaValorZero() {
        Movimentacao movimentacao = new Movimentacao(0.0, Movimentacao.TipoDaMovimentacao.ENTRADA);

        assertEquals(MES_FIXO, movimentacao.getMesAtual());
        assertEquals(0.0, movimentacao.getDinheiroMovimentado(), DELTA);
        assertSame(Movimentacao.TipoDaMovimentacao.ENTRADA, movimentacao.getTipoDaMovimentacao());
    }

    @Test
    // Valor negativo é armazenado como informado (a entidade não valida sinal).
    void construtorPadraoAceitaValorNegativo() {
        Movimentacao movimentacao = new Movimentacao(-50.0, Movimentacao.TipoDaMovimentacao.SAIDA);

        assertEquals(-50.0, movimentacao.getDinheiroMovimentado(), DELTA);
        assertSame(Movimentacao.TipoDaMovimentacao.SAIDA, movimentacao.getTipoDaMovimentacao());
    }

    @Test
    // O mês é capturado no momento da criação: alterar o Ano DEPOIS não muda a movimentação já criada.
    void construtorPadraoCapturaMesNoMomentoDaCriacaoNaoMudaDepois() {
        Movimentacao movimentacao = new Movimentacao(10.0, Movimentacao.TipoDaMovimentacao.ENTRADA);

        Ano.getInstancia().setMesAtual("Dezembro");

        assertEquals(MES_FIXO, movimentacao.getMesAtual());
    }

    @Test
    // Construtor de leitura de JSON aceita tipo SAIDA (complementa o teste de ENTRADA existente).
    void construtorParaLeituraDeJsonConverteTipoSaida() {
        Movimentacao movimentacao = new Movimentacao("12/2025", 80.0, "SAIDA");

        assertEquals("12/2025", movimentacao.getMesAtual());
        assertEquals(80.0, movimentacao.getDinheiroMovimentado(), DELTA);
        assertSame(Movimentacao.TipoDaMovimentacao.SAIDA, movimentacao.getTipoDaMovimentacao());
    }

    @Test
    // valueOf é sensível a maiúsculas: tipo em minúsculas deve falhar (validação de robustez).
    void construtorParaLeituraDeJsonComTipoEmMinusculasLancaErro() {
        assertThrows(IllegalArgumentException.class,
                () -> new Movimentacao("12/2025", 10.0, "entrada"));
    }

    @Test
    // O construtor com provider não consulta o singleton Ano quando um provider real é fornecido.
    void construtorComProviderNaoConsultaSingletonAnoQuandoProviderFornecido() {
        MesAtualProvider provider = mock(MesAtualProvider.class);
        when(provider.getMesAtual()).thenReturn("Janeiro");

        Movimentacao movimentacao = new Movimentacao(provider, 10.0, Movimentacao.TipoDaMovimentacao.ENTRADA);

        verify(provider).getMesAtual();
        assertEquals("Janeiro", movimentacao.getMesAtual());
    }

    @Test
    // Duas movimentações criadas com o mesmo provider compartilham o mês fornecido (sem acoplar ao Ano).
    void construtorComProviderUsaMesmoMesEmMovimentacoesDistintas() {
        MesAtualProvider provider = mock(MesAtualProvider.class);
        when(provider.getMesAtual()).thenReturn("Marco");

        Movimentacao entrada = new Movimentacao(provider, 10.0, Movimentacao.TipoDaMovimentacao.ENTRADA);
        Movimentacao saida = new Movimentacao(provider, 20.0, Movimentacao.TipoDaMovimentacao.SAIDA);

        assertEquals("Marco", entrada.getMesAtual());
        assertEquals("Marco", saida.getMesAtual());
    }
}
