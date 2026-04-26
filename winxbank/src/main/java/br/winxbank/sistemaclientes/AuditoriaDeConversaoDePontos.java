package br.winxbank.sistemaclientes;

import br.winxbank.sistemabancario.Conta;
import br.winxbank.sistemabancario.Movimentacao;

/**
 * Auditoria/telemetria de conversão de pontos.
 * Dependência real: pode gravar log, persistir, enviar para relatórios, etc.
 */
public interface AuditoriaDeConversaoDePontos {

    void registrarConversao(ClienteWinx cliente, Conta conta, int pontosConvertidos, double valorConvertido,
                            Movimentacao movimentacaoGerada);
}

