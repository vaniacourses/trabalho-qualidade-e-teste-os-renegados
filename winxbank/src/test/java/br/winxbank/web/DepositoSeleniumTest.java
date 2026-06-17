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

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DepositoSeleniumTest {

    private WebDriver driver;
    private WebDriverWait wait;


    @BeforeEach
    void setupTest() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @AfterEach
    void teardown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    void testDepositoAumentaSaldo() throws InterruptedException {

        driver.get("http://localhost:8080");


        if (!driver.findElements(By.id("btn-logout")).isEmpty()) {
            driver.findElement(By.id("btn-logout")).click();
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("cpfLogin")));
        }

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("cpfLogin")))
                .sendKeys("11111111111");

        driver.findElement(By.id("btn-login")).click();

  
        WebElement saldoElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("metric-saldo")));
        double saldoInicial = parseSaldo(saldoElement.getText());

   
        WebElement navDepositar = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("nav-depositar")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", navDepositar);

 
        WebElement inputConta = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("numeroContaDeposito")));
        inputConta.clear();
        inputConta.sendKeys("80414");

        WebElement inputValor = driver.findElement(By.id("valorDeposito"));
        inputValor.clear();
        inputValor.sendKeys("5000");


        WebElement btnDepositar = driver.findElement(By.id("btn-depositar"));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btnDepositar);


        WebElement novoSaldoElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("metric-saldo")));
        double saldoFinal = parseSaldo(novoSaldoElement.getText());


        assertEquals(saldoInicial + 5000.0, saldoFinal, 0.01, "O saldo deveria ter aumentado em 5000");
    }

    private double parseSaldo(String textoSaldo) {
        String limpo = textoSaldo.replace("R$ ", "")
                                 .replace(".", "")
                                 .replace(",", ".");
        return Double.parseDouble(limpo);
    }
}
