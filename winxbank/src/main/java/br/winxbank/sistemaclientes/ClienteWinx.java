package br.winxbank.sistemaclientes;

import br.winxbank.sistemabancario.Conta;
import br.winxbank.sistemabancario.Movimentacao;

/**
 * @author Dani
 * Esta classe é responsável por representar uma entidade ClienteWinx.
 * Um tipo de cliente sem vantagens no banco.
 */
public class ClienteWinx extends Cliente{

    private int pontosDeCompra;
    private final int BONUSDECOMPRA = 3;

    private PoliticaDeConversaoDePontos politicaDeConversao = new PoliticaDeConversaoPadrao();
    private AuditoriaDeConversaoDePontos auditoriaDeConversao = new AuditoriaDeConversaoNoOp();

    /**
     * Construtor padrão do cliente.
     *
     * @param nome
     * @param cpf
     */
    public ClienteWinx(String nome, String cpf, int pontosDeCompra) {
        super(nome, cpf);
        this.pontosDeCompra = pontosDeCompra;
    }

    /**
     * Construtor com dependências do domínio (útil para testes e para regras variáveis do banco).
     */
    public ClienteWinx(String nome, String cpf, int pontosDeCompra,
                      PoliticaDeConversaoDePontos politicaDeConversao,
                      AuditoriaDeConversaoDePontos auditoriaDeConversao) {
        super(nome, cpf);
        this.pontosDeCompra = pontosDeCompra;
        if (politicaDeConversao != null) {
            this.politicaDeConversao = politicaDeConversao;
        }
        if (auditoriaDeConversao != null) {
            this.auditoriaDeConversao = auditoriaDeConversao;
        }
    }

    /**
     * Cosntrutor alternativo para salvar um determinado cliente atual no sistema de login.
     * @param cliente
     */
    public ClienteWinx(Cliente cliente){
        this.nome = cliente.getNome();
        this.cpf = cliente.getCpf();
        this.pontosDeCompra = ((ClienteWinx) cliente).getPontosDeCompra();
    }

    /**
     * Construtor alternativo com dependências do domínio.
     */
    public ClienteWinx(Cliente cliente,
                      PoliticaDeConversaoDePontos politicaDeConversao,
                      AuditoriaDeConversaoDePontos auditoriaDeConversao){
        this(cliente);
        if (politicaDeConversao != null) {
            this.politicaDeConversao = politicaDeConversao;
        }
        if (auditoriaDeConversao != null) {
            this.auditoriaDeConversao = auditoriaDeConversao;
        }
    }

    public void setPoliticaDeConversao(PoliticaDeConversaoDePontos politicaDeConversao) {
        if (politicaDeConversao != null) {
            this.politicaDeConversao = politicaDeConversao;
        }
    }

    public void setAuditoriaDeConversao(AuditoriaDeConversaoDePontos auditoriaDeConversao) {
        if (auditoriaDeConversao != null) {
            this.auditoriaDeConversao = auditoriaDeConversao;
        }
    }

    public int getPontosDeCompra() {
        return pontosDeCompra;
    }

    /**
     * Método responsável por converter pontos de compra em saldo.
     */
    public void obterPontosDeCompra(){
        this.pontosDeCompra = this.pontosDeCompra + 1;
    }

    /**
     * Método responsável por converter os pontos do ClienteWinx em saldo na conta.
     * Regras de conversão ficam centralizadas na politica.
     *
     * @param conta
     */
    public void converterPontosEmSaldo(Conta conta){
        politicaDeConversao.validarConversao(this.pontosDeCompra);

        double saldoConvertido = politicaDeConversao.converter(this.pontosDeCompra);
        conta.setSaldo(saldoConvertido);

        int pontosConvertidos = this.pontosDeCompra;
        this.pontosDeCompra = 0;

        // Mantém compatibilidade com o extrato existente (sem mexer em persistência/relatório).
        Movimentacao movimentacao = new Movimentacao(saldoConvertido, Movimentacao.TipoDaMovimentacao.ENTRADA);
        conta.setExtrato(movimentacao);

        auditoriaDeConversao.registrarConversao(this, conta, pontosConvertidos, saldoConvertido, movimentacao);
    }
}
