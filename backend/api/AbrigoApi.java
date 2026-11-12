package backend.api;

import backend.exception.AcessoNegadoException;
import backend.exception.AnimalAdotadoException;
import backend.exception.AnimalIndisponivelException;
import backend.exception.AutenticacaoException;
import backend.exception.VacinaDuplicadaException;
import backend.model.*;
import backend.service.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public class AbrigoApi {
    private final ObjectMapper json = new ObjectMapper();
    private final AnimalService animalService = new AnimalService();
    private final UsuarioService usuarioService = new UsuarioService();
    private final AutenticacaoService autenticacaoService = new AutenticacaoService(usuarioService);
    private final AdocaoService adocaoService = new AdocaoService();
    private final SaudeService saudeService = new SaudeService();
    private final AtividadeService atividadeService = new AtividadeService();

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        AbrigoApi api = new AbrigoApi();
        server.createContext("/api", api::handle);
        server.start();
        System.out.println("API do Abrigo de Animais disponível em http://localhost:" + port + "/api");
    }

    private AbrigoApi() {
        String emailAdmin = System.getenv("ABRIGO_ADMIN_EMAIL");
        String senhaAdmin = System.getenv("ABRIGO_ADMIN_PASSWORD");
        if (emailAdmin == null || emailAdmin.isBlank() || senhaAdmin == null || senhaAdmin.isBlank()) {
            throw new IllegalStateException("Configure ABRIGO_ADMIN_EMAIL e ABRIGO_ADMIN_PASSWORD para iniciar a API.");
        }
        AutenticacaoService.validarSenha(senhaAdmin);
        Usuario administradorInicial = usuarioService.cadastrar("Administrador",
                System.getenv().getOrDefault("ABRIGO_ADMIN_NAME", "Administrador inicial"),
                emailAdmin, "", "", "Total");
        autenticacaoService.registrarSenha(administradorInicial, senhaAdmin);
        atividadeService.cadastrarAtividade(new Atividade(
                "AT001", "Mutirão de limpeza", "Ajude a organizar e higienizar os recintos.", LocalDate.now().plusDays(5)));
        atividadeService.cadastrarAtividade(new Atividade(
                "AT002", "Campanha de adoção", "Apoie a equipe em um evento de adoção responsável.", LocalDate.now().plusDays(12)));
    }

    private void handle(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        try {
            route(exchange);
        } catch (AutenticacaoException exception) {
            sendError(exchange, 401, exception.getMessage());
        } catch (AcessoNegadoException exception) {
            sendError(exchange, 403, exception.getMessage());
        } catch (AnimalAdotadoException | AnimalIndisponivelException | VacinaDuplicadaException
                | IllegalStateException exception) {
            sendError(exchange, 409, exception.getMessage());
        } catch (NoSuchElementException exception) {
            sendError(exchange, 404, exception.getMessage());
        } catch (IllegalArgumentException | java.time.DateTimeException exception) {
            sendError(exchange, 400, exception.getMessage());
        } catch (Exception exception) {
            exception.printStackTrace(System.err);
            sendError(exchange, 500, "Erro interno ao processar a solicitação.");
        } finally {
            exchange.close();
        }
    }

    private void route(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String[] parts = exchange.getRequestURI().getPath().substring("/api".length()).split("/");
        if (parts.length >= 2 && "auth".equals(parts[1])) {
            routeAuth(exchange, parts, method);
            return;
        }
        if ("/api/state".equals(exchange.getRequestURI().getPath()) && "GET".equals(method)) {
            sendJson(exchange, 200, state());
            return;
        }

        if (parts.length >= 2 && "animals".equals(parts[1])) {
            routeAnimal(exchange, parts, method);
        } else if (parts.length >= 2 && "users".equals(parts[1])) {
            routeUser(exchange, parts, method);
        } else if (parts.length >= 2 && "requests".equals(parts[1])) {
            routeRequest(exchange, parts, method);
        } else if (parts.length >= 2 && "medical-records".equals(parts[1])) {
            routeMedicalRecord(exchange, parts, method);
        } else if (parts.length >= 2 && "activities".equals(parts[1])) {
            routeActivity(exchange, parts, method);
        } else {
            sendError(exchange, 404, "Rota não encontrada.");
        }
    }

    private void routeAuth(HttpExchange exchange, String[] parts, String method) throws IOException {
        if (parts.length == 3 && "login".equals(parts[2]) && "POST".equals(method)) {
            JsonNode body = readBody(exchange);
            AutenticacaoService.Sessao sessao = autenticacaoService.entrar(text(body, "email"), text(body, "senha"));
            sendJson(exchange, 200, Map.of("token", sessao.token(), "usuario", userDto(sessao.usuario())));
        } else if (parts.length == 3 && "logout".equals(parts[2]) && "POST".equals(method)) {
            String token = token(exchange);
            autenticado(exchange);
            autenticacaoService.sair(token);
            exchange.sendResponseHeaders(204, -1);
        } else {
            sendError(exchange, 405, "Método não permitido para autenticação.");
        }
    }

    private void routeAnimal(HttpExchange exchange, String[] parts, String method) throws IOException {
        if (parts.length == 4 && "availability".equals(parts[3]) && "POST".equals(method)) {
            Animal animal = animalService.buscarPorId(parts[2]);
            animalService.disponibilizarParaAdocao(animal.getId());
            sendJson(exchange, 200, animalDto(animal));
        } else if (parts.length == 2 && "POST".equals(method)) {
            requireVeterinario(exchange);
            JsonNode body = readBody(exchange);
            Animal animal = animalService.cadastrarAnimal(
                    text(body, "nome"), text(body, "especie"), text(body, "raca"), integer(body, "idade"),
                    text(body, "porte"), text(body, "estadoSaude"), optionalDate(body, "dataResgate"), optionalText(body, "imagem"));
            sendJson(exchange, 201, animalDto(animal));
        } else if (parts.length == 3 && "PUT".equals(method)) {
            JsonNode body = readBody(exchange);
            Animal existente = animalService.buscarPorId(parts[2]);
            String estadoSaude = text(body, "estadoSaude");
            if (!normalizar(existente.getEstadoSaude()).equals(normalizar(estadoSaude))) {
                requireVeterinario(exchange);
            }
            Animal animal = animalService.atualizarAnimal(parts[2], text(body, "nome"), text(body, "especie"),
                    text(body, "raca"), integer(body, "idade"), text(body, "porte"), estadoSaude,
                    optionalDate(body, "dataResgate"), optionalText(body, "imagem"));
            sendJson(exchange, 200, animalDto(animal));
        } else if (parts.length == 3 && "DELETE".equals(method)) {
            animalService.removerAnimal(parts[2]);
            exchange.sendResponseHeaders(204, -1);
        } else {
            sendError(exchange, 405, "Método não permitido para animais.");
        }
    }

    private void routeUser(HttpExchange exchange, String[] parts, String method) throws IOException {
        if (parts.length == 2 && "POST".equals(method)) {
            requireAdministrador(exchange);
            JsonNode body = readBody(exchange);
            Usuario usuario = saveUser(body, null);
            sendJson(exchange, 201, userDto(usuario));
        } else if (parts.length == 3 && "PUT".equals(method)) {
            requireAdministrador(exchange);
            JsonNode body = readBody(exchange);
            String senha = optionalText(body, "senha");
            Usuario usuario = saveUser(body, parts[2]);
            if (!senha.isBlank()) {
                autenticacaoService.registrarSenha(usuario, senha);
                autenticacaoService.revogarSessoes(usuario.getId(), token(exchange));
            }
            sendJson(exchange, 200, userDto(usuario));
        } else if (parts.length == 3 && "DELETE".equals(method)) {
            requireAdministrador(exchange);
            Usuario usuario = usuarioService.buscarPorId(parts[2]);
            usuarioService.remover(parts[2]);
            autenticacaoService.revogarSessoes(usuario.getId(), null);
            sendJson(exchange, 200, state());
        } else {
            sendError(exchange, 405, "Método não permitido para usuários.");
        }
    }

    private Usuario saveUser(JsonNode body, String id) {
        String tipo = text(body, "tipo");
        String nome = text(body, "nome");
        String email = text(body, "email");
        String telefone = text(body, "telefone");
        String crmv = optionalText(body, "crmv");
        String nivelAcesso = optionalText(body, "nivelAcesso");
        String senha = optionalText(body, "senha");
        if (id == null || !senha.isBlank()) {
            AutenticacaoService.validarSenha(senha);
        }
        if ("Veterinário".equals(tipo) && crmv.isBlank()) {
            throw new IllegalArgumentException("Informe o CRMV do veterinário.");
        }
        if ("Administrador".equals(tipo) && nivelAcesso.isBlank()) {
            throw new IllegalArgumentException("Informe o nível de acesso do administrador.");
        }
        Usuario usuario = id == null
                ? usuarioService.cadastrar(tipo, nome, email, telefone, crmv, nivelAcesso)
                : usuarioService.atualizar(id, tipo, nome, email, telefone, crmv, nivelAcesso);
        if (id == null) {
            autenticacaoService.registrarSenha(usuario, senha);
        }
        return usuario;
    }

    private void routeRequest(HttpExchange exchange, String[] parts, String method) throws IOException {
        if (parts.length == 2 && "POST".equals(method)) {
            JsonNode body = readBody(exchange);
            Animal animal = animalService.buscarPorId(text(body, "animalId"));
            SolicitacaoAdocao solicitacao = adocaoService.solicitarAdocao(
                    animal, text(body, "adotanteNome"), text(body, "adotanteContato"));
            sendJson(exchange, 201, requestDto(solicitacao));
        } else if (parts.length == 3 && "PATCH".equals(method)) {
            JsonNode body = readBody(exchange);
            Administrador administrador = requireAdministrador(exchange);
            adocaoService.atualizarSolicitacao(parts[2], text(body, "decisao"), administrador);
            sendJson(exchange, 200, state());
        } else {
            sendError(exchange, 405, "Método não permitido para solicitações.");
        }
    }

    private void routeMedicalRecord(HttpExchange exchange, String[] parts, String method) throws IOException {
        if (parts.length != 2 || !"POST".equals(method)) {
            sendError(exchange, 405, "Método não permitido para prontuários.");
            return;
        }
        JsonNode body = readBody(exchange);
        String animalId = text(body, "animalId");
        animalService.buscarPorId(animalId);
        Veterinario veterinario = requireVeterinario(exchange);
        RegistroMedico registro = saudeService.registrarAtendimento(animalId, veterinario,
                text(body, "descricao"), text(body, "tipo"), date(body, "data"));
        sendJson(exchange, 201, recordDto(registro));
    }

    private void routeActivity(HttpExchange exchange, String[] parts, String method) throws IOException {
        if (parts.length == 2 && "POST".equals(method)) {
            JsonNode body = readBody(exchange);
            Administrador administrador = requireAdministrador(exchange);
            Atividade atividade = atividadeService.cadastrarAtividade(
                    text(body, "titulo"), text(body, "descricao"), date(body, "data"), administrador);
            sendJson(exchange, 201, activityDto(atividade));
        } else if (parts.length == 4 && "registrations".equals(parts[3]) && "POST".equals(method)) {
            JsonNode body = readBody(exchange);
            Voluntario voluntario = usuarioService.buscarVoluntario(text(body, "voluntarioId"));
            atividadeService.inscreverVoluntario(parts[2], voluntario);
            sendJson(exchange, 200, state());
        } else {
            sendError(exchange, 405, "Método não permitido para atividades.");
        }
    }

    private Usuario autenticado(HttpExchange exchange) {
        return autenticacaoService.usuarioDaSessao(token(exchange));
    }

    private Administrador requireAdministrador(HttpExchange exchange) {
        Usuario usuario = autenticado(exchange);
        if (!(usuario instanceof Administrador administrador)) {
            throw new AcessoNegadoException("Apenas administradores podem executar esta ação.");
        }
        return administrador;
    }

    private Veterinario requireVeterinario(HttpExchange exchange) {
        Usuario usuario = autenticado(exchange);
        if (!(usuario instanceof Veterinario veterinario)) {
            throw new AcessoNegadoException("Apenas veterinários podem alterar informações médicas.");
        }
        return veterinario;
    }

    private String token(HttpExchange exchange) {
        String authorization = exchange.getRequestHeaders().getFirst("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new AutenticacaoException("Autenticação obrigatória.");
        }
        String token = authorization.substring("Bearer ".length()).trim();
        if (token.isEmpty()) {
            throw new AutenticacaoException("Autenticação obrigatória.");
        }
        return token;
    }

    private String normalizar(String valor) {
        return valor == null ? "" : java.text.Normalizer.normalize(valor, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").trim().toLowerCase(java.util.Locale.ROOT);
    }

    private Map<String, Object> state() {
        List<Map<String, Object>> animals = animalService.listarTodos().stream().map(this::animalDto).toList();
        List<Map<String, Object>> users = usuarioService.listarTodos().stream().map(this::userDto).toList();
        List<Map<String, Object>> requests = adocaoService.listarSolicitacoes().stream().map(this::requestDto).toList();
        List<Map<String, Object>> records = saudeService.listarRegistros().stream().map(this::recordDto).toList();
        List<Map<String, Object>> activities = atividadeService.listarAtividades().stream().map(this::activityDto).toList();
        List<Map<String, Object>> registrations = new ArrayList<>();
        for (Atividade atividade : atividadeService.listarAtividades()) {
            for (String volunteerId : atividade.getVoluntariosInscritosId()) {
                registrations.add(Map.of("atividadeId", atividade.getId(), "voluntarioId", volunteerId));
            }
        }
        return Map.of("animais", animals, "usuarios", users, "solicitacoes", requests,
                "registrosMedicos", records, "atividades", activities, "inscricoes", registrations);
    }

    private Map<String, Object> animalDto(Animal animal) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", animal.getId());
        dto.put("nome", animal.getNome());
        dto.put("especie", animal.getEspecie());
        dto.put("raca", animal.getRaca());
        dto.put("idade", animal.getIdade());
        dto.put("porte", animal.getPorte());
        dto.put("estadoSaude", animal.getEstadoSaude());
        dto.put("dataResgate", stringDate(animal.getDataResgate()));
        dto.put("statusAdocao", animal.getStatusAdocao());
        dto.put("imagem", animal.getImagem());
        return dto;
    }

    private Map<String, Object> userDto(Usuario usuario) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", usuario.getId());
        dto.put("tipo", usuario instanceof Voluntario ? "Voluntário"
                : usuario instanceof Veterinario ? "Veterinário" : "Administrador");
        dto.put("nome", usuario.getNome());
        dto.put("email", usuario.getEmail());
        dto.put("telefone", usuario.getTelefone());
        if (usuario instanceof Veterinario veterinario) {
            dto.put("crmv", veterinario.getCrmv());
        } else if (usuario instanceof Administrador administrador) {
            dto.put("nivelAcesso", administrador.getNivelAcesso());
        }
        return dto;
    }

    private Map<String, Object> requestDto(SolicitacaoAdocao solicitacao) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", solicitacao.getId());
        dto.put("animalId", solicitacao.getAnimal().getId());
        dto.put("adotanteNome", solicitacao.getAdotanteNome());
        dto.put("adotanteContato", solicitacao.getAdotanteContato());
        dto.put("dataSolicitacao", stringDate(solicitacao.getDataSolicitacao()));
        dto.put("status", solicitacao.getStatus());
        return dto;
    }

    private Map<String, Object> recordDto(RegistroMedico registro) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", registro.getId());
        dto.put("animalId", registro.getAnimalId());
        dto.put("veterinarioCrmv", registro.getVeterinarioCrmv());
        dto.put("descricao", registro.getDescricao());
        dto.put("tipo", registro.getTipo());
        dto.put("data", stringDate(registro.getData()));
        return dto;
    }

    private Map<String, Object> activityDto(Atividade atividade) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", atividade.getId());
        dto.put("titulo", atividade.getTitulo());
        dto.put("descricao", atividade.getDescricao());
        dto.put("data", stringDate(atividade.getData()));
        dto.put("aberta", true);
        return dto;
    }

    private JsonNode readBody(HttpExchange exchange) throws IOException {
        JsonNode body = json.readTree(exchange.getRequestBody());
        if (body == null || !body.isObject()) {
            throw new IllegalArgumentException("O corpo da solicitação deve ser um objeto JSON.");
        }
        return body;
    }

    private String text(JsonNode body, String field) {
        String value = optionalText(body, field).trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("O campo \"" + field + "\" é obrigatório.");
        }
        return value;
    }

    private String optionalText(JsonNode body, String field) {
        JsonNode value = body.get(field);
        return value == null || value.isNull() ? "" : value.asText();
    }

    private int integer(JsonNode body, String field) {
        JsonNode value = body.get(field);
        if (value == null || !value.canConvertToInt()) {
            throw new IllegalArgumentException("O campo \"" + field + "\" deve ser um número inteiro.");
        }
        return value.asInt();
    }

    private LocalDate date(JsonNode body, String field) {
        String value = text(body, field);
        return LocalDate.parse(value);
    }

    private LocalDate optionalDate(JsonNode body, String field) {
        String value = optionalText(body, field).trim();
        return value.isEmpty() ? null : LocalDate.parse(value);
    }

    private String stringDate(LocalDate date) {
        return date == null ? null : date.toString();
    }

    private void sendError(HttpExchange exchange, int status, String message) throws IOException {
        sendJson(exchange, status, Map.of("erro", message == null ? "Erro na solicitação." : message));
    }

    private void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] response = json.writeValueAsString(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, response.length);
        exchange.getResponseBody().write(response);
    }

    private void addCorsHeaders(HttpExchange exchange) {
        String origin = exchange.getRequestHeaders().getFirst("Origin");
        if ("http://localhost:5173".equals(origin) || "http://127.0.0.1:5173".equals(origin)) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", origin);
            exchange.getResponseHeaders().set("Vary", "Origin");
        }
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }
}
