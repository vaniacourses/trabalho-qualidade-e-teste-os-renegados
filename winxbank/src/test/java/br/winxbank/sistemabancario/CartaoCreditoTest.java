package br.winxbank.sistemabancario;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.winxbank.tempo.Ano;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;

class CartaoCreditoTest {

    private CartaoCredito cartao;

    @BeforeEach
    void prepararCenario() {
        cartao = new CartaoCredito(1234567, 123);
    }

    @Test
    void CreditarFatura() {
        cartao.creditar(150.0);
        assertEquals(150.0, cartao.getFatura(), 0.001);
    }

    @Test
    void TentativaExtourarLimite() {
        cartao.creditar(700.0);
        cartao.creditar(200.0);
        cartao.creditar(300.0);
        assertEquals(900.0, cartao.getFatura(), "A fatura deve ignorar valores que excedam o limite.");
    }
    
    @Test
    void CreditoNoLimiteDoCartao() {
        cartao.creditar(1000.0); 
        assertEquals(1000.0, cartao.getFatura(), 0.001);
    }
    
    @Test
    void CreditoAcimaLimiteDoCartao() {
        cartao.creditar(1500.0); 
        assertEquals(0.0, cartao.getFatura(), 0.001);
    }
    
    @Test
    void CreditoAposTentativaAcimaLimite() {
        cartao.creditar(2000.0);
        
        cartao.creditar(800.0);
        
        cartao.setFatura(-200.0);
        
        assertEquals(600.0, cartao.getFatura(), "O cartão deve processar a transação válida mesmo após a falha anterior.");
    }
    
    @Test
    void AtingirLimiteComSomaDeCreditos() {
        cartao.creditar(300.0);
        cartao.creditar(250.0);
        cartao.creditar(350.0);
        cartao.creditar(100.0);
        cartao.creditar(10.0);
        
        assertEquals(1000.0, cartao.getFatura(), 0.001, "A soma de várias compras deve respeitar o limite de 1000.0.");
    }
    
    @Test
    void LimiteNegativoNoSetFatura() {
        cartao.creditar(200.0);
        cartao.setFatura(-250.0);
        
        assertTrue(cartao.getFatura() <= 0);
    }
    
    
    @Test
    void PagamentoParcialFatura() {
        cartao.creditar(1000.0);
        cartao.setFatura(-500.0);
        
        assertEquals(500.0, cartao.getFatura());
    }

    @Test
    void PagamentoTotalFatura() {
        cartao.creditar(1000.0);
        cartao.setFatura(-1000.0);
        
        assertEquals(0.0, cartao.getFatura());
    }
    
    @Test
    void CobrarJurosMudancaDeMes() {
        try (MockedStatic<Ano> mockedAno = mockStatic(Ano.class);
             MockedStatic<Banco> mockedBanco = mockStatic(Banco.class)) {
            
            Ano mockAno = mock(Ano.class);
            mockedAno.when(Ano::getInstancia).thenReturn(mockAno);
            
            Banco mockBanco = mock(Banco.class);
            mockedBanco.when(Banco::getInstancia).thenReturn(mockBanco);
            
            when(mockAno.getIndexMesAtual()).thenReturn(4);
            cartao.creditar(600.0);
            
            when(mockAno.getIndexMesAtual()).thenReturn(5); 
            
            cartao.cobrarJurus();
            
            assertTrue(cartao.getFatura() > 600.0, "A fatura deveria ter aumentado por causa dos juros.");
            
        }
    }
    
    @Test
    void NaoCobrarJurosSeMesIgual() {
        try (MockedStatic<Ano> mockedAno = mockStatic(Ano.class)) {
            
            Ano mockAno = mock(Ano.class);
            mockedAno.when(Ano::getInstancia).thenReturn(mockAno);      
            
            when(mockAno.getIndexMesAtual()).thenReturn(4);
            cartao.creditar(600.0);
            
            cartao.cobrarJurus();
            
            assertEquals(600.0, cartao.getFatura(), "A fatura não deve mudar se o mês for o mesmo.");
            
        }
    }
    
    @Test
    void NaoCobrarJurosSeFaturaPaga() {
        try (MockedStatic<Ano> mockedAno = mockStatic(Ano.class);
             MockedStatic<Banco> mockedBanco = mockStatic(Banco.class)) {
            
            Ano mockAno = mock(Ano.class);
            mockedAno.when(Ano::getInstancia).thenReturn(mockAno);
            
            Banco mockBanco = mock(Banco.class);
            mockedBanco.when(Banco::getInstancia).thenReturn(mockBanco);
            
            when(mockAno.getIndexMesAtual()).thenReturn(4);
            
            cartao.creditar(500.0);
            cartao.setFatura(-500.0);
            
            cartao.cobrarJurus();
            
            assertEquals(0.0, cartao.getFatura());
            mockedBanco.verify(() -> Banco.getInstancia(), never());
            
        }
    }
    
    @Test
    void NaoCobrarJurosSeSaldoNegativo() {
        try (MockedStatic<Ano> mockedAno = mockStatic(Ano.class)) {
            
        	Ano mockAno = mock(Ano.class);
            mockedAno.when(Ano::getInstancia).thenReturn(mockAno);
            
            when(mockAno.getIndexMesAtual()).thenReturn(4);
            cartao.creditar(400.0);
            cartao.setFatura(-500.0);
            
            when(mockAno.getIndexMesAtual()).thenReturn(5);
            cartao.cobrarJurus();
            
            assertEquals(-100.0, cartao.getFatura(), "Não deve aplicar juros sobre saldo negativo(crédito).");
        }
    }
    
    @Test
    void GetNumeroCSV() {
        assertEquals(1234567, cartao.getNumero());
        assertEquals(123, cartao.getCsv());
    }
    
    @Test
    void CreditarZeroNaoAlteraFatura() {
        cartao.creditar(200.0);
        cartao.setFatura(0.0);  	
    	
    	assertEquals(200.0, cartao.getFatura());
    }
    
    @Test
    void JurosAcumuladosMesAMes() {
        try (MockedStatic<Ano> mockedAno = mockStatic(Ano.class)) {
            
        	Ano mockAno = mock(Ano.class);
            mockedAno.when(Ano::getInstancia).thenReturn(mockAno);
            
            when(mockAno.getIndexMesAtual()).thenReturn(4);
            cartao.creditar(200.0);
            
            when(mockAno.getIndexMesAtual()).thenReturn(5);
            cartao.cobrarJurus();
            double faturaMes5 = cartao.getFatura();
            
            when(mockAno.getIndexMesAtual()).thenReturn(6);
            cartao.cobrarJurus();
            
            assertTrue(cartao.getFatura() > faturaMes5, "Fatura deveria ser maior no mês 6.");
        }
    }
    
    @Test
    void VerificarInteracaoComBancoAoCobrarJuros() {
        try (MockedStatic<Ano> mockedAno = mockStatic(Ano.class);
             MockedStatic<Banco> mockedBanco = mockStatic(Banco.class)) {
            
            Ano mockAno = mock(Ano.class);
            mockedAno.when(Ano::getInstancia).thenReturn(mockAno);
            Banco mockBanco = mock(Banco.class);
            mockedBanco.when(Banco::getInstancia).thenReturn(mockBanco);
            
            when(mockAno.getIndexMesAtual()).thenReturn(4, 5);
            
            cartao.creditar(500.0);
            cartao.cobrarJurus();
            
            verify(mockBanco, times(1)).setReceitas(anyDouble());
        }
    }
    
}