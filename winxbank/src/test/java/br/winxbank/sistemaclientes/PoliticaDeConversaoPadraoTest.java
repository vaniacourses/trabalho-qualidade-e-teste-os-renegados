package br.winxbank.sistemaclientes;

import br.winxbank.exception.NotEnaughPurchasePoints;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Testes unitários da regra de negócio do sistema de pontos.
 *
 * Técnicas aplicadas:
 * - Funcional / caixa-preta: análise de valor-limite em torno de MINIMO_PONTOS (10)
 *   e classes de equivalência (abaixo do mínimo, no mínimo, acima do mínimo).
 * - Estrutural: cobre os dois ramos do if em validarConversao (true e false)
 *   e a expressão aritmética de converter().
 *
 * A classe não tem colaboradores externos, então não há dependências a isolar:
 * é uma unidade pura (lógica de cálculo + validação).
 */
class PoliticaDeConversaoPadraoTest {

    private static final double DELTA = 0.0001;

    private final PoliticaDeConversaoPadrao politica = new PoliticaDeConversaoPadrao();

    // ==================== validarConversao ====================

    @Test
    // Valor-limite inferior imediatamente abaixo do mínimo: deve rejeitar (ramo true do if).
    void validarConversaoComUmPontoAbaixoDoMinimoLancaExcecao() {
        assertThrows(NotEnaughPurchasePoints.class,
                () -> politica.validarConversao(PoliticaDeConversaoPadrao.MINIMO_PONTOS - 1));
    }

    @Test
    // Valor-limite exatamente no mínimo: NÃO deve rejeitar (fronteira do ramo false do if).
    void validarConversaoExatamenteNoMinimoNaoLancaExcecao() {
        assertDoesNotThrow(() -> politica.validarConversao(PoliticaDeConversaoPadrao.MINIMO_PONTOS));
    }

    @Test
    // Valor-limite imediatamente acima do mínimo: NÃO deve rejeitar.
    void validarConversaoUmPontoAcimaDoMinimoNaoLancaExcecao() {
        assertDoesNotThrow(() -> politica.validarConversao(PoliticaDeConversaoPadrao.MINIMO_PONTOS + 1));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 5, 9})
    // Classe de equivalência "pontos insuficientes": todos abaixo de 10 devem rejeitar.
    void validarConversaoComPontosInsuficientesSempreLancaExcecao(int pontos) {
        assertThrows(NotEnaughPurchasePoints.class, () -> politica.validarConversao(pontos));
    }

    @ParameterizedTest
    @ValueSource(ints = {10, 11, 50, 1000})
    // Classe de equivalência "pontos suficientes": de 10 em diante a validação passa.
    void validarConversaoComPontosSuficientesNuncaLancaExcecao(int pontos) {
        assertDoesNotThrow(() -> politica.validarConversao(pontos));
    }

    @Test
    // Pontos negativos (entrada inválida defensiva) também são tratados como insuficientes.
    void validarConversaoComPontosNegativosLancaExcecao() {
        assertThrows(NotEnaughPurchasePoints.class, () -> politica.validarConversao(-5));
    }

    @Test
    // A mensagem da exceção deve ser a definida no domínio (garante o tipo/mensagem correta).
    void validarConversaoLancaExcecaoComMensagemDeDominio() {
        NotEnaughPurchasePoints excecao = assertThrows(NotEnaughPurchasePoints.class,
                () -> politica.validarConversao(0));
        assertEquals("Pontos de compra insuficientes para a conversao.", excecao.getMessage());
    }

    // ==================== converter ====================

    @ParameterizedTest
    @CsvSource({
            "0, 0.0",
            "1, 3.0",
            "10, 30.0",
            "11, 33.0",
            "100, 300.0"
    })
    // converter() é linear: cada ponto vale BONUS_POR_PONTO (3). Cobre vários valores representativos.
    void converterMultiplicaPontosPeloBonusPorPonto(int pontos, double valorEsperado) {
        assertEquals(valorEsperado, politica.converter(pontos), DELTA);
    }

    @Test
    // Confere explicitamente a constante de bônus para travar regressões no valor do ponto.
    void converterUsaConstanteDeBonusPorPonto() {
        assertEquals(PoliticaDeConversaoPadrao.BONUS_POR_PONTO, politica.converter(1), DELTA);
    }

    @Test
    // converter() não valida: mesmo abaixo do mínimo ele apenas calcula (responsabilidade separada).
    void converterNaoValidaQuantidadeMinimaApenasCalcula() {
        assertEquals(15.0, politica.converter(5), DELTA);
    }
}
