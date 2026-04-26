package br.winxbank.sistemaclientes;

import br.winxbank.exception.NotEnaughPurchasePoints;

/**
 * Regra padrão do banco:
 * - mínimo de 10 pontos para converter
 * - cada ponto vale 3 unidades de saldo
 */
public class PoliticaDeConversaoPadrao implements PoliticaDeConversaoDePontos {

    public static final int MINIMO_PONTOS = 10;
    public static final int BONUS_POR_PONTO = 3;

    @Override
    public void validarConversao(int pontosDisponiveis) {
        if (pontosDisponiveis < MINIMO_PONTOS) {
            throw new NotEnaughPurchasePoints();
        }
    }

    @Override
    public double converter(int pontosDisponiveis) {
        return (double) pontosDisponiveis * BONUS_POR_PONTO;
    }
}
