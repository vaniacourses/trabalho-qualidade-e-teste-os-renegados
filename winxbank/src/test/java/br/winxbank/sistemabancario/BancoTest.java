package br.winxbank.sistemabancario;

import br.winxbank.exception.BankAccountNotFoundException;
import br.winxbank.sistemaclientes.Cliente;
import br.winxbank.sistemaclientes.RegistroDeClientes;
import br.winxbank.tempo.Ano;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BancoTest {

    private static final double DELTA = 0.0001;

    private InputStream systemInOriginal;
    private PrintStream systemOutOriginal;
    private ByteArrayOutputStream saidaCapturada;

    @BeforeEach
    void configurarAmbiente() {
        systemInOriginal = System.in;
        systemOutOriginal = System.out;
        saidaCapturada = new ByteArrayOutputStream();
        System.setOut(new PrintStream(saidaCapturada, true, StandardCharsets.UTF_8));
        limparEstadoCompartilhado();
        Ano.getInstancia().setMesAtual("Janeiro");
    }

    @AfterEach
    void restaurarAmbiente() {
        System.setIn(systemInOriginal);
        System.setOut(systemOutOriginal);
        limparEstadoCompartilhado();
        Ano.getInstancia().setMesAtual("Janeiro");
    }

    // Verifica que a abertura de conta corrente com entrada valida cria a conta,
    // preserva o saldo informado e gera numeros dentro dos intervalos esperados.
    @Test
    void abrirNovaContaQuandoEntradaEhUmCriaContaCorrenteComDadosEsperados() {
        configurarEntrada("1\n250.75\n");

        Conta conta = Banco.getInstancia().abrirNovaConta();

        ContaCorrente contaCorrente = assertInstanceOf(ContaCorrente.class, conta);
        assertEquals(250.75, contaCorrente.getSaldo(), DELTA);
        assertEquals(0.0, contaCorrente.getDividaDeEmprestimo(), DELTA);
        assertTrue(contaCorrente.getNumeroConta() >= 10000 && contaCorrente.getNumeroConta() < 100000);
        assertTrue(contaCorrente.getCartao().getNumero() >= 1000 && contaCorrente.getCartao().getNumero() < 10000);
        assertTrue(contaCorrente.getCartao().getCsv() >= 100 && contaCorrente.getCartao().getCsv() < 1000);
        assertEquals(contaCorrente.getCartao().getNumero(), contaCorrente.getCartaoCredito().getNumero());
        assertEquals(contaCorrente.getCartao().getCsv(), contaCorrente.getCartaoCredito().getCsv());
    }

    // Verifica que a abertura de conta corrente aceita saldo negativo,
    // expondo a ausencia de validacao para esse caso de borda.
    @Test
    void abrirNovaContaQuandoEntradaEhUmMantemSaldoNegativoSemValidacao() {
        configurarEntrada("1\n-25.0\n");

        Conta conta = Banco.getInstancia().abrirNovaConta();

        ContaCorrente contaCorrente = assertInstanceOf(ContaCorrente.class, conta);
        assertEquals(-25.0, contaCorrente.getSaldo(), DELTA);
        assertEquals(0.0, contaCorrente.getDividaDeEmprestimo(), DELTA);
    }

    // Verifica que a abertura de conta poupanca com entrada valida cria a conta,
    // preserva o saldo negativo informado e gera identificadores esperados.
    @Test
    void abrirNovaContaQuandoEntradaEhDoisCriaContaPoupancaEMantemSaldoNegativo() {
        configurarEntrada("2\n-50.0\n");

        Conta conta = Banco.getInstancia().abrirNovaConta();

        ContaPoupanca contaPoupanca = assertInstanceOf(ContaPoupanca.class, conta);
        assertEquals(-50.0, contaPoupanca.getSaldo(), DELTA);
        assertEquals(0.0, contaPoupanca.getDividaDeEmprestimo(), DELTA);
        assertTrue(contaPoupanca.getNumeroConta() >= 10000 && contaPoupanca.getNumeroConta() < 100000);
        assertTrue(contaPoupanca.getCartao().getNumero() >= 1000 && contaPoupanca.getCartao().getNumero() < 10000);
        assertTrue(contaPoupanca.getCartao().getCsv() >= 100 && contaPoupanca.getCartao().getCsv() < 1000);
    }

    // Verifica que uma opcao de tipo de conta fora do fluxo previsto
    // faz o metodo retornar null em vez de criar uma conta.
    @Test
    void abrirNovaContaQuandoEntradaEhInvalidaRetornaNull() {
        configurarEntrada("9\n");

        Conta conta = Banco.getInstancia().abrirNovaConta();

        assertNull(conta);
    }

    // Verifica que o banco consulta a conta pelo numero digitado
    // e delega ao cliente a remocao da conta encontrada.
    @Test
    void fecharContaQuandoContaPertenceAoClienteSolicitaApagarContaSelecionada() {
        Cliente cliente = mock(Cliente.class);
        Conta conta = mock(Conta.class);
        configurarEntrada("11\n");
        when(cliente.selecionarConta(11)).thenReturn(conta);

        Banco.getInstancia().fecharConta(cliente);

        verify(cliente).selecionarConta(11);
        verify(cliente).apagarConta(conta);
    }

    // Verifica que o banco lanca excecao quando a conta informada nao existe
    // e nao tenta apagar nenhuma conta nesse fluxo.
    @Test
    void fecharContaQuandoNumeroNaoExisteLancaExcecaoENaoApagaNada() {
        Cliente cliente = mock(Cliente.class);
        configurarEntrada("99\n");
        when(cliente.selecionarConta(99)).thenReturn(null);

        assertThrows(BankAccountNotFoundException.class, () -> Banco.getInstancia().fecharConta(cliente));

        verify(cliente).selecionarConta(99);
        verify(cliente, never()).apagarConta(org.mockito.ArgumentMatchers.any());
    }

    // Verifica que tentar remover uma conta que nao pertence ao cliente atual
    // resulta no mesmo comportamento de conta nao encontrada.
    @Test
    void fecharContaQuandoContaEhDeOutroClienteSeComportaComoContaNaoEncontrada() {
        Cliente clienteSolicitante = mock(Cliente.class);
        configurarEntrada("22\n");
        when(clienteSolicitante.selecionarConta(22)).thenReturn(null);

        assertThrows(BankAccountNotFoundException.class, () -> Banco.getInstancia().fecharConta(clienteSolicitante));

        verify(clienteSolicitante).selecionarConta(22);
        verify(clienteSolicitante, never()).apagarConta(org.mockito.ArgumentMatchers.any());
    }

    // Verifica que toda conta listada no registro passa pela etapa de cobranca
    // de juros de emprestimo, independentemente do tipo concreto.
    @Test
    void movimentarEntreBancoContaSempreTentaCobrarJurosDeEmprestimoDasContasListadas() {
        Cliente cliente = mock(Cliente.class);
        Conta conta = mock(Conta.class);
        when(cliente.getContas()).thenReturn(new ArrayList<>(List.of(conta)));
        RegistroDeClientes.getInstancia().getClientes().add(cliente);

        Banco.getInstancia().movimentarEntreBancoConta();

        verify(cliente).getContas();
        verify(conta).cobrarJurusEmprestimo();
    }

    // Verifica que uma conta poupanca recebe rendimento automatico
    // e que o banco registra esse impacto como despesa.
    @Test
    void movimentarEntreBancoContaQuandoClienteTemPoupancaAplicaRendimento() {
        Cliente cliente = mock(Cliente.class);
        Cartao cartao = mock(Cartao.class);
        ContaPoupanca contaPoupanca = new ContaPoupanca(1, 80.0, cartao, 0);
        when(cliente.getContas()).thenReturn(new ArrayList<>(List.of(contaPoupanca)));
        RegistroDeClientes.getInstancia().getClientes().add(cliente);

        Banco.getInstancia().movimentarEntreBancoConta();

        verify(cliente).getContas();
        assertEquals(180.0, contaPoupanca.getSaldo(), DELTA);
        assertEquals(100.0, Banco.getInstancia().getDespesas(), DELTA);
        assertEquals(1, contaPoupanca.getInformeRendimento().size());
    }

    // Verifica que uma conta corrente sofre desconto de taxa de manutencao
    // e que uma fatura positiva dispara a cobranca de juros do cartao.
    @Test
    void movimentarEntreBancoContaQuandoHaContaCorrenteDescontaTaxaECobraJurosDaFaturaPositiva() {
        Cliente cliente = mock(Cliente.class);
        Cartao cartaoDebito = mock(Cartao.class);
        CartaoCredito cartaoCredito = mock(CartaoCredito.class);
        ContaCorrente contaCorrente = new ContaCorrente(2, 200.0, cartaoDebito, 0, cartaoCredito);
        when(cliente.getContas()).thenReturn(new ArrayList<>(List.of(contaCorrente)));
        when(cartaoCredito.getFatura()).thenReturn(100.0);
        RegistroDeClientes.getInstancia().getClientes().add(cliente);

        Banco.getInstancia().movimentarEntreBancoConta();

        verify(cliente).getContas();
        verify(cartaoCredito, times(1)).getFatura();
        verify(cartaoCredito, times(1)).cobrarJurus();
        assertEquals(187.0, contaCorrente.getSaldo(), DELTA);
        assertEquals(13.0, Banco.getInstancia().getReceitas(), DELTA);
        assertEquals(1, contaCorrente.getExtrato().size());
    }

    // Verifica que uma conta corrente com fatura zerada sofre apenas a taxa
    // de manutencao e nao dispara cobranca de juros do cartao.
    @Test
    void movimentarEntreBancoContaQuandoFaturaEhZeroNaoCobraJurosDoCartao() {
        Cliente cliente = mock(Cliente.class);
        Cartao cartaoDebito = mock(Cartao.class);
        CartaoCredito cartaoCredito = mock(CartaoCredito.class);
        ContaCorrente contaCorrente = new ContaCorrente(2, 200.0, cartaoDebito, 0, cartaoCredito);
        when(cliente.getContas()).thenReturn(new ArrayList<>(List.of(contaCorrente)));
        when(cartaoCredito.getFatura()).thenReturn(0.0);
        RegistroDeClientes.getInstancia().getClientes().add(cliente);

        Banco.getInstancia().movimentarEntreBancoConta();

        verify(cartaoCredito, times(1)).getFatura();
        verify(cartaoCredito, never()).cobrarJurus();
        assertEquals(187.0, contaCorrente.getSaldo(), DELTA);
        assertEquals(13.0, Banco.getInstancia().getReceitas(), DELTA);
    }

    // Verifica que o metodo de movimentacao nao altera receitas nem despesas
    // quando o registro de clientes esta vazio.
    @Test
    void movimentarEntreBancoContaQuandoNaoHaClientesNaoAlteraEstado() {
        Banco banco = Banco.getInstancia();

        banco.movimentarEntreBancoConta();

        assertEquals(0.0, banco.getDespesas(), DELTA);
        assertEquals(0.0, banco.getReceitas(), DELTA);
    }

    // Verifica que a impressao do banco mantem o estado quando despesas nao
    // superam receitas e apresenta os valores formatados na saida.
    @Test
    void printarBancoQuandoDespesasMenoresOuIguaisReceitasMantemEstadoEImprimeValoresFormatados() {
        Banco banco = Banco.getInstancia();
        banco.setDespesas(10.0);
        banco.setReceitas(20.0);

        banco.printarBanco();

        assertEquals(10.0, banco.getDespesas(), DELTA);
        String saida = recuperarSaida();
        assertTrue(saida.contains("Despesas do banco: 10.00"));
        assertTrue(saida.contains("Receitas do banco: 20.00"));
    }

    // Verifica que a impressao do banco reduz o valor de despesas quando ele
    // e maior que receitas, sem alterar o texto ja exibido na saida.
    @Test
    void printarBancoQuandoDespesasMaioresQueReceitasReduzDespesasDepoisDeImprimir() {
        Banco banco = Banco.getInstancia();
        banco.setDespesas(1000.0);
        banco.setReceitas(100.0);

        banco.printarBanco();

        assertTrue(banco.getDespesas() >= 0.0);
        assertTrue(banco.getDespesas() < 1000.0);
        String saida = recuperarSaida();
        assertTrue(saida.contains("Despesas do banco: 1000.0"));
        assertTrue(saida.contains("Receitas do banco: 100.0"));
    }

    // Verifica que os setters de receitas e despesas ignoram valores negativos
    // e preservam os acumulados anteriores.
    @Test
    void setReceitasESetDespesasIgnoramValoresNegativos() {
        Banco banco = Banco.getInstancia();
        banco.setReceitas(10.0);
        banco.setDespesas(5.0);

        banco.setReceitas(-3.0);
        banco.setDespesas(-4.0);

        assertEquals(10.0, banco.getReceitas(), DELTA);
        assertEquals(5.0, banco.getDespesas(), DELTA);
    }

    // Verifica que setBanco copia corretamente receitas e despesas
    // de outra instancia de Banco.
    @Test
    void setBancoCopiaReceitasEDespesasDeOutraInstancia() {
        Banco origem = new Banco();
        Banco destino = new Banco();
        origem.setReceitas(10.0);
        origem.setDespesas(5.0);

        destino.setBanco(origem);

        assertEquals(10.0, destino.getReceitas(), DELTA);
        assertEquals(5.0, destino.getDespesas(), DELTA);
    }

    // Verifica que o singleton do banco devolve sempre a mesma instancia
    // em chamadas repetidas ao metodo getInstancia.
    @Test
    void getInstanciaRetornaSempreOMesmoObjeto() {
        assertSame(Banco.getInstancia(), Banco.getInstancia());
    }

    private void configurarEntrada(String entrada) {
        System.setIn(new ByteArrayInputStream(entrada.getBytes(StandardCharsets.UTF_8)));
    }

    private String recuperarSaida() {
        return saidaCapturada.toString(StandardCharsets.UTF_8);
    }

    private void limparEstadoCompartilhado() {
        RegistroDeClientes.getInstancia().limparListaDeClientes();
        Banco.getInstancia().despesas = 0;
        Banco.getInstancia().receitas = 0;
    }
}
