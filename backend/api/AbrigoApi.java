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
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

public class AbrigoApi {
    private final ObjectMapper json = new ObjectMapper();
    private final AnimalService animalService = new AnimalService();
    private final UsuarioService usuarioService = new UsuarioService();
    private final AutenticacaoService autenticacaoService = new AutenticacaoService(usuarioService);
    private final AdocaoService adocaoService = new AdocaoService();
    private final SaudeService saudeService = new SaudeService();
    private final AtividadeService atividadeService = new AtividadeService();
    private final NotificacaoService notificacaoService = new NotificacaoService();
    private final RelatorioService relatorioService =
            new RelatorioService(animalService, adocaoService, usuarioService);

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
        AutenticacaoService.validarSenhaAdministrador(senhaAdmin);
        Administrador administradorInicial = usuarioService.cadastrarAdministradorInicial(
                System.getenv().getOrDefault("ABRIGO_ADMIN_NAME", "Administrador inicial"),
                emailAdmin, "");
        autenticacaoService.registrarSenha(administradorInicial, senhaAdmin);
        atividadeService.cadastrarAtividade(Atividade.Tipo.MUTIRAO_LIMPEZA, "Mutirão de limpeza",
                "Ajude a organizar e higienizar os recintos.", LocalDate.now().plusDays(5), administradorInicial);
        atividadeService.cadastrarAtividade(Atividade.Tipo.FEIRA_ADOCAO, "Feira de adoção",
                "Apoie a equipe em um evento de adoção responsável.", LocalDate.now().plusDays(12), administradorInicial);
        atividadeService.cadastrarAtividade(Atividade.Tipo.CAMPANHA_VACINACAO, "Campanha de vacinação",
                "Ajude a equipe durante a campanha de vacinação.", LocalDate.now().plusDays(19), administradorInicial);
        atividadeService.cadastrarAtividade(Atividade.Tipo.EVENTO_BENEFICENTE, "Evento beneficente",
                "Participe da organização de um evento para apoiar o abrigo.",
                LocalDate.now().plusDays(26), administradorInicial);
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

    private void routeNotification(HttpExchange exchange, String[] parts, String method) throws IOException {
        if (parts.length == 2 && "GET".equals(method)) {
            Usuario usuario = autenticado(exchange);
            List<Map<String, Object>> notifications = notificacaoService
                    .listar(usuario, atividadeService, saudeService).stream()
                    .map(this::notificationDto).toList();
            sendJson(exchange, 200, notifications);
        } else if (parts.length == 2 && "POST".equals(method)) {
            Administrador administrador = requireAdministrador(exchange);
            JsonNode body = readBody(exchange);
            notificacaoService.publicarAviso(text(body, "titulo"), text(body, "mensagem"), administrador);
            sendJson(exchange, 201, Map.of("publicado", true));
        } else {
            sendError(exchange, 405, "Método não permitido para notificações.");
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
            sendJson(exchange, 200, stateFor(autenticado(exchange)));
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
        } else if (parts.length >= 2 && "notifications".equals(parts[1])) {
            routeNotification(exchange, parts, method);
        } else if (parts.length >= 2 && "reports".equals(parts[1])) {
            routeReport(exchange, parts, method);
        } else {
            sendError(exchange, 404, "Rota não encontrada.");
        }
    }

