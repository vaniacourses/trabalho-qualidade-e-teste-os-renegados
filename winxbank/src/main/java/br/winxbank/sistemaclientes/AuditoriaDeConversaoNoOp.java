package br.winxbank.sistemaclientes;

import br.winxbank.sistemabancario.Conta;
import br.winxbank.sistemabancario.Movimentacao;

/**
 * Implementação padrão que não faz nada.
 */
public class AuditoriaDeConversaoNoOp implements AuditoriaDeConversaoDePontos {
    @Override
    public void registrarConversao(ClienteWinx cliente, Conta conta, int pontosConvertidos, double valorConvertido,
                                   Movimentacao movimentacaoGerada) {
        // no-op
    }
}

