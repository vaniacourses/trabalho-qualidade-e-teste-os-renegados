package br.winxbank.sistemaclientes;

import br.winxbank.sistemabancario.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegistroDeClientesTest {

    private InputStream systemInOriginal;
    private Banco banco;
    private Field instanciaBanco;
    private Field campoClientes;

    @BeforeEach
    void configurar() throws Exception {
        systemInOriginal = System.in;
        banco = mock(Banco.class);

        instanciaBanco = Banco.class.getDeclaredField("instancia");
        instanciaBanco.setAccessible(true);
        instanciaBanco.set(null, banco);

        campoClientes = RegistroDeClientes.class.getDeclaredField("clientes");
        campoClientes.setAccessible(true);
    }

    @AfterEach
    void restaurar() throws Exception {
        System.setIn(systemInOriginal);
        instanciaBanco.set(null, null);
    }

    @Test
    // CPF NAO NOVO E LISTA VAZIA (CADASTRAR - CASO IMPOSSIVEL)   - - - - - - (CPF EXISTE NA LISTA, MAS ELA ESTA VAZIA)
    // A logica de checarCpf() precisa ser alterada pois esta confusa demais
    // A logica de verificar if(cpfExistente || clientes.isEmpty()) deveria ser alterada verificar se o arquivo está vazio primeiro, o que pouparia a pesquisa em clientes.
    // E o metodo cadastrarCliente() tem uma coesão muuito baixa. O ideal seria refatorar para que ele tivesse apenas uma responsabilidade.

    // cadastrarCliente
    void cadastrarCpfNaoNovoEListaVaziaTest() throws Exception {
        RegistroDeClientes registro = spy(new RegistroDeClientes());
        when(registro.checarCpf("123")).thenReturn(false);

        ArrayList<Cliente> clientes = mock(ArrayList.class);
        campoClientes.set(registro, clientes);

        ContaCorrente conta = mock(ContaCorrente.class);
        System.setIn(new ByteArrayInputStream("JoaoPedro\n123\n".getBytes(StandardCharsets.UTF_8)));
        when(clientes.isEmpty()).thenReturn(true);
        when(banco.abrirNovaConta()).thenReturn(conta);
        when(conta.getSaldo()).thenReturn(50.0);

        registro.cadastrarCliente();

        ArgumentCaptor<Cliente> clienteCadastrado = ArgumentCaptor.forClass(Cliente.class);
        verify(registro).checarCpf("123");
        verify(clientes).isEmpty();
        verify(banco).abrirNovaConta();
        verify(conta).setExtrato(any(Movimentacao.class));
        verify(clientes).add(clienteCadastrado.capture());
        assertEquals(Cliente.class, clienteCadastrado.getValue().getClass());
        assertEquals("JoaoPedro", clienteCadastrado.getValue().getNome());
        assertEquals("123", clienteCadastrado.getValue().getCpf());
        assertSame(conta, clienteCadastrado.getValue().getContas().get(0));
    }

    @Test
    // CPF NOVO E LISTA NÃO VAZIA (CADASTRAR)
    // cadastrarCliente
    void cadastrarCpfNovoEListaNaoVaziaTest() throws Exception {
        RegistroDeClientes registro = spy(new RegistroDeClientes());
        when(registro.checarCpf("123")).thenReturn(true);

        ArrayList<Cliente> clientes = mock(ArrayList.class);
        campoClientes.set(registro, clientes);

        ContaCorrente conta = mock(ContaCorrente.class);
        System.setIn(new ByteArrayInputStream("JoaoPedro\n123\n".getBytes(StandardCharsets.UTF_8)));
        when(clientes.isEmpty()).thenReturn(false);
        when(banco.abrirNovaConta()).thenReturn(conta);

        when(conta.getSaldo()).thenReturn(50.0);

        registro.cadastrarCliente();

        ArgumentCaptor<Cliente> clienteCadastrado = ArgumentCaptor.forClass(Cliente.class); // criação de um capturador de argumentos (para sabermos se clientes foi passando dentro do add)
        verify(registro).checarCpf("123");
        verify(clientes, never()).isEmpty();
        verify(banco).abrirNovaConta();
        verify(conta).setExtrato(any(Movimentacao.class));
        verify(clientes).add(clienteCadastrado.capture()); // captura o objeto que foi passado como argumento.
        assertEquals(Cliente.class, clienteCadastrado.getValue().getClass());
        assertEquals("JoaoPedro", clienteCadastrado.getValue().getNome());
        assertEquals("123", clienteCadastrado.getValue().getCpf());
        assertSame(conta, clienteCadastrado.getValue().getContas().get(0));
    }

    @Test
    // CPF NAO NOVO e lista nao vazia (NAO CADASTRAR)
    // cadastrarCliente
    void naoCadastrarCpfNaoNovoElistaNaoVaziaTest() throws Exception {
        RegistroDeClientes registro = spy(new RegistroDeClientes());
        when(registro.checarCpf("123")).thenReturn(false);

        ArrayList<Cliente> clientes = mock(ArrayList.class);
        campoClientes.set(registro, clientes);

        System.setIn(new ByteArrayInputStream("JoaoPedro\n123\n".getBytes(StandardCharsets.UTF_8)));
        when(clientes.isEmpty()).thenReturn(false);

        registro.cadastrarCliente();

        verify(registro).checarCpf("123");
        verify(clientes).isEmpty();
        verify(banco, never()).abrirNovaConta();
        verify(clientes, never()).add(any(Cliente.class));
    }

    @Test
    // CPF NOVO e Lista vazia (CADASTRAR)
    // cadastrarCliente
    void cadastrarCpfNovoElistaVaziaTest() throws Exception {
        RegistroDeClientes registro = spy(new RegistroDeClientes());
        when(registro.checarCpf("123")).thenReturn(true);

        ArrayList<Cliente> clientes = mock(ArrayList.class);
        campoClientes.set(registro, clientes);

        ContaCorrente conta = mock(ContaCorrente.class);
        System.setIn(new ByteArrayInputStream("JoaoPedro\n123\n".getBytes(StandardCharsets.UTF_8)));
        when(clientes.isEmpty()).thenReturn(true);
        when(banco.abrirNovaConta()).thenReturn(conta);
        when(conta.getSaldo()).thenReturn(50.0);

        registro.cadastrarCliente();

        ArgumentCaptor<Cliente> clienteCadastrado = ArgumentCaptor.forClass(Cliente.class);
        verify(registro).checarCpf("123");
        verify(clientes, never()).isEmpty();
        verify(banco).abrirNovaConta();
        verify(conta).setExtrato(any(Movimentacao.class));
        verify(clientes).add(clienteCadastrado.capture());
        assertEquals(Cliente.class, clienteCadastrado.getValue().getClass());
        assertEquals("JoaoPedro", clienteCadastrado.getValue().getNome());
        assertEquals("123", clienteCadastrado.getValue().getCpf());
        assertSame(conta, clienteCadastrado.getValue().getContas().get(0));
    }

    @Test
    // cadastrarCliente
    void cadastrarClienteComumContaCorrenteTest() throws Exception {
        RegistroDeClientes registro = spy(new RegistroDeClientes());
        when(registro.checarCpf("123")).thenReturn(true);

        ArrayList<Cliente> clientes = mock(ArrayList.class);
        campoClientes.set(registro, clientes);

        ContaCorrente conta = mock(ContaCorrente.class);
        System.setIn(new ByteArrayInputStream("JoaoPedro\n123\n".getBytes(StandardCharsets.UTF_8)));
        when(clientes.isEmpty()).thenReturn(false);
        when(banco.abrirNovaConta()).thenReturn(conta);
        when(conta.getSaldo()).thenReturn(99999.9);

        registro.cadastrarCliente();

        ArgumentCaptor<Cliente> clienteCadastrado = ArgumentCaptor.forClass(Cliente.class);
        verify(banco).abrirNovaConta();
        verify(conta).setExtrato(any(Movimentacao.class));
        verify(clientes).add(clienteCadastrado.capture());
        assertEquals(Cliente.class, clienteCadastrado.getValue().getClass());
        assertSame(conta, clienteCadastrado.getValue().getContas().get(0));
    }

    @Test
    // cadastrarCliente
    void cadastrarClienteWinxContaCorrenteTest() throws Exception {
        RegistroDeClientes registro = spy(new RegistroDeClientes());
        when(registro.checarCpf("123")).thenReturn(true);

        ArrayList<Cliente> clientes = mock(ArrayList.class);
        campoClientes.set(registro, clientes);

        ContaCorrente conta = mock(ContaCorrente.class);
        System.setIn(new ByteArrayInputStream("JoaoPedro\n123\n".getBytes(StandardCharsets.UTF_8)));
        when(clientes.isEmpty()).thenReturn(false);
        when(banco.abrirNovaConta()).thenReturn(conta);
        when(conta.getSaldo()).thenReturn(100000.5);

        registro.cadastrarCliente();

        ArgumentCaptor<Cliente> clienteCadastrado = ArgumentCaptor.forClass(Cliente.class);
        verify(banco).abrirNovaConta();
        verify(conta).setExtrato(any(Movimentacao.class));
        verify(clientes).add(clienteCadastrado.capture());
        assertInstanceOf(ClienteWinx.class, clienteCadastrado.getValue());
        assertSame(conta, clienteCadastrado.getValue().getContas().get(0));
    }

    @Test
    // cadastrarCliente
    void cadastrarClienteComumContaPoupancaTest() throws Exception {
        RegistroDeClientes registro = spy(new RegistroDeClientes());
        when(registro.checarCpf("123")).thenReturn(true);

        ArrayList<Cliente> clientes = mock(ArrayList.class);
        campoClientes.set(registro, clientes);

        ContaPoupanca conta = mock(ContaPoupanca.class);
        System.setIn(new ByteArrayInputStream("JoaoPedro\n123\n".getBytes(StandardCharsets.UTF_8)));
        when(clientes.isEmpty()).thenReturn(false);
        when(banco.abrirNovaConta()).thenReturn(conta);
        when(conta.getSaldo()).thenReturn(99999.9);

        registro.cadastrarCliente();

        ArgumentCaptor<Cliente> clienteCadastrado = ArgumentCaptor.forClass(Cliente.class);
        verify(banco).abrirNovaConta();
        verify(conta).setExtrato(any(Movimentacao.class));
        verify(conta).setInformeRendimento(any(Movimentacao.class));
        verify(clientes).add(clienteCadastrado.capture());
        assertEquals(Cliente.class, clienteCadastrado.getValue().getClass());
        assertSame(conta, clienteCadastrado.getValue().getContas().get(0));
    }

    @Test
    // cadastrarCliente
    void cadastrarClienteWinxContaPoupancaTest() throws Exception {
        RegistroDeClientes registro = spy(new RegistroDeClientes());
        when(registro.checarCpf("123")).thenReturn(true);

        ArrayList<Cliente> clientes = mock(ArrayList.class);
        campoClientes.set(registro, clientes);

        ContaPoupanca conta = mock(ContaPoupanca.class);
        System.setIn(new ByteArrayInputStream("JoaoPedro\n123\n".getBytes(StandardCharsets.UTF_8)));
        when(clientes.isEmpty()).thenReturn(false);
        when(banco.abrirNovaConta()).thenReturn(conta);
        when(conta.getSaldo()).thenReturn(100000.0);

        registro.cadastrarCliente();

        ArgumentCaptor<Cliente> clienteCadastrado = ArgumentCaptor.forClass(Cliente.class);
        verify(banco).abrirNovaConta();
        verify(conta).setExtrato(any(Movimentacao.class));
        verify(conta).setInformeRendimento(any(Movimentacao.class));
        verify(clientes).add(clienteCadastrado.capture());
        assertInstanceOf(ClienteWinx.class, clienteCadastrado.getValue());
        assertSame(conta, clienteCadastrado.getValue().getContas().get(0));
    }


    @Test
    //atualizarCliente - CPF encontrando
    void AtualizarClienteCpfEncontradoTest() throws Exception {
        //clientes.get(i).cpf
        RegistroDeClientes registro = new RegistroDeClientes();
        ArrayList<Cliente> clientes = new ArrayList<>();
        campoClientes.set(registro, clientes);

        Cliente clienteExistente = new Cliente("Teste", "123");
        Cliente clienteAtualizado = new Cliente("Teste Atualizado", "123");
        Cliente outroCliente = new Cliente("Outro", "456");

        clientes.add(clienteExistente);
        clientes.add(outroCliente);

        registro.atualizarCliente(clienteAtualizado);

        assertEquals(2, clientes.size());
        assertEquals("Outro", clientes.get(0).getNome());
        assertEquals("456", clientes.get(0).getCpf());
        assertEquals("Teste Atualizado", clientes.get(1).getNome());
        assertEquals("123", clientes.get(1).getCpf());
    }

    @Test
    // atualizarCliente - CPF NAO ENCONTRADO
    void naoAtualizarCpfNaoEncontradoTest() throws Exception {
        RegistroDeClientes registro = new RegistroDeClientes();
        ArrayList<Cliente> clientes = new ArrayList<>();
        campoClientes.set(registro, clientes);

        Cliente clienteExistente = new Cliente("Teste", "456");
        Cliente clienteAtualizado = new Cliente("Teste Atualizado", "123");
        Cliente outroCliente = new Cliente("Outro", "789");
        
        clientes.add(clienteExistente);
        clientes.add(outroCliente);

        registro.atualizarCliente(clienteAtualizado);

        assertEquals(2, clientes.size());
        assertEquals("Teste", clientes.get(0).getNome());
        assertEquals("456", clientes.get(0).getCpf());
        assertEquals("Outro", clientes.get(1).getNome());
        assertEquals("789", clientes.get(1).getCpf());
    }

    @Test
    // removerCliente
    void removerClienteCpfEncontradoTest() throws Exception {
        // se o metodo fosse clientes.get(i).getcpf() daria certo o mock
        RegistroDeClientes registro = new RegistroDeClientes();
        ArrayList<Cliente> clientes = new ArrayList<>();
        campoClientes.set(registro, clientes);

        Cliente clienteParaRemover = new Cliente("Teste", "123");
        Cliente clienteExistente = new Cliente("Teste", "123");
        Cliente outroCliente = new Cliente("Outro", "456");
        
        clientes.add(clienteExistente);
        clientes.add(outroCliente);

        registro.removerCliente(clienteParaRemover);

        assertEquals(1, clientes.size());
        assertEquals("Outro", clientes.get(0).getNome());
        assertEquals("456", clientes.get(0).getCpf());
    }

    @Test
    // removerCliente
    void naoRemoverCpfNaoEncontradoTest() throws Exception {
        RegistroDeClientes registro = new RegistroDeClientes();
        ArrayList<Cliente> clientes = new ArrayList<>();
        campoClientes.set(registro, clientes);

        Cliente clienteParaRemover = new Cliente("Teste", "123");
        Cliente clienteExistente = new Cliente("Teste", "456");
        Cliente outroCliente = new Cliente("Outro", "789");
        
        clientes.add(clienteExistente);
        clientes.add(outroCliente);

        registro.removerCliente(clienteParaRemover);

        assertEquals(2, clientes.size());
        assertEquals("Teste", clientes.get(0).getNome());
        assertEquals("456", clientes.get(0).getCpf());
        assertEquals("Outro", clientes.get(1).getNome());
        assertEquals("789", clientes.get(1).getCpf());
    }

    @Test
    // checarCpf
    void checarCpfCadastradoTest() throws Exception {
        // cliente.cpf.equals(cpf) não da pra mockar
        RegistroDeClientes registro = new RegistroDeClientes();
        ArrayList<Cliente> clientes = new ArrayList<>();
        campoClientes.set(registro, clientes);

        Cliente clienteExistente = new Cliente("Teste", "123");
        clientes.add(clienteExistente);

        boolean resultado = registro.checarCpf("123");

        assertFalse(resultado);
    }

    @Test
    //checarCpf
    void checarCpfCpfNaoCadastradoTest() throws Exception {
        RegistroDeClientes registro = new RegistroDeClientes();
        ArrayList<Cliente> clientes = new ArrayList<>();
        campoClientes.set(registro, clientes);

        Cliente clienteExistente = new Cliente("Teste", "456");
        clientes.add(clienteExistente);

        boolean resultado = registro.checarCpf("123");

        assertTrue(resultado);
    }

    @Test
    // checarCpf - ListaVazia nao executa o loop
    void checarCpfListaVaziaTest() throws Exception {
        RegistroDeClientes registro = new RegistroDeClientes();
        ArrayList<Cliente> clientes = new ArrayList<>();
        campoClientes.set(registro, clientes);

        boolean resultado = registro.checarCpf("123");

        assertTrue(resultado);
    }

    @Test
    // retornarCliente
    void RetornarClienteCpfEncontradoTest() throws Exception {
        //cliente.cpf.equals(cpf)
        RegistroDeClientes registro = new RegistroDeClientes();
        ArrayList<Cliente> clientes = new ArrayList<>();
        campoClientes.set(registro, clientes);

        Cliente clienteEsperado = new Cliente("Teste", "123");
        clientes.add(clienteEsperado);

        Cliente resultado = registro.retornarCliente("123");

        assertEquals(clienteEsperado, resultado);
    }

    @Test
    // retornarCliente
    void RetornarClienteCpfNaoEncontradoTest() throws Exception {
        RegistroDeClientes registro = new RegistroDeClientes();
        ArrayList<Cliente> clientes = new ArrayList<>();
        campoClientes.set(registro, clientes);

        Cliente clienteExistente = new Cliente("Teste", "456");
        clientes.add(clienteExistente);

        Cliente resultado = registro.retornarCliente("123");

        assertNull(resultado);
    }

    @Test
    // retornarCliente
    void retornarClientListaNulaTest() throws Exception {
        // se a gente n adicionar null, o tamanho da lista eh 0 e n entra no throws
        RegistroDeClientes registro = new RegistroDeClientes();
        ArrayList<Cliente> clientes = new ArrayList<>();
        clientes.add(null);
        campoClientes.set(registro, clientes);

        assertThrows(NullPointerException.class, () -> registro.retornarCliente("123"));
    }

    @Test
    //limparListaDeClientes
    void limparListaDeClientesTest() throws Exception {
        RegistroDeClientes registro = spy(new RegistroDeClientes());
        ArrayList<Cliente> clientes = mock(ArrayList.class);
        campoClientes.set(registro, clientes);

        registro.limparListaDeClientes();

        verify(clientes).clear();
    }

    @Test
    //setClientes
    void setClientesTest() throws Exception {
        RegistroDeClientes registro = spy(new RegistroDeClientes());
        ArrayList<Cliente> clientes = mock(ArrayList.class);
        campoClientes.set(registro, clientes);

        ArrayList<Cliente> novosClientes = new ArrayList<>();
        novosClientes.add(new Cliente("JoaoPedro", "123"));
        novosClientes.add(new Cliente("Murilo", "456"));

        registro.setClientes(novosClientes);

        verify(clientes).addAll(novosClientes);
    }

    @Test
    // getClienetes
    void getClientesTest() throws Exception {
        RegistroDeClientes registro = new RegistroDeClientes();
        ArrayList<Cliente> clientes = new ArrayList<>();
        campoClientes.set(registro, clientes);

        ArrayList<Cliente> resultado = registro.getClientes();

        assertEquals(clientes, resultado);
    }

    @Test
    //getInstancia - garantir singleton
    void getInstanciaRetornarMesmaInstanciaTest() {

        RegistroDeClientes instancia1 = RegistroDeClientes.getInstancia();
        RegistroDeClientes instancia2 = RegistroDeClientes.getInstancia();

        assertSame(instancia1, instancia2);
    }

    @Test
    // visualizarContas - ContaPoupanca
    void visualizarContasPoupancaClienteTest(){
        RegistroDeClientes registro = spy(new RegistroDeClientes());

        Cliente cliente = mock(Cliente.class);
        Cartao cartao = mock(Cartao.class);
        ContaPoupanca contaPoupanca = mock(ContaPoupanca.class);

        ArrayList<Conta> contas = new ArrayList<>();
        contas.add(contaPoupanca);

        when(cliente.getContas()).thenReturn(contas);
        when(contaPoupanca.getTipoDaConta()).thenReturn("Poupanca");
        when(contaPoupanca.getNumeroConta()).thenReturn(123);
        when(contaPoupanca.getSaldo()).thenReturn(1000.0);
        when(contaPoupanca.getDividaDeEmprestimo()).thenReturn(0.0);
        when(contaPoupanca.getCartao()).thenReturn(cartao);
        when(cartao.getNumero()).thenReturn(456);
        when(cartao.getCsv()).thenReturn(123);

        registro.visualizarContas(cliente);

        verify(cliente).getContas();
        verify(contaPoupanca).getTipoDaConta();
        verify(contaPoupanca).getNumeroConta();
        verify(contaPoupanca).getSaldo();
        verify(contaPoupanca).getDividaDeEmprestimo();
        verify(contaPoupanca, times(2)).getCartao();
        verify(cartao).getNumero();
        verify(cartao).getCsv();
    }

    @Test
    // visualizarContas - ContaCorrente
    void visualizarContasCorrenteClienteTest(){
        RegistroDeClientes registro = spy(new RegistroDeClientes());

        Cliente cliente = mock(Cliente.class);
        Cartao cartao = mock(Cartao.class);
        CartaoCredito cartaoCredito = mock(CartaoCredito.class);
        ContaCorrente contaCorrente = mock(ContaCorrente.class);

        ArrayList<Conta> contas = new ArrayList<>();
        contas.add(contaCorrente);

        when(cliente.getContas()).thenReturn(contas);
        when(contaCorrente.getTipoDaConta()).thenReturn("Corrente");
        when(contaCorrente.getNumeroConta()).thenReturn(123);
        when(contaCorrente.getSaldo()).thenReturn(1000.0);
        when(contaCorrente.getDividaDeEmprestimo()).thenReturn(0.0);
        when(contaCorrente.getCartao()).thenReturn(cartao);
        when(contaCorrente.getCartaoCredito()).thenReturn(cartaoCredito);
        when(cartao.getNumero()).thenReturn(456534534);
        when(cartao.getCsv()).thenReturn(123);
        when(cartaoCredito.getNumero()).thenReturn(789);
        when(cartaoCredito.getCsv()).thenReturn(456);
        when(cartaoCredito.getFatura()).thenReturn(500.0);

        registro.visualizarContas(cliente);

        verify(cliente).getContas();
        verify(contaCorrente).getTipoDaConta();
        verify(contaCorrente).getNumeroConta();
        verify(contaCorrente).getSaldo();
        verify(contaCorrente).getDividaDeEmprestimo();
        verify(contaCorrente, times(2)).getCartao();
        verify(contaCorrente, times(3)).getCartaoCredito();
        verify(cartao).getNumero();
        verify(cartao).getCsv();
        verify(cartaoCredito).getNumero();
        verify(cartaoCredito).getCsv();
        verify(cartaoCredito).getFatura();
    }

    @Test
    // visualizarDetalhesDoCliente - ClienteWinx
    void visualizarDetalhesClienteWinxCpfEncontradoTest() throws Exception {
        RegistroDeClientes registro = spy(new RegistroDeClientes());
        ArrayList<Cliente> clientes = new ArrayList<>();
        campoClientes.set(registro, clientes);

        ClienteWinx clienteWinx = spy(new ClienteWinx("Teste", "123", 100));
        ArrayList<Conta> contas = new ArrayList<>();

        clientes.add(clienteWinx);
        when(clienteWinx.getContas()).thenReturn(contas);

        registro.visualizarDetalhesDoCliente("123");

        verify(clienteWinx).getNome();
        verify(clienteWinx).getCpf();
        verify(clienteWinx).getPontosDeCompra();
        verify(clienteWinx).getContas();
        verify(registro).visualizarContas(clienteWinx);
    }

    @Test
    //visualizarDetelhesDoCliente - getClass()
    void visualizarDetalhesDoClienteComumCpfEncontradoTest() throws Exception {
        RegistroDeClientes registro = spy(new RegistroDeClientes());
        ArrayList<Cliente> clientes = new ArrayList<>();
        campoClientes.set(registro, clientes);

        Cliente cliente = spy(new Cliente("Teste", "123"));
        Cartao cartao = spy(new Cartao(456, 123));
        ContaPoupanca conta = spy(new ContaPoupanca(789, 1000.0, cartao, 0.0));
        ArrayList<Conta> contas = new ArrayList<>();
        contas.add(conta);
        
        clientes.add(cliente);
        when(cliente.getContas()).thenReturn(contas);

        registro.visualizarDetalhesDoCliente("123");

        verify(cliente).getNome();
        verify(cliente).getCpf();
        verify(cliente).getContas();
        verify(registro).visualizarContas(cliente);
    }

    @Test
    // cliente.cpf.equals(cpf) - ClienteWinx com CPF diferente
    void naoVisualizarDetalhesClienteWinxCpfNaoEncontradoTest() throws Exception {
        RegistroDeClientes registro = spy(new RegistroDeClientes());
        ArrayList<Cliente> clientes = new ArrayList<>();
        campoClientes.set(registro, clientes);

        ClienteWinx clienteWinx = spy(new ClienteWinx("Teste", "456", 100));
        clientes.add(clienteWinx);

        registro.visualizarDetalhesDoCliente("123");

        verify(clienteWinx, never()).getNome();
        verify(clienteWinx, never()).getCpf();
        verify(clienteWinx, never()).getPontosDeCompra();
        verify(registro, never()).visualizarContas(any());
    }

    @Test
    // visualizarDetalhesDoCliente - Cliente comum
    void naoVisualizarDetalhesClienteComumCpfNaoEncontradoTest() throws Exception {
        RegistroDeClientes registro = spy(new RegistroDeClientes());
        ArrayList<Cliente> clientes = new ArrayList<>();
        campoClientes.set(registro, clientes);

        Cliente cliente = spy(new Cliente("Teste", "456"));
        clientes.add(cliente);

        registro.visualizarDetalhesDoCliente("123");

        verify(cliente, never()).getNome();
        verify(cliente, never()).getCpf();
        verify(registro, never()).visualizarContas(any());
    }

    @Test
    // printarListaDeClientes - ClienteWinx
    void printarListaClientesWinxTest() throws Exception {
        RegistroDeClientes registro = spy(new RegistroDeClientes());
        ArrayList<Cliente> clientes = new ArrayList<>();
        campoClientes.set(registro, clientes);

        ClienteWinx clienteWinx = spy(new ClienteWinx("Cliente Winx", "123", 100));
        
        clientes.add(clienteWinx);
        
        when(clienteWinx.getContas()).thenReturn(new ArrayList<>());

        registro.printarListaDeClientes();

        verify(clienteWinx).getNome();
        verify(clienteWinx).getCpf();
        verify(clienteWinx).getPontosDeCompra();
        verify(clienteWinx).getContas();
        verify(registro).visualizarContas(clienteWinx);
    }

    @Test
    //printarListaDeClientes - Cliente comum
    void printarListaClientesComumTest() throws Exception {
        RegistroDeClientes registro = spy(new RegistroDeClientes());
        ArrayList<Cliente> clientes = new ArrayList<>();
        campoClientes.set(registro, clientes);

        Cliente cliente = spy(new Cliente("Cliente Comum", "456"));

        clientes.add(cliente);

        when(cliente.getContas()).thenReturn(new ArrayList<>());

        registro.printarListaDeClientes();

        verify(cliente).getNome();
        verify(cliente).getCpf();
        verify(cliente).getContas();
        verify(registro).visualizarContas(cliente);
    }

    // ==================== TESTES PARA cadastrarCliente(String, String, int, double) ====================

    @Test
    // cadastrarCliente(String, String, int, double) - CPF já existe, lista não vazia
    void cadastrarClienteParametrizadoCpfExistenteListaNaoVaziaTest() throws Exception {
        RegistroDeClientes registro = spy(new RegistroDeClientes());
        when(registro.checarCpf("123")).thenReturn(false);

        ArrayList<Cliente> clientes = new ArrayList<>();
        Cliente clienteExistente = new Cliente("Existente", "456");
        clientes.add(clienteExistente);
        campoClientes.set(registro, clientes);

        Cliente resultado = registro.cadastrarCliente("Joao", "123", 1, 1000.0);

        assertNull(resultado);
        assertEquals(1, clientes.size());
        assertEquals("Existente", clientes.get(0).getNome());
        verify(registro).checarCpf("123");
        verify(banco, never()).abrirNovaConta(anyInt(), anyDouble());
    }

    @Test
    // cadastrarCliente(String, String, int, double) - Tipo de conta inválido (conta null)
    void cadastrarClienteParametrizadoTipoContaInvalidoTest() throws Exception {
        RegistroDeClientes registro = spy(new RegistroDeClientes());
        when(registro.checarCpf("123")).thenReturn(true);

        ArrayList<Cliente> clientes = new ArrayList<>();
        campoClientes.set(registro, clientes);

        when(banco.abrirNovaConta(99, 1000.0)).thenReturn(null);

        Cliente resultado = registro.cadastrarCliente("Joao", "123", 99, 1000.0);

        assertNull(resultado);
        assertTrue(clientes.isEmpty());
        verify(banco).abrirNovaConta(99, 1000.0);
    }

    @Test
    // cadastrarCliente(String, String, int, double) - CPF novo, lista vazia, ContaCorrente, saldo < 100000
    void cadastrarClienteParametrizadoCpfNovoListaVaziaCorrenteSaldoBaixoTest() throws Exception {
        RegistroDeClientes registro = spy(new RegistroDeClientes());
        when(registro.checarCpf("123")).thenReturn(true);

        ArrayList<Cliente> clientes = new ArrayList<>();
        campoClientes.set(registro, clientes);

        ContaCorrente conta = mock(ContaCorrente.class);
        when(banco.abrirNovaConta(1, 50000.0)).thenReturn(conta);
        when(conta.getSaldo()).thenReturn(50000.0);

        Cliente resultado = registro.cadastrarCliente("Joao", "123", 1, 50000.0);

        assertNotNull(resultado);
        assertEquals(Cliente.class, resultado.getClass());
        assertEquals("Joao", resultado.getNome());
        assertEquals("123", resultado.getCpf());
        assertEquals(1, clientes.size());
        assertSame(resultado, clientes.get(0));
        verify(banco).abrirNovaConta(1, 50000.0);
        verify(conta).setExtrato(any(Movimentacao.class));
    }

    @Test
    // cadastrarCliente(String, String, int, double) - CPF novo, lista vazia, ContaCorrente, saldo >= 100000
    void cadastrarClienteParametrizadoCpfNovoListaVaziaCorrenteSaldoAltoTest() throws Exception {
        RegistroDeClientes registro = spy(new RegistroDeClientes());
        when(registro.checarCpf("123")).thenReturn(true);

        ArrayList<Cliente> clientes = new ArrayList<>();
        campoClientes.set(registro, clientes);

        ContaCorrente conta = mock(ContaCorrente.class);
        when(banco.abrirNovaConta(1, 150000.0)).thenReturn(conta);
        when(conta.getSaldo()).thenReturn(150000.0);

        Cliente resultado = registro.cadastrarCliente("Joao", "123", 1, 150000.0);

        assertNotNull(resultado);
        assertInstanceOf(ClienteWinx.class, resultado);
        assertEquals("Joao", resultado.getNome());
        assertEquals("123", resultado.getCpf());
        assertEquals(1, clientes.size());
        assertSame(resultado, clientes.get(0));
        verify(banco).abrirNovaConta(1, 150000.0);
        verify(conta).setExtrato(any(Movimentacao.class));
    }

    @Test
    // cadastrarCliente(String, String, int, double) - CPF novo, lista vazia, ContaPoupanca, saldo < 100000
    void cadastrarClienteParametrizadoCpfNovoListaVaziaPoupancaSaldoBaixoTest() throws Exception {
        RegistroDeClientes registro = spy(new RegistroDeClientes());
        when(registro.checarCpf("123")).thenReturn(true);

        ArrayList<Cliente> clientes = new ArrayList<>();
        campoClientes.set(registro, clientes);

        ContaPoupanca conta = mock(ContaPoupanca.class);
        when(banco.abrirNovaConta(2, 50000.0)).thenReturn(conta);
        when(conta.getSaldo()).thenReturn(50000.0);

        Cliente resultado = registro.cadastrarCliente("Joao", "123", 2, 50000.0);

        assertNotNull(resultado);
        assertEquals(Cliente.class, resultado.getClass());
        assertEquals("Joao", resultado.getNome());
        assertEquals("123", resultado.getCpf());
        assertEquals(1, clientes.size());
        assertSame(resultado, clientes.get(0));
        verify(banco).abrirNovaConta(2, 50000.0);
        verify(conta).setExtrato(any(Movimentacao.class));
        verify(conta).setInformeRendimento(any(Movimentacao.class));
    }

    @Test
    // cadastrarCliente(String, String, int, double) - CPF novo, lista vazia, ContaPoupanca, saldo >= 100000
    void cadastrarClienteParametrizadoCpfNovoListaVaziaPoupancaSaldoAltoTest() throws Exception {
        RegistroDeClientes registro = spy(new RegistroDeClientes());
        when(registro.checarCpf("123")).thenReturn(true);

        ArrayList<Cliente> clientes = new ArrayList<>();
        campoClientes.set(registro, clientes);

        ContaPoupanca conta = mock(ContaPoupanca.class);
        when(banco.abrirNovaConta(2, 150000.0)).thenReturn(conta);
        when(conta.getSaldo()).thenReturn(150000.0);

        Cliente resultado = registro.cadastrarCliente("Joao", "123", 2, 150000.0);

        assertNotNull(resultado);
        assertInstanceOf(ClienteWinx.class, resultado);
        assertEquals("Joao", resultado.getNome());
        assertEquals("123", resultado.getCpf());
        assertEquals(1, clientes.size());
        assertSame(resultado, clientes.get(0));
        verify(banco).abrirNovaConta(2, 150000.0);
        verify(conta).setExtrato(any(Movimentacao.class));
        verify(conta).setInformeRendimento(any(Movimentacao.class));
    }

    @Test
    // cadastrarCliente(String, String, int, double) - CPF novo, lista não vazia, ContaCorrente, saldo < 100000
    void cadastrarClienteParametrizadoCpfNovoListaNaoVaziaCorrenteSaldoBaixoTest() throws Exception {
        RegistroDeClientes registro = spy(new RegistroDeClientes());
        when(registro.checarCpf("123")).thenReturn(true);

        ArrayList<Cliente> clientes = new ArrayList<>();
        Cliente clienteExistente = new Cliente("Existente", "456");
        clientes.add(clienteExistente);
        campoClientes.set(registro, clientes);

        ContaCorrente conta = mock(ContaCorrente.class);
        when(banco.abrirNovaConta(1, 50000.0)).thenReturn(conta);
        when(conta.getSaldo()).thenReturn(50000.0);

        Cliente resultado = registro.cadastrarCliente("Joao", "123", 1, 50000.0);

        assertNotNull(resultado);
        assertEquals(Cliente.class, resultado.getClass());
        assertEquals("Joao", resultado.getNome());
        assertEquals("123", resultado.getCpf());
        assertEquals(2, clientes.size());
        assertSame(resultado, clientes.get(1));
        verify(banco).abrirNovaConta(1, 50000.0);
        verify(conta).setExtrato(any(Movimentacao.class));
    }

    @Test
    // cadastrarCliente(String, String, int, double) - CPF novo, lista não vazia, ContaCorrente, saldo >= 100000
    void cadastrarClienteParametrizadoCpfNovoListaNaoVaziaCorrenteSaldoAltoTest() throws Exception {
        RegistroDeClientes registro = spy(new RegistroDeClientes());
        when(registro.checarCpf("123")).thenReturn(true);

        ArrayList<Cliente> clientes = new ArrayList<>();
        Cliente clienteExistente = new Cliente("Existente", "456");
        clientes.add(clienteExistente);
        campoClientes.set(registro, clientes);

        ContaCorrente conta = mock(ContaCorrente.class);
        when(banco.abrirNovaConta(1, 150000.0)).thenReturn(conta);
        when(conta.getSaldo()).thenReturn(150000.0);

        Cliente resultado = registro.cadastrarCliente("Joao", "123", 1, 150000.0);

        assertNotNull(resultado);
        assertInstanceOf(ClienteWinx.class, resultado);
        assertEquals("Joao", resultado.getNome());
        assertEquals("123", resultado.getCpf());
        assertEquals(2, clientes.size());
        assertSame(resultado, clientes.get(1));
        verify(banco).abrirNovaConta(1, 150000.0);
        verify(conta).setExtrato(any(Movimentacao.class));
    }

    @Test
    // cadastrarCliente(String, String, int, double) - CPF novo, lista não vazia, ContaPoupanca, saldo < 100000
    void cadastrarClienteParametrizadoCpfNovoListaNaoVaziaPoupancaSaldoBaixoTest() throws Exception {
        RegistroDeClientes registro = spy(new RegistroDeClientes());
        when(registro.checarCpf("123")).thenReturn(true);

        ArrayList<Cliente> clientes = new ArrayList<>();
        Cliente clienteExistente = new Cliente("Existente", "456");
        clientes.add(clienteExistente);
        campoClientes.set(registro, clientes);

        ContaPoupanca conta = mock(ContaPoupanca.class);
        when(banco.abrirNovaConta(2, 50000.0)).thenReturn(conta);
        when(conta.getSaldo()).thenReturn(50000.0);

        Cliente resultado = registro.cadastrarCliente("Joao", "123", 2, 50000.0);

        assertNotNull(resultado);
        assertEquals(Cliente.class, resultado.getClass());
        assertEquals("Joao", resultado.getNome());
        assertEquals("123", resultado.getCpf());
        assertEquals(2, clientes.size());
        assertSame(resultado, clientes.get(1));
        verify(banco).abrirNovaConta(2, 50000.0);
        verify(conta).setExtrato(any(Movimentacao.class));
        verify(conta).setInformeRendimento(any(Movimentacao.class));
    }

    @Test
    // cadastrarCliente(String, String, int, double) - CPF novo, lista não vazia, ContaPoupanca, saldo >= 100000
    void cadastrarClienteParametrizadoCpfNovoListaNaoVaziaPoupancaSaldoAltoTest() throws Exception {
        RegistroDeClientes registro = spy(new RegistroDeClientes());
        when(registro.checarCpf("123")).thenReturn(true);

        ArrayList<Cliente> clientes = new ArrayList<>();
        Cliente clienteExistente = new Cliente("Existente", "456");
        clientes.add(clienteExistente);
        campoClientes.set(registro, clientes);

        ContaPoupanca conta = mock(ContaPoupanca.class);
        when(banco.abrirNovaConta(2, 150000.0)).thenReturn(conta);
        when(conta.getSaldo()).thenReturn(150000.0);

        Cliente resultado = registro.cadastrarCliente("Joao", "123", 2, 150000.0);

        assertNotNull(resultado);
        assertInstanceOf(ClienteWinx.class, resultado);
        assertEquals("Joao", resultado.getNome());
        assertEquals("123", resultado.getCpf());
        assertEquals(2, clientes.size());
        assertSame(resultado, clientes.get(1));
        verify(banco).abrirNovaConta(2, 150000.0);
        verify(conta).setExtrato(any(Movimentacao.class));
        verify(conta).setInformeRendimento(any(Movimentacao.class));
    }

    @Test
    // cadastrarCliente(String, String, int, double) - CPF existe, lista vazia (bug na lógica OR)
    void cadastrarClienteParametrizadoCpfExistenteListaVaziaTest() throws Exception {
        RegistroDeClientes registro = spy(new RegistroDeClientes());
        when(registro.checarCpf("123")).thenReturn(false);

        ArrayList<Cliente> clientes = new ArrayList<>();
        campoClientes.set(registro, clientes);

        ContaCorrente conta = mock(ContaCorrente.class);
        when(banco.abrirNovaConta(1, 50000.0)).thenReturn(conta);
        when(conta.getSaldo()).thenReturn(50000.0);

        Cliente resultado = registro.cadastrarCliente("Joao", "123", 1, 50000.0);

        assertNotNull(resultado);
        assertEquals(Cliente.class, resultado.getClass());
        assertEquals("Joao", resultado.getNome());
        assertEquals("123", resultado.getCpf());
        assertEquals(1, clientes.size());
        verify(banco).abrirNovaConta(1, 50000.0);
        verify(conta).setExtrato(any(Movimentacao.class));
    }

    @Test
    // cadastrarCliente(String, String, int, double) - Saldo exatamente 100000 (limite)
    void cadastrarClienteParametrizadoSaldoExatamenteLimiteTest() throws Exception {
        RegistroDeClientes registro = spy(new RegistroDeClientes());
        when(registro.checarCpf("123")).thenReturn(true);

        ArrayList<Cliente> clientes = new ArrayList<>();
        campoClientes.set(registro, clientes);

        ContaCorrente conta = mock(ContaCorrente.class);
        when(banco.abrirNovaConta(1, 100000.0)).thenReturn(conta);
        when(conta.getSaldo()).thenReturn(100000.0);

        Cliente resultado = registro.cadastrarCliente("Joao", "123", 1, 100000.0);

        assertNotNull(resultado);
        assertInstanceOf(ClienteWinx.class, resultado);
        assertEquals("Joao", resultado.getNome());
        assertEquals("123", resultado.getCpf());
        verify(banco).abrirNovaConta(1, 100000.0);
        verify(conta).setExtrato(any(Movimentacao.class));
    }
}
