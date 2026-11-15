# Abrigo-Animais

Sistema de gerenciamento de abrigos e resgates de animais, em Java (console).
Trabalho final de Programação Orientada a Objetos.

## Como executar
1. Clone o repositório e entre na pasta do projeto, onde está o `pom.xml`:

   ```bash
   git clone https://github.com/GeciGomes/Abrigo-Animais.git
   cd Abrigo-Animais
   ```

2. Compile e execute o programa de console:

   ```bash
   mvn clean compile exec:java -Dexec.mainClass=backend.Main
   ```

## Primeiro acesso e perfis
- Na primeira execução, configure a conta do administrador inicial. Depois, o programa volta ao menu inicial para você escolher entre entrar e criar uma conta de voluntário ou veterinário.
- Depois, o administrador pode cadastrar usuários. Também é possível criar uma conta pública de voluntário ou veterinário; o cadastro de veterinário precisa ser aprovado pelo administrador.
- A senha do administrador deve ter pelo menos 6 caracteres; as senhas de voluntários e veterinários devem ter pelo menos 12. O perfil autenticado determina quais opções do menu ficam disponíveis; use `0` para sair da conta.
- Os dados e as contas ficam em memória e são perdidos ao encerrar o programa. Ao iniciar novamente, será necessário criar o administrador inicial outra vez.
