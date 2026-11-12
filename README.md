# Abrigo-Animais

Sistema de gerenciamento de abrigos e resgates de animais com front-end React e API Java.

---

<<<<<<< HEAD
## Passo a Passo para executar o Projeto

1. **Navegue até a raiz do projeto** (onde o arquivo `pom.xml` se encontra):
   ```bash
   cd /caminho/para/Abrigo-Animais
   ```

2. **Execute o comando do Maven no terminal:**
   ```bash
   mvn clean compile exec:java -Dexec.mainClass=backend.api.AbrigoApi
   ```

=======
Configure as credenciais do primeiro administrador no terminal e inicie a API:

```bash
export ABRIGO_ADMIN_EMAIL="admin@exemplo.com"
export ABRIGO_ADMIN_PASSWORD="defina-uma-senha-forte-com-12-ou-mais-caracteres"
mvn compile exec:java -Dexec.mainClass=backend.api.AbrigoApi
```

Não há senha padrão. A API não inicia sem as duas variáveis. Depois de entrar, o administrador pode cadastrar usuários e definir senhas com pelo menos 12 caracteres. As senhas são armazenadas com hash; sessões são mantidas em memória e expiram quando a API reinicia.

Em outro terminal, inicie o front-end:

```bash
cd frontend
npm run dev
```

Abra o endereço mostrado pelo Vite, normalmente `http://localhost:5173`.

A API fica em `http://localhost:8080`; o servidor Vite encaminha as chamadas `/api` para ela. Os dados são mantidos em memória e são reiniciados quando a API é encerrada. Apenas veterinários autenticados podem cadastrar animais ou alterar informações de saúde e prontuários; apenas administradores autenticados podem aprovar adoções ou gerenciar usuários.
>>>>>>> 6a916b7 (fiz algumas alteracoes nas regras de negocio, garantindo que todas estejam funcionando de forma correta)
