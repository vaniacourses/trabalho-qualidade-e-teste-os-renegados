package br.winxbank.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
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

import static org.junit.jupiter.api.Assertions.*;

public class CadastroSeleniumTestFuncional {

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeAll
    static void iniciarServidor() {

        new Thread(() -> {
            try {
                WinxBankWebServer.main(new String[]{});
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @BeforeEach
    void setupTest() {

        System.setProperty(
                "webdriver.chrome.driver",
                "chromedriver-win64/chromedriver.exe"
        );
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, 10);
    }

    @AfterEach
    void teardown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    void testCadastroELogin() {
        driver.get("http://localhost:8080");

        WebElement authPage = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("auth-page")));
        assertNotNull(authPage, "A página de autenticação deveria estar visível");

        WebElement tabCadastro = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("tab-cadastro")));
        assertNotNull(tabCadastro, "A aba de cadastro deveria existir");
        assertTrue(tabCadastro.isDisplayed(), "A aba de cadastro deveria estar visível");

        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", tabCadastro);

        WebElement painelCadastro = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("auth-cadastro-panel")));
        assertNotNull(painelCadastro, "O painel de cadastro deveria estar visível após clicar na aba");

        // Verificar se o campo nome existe e está habilitado
        WebElement inputNome = driver.findElement(By.id("nome"));
        assertNotNull(inputNome, "O campo nome deveria existir");
        assertTrue(inputNome.isDisplayed(), "O campo nome deveria estar visível");
        assertTrue(inputNome.isEnabled(), "O campo nome deveria estar habilitado para inserção de texto");
        inputNome.clear();
        inputNome.sendKeys("MuriloTest");
        assertEquals("MuriloTest", inputNome.getAttribute("value"), "O valor do campo nome deveria ser MuriloTest");

        WebElement inputCpf = driver.findElement(By.id("cpf"));
        assertNotNull(inputCpf, "O campo CPF deveria existir");
        assertTrue(inputCpf.isDisplayed(), "O campo CPF deveria estar visível");
        assertTrue(inputCpf.isEnabled(), "O campo CPF deveria estar habilitado para inserção de texto");
        inputCpf.clear();
        inputCpf.sendKeys("00099988877");
        assertEquals("00099988877", inputCpf.getAttribute("value"), "O valor do campo CPF deveria ser 00099988877");

        WebElement selectTipoConta = driver.findElement(By.id("tipoConta"));
        assertNotNull(selectTipoConta, "O campo tipo de conta deveria existir");
        assertTrue(selectTipoConta.isDisplayed(), "O campo tipo de conta deveria estar visível");
        assertTrue(selectTipoConta.isEnabled(), "O campo tipo de conta deveria estar habilitado");
        selectTipoConta.sendKeys("1"); // Selecionar conta corrente

        WebElement inputSaldoInicial = driver.findElement(By.id("saldoInicial"));
        assertNotNull(inputSaldoInicial, "O campo saldo inicial deveria existir");
        assertTrue(inputSaldoInicial.isDisplayed(), "O campo saldo inicial deveria estar visível");
        assertTrue(inputSaldoInicial.isEnabled(), "O campo saldo inicial deveria estar habilitado para inserção de texto");
        inputSaldoInicial.clear();
        inputSaldoInicial.sendKeys("1000000");
        assertEquals("1000000", inputSaldoInicial.getAttribute("value"), "O valor do campo saldo inicial deveria ser 1000000");

        WebElement btnCadastrar = driver.findElement(By.id("btn-cadastrar"));
        assertNotNull(btnCadastrar, "O botão de cadastrar deveria existir");
        assertTrue(btnCadastrar.isDisplayed(), "O botão de cadastrar deveria estar visível");
        assertTrue(btnCadastrar.isEnabled(), "O botão de cadastrar deveria estar habilitado");

        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btnCadastrar);

        WebElement btnLogout = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("btn-logout")));
        assertNotNull(btnLogout, "O botão de logout deveria aparecer após cadastro bem-sucedido");

        WebElement appPage = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("app-page")));
        assertNotNull(appPage, "Deveria estar na página do dashboard após cadastro");

        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btnLogout);

        WebElement authPageAgain = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("auth-page")));
        assertNotNull(authPageAgain, "Deveria voltar para a página de autenticação após logout");

        WebElement tabLogin = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("tab-login")));
        assertNotNull(tabLogin, "A aba de login deveria existir");
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", tabLogin);

        WebElement painelLogin = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("auth-login-panel")));
        assertNotNull(painelLogin, "O painel de login deveria estar visível");

        WebElement inputCpfLogin = driver.findElement(By.id("cpfLogin"));
        assertNotNull(inputCpfLogin, "O campo CPF de login deveria existir");
        assertTrue(inputCpfLogin.isDisplayed(), "O campo CPF de login deveria estar visível");
        assertTrue(inputCpfLogin.isEnabled(), "O campo CPF de login deveria estar habilitado");
        inputCpfLogin.clear();
        inputCpfLogin.sendKeys("00099988877");
        assertEquals("00099988877", inputCpfLogin.getAttribute("value"), "O valor do campo CPF de login deveria ser 00099988877");

        WebElement btnLogin = driver.findElement(By.id("btn-login"));
        assertNotNull(btnLogin, "O botão de login deveria existir");
        assertTrue(btnLogin.isDisplayed(), "O botão de login deveria estar visível");
        assertTrue(btnLogin.isEnabled(), "O botão de login deveria estar habilitado");

        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btnLogin);

        WebElement btnLogoutAfterLogin = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("btn-logout")));
        assertNotNull(btnLogoutAfterLogin, "O botão de logout deveria aparecer após login bem-sucedido");

        WebElement appPageAfterLogin = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("app-page")));
        assertNotNull(appPageAfterLogin, "Deveria estar na página do dashboard após login");

        WebElement headerCliente = driver.findElement(By.id("header-cliente"));
        assertNotNull(headerCliente, "O nome do cliente deveria aparecer no header");
        assertTrue(headerCliente.getText().contains("MuriloTest"), "O nome MuriloTest deveria aparecer no header");
    }
}
