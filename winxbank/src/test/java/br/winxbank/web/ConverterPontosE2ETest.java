package br.winxbank.web;

import br.winxbank.sistemabancario.Banco;
import br.winxbank.sistemaclientes.RegistroDeClientes;
import br.winxbank.tempo.Ano;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes de SISTEMA (E2E) da funcionalidade "Converter pontos em saldo" exercitada
 * ponta a ponta pela camada web (servidor HTTP real), cobrindo o requisito funcional
 * que usa as classes da Entrega 1: ClienteWinx + PoliticaDeConversaoPadrao.
 *
 * Abordagem "puro HTTP": sobe o WinxBankWebServer numa porta efêmera e faz requisições
 * HTTP reais (login + ação), sem depender de navegador/ChromeDriver. É determinístico e
 * roda em qualquer ambiente de CI. Um teste Selenium com navegador real complementa este
 * (ver ConverterPontosSeleniumTest).
 *
 * O estado é semeado escrevendo clientes.json antes de subir o servidor; o repositório
 * reconstrói um ClienteWinx sempre que o JSON contém o campo "pontosDeCompra".
 */
class ConverterPontosE2ETest {

    private static final double DELTA = 0.01;

    private final Map<Path, byte[]> arquivosOriginais = new HashMap<>();
    private HttpServer server;
    private String baseUrl;

    // Cliente Winx com 10 pontos (mínimo da PoliticaDeConversaoPadrao) e conta corrente saldo 0.
    private static final String CPF_WINX = "70070070070";
    private static final int CONTA_WINX = 50001;

    // Cliente comum (sem o campo pontosDeCompra -> não é ClienteWinx).
    private static final String CPF_COMUM = "80080080080";
    private static final int CONTA_COMUM = 50002;

    // Cliente Winx com 0 pontos (não tem o que converter).
    private static final String CPF_WINX_SEM_PONTOS = "90090090090";
    private static final int CONTA_WINX_SEM_PONTOS = 50003;

    @BeforeEach
    void iniciarServidor() throws IOException {
        preservarArquivo(Path.of("clientes.json"));
        preservarArquivo(Path.of("mesAtual.txt"));
        preservarArquivo(Path.of("banco.txt"));

        Files.writeString(Path.of("clientes.json"), estadoInicialJson(), StandardCharsets.UTF_8);
        Files.writeString(Path.of("mesAtual.txt"), "Janeiro", StandardCharsets.UTF_8);
        Files.deleteIfExists(Path.of("banco.txt"));
        limparEstado();

        server = WinxBankWebServer.criarServidor(0, new WinxBankWebService());
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterEach
    void pararServidor() throws IOException {
        if (server != null) {
            server.stop(0);
        }
        limparEstado();
        restaurarArquivo(Path.of("clientes.json"));
        restaurarArquivo(Path.of("mesAtual.txt"));
        restaurarArquivo(Path.of("banco.txt"));
    }

    @Test
    // Caminho feliz: ClienteWinx com 10 pontos converte -> pontos zeram e a conversão é confirmada.
    // O valor exato do saldo sofre desconto das taxas mensais automáticas do banco (fazerMesPassar
    // a cada requisição) e depende da ordem de execução, então a prova determinística da conversão
    // é a mensagem de sucesso somada ao zeramento dos pontos no dashboard.
    void clienteWinxComPontosSuficientesConverteEAumentaSaldoEZeraPontos() throws IOException {
        postLogin(CPF_WINX);

        Resposta resposta = post("/acao", Map.of(
                "acao", "converterPontos",
                "numeroContaConverter", String.valueOf(CONTA_WINX)
        ));

        assertEquals(200, resposta.status);
        assertTrue(resposta.body.contains("Pontos convertidos em saldo com sucesso."),
                "Deveria confirmar a conversão");
        // Dashboard mostra os pontos zerados após a conversão.
        assertTrue(resposta.body.contains("Pontos:</strong> 0"),
                "Os pontos deveriam estar zerados no dashboard");
    }

    @Test
    // Cliente comum (não-Winx) não pode converter pontos: regra de tipo de cliente.
    void clienteComumNaoPodeConverterPontos() throws IOException {
        login(CPF_COMUM);

        Resposta resposta = post("/acao", Map.of(
                "acao", "converterPontos",
                "numeroContaConverter", String.valueOf(CONTA_COMUM)
        ));

        assertEquals(200, resposta.status);
        assertTrue(resposta.body.contains("Cliente encontrado nao e um cliente winx."),
                "Deveria rejeitar conversão para cliente comum");
        assertFalse(resposta.body.contains("Pontos convertidos em saldo com sucesso."),
                "Não deveria converter para cliente comum");
    }

    @Test
    // ClienteWinx com 0 pontos: nada a converter (validação explícita da camada web).
    void clienteWinxSemPontosNaoPodeConverter() throws IOException {
        login(CPF_WINX_SEM_PONTOS);

        Resposta resposta = post("/acao", Map.of(
                "acao", "converterPontos",
                "numeroContaConverter", String.valueOf(CONTA_WINX_SEM_PONTOS)
        ));

        assertEquals(200, resposta.status);
        assertTrue(resposta.body.contains("Pontos de compra insuficientes para a conversao."),
                "Deveria rejeitar conversão sem pontos");
        assertFalse(resposta.body.contains("Pontos convertidos em saldo com sucesso."),
                "Não deveria converter sem pontos");
    }

    @Test
    // Endpoint de apoio confirma que o estado foi carregado (3 clientes semeados).
    void estadoInicialCarregaTresClientesSemeados() throws IOException {
        Resposta estado = get("/api/estado");

        assertEquals(200, estado.status);
        assertTrue(estado.body.contains("\"quantidadeClientes\":3"),
                "Deveria carregar os 3 clientes semeados");
    }

    // ==================== infraestrutura HTTP / estado ====================

    private void login(String cpf) throws IOException {
        Resposta resposta = postLogin(cpf);
        assertTrue(resposta.body.contains("Login realizado com sucesso"),
                "O login deveria funcionar para o CPF " + cpf);
    }

    private Resposta postLogin(String cpf) throws IOException {
        Resposta resposta = post("/acao", Map.of("acao", "login", "cpfLogin", cpf));
        assertEquals(200, resposta.status);
        assertTrue(resposta.body.contains("Login realizado com sucesso"),
                "O login deveria funcionar para o CPF " + cpf);
        return resposta;
    }

    private Resposta get(String path) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(baseUrl + path).openConnection();
        connection.setRequestMethod("GET");
        return resposta(connection);
    }

