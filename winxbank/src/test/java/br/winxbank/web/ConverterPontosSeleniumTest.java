package br.winxbank.web;

import br.winxbank.sistemabancario.Banco;
import br.winxbank.sistemaclientes.RegistroDeClientes;
import br.winxbank.tempo.Ano;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Teste de SISTEMA (E2E) com NAVEGADOR REAL (Selenium + ChromeDriver) da funcionalidade
 * "Converter pontos em saldo", que exercita as classes da Entrega 1 (ClienteWinx +
 * PoliticaDeConversaoPadrao) ponta a ponta pela interface web.
 *
 * Complementa o ConverterPontosE2ETest (puro HTTP): aqui validamos a jornada real do
 * usuário no navegador — login, navegação pelo menu lateral, preenchimento do formulário
 * e leitura do dashboard.
 *
 * Pré-requisitos de ambiente: Google Chrome instalado + chromedriver compatível em
 * winxbank/chromedriver-win64/chromedriver.exe. Caso o navegador não esteja disponível,
 * o teste é IGNORADO (Assumptions) em vez de falhar o build.
 */
class ConverterPontosSeleniumTest {

    private static final String CPF_WINX = "70070070071";
    private static final int CONTA_WINX = 60001;

    private final Map<Path, byte[]> arquivosOriginais = new HashMap<>();
    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeEach
    void setup() throws IOException {
        // Semeia um ClienteWinx com 10 pontos (mínimo da PoliticaDeConversaoPadrao).
        preservarArquivo(Path.of("clientes.json"));
        preservarArquivo(Path.of("mesAtual.txt"));
        preservarArquivo(Path.of("banco.txt"));
        Files.writeString(Path.of("clientes.json"), estadoInicialJson(), StandardCharsets.UTF_8);
        Files.writeString(Path.of("mesAtual.txt"), "Janeiro", StandardCharsets.UTF_8);
        Files.deleteIfExists(Path.of("banco.txt"));
        limparEstado();

        // Sobe o servidor web (porta 8080) numa thread, como nos demais testes Selenium do projeto.
        new Thread(() -> {
            try {
                WinxBankWebServer.main(new String[]{});
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        dormir(5000);

        System.setProperty("webdriver.chrome.driver", "chromedriver-win64/chromedriver.exe");
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--window-size=1920,1080");

        try {
            driver = new ChromeDriver(options);
        } catch (Throwable t) {
            // Sem navegador Chrome/driver compatível neste ambiente: ignora o teste em vez de falhar.
            System.out.println("[ConverterPontosSeleniumTest] Driver indisponivel: " + t);
            Assumptions.assumeTrue(false,
                    "Chrome/ChromeDriver indisponível neste ambiente: " + t.getMessage());
        }
        wait = new WebDriverWait(driver, 15);
    }

    @AfterEach
    void teardown() throws IOException {
        if (driver != null) {
            driver.quit();
        }
        limparEstado();
        restaurarArquivo(Path.of("clientes.json"));
        restaurarArquivo(Path.of("mesAtual.txt"));
        restaurarArquivo(Path.of("banco.txt"));
    }

    @Test
    void clienteWinxConvertePontosPeloNavegadorEZeraPontosNoDashboard() {
        driver.get("http://localhost:8080");

        // Garante que começamos deslogados.
        if (!driver.findElements(By.id("btn-logout")).isEmpty()) {
            driver.findElement(By.id("btn-logout")).click();
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("cpfLogin")));
        }

        // Login do ClienteWinx semeado.
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("cpfLogin"))).sendKeys(CPF_WINX);
        driver.findElement(By.id("btn-login")).click();

