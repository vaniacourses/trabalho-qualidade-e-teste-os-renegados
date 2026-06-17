package br.winxbank.sistemaclientes.integracao;

import br.winxbank.sistemabancario.*;
import br.winxbank.sistemaclientes.Cliente;
import br.winxbank.sistemaclientes.RegistroDeClientes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class RegistroDeClientesBancoIntegracaoTest {

    private static final double DELTA = 0.0001;

    @BeforeEach
    void configurarAmbiente() {
        limparEstadoCompartilhado();
    }

    @AfterEach
    void restaurarAmbiente() {
        limparEstadoCompartilhado();
    }

    @Test
    void movimentarEntreBancoContaUsaClientesRegistradosNoRegistroGlobal() {
        Cliente cliente = mock(Cliente.class);
        Conta conta = mock(Conta.class);
        when(cliente.getContas()).thenReturn(new ArrayList<>(List.of(conta)));
        registrarClientes(cliente);

        Banco.getInstancia().movimentarEntreBancoConta();

        verify(cliente).getContas();
        verify(conta).cobrarJurosEmprestimo();
    }

    @Test
    void movimentarEntreBancoContaQuandoClienteRegistradoTemContaPoupancaCobraJurosEAcrescentaRendimento() {
        Cliente cliente = mock(Cliente.class);
        ContaPoupanca contaPoupanca = mock(ContaPoupanca.class);
        when(cliente.getContas()).thenReturn(new ArrayList<>(List.of(contaPoupanca)));
        registrarClientes(cliente);

        Banco.getInstancia().movimentarEntreBancoConta();

        verify(cliente).getContas();
        verify(contaPoupanca).cobrarJurosEmprestimo();
        verify(contaPoupanca).acrescentarRendimento();
    }

    @Test
    void movimentarEntreBancoContaQuandoClienteRegistradoTemContaCorrenteComFaturaCobraTaxasDaContaEDoCartao() {
        Cliente cliente = mock(Cliente.class);
        ContaCorrente contaCorrente = mock(ContaCorrente.class);
        CartaoCredito cartaoCredito = mock(CartaoCredito.class);
        when(cliente.getContas()).thenReturn(new ArrayList<>(List.of(contaCorrente)));
        when(contaCorrente.getCartaoCredito()).thenReturn(cartaoCredito);
        when(cartaoCredito.getFatura()).thenReturn(100.0);
        registrarClientes(cliente);

        Banco.getInstancia().movimentarEntreBancoConta();

        verify(cliente).getContas();
        verify(contaCorrente).cobrarJurusEmprestimo();
        verify(contaCorrente).descontarTaxa();
        verify(contaCorrente, times(2)).getCartaoCredito();
        verify(cartaoCredito).getFatura();
        verify(cartaoCredito).cobrarJuros();
    }

    @Test
    void movimentarEntreBancoContaQuandoClienteRegistradoTemContaCorrenteSemFaturaNaoCobraJurosDoCartao() {
        Cliente cliente = mock(Cliente.class);
        ContaCorrente contaCorrente = mock(ContaCorrente.class);
        CartaoCredito cartaoCredito = mock(CartaoCredito.class);
        when(cliente.getContas()).thenReturn(new ArrayList<>(List.of(contaCorrente)));
        when(contaCorrente.getCartaoCredito()).thenReturn(cartaoCredito);
        when(cartaoCredito.getFatura()).thenReturn(0.0);
        registrarClientes(cliente);

        Banco.getInstancia().movimentarEntreBancoConta();

        verify(cliente).getContas();
        verify(contaCorrente).cobrarJurusEmprestimo();
        verify(contaCorrente).descontarTaxa();
        verify(contaCorrente).getCartaoCredito();
        verify(cartaoCredito).getFatura();
        verify(cartaoCredito, never()).cobrarJurus();
    }

    @Test
    void movimentarEntreBancoContaQuandoRegistroNaoTemClientesNaoAlteraEstadoDoBanco() {
        Cliente clienteNaoRegistrado = mock(Cliente.class);
        Banco banco = Banco.getInstancia();
        banco.setReceitas(10.0);
        banco.setDespesas(5.0);

        Banco.getInstancia().movimentarEntreBancoConta();

        assertEquals(10.0, banco.getReceitas(), DELTA);
        assertEquals(5.0, banco.getDespesas(), DELTA);
        verifyNoInteractions(clienteNaoRegistrado);
    }

    private void registrarClientes(Cliente... clientes) {
        RegistroDeClientes.getInstancia().setClientes(new ArrayList<>(List.of(clientes)));
    }

    private void limparEstadoCompartilhado() {
        RegistroDeClientes.getInstancia().limparListaDeClientes();
        Banco.getInstancia().despesas = 0;
        Banco.getInstancia().receitas = 0;
    }
}