    private void routeAuth(HttpExchange exchange, String[] parts, String method) throws IOException {
        if (parts.length == 3 && "register".equals(parts[2]) && "POST".equals(method)) {
            JsonNode body = readBody(exchange);
            String tipo = text(body, "tipo");
            if (!"Voluntário".equals(tipo) && !"Veterinário".equals(tipo)) {
                throw new IllegalArgumentException("O cadastro público está disponível para voluntários e veterinários.");
            }
            String crmv = optionalText(body, "crmv");
            if ("Veterinário".equals(tipo) && crmv.isBlank()) {
                throw new IllegalArgumentException("Informe o CRMV do veterinário.");
            }
            String senha = text(body, "senha");
            AutenticacaoService.validarSenha(senha);
            Usuario usuario = usuarioService.cadastrar(tipo, text(body, "nome"), text(body, "email"),
                    text(body, "telefone"), crmv, "");
            if (usuario instanceof Veterinario veterinario) {
                veterinario.setAprovado(false);
            }
            autenticacaoService.registrarSenha(usuario, senha);
            sendJson(exchange, 201, Map.of(
                    "usuario", userDto(usuario),
                    "aprovacaoPendente", !usuario.isAprovado()));
        } else if (parts.length == 3 && "login".equals(parts[2]) && "POST".equals(method)) {
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
        if (parts.length == 2 && "GET".equals(method)) {
            autenticado(exchange);
            Map<String, String> filtros = queryParams(exchange);
            sendJson(exchange, 200, animalService.buscar(
                    filtros.get("q"),
                    filtros.get("especie"),
                    filtros.get("raca"),
                    filtros.get("porte"),
                    filtros.get("estadoSaude"),
                    filtros.get("statusAdocao")).stream().map(this::animalDto).toList());
        } else if (parts.length == 3 && "available".equals(parts[2]) && "GET".equals(method)) {
            requireVoluntario(exchange);
            sendJson(exchange, 200, animalService.listarTodos().stream()
                    .filter(Animal::estaDisponivelParaAdocao)
                    .map(this::animalDto).toList());
        } else if (parts.length == 3 && "mine".equals(parts[2]) && "GET".equals(method)) {
            Voluntario voluntario = requireVoluntario(exchange);
            sendJson(exchange, 200, animalService.listarTodos().stream()
                    .filter(animal -> voluntario.getId().equals(animal.getVoluntarioResponsavelId()))
                    .map(this::animalDto).toList());
        } else if (parts.length == 4 && "availability".equals(parts[3]) && "POST".equals(method)) {
            requireAdministrador(exchange);
            Animal animal = animalService.buscarPorId(parts[2]);
            animalService.disponibilizarParaAdocao(animal.getId());
            sendJson(exchange, 200, animalDto(animal));
        } else if (parts.length == 2 && "POST".equals(method)) {
            Usuario operador = requireAdministradorOuVeterinario(exchange);
            JsonNode body = readBody(exchange);
            String voluntarioResponsavelId = optionalText(body, "voluntarioResponsavelId");
            if (!voluntarioResponsavelId.isBlank()) {
                if (!(operador instanceof Administrador)) {
                    throw new AcessoNegadoException("Apenas administradores podem atribuir animais a voluntários.");
                }
                usuarioService.buscarVoluntario(voluntarioResponsavelId);
            }
            String estadoSaude = text(body, "estadoSaude");
            if (operador instanceof Administrador && !"saudavel".equals(normalizar(estadoSaude))) {
                throw new AcessoNegadoException("Somente veterinários podem definir o estado de saúde no cadastro.");
            }
            Animal animal = animalService.cadastrarAnimal(
                    text(body, "nome"), text(body, "especie"), text(body, "raca"), integer(body, "idade"),
                    text(body, "porte"), estadoSaude, date(body, "dataResgate"),
                    optionalText(body, "imagem"), voluntarioResponsavelId, text(body, "statusAdocao"));
            notificacaoService.notificarNovoAnimal(animal);
            sendJson(exchange, 201, animalDto(animal));
        } else if (parts.length == 3 && "PUT".equals(method)) {
            Usuario operador = requireAdministradorOuVeterinario(exchange);
            JsonNode body = readBody(exchange);
            Animal existente = animalService.buscarPorId(parts[2]);
            String estadoSaude = text(body, "estadoSaude");
            if (!normalizar(existente.getEstadoSaude()).equals(normalizar(estadoSaude))) {
                if (!(operador instanceof Veterinario)) {
                    throw new AcessoNegadoException("Apenas veterinários podem alterar informações médicas.");
                }
            }
            String voluntarioResponsavelId = body.has("voluntarioResponsavelId")
                    ? optionalText(body, "voluntarioResponsavelId")
                    : existente.getVoluntarioResponsavelId();
            LocalDate dataResgate = body.has("dataResgate") && !optionalText(body, "dataResgate").isBlank()
                    ? date(body, "dataResgate") : existente.getDataResgate();
            String statusAdocao = body.has("statusAdocao") && !optionalText(body, "statusAdocao").isBlank()
                    ? text(body, "statusAdocao") : existente.getStatusAdocao();
            if (!java.util.Objects.equals(existente.getVoluntarioResponsavelId(), voluntarioResponsavelId)
                    && !(operador instanceof Administrador)) {
                throw new AcessoNegadoException("Apenas administradores podem atribuir animais a voluntários.");
            }
            if (voluntarioResponsavelId != null && !voluntarioResponsavelId.isBlank()) {
                usuarioService.buscarVoluntario(voluntarioResponsavelId);
            }
            Animal animal = animalService.atualizarAnimal(parts[2], text(body, "nome"), text(body, "especie"),
                    text(body, "raca"), integer(body, "idade"), text(body, "porte"), estadoSaude,
                    dataResgate, optionalText(body, "imagem"), voluntarioResponsavelId, statusAdocao);
            sendJson(exchange, 200, animalDto(animal));
        } else if (parts.length == 3 && "DELETE".equals(method)) {
            requireAdministrador(exchange);
            animalService.removerAnimal(parts[2]);
            exchange.sendResponseHeaders(204, -1);
        } else {
            sendError(exchange, 405, "Método não permitido para animais.");
        }
    }

    private void routeUser(HttpExchange exchange, String[] parts, String method) throws IOException {
        if (parts.length == 4 && "approval".equals(parts[3]) && "POST".equals(method)) {
            requireAdministrador(exchange);
            Usuario usuario = usuarioService.buscarPorId(parts[2]);
            if (!(usuario instanceof Veterinario veterinario)) {
                throw new IllegalArgumentException("Somente veterinários podem aguardar aprovação.");
            }
            if (veterinario.isAprovado()) {
                throw new IllegalStateException("Este veterinário já foi aprovado.");
            }
            veterinario.setAprovado(true);
            sendJson(exchange, 200, userDto(veterinario));
        } else if (parts.length == 2 && "POST".equals(method)) {
            Administrador administrador = requireAdministrador(exchange);
            JsonNode body = readBody(exchange);
            if ("Administrador".equals(text(body, "tipo")) && !administrador.isPrincipal()) {
                throw new AcessoNegadoException(
                        "Somente o administrador principal pode criar outros administradores.");
            }
            Usuario usuario = saveUser(body, null);
            sendJson(exchange, 201, userDto(usuario));
        } else if (parts.length == 3 && "PUT".equals(method)) {
            Administrador administrador = requireAdministrador(exchange);
            JsonNode body = readBody(exchange);
            Usuario existente = usuarioService.buscarPorId(parts[2]);
            String tipoAtualizado = text(body, "tipo");
            if (existente instanceof Administrador alvo) {
                if (alvo.isPrincipal() && !"Administrador".equals(tipoAtualizado)) {
                    throw new IllegalArgumentException("O administrador principal não pode ser rebaixado.");
                }
                if (!alvo.isPrincipal() && !administrador.isPrincipal()) {
                    throw new AcessoNegadoException(
                            "Somente o administrador principal pode alterar outros administradores.");
                }
            } else if ("Administrador".equals(tipoAtualizado) && !administrador.isPrincipal()) {
                throw new AcessoNegadoException(
                        "Somente o administrador principal pode promover usuários a administradores.");
            }
            String senha = optionalText(body, "senha");
            if (!mesmoPerfil(existente, tipoAtualizado) && senha.isBlank()) {
                throw new IllegalArgumentException(
                        "Informe uma nova senha ao alterar o perfil de um usuário.");
            }
            Usuario usuario = saveUser(body, parts[2]);
            if (!senha.isBlank()) {
                autenticacaoService.registrarSenha(usuario, senha);
                autenticacaoService.revogarSessoes(usuario.getId(), token(exchange));
            }
            sendJson(exchange, 200, userDto(usuario));
        } else if (parts.length == 3 && "DELETE".equals(method)) {
            Administrador administrador = requireAdministrador(exchange);
            Usuario usuario = usuarioService.buscarPorId(parts[2]);
            if (usuario instanceof Administrador alvo) {
                if (alvo.isPrincipal()) {
                    throw new IllegalArgumentException("O administrador principal não pode ser removido.");
                }
                if (!administrador.isPrincipal()) {
                    throw new AcessoNegadoException(
                            "Somente o administrador principal pode remover outros administradores.");
                }
            }
            if (usuario instanceof Voluntario) {
                animalService.removerAtribuicoesDoVoluntario(usuario.getId());
            }
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
        String nivelAcesso = "Total";
        String senha = optionalText(body, "senha");
        if (id == null || !senha.isBlank()) {
            AutenticacaoService.validarSenha(senha, tipo);
        }
        if ("Veterinário".equals(tipo) && crmv.isBlank()) {
            throw new IllegalArgumentException("Informe o CRMV do veterinário.");
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
            notificacaoService.notificarNovaSolicitacao(solicitacao);
            sendJson(exchange, 201, requestDto(solicitacao));
        } else if (parts.length == 2 && "GET".equals(method)) {
            requireAdministrador(exchange);
            sendJson(exchange, 200, adocaoService.listarSolicitacoes().stream().map(this::requestDto).toList());
        } else if (parts.length == 3 && "history".equals(parts[2]) && "GET".equals(method)) {
            requireAdministrador(exchange);
            sendJson(exchange, 200, adocaoService.getHistoricoAdocoes().stream().map(this::requestDto).toList());
        } else if (parts.length == 4 && "return".equals(parts[3]) && "POST".equals(method)) {
            Administrador administrador = requireAdministrador(exchange);
            JsonNode body = readBody(exchange);
            SolicitacaoAdocao solicitacao = adocaoService.registrarDevolucao(parts[2],
                    text(body, "motivoDevolucao"), administrador, animalService);
            notificacaoService.notificarAtualizacaoSolicitacao(solicitacao);
            sendJson(exchange, 200, requestDto(solicitacao));
        } else if (parts.length == 3 && "PATCH".equals(method)) {
            JsonNode body = readBody(exchange);
            Administrador administrador = requireAdministrador(exchange);
            List<SolicitacaoAdocao> atualizadas = adocaoService.atualizarSolicitacao(
                    parts[2], text(body, "decisao"), administrador);
            atualizadas.forEach(notificacaoService::notificarAtualizacaoSolicitacao);
            sendJson(exchange, 200, state());
        } else {
            sendError(exchange, 405, "Método não permitido para solicitações.");
        }
    }

    private void routeMedicalRecord(HttpExchange exchange, String[] parts, String method) throws IOException {
        if (parts.length == 2 && "POST".equals(method)) {
            JsonNode body = readBody(exchange);
            String animalId = text(body, "animalId");
            animalService.buscarPorId(animalId);
            Veterinario veterinario = requireVeterinario(exchange);
            RegistroMedico registro = saudeService.registrarAtendimento(animalId, veterinario,
                    text(body, "descricao"), text(body, "tipo"), date(body, "data"),
                    optionalDate(body, "dataVencimento"));
            sendJson(exchange, 201, recordDto(registro));
        } else if (parts.length == 3 && "PUT".equals(method)) {
            Veterinario veterinario = requireVeterinario(exchange);
            JsonNode body = readBody(exchange);
            RegistroMedico registro = saudeService.atualizarAtendimento(parts[2], veterinario,
                    text(body, "descricao"), text(body, "tipo"), date(body, "data"),
                    optionalDate(body, "dataVencimento"));
            sendJson(exchange, 200, recordDto(registro));
        } else {
            sendError(exchange, 405, "Método não permitido para prontuários.");
        }
    }

    private void routeActivity(HttpExchange exchange, String[] parts, String method) throws IOException {
        if (parts.length == 3 && "mine".equals(parts[2]) && "GET".equals(method)) {
            Voluntario voluntario = requireVoluntario(exchange);
            List<Map<String, Object>> inscricoes = atividadeService.listarAtividades().stream()
                    .filter(atividade -> atividade.getVoluntariosInscritosId().contains(voluntario.getId()))
                    .map(this::activityDto).toList();
            List<Map<String, Object>> historico = atividadeService.listarAtividades().stream()
                    .filter(atividade -> atividade.getVoluntariosPresentesId().contains(voluntario.getId()))
                    .map(this::activityDto).toList();
            sendJson(exchange, 200, Map.of("inscricoes", inscricoes, "historico", historico));
        } else if (parts.length == 2 && "GET".equals(method)) {
            autenticado(exchange);
            Map<String, String> filtros = queryParams(exchange);
            String tipo = filtros.getOrDefault("tipo", "").trim();
            if (!tipo.isEmpty()) {
                tipoAtividade(tipo);
            }
            sendJson(exchange, 200, atividadeService.listarAtividades().stream()
                    .filter(atividade -> !atividade.getData().isBefore(LocalDate.now()))
                    .filter(atividade -> tipo.isBlank() || atividade.getTipo().name().equalsIgnoreCase(tipo))
                    .map(this::activityDto).toList());
        } else if (parts.length == 2 && "POST".equals(method)) {
            Administrador administrador = requireAdministrador(exchange);
            JsonNode body = readBody(exchange);
            Atividade atividade = atividadeService.cadastrarAtividade(
                    tipoAtividade(text(body, "tipo")), text(body, "titulo"), text(body, "descricao"),
                    date(body, "data"), administrador);
            sendJson(exchange, 201, activityDto(atividade));
        } else if (parts.length == 4 && "registrations".equals(parts[3]) && "POST".equals(method)) {
            Voluntario voluntario = requireVoluntario(exchange);
            atividadeService.inscreverVoluntario(parts[2], voluntario);
            sendJson(exchange, 200, Map.of("inscrito", true));
        } else if (parts.length == 4 && "attendance".equals(parts[3]) && "POST".equals(method)) {
            JsonNode body = readBody(exchange);
            Administrador administrador = requireAdministrador(exchange);
            Voluntario voluntario = usuarioService.buscarVoluntario(text(body, "voluntarioId"));
            atividadeService.marcarPresenca(parts[2], voluntario, administrador);
            sendJson(exchange, 200, state());
        } else {
            sendError(exchange, 405, "Método não permitido para atividades.");
        }
    }

    private Atividade.Tipo tipoAtividade(String tipo) {
        try {
            return Atividade.Tipo.valueOf(tipo.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Tipo inválido. Use MUTIRAO_LIMPEZA, CAMPANHA_VACINACAO, FEIRA_ADOCAO ou EVENTO_BENEFICENTE.");
        }
    }

    private void routeReport(HttpExchange exchange, String[] parts, String method) throws IOException {
        if (parts.length != 3 || !"GET".equals(method)) {
            sendError(exchange, 405, "Método não permitido para relatórios.");
            return;
        }
        if ("health".equals(parts[2])) {
            requireAcessoRelatorioSaude(exchange);
            List<RegistroMedico> registros = filtrarRegistrosMedicos(exchange);
            sendJson(exchange, 200, Map.of(
                    "total", registros.size(),
                    "registros", registros.stream().map(this::recordDto).toList()));
        } else if ("health.csv".equals(parts[2])) {
            requireAcessoRelatorioSaude(exchange);
            enviarRelatorioSaudeCsv(exchange, filtrarRegistrosMedicos(exchange));
        } else if ("treatment".equals(parts[2])) {
            requireAcessoRelatorioSaude(exchange);
            List<Animal> emTratamento = relatorioService.gerarEstatisticas().animaisEmTratamento();
            sendJson(exchange, 200, Map.of(
                    "total", emTratamento.size(),
                    "animais", emTratamento.stream().map(this::animalDto).toList()));
        } else if ("admin".equals(parts[2])) {
            requireAdministrador(exchange);
            sendJson(exchange, 200, relatorioAdministrador());
        } else {
            sendError(exchange, 404, "Relatório não encontrado.");
        }
    }

    private List<RegistroMedico> filtrarRegistrosMedicos(HttpExchange exchange) {
        Map<String, String> filtros = queryParams(exchange);
        String animalId = filtros.getOrDefault("animalId", "");
        String tipo = filtros.getOrDefault("tipo", "");
        LocalDate dataInicio = filtros.containsKey("de") && !filtros.get("de").isBlank()
                ? LocalDate.parse(filtros.get("de")) : null;
        LocalDate dataFim = filtros.containsKey("ate") && !filtros.get("ate").isBlank()
                ? LocalDate.parse(filtros.get("ate")) : null;
        if (dataInicio != null && dataFim != null && dataInicio.isAfter(dataFim)) {
            throw new IllegalArgumentException("A data inicial não pode ser posterior à data final.");
        }
        return saudeService.listarRegistros().stream()
                .filter(registro -> animalId.isBlank() || registro.getAnimalId().equals(animalId))
                .filter(registro -> tipo.isBlank() || registro.getTipo().equalsIgnoreCase(tipo))
                .filter(registro -> dataInicio == null || !registro.getData().isBefore(dataInicio))
                .filter(registro -> dataFim == null || !registro.getData().isAfter(dataFim))
                .toList();
    }

    private Map<String, String> queryParams(HttpExchange exchange) {
        Map<String, String> parametros = new LinkedHashMap<>();
        String query = exchange.getRequestURI().getRawQuery();
        if (query == null || query.isBlank()) {
            return parametros;
        }
        for (String parte : query.split("&")) {
            String[] chaveValor = parte.split("=", 2);
            String chave = URLDecoder.decode(chaveValor[0], StandardCharsets.UTF_8);
            String valor = chaveValor.length == 2
                    ? URLDecoder.decode(chaveValor[1], StandardCharsets.UTF_8) : "";
            parametros.put(chave, valor);
        }
        return parametros;
    }

    private Map<String, Object> relatorioAdministrador() {
        RelatorioService.Estatisticas estatisticas = relatorioService.gerarEstatisticas();
        List<Animal> todosAnimais = animalService.listarTodosIncluindoArquivados();
        List<Animal> animaisAtivos = todosAnimais.stream().filter(animal -> !animal.isArquivado()).toList();
        List<Usuario> usuarios = usuarioService.listarTodos();
        List<SolicitacaoAdocao> solicitacoes = adocaoService.listarSolicitacoes();
        List<RegistroMedico> registros = saudeService.listarRegistros();
        List<Atividade> atividades = atividadeService.listarAtividades();

        Map<String, Long> usuariosPorPerfil = usuarios.stream().collect(Collectors.groupingBy(
                usuario -> usuario instanceof Voluntario ? "Voluntário"
                        : usuario instanceof Veterinario ? "Veterinário" : "Administrador",
                Collectors.counting()));
        Map<String, Long> atendimentosPorTipo = registros.stream().collect(Collectors.groupingBy(
                RegistroMedico::getTipo, Collectors.counting()));

        Map<String, Object> animais = Map.of(
                "ativos", animaisAtivos.size(),
                "disponiveis", animaisAtivos.stream().filter(Animal::estaDisponivelParaAdocao).count(),
                "indisponiveis", animaisAtivos.stream()
                        .filter(animal -> "INDISPONIVEL".equalsIgnoreCase(animal.getStatusAdocao())).count(),
                "adotados", animaisAtivos.stream()
                        .filter(animal -> "ADOTADO".equalsIgnoreCase(animal.getStatusAdocao())).count(),
                "criticos", animaisAtivos.stream().filter(Animal::estaEmTratamentoCritico).count(),
                "arquivados", todosAnimais.size() - animaisAtivos.size());
        Map<String, Object> adocoes = Map.of(
                "pendentes", solicitacoes.stream().filter(item -> "PENDENTE".equals(item.getStatus())).count(),
                "aprovadas", solicitacoes.stream().filter(item -> "APROVADO".equals(item.getStatus())).count(),
                "rejeitadas", solicitacoes.stream().filter(item -> "REJEITADO".equals(item.getStatus())).count(),
                "devolvidas", solicitacoes.stream().filter(item -> "DEVOLVIDO".equals(item.getStatus())).count());
        Map<String, Object> saude = Map.of(
                "total", registros.size(),
                "porTipo", atendimentosPorTipo);
        Map<String, Object> relatorioAtividades = Map.of(
                "total", atividades.size(),
                "abertas", atividades.stream().filter(item -> !item.getData().isBefore(LocalDate.now())).count(),
                "inscricoes", atividades.stream().mapToInt(item -> item.getVoluntariosInscritosId().size()).sum(),
                "presencas", atividades.stream().mapToInt(item -> item.getVoluntariosPresentesId().size()).sum());
        Map<String, Object> relatorioUsuarios = Map.of(
                "total", usuarios.size(),
                "porPerfil", usuariosPorPerfil,
                "voluntariosAtivos", estatisticas.voluntariosAtivos(),
                "veterinariosAguardandoAprovacao", usuarios.stream()
                        .filter(usuario -> usuario instanceof Veterinario && !usuario.isAprovado()).count());
        return Map.of(
                "animais", animais,
                "adocoes", adocoes,
                "estatisticas", estatisticasDto(estatisticas),
                "saude", saude,
                "atividades", relatorioAtividades,
                "usuarios", relatorioUsuarios);
    }

    private Map<String, Object> estatisticasDto(RelatorioService.Estatisticas estatisticas) {
        List<Map<String, Object>> especies = estatisticas.especiesMaisResgatadas().stream()
                .map(item -> Map.<String, Object>of(
                        "especie", item.especie(),
                        "quantidade", item.quantidade()))
                .toList();
        List<Map<String, Object>> taxaMensal = estatisticas.taxaAdocaoMensal().stream()
                .map(item -> {
                    Map<String, Object> dto = new LinkedHashMap<>();
                    dto.put("mes", item.mes().toString());
                    dto.put("animaisResgatados", item.animaisResgatados());
                    dto.put("adocoesRealizadas", item.adocoesRealizadas());
                    dto.put("taxaPercentual", item.taxaPercentual());
                    return dto;
                })
                .toList();
        return Map.of(
                "animaisResgatados", estatisticas.animaisResgatados(),
                "adocoesRealizadas", estatisticas.adocoesRealizadas(),
                "especiesMaisResgatadas", especies,
                "taxaAdocaoMensal", taxaMensal,
                "voluntariosAtivos", estatisticas.voluntariosAtivos(),
                "animaisEmTratamento", estatisticas.animaisEmTratamento().stream().map(this::animalDto).toList());
    }

    private void enviarRelatorioSaudeCsv(HttpExchange exchange, List<RegistroMedico> registros) throws IOException {
        StringBuilder csv = new StringBuilder("\uFEFF")
                .append("ID do animal;Data;Tipo;Descrição;CRMV do veterinário;Vencimento da vacina\r\n");
        for (RegistroMedico registro : registros) {
            csv.append(celulaCsv(registro.getAnimalId())).append(';')
                    .append(celulaCsv(registro.getData().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/uuuu"))))
                    .append(';').append(celulaCsv(registro.getTipo()))
                    .append(';').append(celulaCsv(registro.getDescricao()))
                    .append(';').append(celulaCsv(registro.getVeterinarioCrmv()))
                    .append(';').append(celulaCsv(stringDate(registro.getDataVencimento())))
                    .append("\r\n");
        }
        byte[] resposta = csv.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/csv; charset=utf-8");
        exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"relatorio-de-saude.csv\"");
        exchange.sendResponseHeaders(200, resposta.length);
        exchange.getResponseBody().write(resposta);
    }

    private String celulaCsv(String valor) {
        String texto = valor == null ? "" : valor;
        if (texto.stripLeading().matches("^[=+@\\-].*")) {
            texto = "'" + texto;
        }
        return "\"" + texto.replace("\"", "\"\"") + "\"";
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

    private Usuario requireAdministradorOuVeterinario(HttpExchange exchange) {
        Usuario usuario = autenticado(exchange);
        if (!(usuario instanceof Administrador) && !(usuario instanceof Veterinario)) {
            throw new AcessoNegadoException("Apenas administradores ou veterinários podem executar esta ação.");
        }
        return usuario;
    }

    private Voluntario requireVoluntario(HttpExchange exchange) {
        Usuario usuario = autenticado(exchange);
        if (!(usuario instanceof Voluntario voluntario)) {
            throw new AcessoNegadoException("Apenas voluntários podem se inscrever em atividades.");
        }
        return voluntario;
    }

    private void requireAcessoRelatorioSaude(HttpExchange exchange) {
        Usuario usuario = autenticado(exchange);
        if (!(usuario instanceof Veterinario) && !(usuario instanceof Administrador)) {
            throw new AcessoNegadoException("Apenas veterinários e administradores podem consultar relatórios de saúde.");
        }
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
        List<Map<String, Object>> animals = animalService.listarTodosIncluindoArquivados().stream()
                .map(this::animalDto).toList();
        List<Map<String, Object>> users = usuarioService.listarTodos().stream().map(this::userDto).toList();
        List<Map<String, Object>> requests = adocaoService.listarSolicitacoes().stream().map(this::requestDto).toList();
        List<Map<String, Object>> adoptionHistory = adocaoService.getHistoricoAdocoes().stream()
                .map(this::requestDto).toList();
        List<Map<String, Object>> records = saudeService.listarRegistros().stream().map(this::recordDto).toList();
        List<Map<String, Object>> activities = atividadeService.listarAtividades().stream().map(this::activityDto).toList();
        List<Map<String, Object>> registrations = new ArrayList<>();
        List<Map<String, Object>> participations = new ArrayList<>();
        for (Atividade atividade : atividadeService.listarAtividades()) {
            for (String volunteerId : atividade.getVoluntariosInscritosId()) {
                registrations.add(Map.of("atividadeId", atividade.getId(), "voluntarioId", volunteerId));
            }
            for (String volunteerId : atividade.getVoluntariosPresentesId()) {
                participations.add(Map.of("atividadeId", atividade.getId(), "voluntarioId", volunteerId));
            }
        }
        return Map.of("animais", animals, "usuarios", users, "solicitacoes", requests,
                "historicoAdocoes", adoptionHistory,
                "registrosMedicos", records, "atividades", activities, "inscricoes", registrations,
                "participacoes", participations);
    }

    private Map<String, Object> stateFor(Usuario usuario) {
        if (usuario instanceof Administrador) {
            return state();
        }
        List<Map<String, Object>> animals = animalService.listarTodos().stream()
                .map(this::animalDto).toList();
        List<Map<String, Object>> users = List.of(userDto(usuario));
        List<Map<String, Object>> records = usuario instanceof Veterinario
                ? saudeService.listarRegistros().stream().map(this::recordDto).toList()
                : List.of();
        List<Map<String, Object>> activities = atividadeService.listarAtividades().stream()
                .map(this::activityDto).toList();
        List<Map<String, Object>> registrations = new ArrayList<>();
        List<Map<String, Object>> participations = new ArrayList<>();
        if (usuario instanceof Voluntario voluntario) {
            for (Atividade atividade : atividadeService.listarAtividades()) {
                if (atividade.getVoluntariosInscritosId().contains(voluntario.getId())) {
                    registrations.add(Map.of("atividadeId", atividade.getId(), "voluntarioId", voluntario.getId()));
                }
                if (atividade.getVoluntariosPresentesId().contains(voluntario.getId())) {
                    participations.add(Map.of("atividadeId", atividade.getId(), "voluntarioId", voluntario.getId()));
                }
            }
        }
        return Map.of(
                "animais", animals,
                "usuarios", users,
                "solicitacoes", List.of(),
                "historicoAdocoes", List.of(),
                "registrosMedicos", records,
                "atividades", activities,
                "inscricoes", registrations,
                "participacoes", participations);
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
        dto.put("voluntarioResponsavelId", animal.getVoluntarioResponsavelId());
        dto.put("arquivado", animal.isArquivado());
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
        dto.put("aprovado", usuario.isAprovado());
        if (usuario instanceof Veterinario veterinario) {
            dto.put("crmv", veterinario.getCrmv());
        } else if (usuario instanceof Administrador administrador) {
            dto.put("nivelAcesso", administrador.getNivelAcesso());
            dto.put("administradorPrincipal", administrador.isPrincipal());
        } else if (usuario instanceof Voluntario voluntario) {
            dto.put("atividadesInscritas", voluntario.getAtividadesInscritas());
            dto.put("historicoParticipacoes", voluntario.getHistoricoParticipacoes());
        }
        return dto;
    }

    private boolean mesmoPerfil(Usuario usuario, String tipo) {
        return switch (tipo) {
            case "Administrador" -> usuario instanceof Administrador;
            case "Veterinário" -> usuario instanceof Veterinario;
            case "Voluntário" -> usuario instanceof Voluntario;
            default -> false;
        };
    }

    private Map<String, Object> requestDto(SolicitacaoAdocao solicitacao) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", solicitacao.getId());
        dto.put("animalId", solicitacao.getAnimal().getId());
        dto.put("adotanteNome", solicitacao.getAdotanteNome());
        dto.put("adotanteContato", solicitacao.getAdotanteContato());
        dto.put("dataSolicitacao", stringDate(solicitacao.getDataSolicitacao()));
        dto.put("dataDecisao", stringDate(solicitacao.getDataDecisao()));
        dto.put("dataAdocao", stringDate(solicitacao.getDataAdocao()));
        dto.put("dataDevolucao", stringDate(solicitacao.getDataDevolucao()));
        dto.put("motivoDevolucao", solicitacao.getMotivoDevolucao());
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
        dto.put("dataVencimento", stringDate(registro.getDataVencimento()));
        return dto;
    }

    private Map<String, Object> notificationDto(Notificacao notificacao) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", notificacao.getId());
        dto.put("tipo", notificacao.getTipo().name());
        dto.put("titulo", notificacao.getTitulo());
        dto.put("mensagem", notificacao.getMensagem());
        dto.put("data", stringDate(notificacao.getData()));
        dto.put("referenciaId", notificacao.getReferenciaId());
        return dto;
    }

    private Map<String, Object> activityDto(Atividade atividade) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", atividade.getId());
        dto.put("tipo", atividade.getTipo().name());
        dto.put("titulo", atividade.getTitulo());
        dto.put("descricao", atividade.getDescricao());
        dto.put("data", stringDate(atividade.getData()));
        dto.put("aberta", !atividade.getData().isBefore(LocalDate.now()));
        dto.put("totalInscritos", atividade.getVoluntariosInscritosId().size());
        dto.put("voluntariosPresentesId", atividade.getVoluntariosPresentesId());
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
