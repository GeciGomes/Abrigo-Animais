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
- No menu inicial, escolha fazer login ou cadastrar uma conta de voluntário, veterinário ou administrador. O primeiro administrador cadastrado se torna o administrador principal e recebe o ID fixo `ADM001`.
- Se o administrador principal já existe, entre com essa conta e escolha `6. Cadastrar usuário` para criar outro administrador. Assim, os dados de login são solicitados uma única vez para a nova conta. Todos os administradores usam as mesmas funções do abrigo; somente o principal pode criar outros administradores.
- Contas de administrador não podem ser removidas ou editadas por outro administrador. Cada administrador pode atualizar os próprios dados.
- Voluntários podem criar conta pública. Veterinários também podem se cadastrar, mas precisam da aprovação de um administrador para entrar.
- As contas são salvas localmente em `~/.abrigo-animais/contas.json`; as senhas são armazenadas como hashes. Os demais dados do abrigo continuam em memória e são perdidos ao encerrar o programa.
- A conta principal não pode ser rebaixada nem removida.
- As senhas de todos os perfis devem ter pelo menos 6 caracteres. O perfil autenticado determina quais opções do menu ficam disponíveis; use `0` para sair da conta.
