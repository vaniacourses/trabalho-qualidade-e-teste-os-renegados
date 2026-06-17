package br.winxbank.sistemabancario;

import br.winxbank.exception.BankAccountNotFoundException;
import br.winxbank.random.RandomNumberGenerator;
import br.winxbank.sistemaclientes.Cliente;
import br.winxbank.sistemaclientes.RegistroDeClientes;
import br.winxbank.tempo.Ano;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BancoTest {

    private static final double DELTA = 0.0001;

    private PrintStream systemOutOriginal;
    private ByteArrayOutputStream saidaCapturada;

    @BeforeEach
    void configurarAmbiente() {
        systemOutOriginal = System.out;
        saidaCapturada = new ByteArrayOutputStream();
        System.setOut(new PrintStream(saidaCapturada, true, StandardCharsets.UTF_8));
        limparEstadoCompartilhado();
        Ano.getInstancia().setMesAtual("Abril");
    }

    @AfterEach
    void restaurarAmbiente() {
        System.setOut(systemOutOriginal);
        limparEstadoCompartilhado();
        Ano.getInstancia().setMesAtual("Abril");
    }


    /**
     * Método responsável por testar a abertura de uma conta corrente com entradas esperadas.
     */
    @Test
    void abrirNovaContaQuandoEntradaEhUmCriaContaCorrenteComDadosEsperados() {
        try (MockedConstruction<Scanner> scannerMock = mockConstruction(Scanner.class,
                (mock, context) -> {
                    when(mock.nextInt()).thenReturn(1);
                    when(mock.nextDouble()).thenReturn(250.75);
                });
             MockedStatic<RandomNumberGenerator> randomMock = mockStatic(RandomNumberGenerator.class)) {
            randomMock.when(RandomNumberGenerator::gerarNumCartao).thenReturn(1234);
            randomMock.when(RandomNumberGenerator::gerarCsv).thenReturn(456);
            randomMock.when(RandomNumberGenerator::gerarNumConta).thenReturn(54321);

            Conta conta = Banco.getInstancia().abrirNovaConta();

            ContaCorrente contaCorrente = assertInstanceOf(ContaCorrente.class, conta);
            assertEquals(250.75, contaCorrente.getSaldo(), DELTA);
            assertEquals(0.0, contaCorrente.getDividaDeEmprestimo(), DELTA);
            assertEquals(54321, contaCorrente.getNumeroConta());
            assertEquals(1234, contaCorrente.getCartao().getNumero());
            assertEquals(456, contaCorrente.getCartao().getCsv());
            assertEquals(1234, contaCorrente.getCartaoCredito().getNumero());
            assertEquals(456, contaCorrente.getCartaoCredito().getCsv());
        }
    }

    @Test
    void abrirNovaContaWebQuandoTipoEhUmCriaContaCorrenteSemScanner() {
        try (MockedStatic<RandomNumberGenerator> randomMock = mockStatic(RandomNumberGenerator.class)) {
            randomMock.when(RandomNumberGenerator::gerarNumCartao).thenReturn(7777);
            randomMock.when(RandomNumberGenerator::gerarCsv).thenReturn(321);
            randomMock.when(RandomNumberGenerator::gerarNumConta).thenReturn(45678);

            Conta conta = Banco.getInstancia().abrirNovaConta(1, 800.0);

            ContaCorrente contaCorrente = assertInstanceOf(ContaCorrente.class, conta);
            assertEquals(45678, contaCorrente.getNumeroConta());
            assertEquals(800.0, contaCorrente.getSaldo(), DELTA);
            assertEquals(7777, contaCorrente.getCartao().getNumero());
            assertEquals(321, contaCorrente.getCartao().getCsv());
            assertEquals(7777, contaCorrente.getCartaoCredito().getNumero());
            assertEquals(321, contaCorrente.getCartaoCredito().getCsv());
        }
    }

    @Test
    void abrirNovaContaWebQuandoTipoEhDoisCriaContaPoupancaSemScanner() {
        try (MockedStatic<RandomNumberGenerator> randomMock = mockStatic(RandomNumberGenerator.class)) {
            randomMock.when(RandomNumberGenerator::gerarNumCartao).thenReturn(8888);
            randomMock.when(RandomNumberGenerator::gerarCsv).thenReturn(654);
            randomMock.when(RandomNumberGenerator::gerarNumConta).thenReturn(56789);

            Conta conta = Banco.getInstancia().abrirNovaConta(2, 900.0);

            ContaPoupanca contaPoupanca = assertInstanceOf(ContaPoupanca.class, conta);
            assertEquals(56789, contaPoupanca.getNumeroConta());
            assertEquals(900.0, contaPoupanca.getSaldo(), DELTA);
            assertEquals(8888, contaPoupanca.getCartao().getNumero());
            assertEquals(654, contaPoupanca.getCartao().getCsv());
        }
    }

    @Test
    void abrirNovaContaWebQuandoTipoEhInvalidoRetornaNullENaoGeraNumeros() {
        try (MockedStatic<RandomNumberGenerator> randomMock = mockStatic(RandomNumberGenerator.class)) {
            Conta conta = Banco.getInstancia().abrirNovaConta(0, 100.0);

            assertNull(conta);
            randomMock.verifyNoInteractions();
        }
    }

    /**
     * Método responsável por testar a abertura de uma conta poupanca com entradas esperadas.
     */
    @Test
    void abrirNovaContaQuandoEntradaEhDoisCriaContaPoupancaComDadosEsperados() {
        try (MockedConstruction<Scanner> scannerMock = mockConstruction(Scanner.class,
                (mock, context) -> {
                    when(mock.nextInt()).thenReturn(2);
                    when(mock.nextDouble()).thenReturn(250.75);
                });
             MockedStatic<RandomNumberGenerator> randomMock = mockStatic(RandomNumberGenerator.class)) {
            randomMock.when(RandomNumberGenerator::gerarNumCartao).thenReturn(1234);
            randomMock.when(RandomNumberGenerator::gerarCsv).thenReturn(456);
            randomMock.when(RandomNumberGenerator::gerarNumConta).thenReturn(54321);

            Conta conta = Banco.getInstancia().abrirNovaConta();

            ContaPoupanca contaPoupanca = assertInstanceOf(ContaPoupanca.class, conta);
            assertEquals(250.75, contaPoupanca.getSaldo(), DELTA);
            assertEquals(0.0, contaPoupanca.getDividaDeEmprestimo(), DELTA);
            assertEquals(54321, contaPoupanca.getNumeroConta());
            assertEquals(1234, contaPoupanca.getCartao().getNumero());
            assertEquals(456, contaPoupanca.getCartao().getCsv());
        }
    }

    /**
     * Método responsável por testar a abertura de uma conta corrente com entradas de saldo negativas.
     */
    @Test
    void abrirNovaContaQuandoEntradaEhUmMantemSaldoNegativoSemValidacao() {
        try (MockedConstruction<Scanner> scannerMock = mockConstruction(Scanner.class,
                (mock, context) -> {
                    when(mock.nextInt()).thenReturn(1);
                    when(mock.nextDouble()).thenReturn(-25.0);
                });
             MockedStatic<RandomNumberGenerator> randomMock = mockStatic(RandomNumberGenerator.class)) {
            randomMock.when(RandomNumberGenerator::gerarNumCartao).thenReturn(1111);
            randomMock.when(RandomNumberGenerator::gerarCsv).thenReturn(222);
            randomMock.when(RandomNumberGenerator::gerarNumConta).thenReturn(33333);

            Conta conta = Banco.getInstancia().abrirNovaConta();

            ContaCorrente contaCorrente = assertInstanceOf(ContaCorrente.class, conta);
            assertEquals(-25.0, contaCorrente.getSaldo(), DELTA);
            assertEquals(0.0, contaCorrente.getDividaDeEmprestimo(), DELTA);
            assertEquals(33333, contaCorrente.getNumeroConta());
        }
    }

    /**
     * Método responsável por testar a abertura de uma conta poupanca com entradas de saldo negativas.
     */
    @Test
    void abrirNovaContaQuandoEntradaEhDoisMantemSaldoNegativoSemValidacao() {
        try (MockedConstruction<Scanner> scannerMock = mockConstruction(Scanner.class,
                (mock, context) -> {
                    when(mock.nextInt()).thenReturn(2);
                    when(mock.nextDouble()).thenReturn(-50.0);
                });
             MockedStatic<RandomNumberGenerator> randomMock = mockStatic(RandomNumberGenerator.class)) {
            randomMock.when(RandomNumberGenerator::gerarNumCartao).thenReturn(4444);
            randomMock.when(RandomNumberGenerator::gerarCsv).thenReturn(555);
            randomMock.when(RandomNumberGenerator::gerarNumConta).thenReturn(66666);

            Conta conta = Banco.getInstancia().abrirNovaConta();

            ContaPoupanca contaPoupanca = assertInstanceOf(ContaPoupanca.class, conta);
            assertEquals(-50.0, contaPoupanca.getSaldo(), DELTA);
            assertEquals(0.0, contaPoupanca.getDividaDeEmprestimo(), DELTA);
            assertEquals(66666, contaPoupanca.getNumeroConta());
            assertEquals(4444, contaPoupanca.getCartao().getNumero());
            assertEquals(555, contaPoupanca.getCartao().getCsv());
        }
    }

    /**
     * Método responsável por testar a abertura de uma conta com a entrada de decisão entre
     * corrente e poupança fora do fluxo
     */
    @Test
    void abrirNovaContaQuandoEntradaEhInvalidaRetornaNull() {
        try (MockedConstruction<Scanner> scannerMock = mockConstruction(Scanner.class,
                (mock, context) -> when(mock.nextInt()).thenReturn(9));
             MockedStatic<RandomNumberGenerator> randomMock = mockStatic(RandomNumberGenerator.class)) {
            Conta conta = Banco.getInstancia().abrirNovaConta();

            assertNull(conta);
            verify(scannerMock.constructed().get(0)).nextInt();
            randomMock.verifyNoInteractions();
        }
    }

    /**
     * Método responsável por testar o fechamento de uma conta quando é o próprio cliente solicitando
     *
     */
    @Test
    void fecharContaQuandoContaPertenceAoClienteSolicitaApagarContaSelecionada() {
        Cliente cliente = mock(Cliente.class);

        try (MockedConstruction<Scanner> scannerMock = mockConstruction(Scanner.class,
                (mock, context) -> when(mock.nextInt()).thenReturn(11))) {
            Conta conta = mock(Conta.class);
            when(cliente.selecionarConta(11)).thenReturn(conta);

            Banco.getInstancia().fecharConta(cliente);

            verify(cliente).selecionarConta(11);
            verify(cliente).apagarConta(conta);
        }
    }

    /**
     * Método responsável por testar o fechamento de uma conta quando a conta nao existe
     *
     */
    @Test
    void fecharContaQuandoNumeroNaoExisteLancaExcecaoENaoApagaNada() {
        Cliente cliente = mock(Cliente.class);

        try (MockedConstruction<Scanner> scannerMock = mockConstruction(Scanner.class,
                (mock, context) -> when(mock.nextInt()).thenReturn(99))) {
            when(cliente.selecionarConta(99)).thenReturn(null);

            assertThrows(BankAccountNotFoundException.class, () -> Banco.getInstancia().fecharConta(cliente));

            verify(cliente).selecionarConta(99);
            verify(cliente, never()).apagarConta(org.mockito.ArgumentMatchers.any());
        }
    }

    /**
     * Método responsável por testar o fechamento de uma conta quando um cliente tenta
     * fechar uma conta que nao e sua
     */
    @Test
    void fecharContaQuandoContaEhDeOutroClienteSeComportaComoContaNaoEncontrada() {
        Cliente clienteSolicitante = mock(Cliente.class);

        try (MockedConstruction<Scanner> scannerMock = mockConstruction(Scanner.class,
                (mock, context) -> when(mock.nextInt()).thenReturn(22))) {
            when(clienteSolicitante.selecionarConta(22)).thenReturn(null);

            assertThrows(BankAccountNotFoundException.class, () -> Banco.getInstancia().fecharConta(clienteSolicitante));

            verify(clienteSolicitante).selecionarConta(22);
            verify(clienteSolicitante, never()).apagarConta(org.mockito.ArgumentMatchers.any());
        }
    }

    @Test
    void fecharContaWebQuandoContaExisteApagaContaSelecionada() {
        Cliente cliente = mock(Cliente.class);
        Conta conta = mock(Conta.class);
        when(cliente.selecionarConta(33)).thenReturn(conta);

        Banco.getInstancia().fecharConta(cliente, 33);

        verify(cliente).selecionarConta(33);
        verify(cliente).apagarConta(conta);
    }

    @Test
    void fecharContaWebQuandoContaNaoExisteLancaExcecaoENaoApagaConta() {
        Cliente cliente = mock(Cliente.class);
        when(cliente.selecionarConta(44)).thenReturn(null);

        assertThrows(BankAccountNotFoundException.class, () -> Banco.getInstancia().fecharConta(cliente, 44));

        verify(cliente).selecionarConta(44);
        verify(cliente, never()).apagarConta(org.mockito.ArgumentMatchers.any());
    }

    /**
     * Método responsável por testar a movimentação do banco para com uma conta
     *
     */
    @Test
    void movimentarEntreBancoContaSempreTentaCobrarJurosDeEmprestimoDasContasListadas() {
        Cliente cliente = mock(Cliente.class);
        Conta conta = mock(Conta.class);
        when(cliente.getContas()).thenReturn(new ArrayList<>(List.of(conta)));
        RegistroDeClientes.getInstancia().setClientes(new ArrayList<>(List.of(cliente)));

        Banco.getInstancia().movimentarEntreBancoConta();

        verify(cliente).getContas();
        verify(conta).cobrarJurusEmprestimo();
    }

    /**
     * Método responsável por testar a movimentação do banco para com uma conta quando nao existem contas
     *
     */
    @Test
    void movimentarEntreBancoContaQuandoContaEhPoupancaAcrescentaRendimento() {
        Cliente cliente = mock(Cliente.class);
        ContaPoupanca contaPoupanca = mock(ContaPoupanca.class);
        when(cliente.getContas()).thenReturn(new ArrayList<>(List.of(contaPoupanca)));
        RegistroDeClientes.getInstancia().setClientes(new ArrayList<>(List.of(cliente)));

        Banco.getInstancia().movimentarEntreBancoConta();

        verify(cliente).getContas();
        verify(contaPoupanca).cobrarJurusEmprestimo();
        verify(contaPoupanca).acrescentarRendimento();
    }

    /**
     * Metodo responsavel por testar a movimentacao do banco para com uma conta corrente
     *
     */
    @Test
    void movimentarEntreBancoContaQuandoContaEhCorrenteDescontaTaxaECobraJurosDaFatura() {
        Cliente cliente = mock(Cliente.class);
        ContaCorrente contaCorrente = mock(ContaCorrente.class);
        CartaoCredito cartaoCredito = mock(CartaoCredito.class);
        when(cliente.getContas()).thenReturn(new ArrayList<>(List.of(contaCorrente)));
        when(contaCorrente.getCartaoCredito()).thenReturn(cartaoCredito);
        when(cartaoCredito.getFatura()).thenReturn(100.0);
        RegistroDeClientes.getInstancia().setClientes(new ArrayList<>(List.of(cliente)));

        Banco.getInstancia().movimentarEntreBancoConta();

        verify(cliente).getContas();
        verify(contaCorrente).cobrarJurusEmprestimo();
        verify(contaCorrente).descontarTaxa();
        verify(cartaoCredito).getFatura();
        verify(cartaoCredito).cobrarJurus();
    }

    @Test
    void movimentarEntreBancoContaQuandoContaCorrenteNaoTemFaturaNaoCobraJurosDaFatura() {
        Cliente cliente = mock(Cliente.class);
        ContaCorrente contaCorrente = mock(ContaCorrente.class);
        CartaoCredito cartaoCredito = mock(CartaoCredito.class);
        when(cliente.getContas()).thenReturn(new ArrayList<>(List.of(contaCorrente)));
        when(contaCorrente.getCartaoCredito()).thenReturn(cartaoCredito);
        when(cartaoCredito.getFatura()).thenReturn(0.0);
        RegistroDeClientes.getInstancia().setClientes(new ArrayList<>(List.of(cliente)));

        Banco.getInstancia().movimentarEntreBancoConta();

        verify(contaCorrente).cobrarJurusEmprestimo();
        verify(contaCorrente).descontarTaxa();
        verify(cartaoCredito).getFatura();
        verify(cartaoCredito, never()).cobrarJurus();
    }

    @Test
    void movimentarEntreBancoContaQuandoClienteNaoTemContasNaoMovimentaNada() {
        Cliente cliente = mock(Cliente.class);
        when(cliente.getContas()).thenReturn(new ArrayList<>());
        RegistroDeClientes.getInstancia().setClientes(new ArrayList<>(List.of(cliente)));

        Banco.getInstancia().movimentarEntreBancoConta();

        verify(cliente).getContas();
        assertEquals(0.0, Banco.getInstancia().getReceitas(), DELTA);
        assertEquals(0.0, Banco.getInstancia().getDespesas(), DELTA);
    }

    /**
     * Metodo responsavel por testar a movimentacao do banco para com uma conta quando nao existem contas
     *
     */
    @Test
    void movimentarEntreBancoContaQuandoNaoHaClientesNaoAlteraEstado() {
        Banco banco = Banco.getInstancia();

        banco.movimentarEntreBancoConta();

        assertEquals(0.0, banco.getDespesas(), DELTA);
        assertEquals(0.0, banco.getReceitas(), DELTA);
    }

    /**
     * Método responsável por testar a saída de receitas e despesas do banco
     *
     */
    @Test
    void printarBancoQuandoDespesasMenoresOuIguaisReceitasMantemEstadoEImprimeValoresFormatados() {
        Banco banco = Banco.getInstancia();
        banco.setDespesas(10.0);
        banco.setReceitas(20.0);

        banco.printarBanco();

        assertEquals(10.0, banco.getDespesas(), DELTA);
        String saida = recuperarSaida();
        assertTrue(saida.contains("Despesas do banco: 10,00"));
        assertTrue(saida.contains("Receitas do banco: 20,00"));
    }

    /**
     * Método responsável por testar a saída de receitas e despesas do banco
     * quando receita < despeza, o valor de despeza e deduzido por um porcentagem aleatoria
     * nesse caso, os valores estão sendo mudados apos serem imprimidos (o que implica num erro de logica)
     */
    @Test
    void printarBancoQuandoDespesasMaioresQueReceitasReduzDespesasDepoisDeImprimir() {
        Banco banco = Banco.getInstancia();
        banco.setDespesas(1000.0);
        banco.setReceitas(100.0);

        banco.printarBanco();

        assertTrue(banco.getDespesas() >= 0.0);
        assertTrue(banco.getDespesas() < 1000.0);
        String saida = recuperarSaida();
        assertTrue(saida.contains("Despesas do banco: 1000,0"));
        assertTrue(saida.contains("Receitas do banco: 100,0"));
    }

    /**
     * Método responsável por testar a adição de valores negativos em receitas e despezas
     *
     */
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

    /**
     * Método responsável por testar a movimentação de despesas e receitas de uma instancia de banco para outra
     *
     */
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

    /**
     * Método responsável por testar se uma mesma instancia retorna o mesmo objeto
     *
     */
    @Test
    void getInstanciaRetornaSempreOMesmoObjeto() {
        assertSame(Banco.getInstancia(), Banco.getInstancia());
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
