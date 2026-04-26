package br.winxbank.sistemaclientes;

/**
 * Define como pontos de compra viram saldo.
 * Essa regra é uma dependência real do domínio (pode mudar com promoções, níveis, etc.).
 */
public interface PoliticaDeConversaoDePontos {

    /**
     * Valida a conversão. Lança exceção se não for possível.
     */
    void validarConversao(int pontosDisponiveis);

    /**
     * Converte pontos em valor monetário.
     */
    double converter(int pontosDisponiveis);
}

