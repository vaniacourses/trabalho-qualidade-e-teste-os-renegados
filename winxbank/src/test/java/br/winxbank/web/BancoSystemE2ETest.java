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

class BancoSystemE2ETest {

    private final Map<Path, byte[]> arquivosOriginais = new HashMap<>();
    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void iniciarServidor() throws IOException {
        preservarArquivo(Path.of("clientes.json"));
        preservarArquivo(Path.of("mesAtual.txt"));
        preservarArquivo(Path.of("banco.txt"));
        Files.writeString(Path.of("clientes.json"), "[]", StandardCharsets.UTF_8);
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
    void cadastroWebCriaClienteContaDashboardEProtegeHtmlContraScriptNoNome() throws IOException {
        Resposta resposta = post("/acao", Map.of(
                "acao", "cadastrar",
                "nome", "<script>alert(1)</script>",
                "cpf", "123.456.789-00",
                "tipoConta", "1",
                "saldoInicial", "2500"
        ));

        assertEquals(200, resposta.status);
        assertTrue(resposta.body.contains("Cadastro realizado com sucesso"));
        assertTrue(resposta.body.contains("id=\"app-page\""));
        assertTrue(resposta.body.contains("id=\"tabela-contas\""));
        assertTrue(resposta.body.contains("R$ 2500,00"));
        assertTrue(resposta.body.contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
        assertFalse(resposta.body.contains("<script>alert(1)</script>"));

        Resposta estado = get("/api/estado");

        assertEquals(200, estado.status);
        assertTrue(estado.body.contains("\"clienteLogado\":\"12345678900\""));
        assertTrue(estado.body.contains("\"quantidadeClientes\":1"));
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
