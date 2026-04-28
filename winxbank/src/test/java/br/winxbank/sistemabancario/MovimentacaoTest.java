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
}
