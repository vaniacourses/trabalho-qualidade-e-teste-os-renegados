package br.winxbank.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Servidor HTTP local sem troca de linguagem e sem framework obrigatório.
 * Execute e acesse http://localhost:8080 no navegador.
 */
public class WinxBankWebServer {

    private static final WinxBankWebService SERVICE = new WinxBankWebService();

    public static void main(String[] args) throws IOException {
        int port = porta();
        SERVICE.inicializar();
        HttpServer server = criarServidor(port, SERVICE);
        server.start();
        System.out.println("WinxBank Web iniciado em http://localhost:" + port);
    }

    public static HttpServer criarServidor(int port, WinxBankWebService service) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", exchange -> index(exchange, service));
        server.createContext("/acao", exchange -> acao(exchange, service));
        server.createContext("/api/estado", exchange -> apiEstado(exchange, service));
        server.setExecutor(null);
        return server;
    }

    private static void index(HttpExchange exchange, WinxBankWebService service) throws IOException {
        if(!"GET".equalsIgnoreCase(exchange.getRequestMethod())){
            responder(exchange, 405, "text/plain; charset=UTF-8", "Método não permitido");
            return;
        }
        responder(exchange, 200, "text/html; charset=UTF-8", service.renderizarPagina());
    }

    private static void acao(HttpExchange exchange, WinxBankWebService service) throws IOException {
        if(!"POST".equalsIgnoreCase(exchange.getRequestMethod())){
            responder(exchange, 405, "text/plain; charset=UTF-8", "Método não permitido");
            return;
        }
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> form = parseForm(body);
        service.executar(form.getOrDefault("acao", ""), form);
        responder(exchange, 200, "text/html; charset=UTF-8", service.renderizarPagina());
    }

    private static void apiEstado(HttpExchange exchange, WinxBankWebService service) throws IOException {
        if(!"GET".equalsIgnoreCase(exchange.getRequestMethod())){
            responder(exchange, 405, "application/json; charset=UTF-8", "{\"erro\":\"Método não permitido\"}");
            return;
        }
        responder(exchange, 200, "application/json; charset=UTF-8", service.estadoJson());
    }

    private static void responder(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try(OutputStream os = exchange.getResponseBody()){
            os.write(bytes);
        }
    }

    private static Map<String, String> parseForm(String body) {
        Map<String, String> form = new LinkedHashMap<>();
        if(body == null || body.isBlank()){
            return form;
        }
        for(String pair : body.split("&")){
            String[] parts = pair.split("=", 2);
            String key = decode(parts[0]);
            String value = parts.length > 1 ? decode(parts[1]) : "";
            form.put(key, value);
        }
        return form;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static int porta() {
        String port = System.getProperty("server.port");
        if(port == null || port.isBlank()){
            port = System.getenv("PORT");
        }
        if(port == null || port.isBlank()){
            return 8080;
        }
        try {
            return Integer.parseInt(port.trim());
        } catch (NumberFormatException e) {
            return 8080;
        }
    }
}
