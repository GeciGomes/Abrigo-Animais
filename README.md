# Abrigo-Animais

Sistema de gerenciamento de abrigos e resgates de animais com front-end React e API Java.

## Executar localmente

Em um terminal, inicie a API:

```bash
mvn compile exec:java -Dexec.mainClass=backend.api.AbrigoApi
```

Em outro terminal, inicie o front-end:

```bash
cd frontend
npm run dev
```

Abra o endereço mostrado pelo Vite, normalmente `http://localhost:5173`.

A API fica em `http://localhost:8080`; o servidor Vite encaminha as chamadas `/api` para ela. Os dados são mantidos em memória e são reiniciados quando a API é encerrada. Os perfis selecionados nas telas identificam o tipo de usuário para as regras de negócio, mas o sistema ainda não possui autenticação de contas.