        // Dashboard deve aparecer e mostrar os pontos antes da conversão.
        WebElement appPage = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("app-page")));
        assertTrue(appPage.getText().contains("Pontos"), "O dashboard deveria mostrar os pontos do ClienteWinx");

        // Navega para a funcionalidade de converter pontos.
        WebElement navConverter = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("nav-converterPontos")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", navConverter);

        // Preenche a conta e envia.
        WebElement inputConta = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("numeroContaConverter")));
        inputConta.clear();
        inputConta.sendKeys(String.valueOf(CONTA_WINX));

        WebElement btnConverter = driver.findElement(By.id("btn-converterPontos"));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btnConverter);

        // Após converter, a página recarrega mostrando a mensagem de sucesso e pontos zerados.
        WebElement corpo = wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("body")));
        wait.until(ExpectedConditions.textToBePresentInElement(corpo, "Pontos convertidos em saldo com sucesso."));
        assertTrue(driver.getPageSource().contains("Pontos:</strong> 0"),
                "Os pontos deveriam estar zerados após a conversão");
    }

    // ==================== Testes de valor de borda — campo numeroContaConverter ====================

    @Test
    // Valor inválido: conta que não existe no sistema.
    void contaInexistenteNoNavegadorExibeErroContaNaoEncontrada() {
        fazerLogin();
        navegarParaConverterPontos();

        WebElement inputConta = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("numeroContaConverter")));
        inputConta.clear();
        inputConta.sendKeys("99999");

        WebElement btnConverter = driver.findElement(By.id("btn-converterPontos"));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btnConverter);

        WebElement corpo = wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("body")));
        wait.until(ExpectedConditions.textToBePresentInElement(corpo, "Conta bancaria nao encontrada."));
        assertTrue(driver.getPageSource().contains("Conta bancaria nao encontrada."),
                "Conta inexistente deveria exibir erro no navegador");
    }

    @Test
    // Abaixo do limite: número negativo — valor menor que qualquer ID de conta válido.
    void numeroContaNegativoNoNavegadorExibeErroContaNaoEncontrada() {
        fazerLogin();
        navegarParaConverterPontos();

        WebElement inputConta = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("numeroContaConverter")));
        inputConta.clear();
        inputConta.sendKeys("-1");

        WebElement btnConverter = driver.findElement(By.id("btn-converterPontos"));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btnConverter);

        WebElement corpo = wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("body")));
        wait.until(ExpectedConditions.textToBePresentInElement(corpo, "Conta bancaria nao encontrada."));
        assertTrue(driver.getPageSource().contains("Conta bancaria nao encontrada."),
                "Número negativo deveria exibir erro de conta não encontrada no navegador");
    }

    @Test
    // Valor não numérico ("banana"): entrada inválida — verifica que o sistema rejeita sem travar.
    void textoNaoNumericoNoNavegadorExibeErroDeValidacao() {
        fazerLogin();
        navegarParaConverterPontos();

        WebElement inputConta = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("numeroContaConverter")));
        inputConta.clear();
        inputConta.sendKeys("banana");

        WebElement btnConverter = driver.findElement(By.id("btn-converterPontos"));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btnConverter);

        WebElement corpo = wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("body")));
        wait.until(ExpectedConditions.textToBePresentInElement(corpo, "numeroContaConverter"));
        assertFalse(driver.getPageSource().contains("Pontos convertidos em saldo com sucesso."),
                "Texto não numérico não deveria resultar em conversão bem-sucedida");
    }

    @Test
    // Acima do limite: número que ultrapassa Integer.MAX_VALUE — estoura parseInt no servidor.
    void numeroAcimaDoLimiteNoNavegadorExibeErroDeValidacao() {
        fazerLogin();
        navegarParaConverterPontos();

        WebElement inputConta = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("numeroContaConverter")));
        inputConta.clear();
        inputConta.sendKeys("999999999999999");

        WebElement btnConverter = driver.findElement(By.id("btn-converterPontos"));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btnConverter);

        WebElement corpo = wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("body")));
        wait.until(ExpectedConditions.textToBePresentInElement(corpo, "numeroContaConverter"));
        assertFalse(driver.getPageSource().contains("Pontos convertidos em saldo com sucesso."),
                "Número acima do limite não deveria resultar em conversão bem-sucedida");
    }

    // ==================== infraestrutura ====================

    private String estadoInicialJson() {
        String conta = "{\"cartaoCredito\":{\"fatura\":0.0,\"mesDaFatura\":null,\"indexMesDaFatura\":0,"
                + "\"faturaPaga\":false,\"limite\":1000.0,\"numero\":1000,\"csv\":100},"
                + "\"numeroConta\":" + CONTA_WINX + ",\"saldo\":0.0,"
                + "\"cartao\":{\"numero\":1000,\"csv\":100},\"dividaDeEmprestimo\":0.0,"
                + "\"extrato\":[{\"mesAtual\":\"Janeiro\",\"dinheiroMovimentado\":0.0,\"tipoDaMovimentacao\":\"ENTRADA\"}]}";
        return "[{\"nome\":\"WINX SELENIUM\",\"cpf\":\"" + CPF_WINX + "\",\"pontosDeCompra\":10,"
                + "\"contas\":[" + conta + "]}]";
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

    private void fazerLogin() {
        driver.get("http://localhost:8080");
        if (!driver.findElements(By.id("btn-logout")).isEmpty()) {
            driver.findElement(By.id("btn-logout")).click();
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("cpfLogin")));
        }
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("cpfLogin"))).sendKeys(CPF_WINX);
        driver.findElement(By.id("btn-login")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("app-page")));
    }

    private void navegarParaConverterPontos() {
        WebElement navConverter = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("nav-converterPontos")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", navConverter);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("numeroContaConverter")));
    }

    private void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