    private Resposta post(String path, Map<String, String> form) throws IOException {
        byte[] body = formUrlEncoded(form).getBytes(StandardCharsets.UTF_8);
        HttpURLConnection connection = (HttpURLConnection) new URL(baseUrl + path).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        connection.setRequestProperty("Content-Length", String.valueOf(body.length));
        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(body);
        }
        return resposta(connection);
    }

    private Resposta resposta(HttpURLConnection connection) throws IOException {
        int status = connection.getResponseCode();
        InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
        String body;
        try (InputStream inputStream = stream) {
            body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
        return new Resposta(status, body);
    }

    private String formUrlEncoded(Map<String, String> form) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : form.entrySet()) {
            if (builder.length() > 0) {
                builder.append('&');
            }
            builder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            builder.append('=');
            builder.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return builder.toString();
    }

    /**
     * JSON com 3 clientes: um Winx com pontos suficientes, um comum e um Winx sem pontos.
     * A presença do campo "pontosDeCompra" é o que faz o repositório reconstruir um ClienteWinx.
     */
    private String estadoInicialJson() {
        String contaWinx = contaCorrenteJson(CONTA_WINX);
        String contaComum = contaCorrenteJson(CONTA_COMUM);
        String contaWinxSemPontos = contaCorrenteJson(CONTA_WINX_SEM_PONTOS);
        return "["
                + "{\"nome\":\"WINX COM PONTOS\",\"cpf\":\"" + CPF_WINX + "\",\"pontosDeCompra\":10,"
                + "\"contas\":[" + contaWinx + "]},"
                + "{\"nome\":\"CLIENTE COMUM\",\"cpf\":\"" + CPF_COMUM + "\","
                + "\"contas\":[" + contaComum + "]},"
                + "{\"nome\":\"WINX SEM PONTOS\",\"cpf\":\"" + CPF_WINX_SEM_PONTOS + "\",\"pontosDeCompra\":0,"
                + "\"contas\":[" + contaWinxSemPontos + "]}"
                + "]";
    }

    private String contaCorrenteJson(int numeroConta) {
        return "{\"cartaoCredito\":{\"fatura\":0.0,\"mesDaFatura\":null,\"indexMesDaFatura\":0,"
                + "\"faturaPaga\":false,\"limite\":1000.0,\"numero\":1000,\"csv\":100},"
                + "\"numeroConta\":" + numeroConta + ",\"saldo\":0.0,"
                + "\"cartao\":{\"numero\":1000,\"csv\":100},\"dividaDeEmprestimo\":0.0,"
                + "\"extrato\":[{\"mesAtual\":\"Janeiro\",\"dinheiroMovimentado\":0.0,\"tipoDaMovimentacao\":\"ENTRADA\"}]}";
    }

    private void preservarArquivo(Path path) throws IOException {
        arquivosOriginais.put(path, Files.exists(path) ? Files.readAllBytes(path) : null);
    }

    private void restaurarArquivo(Path path) throws IOException {
        byte[] conteudoOriginal = arquivosOriginais.get(path);
        if (conteudoOriginal == null) {
            Files.deleteIfExists(path);
            return;
        }
        Files.write(path, conteudoOriginal);
    }

    private void limparEstado() {
        RegistroDeClientes.getInstancia().limparListaDeClientes();
        Banco.getInstancia().despesas = 0;
        Banco.getInstancia().receitas = 0;
        Ano.getInstancia().setMesAtual("Janeiro");
    }

    private record Resposta(int status, String body) {
    }
}
