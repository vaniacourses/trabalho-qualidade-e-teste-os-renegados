package br.winxbank.sistemabancario.integracao;

import org.junit.jupiter.api.Test;

import br.winxbank.sistemabancario.Cartao;
import br.winxbank.sistemabancario.CartaoCredito;
import br.winxbank.sistemabancario.Conta;
import br.winxbank.sistemabancario.ContaCorrente;
import br.winxbank.sistemabancario.Movimentacao;
import br.winxbank.sistemabancario.Movimentacao.TipoDaMovimentacao;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

//TESTES FEITOS COM AUXILIO DO CHAT GPT
class ContaIntegrationTest {

    private static final double DELTA = 0.0001;


    @Test
    void integracaoFluxoCompletoDeOperacoesRastreiamNoExtrato() {

        Cartao cartao = new Cartao(1234, 456);
        Conta conta = new ContaTestavel(1001, 1000.0, cartao, 0.0);


        Movimentacao movimentacaoInicial = new Movimentacao(1000.0, Movimentacao.TipoDaMovimentacao.ENTRADA);
        conta.setExtrato(movimentacaoInicial);


        conta.depositar(500.0);
        Movimentacao movimentacaoDeposito = new Movimentacao(500.0, Movimentacao.TipoDaMovimentacao.ENTRADA);
        conta.setExtrato(movimentacaoDeposito);


        conta.sacar(200.0);
        Movimentacao movimentacaoSaque = new Movimentacao(200.0, Movimentacao.TipoDaMovimentacao.SAIDA);
        conta.setExtrato(movimentacaoSaque);


        conta.requisitarEmprestimo(1000.0);
        Movimentacao movimentacaoEmprestimo = new Movimentacao(1000.0, Movimentacao.TipoDaMovimentacao.ENTRADA);
        conta.setExtrato(movimentacaoEmprestimo);


        assertEquals(4, conta.getExtrato().size(),
                "Extrato deve conter 4 movimentações (inicial, depósito, saque, empréstimo)");
        // requisitarEmprestimo() NÃO credita o valor no saldo; apenas registra a dívida.
        // Saldo final = 1000 (inicial) + 500 (depósito) - 200 (saque) = 1300.
        assertEquals(1300.0, conta.getSaldo(), DELTA,
                "Saldo final deve ser 1000 (inicial) + 500 (depósito) - 200 (saque) = 1300");
        assertEquals(1000.0, conta.getDividaDeEmprestimo(), DELTA,
                "Dívida de empréstimo deve ser 1000");
    }


    @Test
    void integracaoContaCorrenteComCartaoCreditoPagamentoFatura() {

        Cartao cartaoDebito = new Cartao(5678, 123);
        CartaoCredito cartaoCredito = new CartaoCredito(5678, 999);
        ContaCorrente contaCorrente = new ContaCorrente(2001, 2000.0, cartaoDebito, 0.0, cartaoCredito);


        contaCorrente.pagarFatura(500.0);


        assertEquals(1500.0, contaCorrente.getSaldo(), DELTA,
                "Saldo deve reduzir de 2000 para 1500 após pagar 500 de fatura");
        assertEquals(-500.0, cartaoCredito.getFatura(), DELTA,
                "Fatura deve reduzir de 0 para -500 após pagamento");
    }

    private static class ContaTestavel extends Conta {

        ContaTestavel(int numeroConta, double saldo, Cartao cartao, double dividaDeEmprestimo) {
            super(numeroConta, saldo, cartao, dividaDeEmprestimo);
        }

        ContaTestavel(int numeroConta, double saldo, Cartao cartao, double dividaDeEmprestimo,
                      ArrayList<Movimentacao> movimentacoes) {
            super(numeroConta, saldo, cartao, dividaDeEmprestimo, movimentacoes);
        }

        @Override
        public void comprar(double valor) {
        }

        @Override
        public void movimentacaoBancaria(double valor) {
        }
    }
}
