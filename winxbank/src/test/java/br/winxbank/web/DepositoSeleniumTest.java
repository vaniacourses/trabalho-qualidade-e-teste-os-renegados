package br.winxbank.web;

import org.junit.jupiter.api.AfterEach;
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

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DepositoSeleniumTest {

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeEach
    void setupTest() {
        System.setProperty("webdriver.chrome.driver", "chromedriver-win64/chromedriver.exe");
        ChromeOptions options = new ChromeOptions();
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

    private String fazerLoginOuCadastrar() {
        driver.get("http://localhost:8080");

        if (!driver.findElements(By.id("btn-logout")).isEmpty()) {
            driver.findElement(By.id("btn-logout")).click();
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("cpfLogin")));
        }

        driver.findElement(By.id("cpfLogin")).sendKeys("11111111111");
        driver.findElement(By.id("btn-login")).click();

        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("metric-saldo")));
        } catch (Exception e) {
            driver.findElement(By.id("tab-cadastro")).click();
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("nome"))).sendKeys("CLIENTE DEPOSITO");
            driver.findElement(By.id("cpf")).sendKeys("11111111111");
            WebElement saldoInit = driver.findElement(By.id("saldoInicial"));
            saldoInit.clear();
            saldoInit.sendKeys("1000");
            driver.findElement(By.id("btn-cadastrar")).click();
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("metric-saldo")));
        }

        WebElement tabela = driver.findElement(By.id("tabela-contas"));
        return tabela.findElement(By.xpath(".//tbody/tr[1]/td[2]")).getText();
    }

    @Test
    void testDepositoAumentaSaldo() throws InterruptedException {
        String contaGerada = fazerLoginOuCadastrar();

        WebElement saldoElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("metric-saldo")));
        double saldoInicial = parseSaldo(saldoElement.getText());

        WebElement navDepositar = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("nav-depositar")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", navDepositar);

        WebElement inputConta = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("numeroContaDeposito")));
        inputConta.clear();
        inputConta.sendKeys(contaGerada);

        WebElement inputValor = driver.findElement(By.id("valorDeposito"));
        inputValor.clear();
        inputValor.sendKeys("5000");

        WebElement btnDepositar = driver.findElement(By.id("btn-depositar"));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btnDepositar);

        WebElement novoSaldoElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("metric-saldo")));
        double saldoFinal = parseSaldo(novoSaldoElement.getText());

        assertEquals(saldoInicial + 5000.0, saldoFinal, 0.01, "O saldo deveria ter aumentado em 5000");
    }

    @Test
    void testDepositoValorZeroMantemSaldo() throws InterruptedException {
        String contaGerada = fazerLoginOuCadastrar();

        WebElement saldoElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("metric-saldo")));
        double saldoInicial = parseSaldo(saldoElement.getText());

        WebElement navDepositar = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("nav-depositar")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", navDepositar);

        WebElement inputConta = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("numeroContaDeposito")));
        inputConta.clear();
        inputConta.sendKeys(contaGerada);

        WebElement inputValor = driver.findElement(By.id("valorDeposito"));
        inputValor.clear();
        inputValor.sendKeys("0");

        WebElement btnDepositar = driver.findElement(By.id("btn-depositar"));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btnDepositar);

        WebElement novoSaldoElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("metric-saldo")));
        double saldoFinal = parseSaldo(novoSaldoElement.getText());

        assertEquals(saldoInicial, saldoFinal, 0.01, "O saldo deve se manter inalterado ao depositar o valor limite de 0");
    }

    @Test
    void testDepositoValorNegativoReduzSaldo() throws InterruptedException {
        String contaGerada = fazerLoginOuCadastrar();

        WebElement saldoElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("metric-saldo")));
        double saldoInicial = parseSaldo(saldoElement.getText());

        WebElement navDepositar = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("nav-depositar")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", navDepositar);

        WebElement inputConta = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("numeroContaDeposito")));
        inputConta.clear();
        inputConta.sendKeys(contaGerada);

        WebElement inputValor = driver.findElement(By.id("valorDeposito"));
        inputValor.clear();
        inputValor.sendKeys("-100");

        WebElement btnDepositar = driver.findElement(By.id("btn-depositar"));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btnDepositar);

        WebElement novoSaldoElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("metric-saldo")));
        double saldoFinal = parseSaldo(novoSaldoElement.getText());

        assertEquals(saldoInicial - 100.0, saldoFinal, 0.01, "O saldo deveria reduzir ao depositar um valor limite negativo");
    }

    private double parseSaldo(String textoSaldo) {
        String limpo = textoSaldo.replace("R$ ", "")
                .replace(".", "")
                .replace(",", ".");
        try {
            return Double.parseDouble(limpo);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Formato de saldo inválido: " + textoSaldo, e);
        }
    }
}
