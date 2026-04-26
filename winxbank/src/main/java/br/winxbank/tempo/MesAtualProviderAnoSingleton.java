package br.winxbank.tempo;

/**
 * Implementação padrão que usa o singleton {@link Ano}.
 */
public class MesAtualProviderAnoSingleton implements MesAtualProvider {

    @Override
    public String getMesAtual() {
        return Ano.getInstancia().getMesAtual();
    }
}

