# WinxBank Web - versão UX/UI com login, cadastro e dashboard

Esta versão mantém o projeto em **Java** e preserva a estrutura de classes de domínio/POO do sistema bancário. A mudança principal foi a criação de uma camada web local para usar o sistema no navegador e permitir testes com Selenium.

## Como executar

Na pasta `winxbank`, execute:

```bash
mvn clean compile exec:java
```

Depois acesse:

```text
http://localhost:8080
```

A classe principal da versão web é:

```text
br.winxbank.web.WinxBankWebServer
```

## Nova organização UX/UI

A interface foi reorganizada como uma experiência real de banco digital para computador:

1. **Tela inicial de autenticação**
   - O usuário entra primeiro em uma tela de **Login/Cadastro**.
   - As funcionalidades bancárias não aparecem antes do login.
   - O cadastro continua criando um cliente com nome, CPF, tipo de conta e saldo inicial.
   - Após cadastrar, o sistema já autentica o cliente automaticamente e abre o dashboard.
   - O login é feito pelo CPF cadastrado. A interface normaliza pontos e traços do CPF, então `12345678900` e `123.456.789-00` são tratados como o mesmo CPF.

2. **Dashboard após login**
   - Após autenticar, o cliente acessa uma área logada com resumo financeiro.
   - O dashboard mostra saldo total, dívida de empréstimo, tipo de cliente e tabela de contas.

3. **Funcionalidades organizadas por categorias**
   - As operações ficam no menu lateral, agrupadas por jornada:
     - Contas
     - Transações
     - Cartões
     - Crédito e benefícios
     - Documentos
     - Administração

4. **Uma funcionalidade por vez**
   - Os formulários não ficam mais todos aparecendo juntos.
   - Ao clicar em uma opção do menu, apenas o formulário daquela operação aparece.

## IDs úteis para Selenium

### Autenticação

```text
#auth-page
#tab-login
#tab-cadastro
#auth-login-panel
#auth-cadastro-panel
#cpfLogin
#btn-login
#nome
#cpf
#tipoConta
#saldoInicial
#btn-cadastrar
```

### Área logada

```text
#app-page
#dashboard
#menu-operacoes
#mensagem-status
#tabela-contas
#btn-logout
```

### Navegação por funcionalidade

```text
#nav-abrirConta
#nav-fecharConta
#nav-depositar
#nav-pix
#nav-sacar
#nav-comprar
#nav-pagarFatura
#nav-ajustarLimite
#nav-requisitarEmprestimo
#nav-pagarParcela
#nav-converterPontos
#nav-gerarExtrato
#nav-gerarInforme
#nav-exibirClientes
#nav-dadosBanco
#nav-apagarUsuario
#nav-limparClientes
```

### Seções das funcionalidades

```text
#sec-abrirConta
#sec-fecharConta
#sec-depositar
#sec-pix
#sec-sacar
#sec-comprar
#sec-pagarFatura
#sec-ajustarLimite
#sec-requisitarEmprestimo
#sec-pagarParcela
#sec-converterPontos
#sec-gerarExtrato
#sec-gerarInforme
#sec-exibirClientes
#sec-dadosBanco
#sec-apagarUsuario
#sec-limparClientes
```

## Endpoint para apoio em testes

```text
http://localhost:8080/api/estado
```

Retorna JSON com mês atual, CPF do cliente logado e quantidade de clientes.

## Observação

A versão de terminal continua preservada. A camada web foi adicionada em `br.winxbank.web` para evitar substituir a estrutura original do projeto.
