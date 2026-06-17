package br.winxbank.sistemabancario.integracao;

import br.winxbank.exception.BankAccountNotFoundException;
import br.winxbank.sistemabancario.Banco;
import br.winxbank.sistemabancario.Cartao;
import br.winxbank.sistemabancario.CartaoCredito;
import br.winxbank.sistemabancario.Conta;
import br.winxbank.sistemabancario.ContaCorrente;
import br.winxbank.sistemabancario.ContaPoupanca;
import br.winxbank.sistemaclientes.Cliente;
import br.winxbank.sistemaclientes.RegistroDeClientes;
import br.winxbank.tempo.Ano;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BancoIntegrationTest {

    private static final double DELTA = 0.0001;

    @BeforeEach
    void configurar() {
        limparEstado();
        Ano.getInstancia().setMesAtual("Abril");
    }

    @AfterEach
    void limpar() {
        limparEstado();
        Ano.getInstancia().setMesAtual("Abril");
    }

    @Test
    void abrirContaPoupancaPeloRegistroEMovimentarBancoAplicaRendimentoEDespesa() {
        Cliente cliente = RegistroDeClientes.getInstancia()
                .cadastrarCliente("Joao", "11111111111", 2, 100.0);
        ContaPoupanca conta = assertInstanceOf(ContaPoupanca.class, cliente.acessarContas());

        Banco.getInstancia().movimentarEntreBancoConta();

        assertEquals(225.0, conta.getSaldo(), DELTA);
        assertEquals(125.0, Banco.getInstancia().getDespesas(), DELTA);
        assertEquals(0.0, Banco.getInstancia().getReceitas(), DELTA);
        assertEquals(2, conta.getInformeRendimento().size());
    }

    @Test
    void movimentarContaCorrenteRealDescontaTaxaECobraJurosDeFaturaVencida() {
        Cliente cliente = new Cliente("Murilo", "22222222222");
        Cartao cartao = new Cartao(1234, 123);
        CartaoCredito cartaoCredito = new CartaoCredito(100.0, 0, false, 2000.0, 5678, 456);
        ContaCorrente conta = new ContaCorrente(10001, 1000.0, cartao, 0.0, cartaoCredito);
        cliente.setContas(conta);
        RegistroDeClientes.getInstancia().setClientes(new ArrayList<>(List.of(cliente)));

        Banco.getInstancia().movimentarEntreBancoConta();

        assertEquals(987.0, conta.getSaldo(), DELTA);
        assertEquals(1275.0, cartaoCredito.getFatura(), DELTA);
        assertEquals(1188.0, Banco.getInstancia().getReceitas(), DELTA);
        assertEquals(0.0, Banco.getInstancia().getDespesas(), DELTA);
    }

    @Test
    void fecharContaRemoveContaDoClienteEImpedeNovoFechamentoDoMesmoNumero() {
        Cliente cliente = RegistroDeClientes.getInstancia()
                .cadastrarCliente("Adriele", "33333333333", 1, 500.0);
        Conta conta = cliente.acessarContas();

        Banco.getInstancia().fecharConta(cliente, conta.getNumeroConta());

        assertEquals(0, cliente.getContas().size());
        assertThrows(BankAccountNotFoundException.class,
                () -> Banco.getInstancia().fecharConta(cliente, conta.getNumeroConta()));
    }

    private void limparEstado() {
        RegistroDeClientes.getInstancia().limparListaDeClientes();
        Banco.getInstancia().despesas = 0;
        Banco.getInstancia().receitas = 0;
    }
}
