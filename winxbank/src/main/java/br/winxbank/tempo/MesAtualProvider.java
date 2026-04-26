package br.winxbank.tempo;

/**
 * Abstrai a origem do mês atual para permitir testes sem depender do singleton Ano.
 */
public interface MesAtualProvider {
    String getMesAtual();
}

