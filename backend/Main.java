package backend;

import backend.exception.AnimalIndisponivelException;
import backend.exception.AutenticacaoException;
import backend.exception.VacinaDuplicadaException;
import backend.model.*;
import backend.service.*;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Main {
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/uuuu");

    public static void main(String[] args) {
        AnimalService animalService = new AnimalService();
        UsuarioService usuarioService = new UsuarioService();
        AutenticacaoService autenticacaoService = new AutenticacaoService(usuarioService);
        AdocaoService adocaoService = new AdocaoService();
        SaudeService saudeService = new SaudeService();
        AtividadeService atividadeService = new AtividadeService();
        NotificacaoService notificacaoService = new NotificacaoService();
        RelatorioService relatorioService = new RelatorioService(animalService, adocaoService, usuarioService);
        ArmazenamentoContas armazenamentoContas = new ArmazenamentoContas();
        try {
            armazenamentoContas.carregar(usuarioService, autenticacaoService);
        } catch (IOException | IllegalArgumentException exception) {
            System.err.println("Não foi possível carregar as contas salvas: " + exception.getMessage());
            return;
        }

        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("=== SISTEMA DE GERENCIAMENTO DE ABRIGOS E RESGATES ===");
            boolean executando = true;

            while (executando) {
                AutenticacaoService.Sessao sessao;
                try {
                    sessao = menuAcesso(scanner, usuarioService, autenticacaoService, armazenamentoContas);
                } catch (EntradaEncerradaException exception) {
                    break;
                }
                if (sessao == null) {
                    break;
                }

                Usuario usuarioAtivo = sessao.usuario();
                System.out.printf("%nOlá, %s (%s).%n", usuarioAtivo.getNome(), tipoUsuario(usuarioAtivo));
                boolean sessaoAtiva = true;
                while (sessaoAtiva && executando) {
                    exibirMenu(usuarioAtivo);
                    try {
                        int opcao = lerInteiro(scanner, "Escolha uma opção: ");
                        if (opcao == 0) {
                            autenticacaoService.sair(sessao.token());
                            sessaoAtiva = false;
                            System.out.println("Você saiu da conta com sucesso.");
                            continue;
                        }
                        if (!podeExecutar(usuarioAtivo, opcao)) {
                            System.out.println("Seu perfil não tem permissão para essa opção.");
                            continue;
                        }
                        switch (opcao) {
                            case 1 -> cadastrarAnimal(scanner, animalService, notificacaoService,
                                    usuarioService, usuarioAtivo);
                            case 2 -> listarAnimais(animalService);
                            case 3 -> solicitarAdocao(scanner, animalService, adocaoService, notificacaoService);
                            case 4 -> analisarSolicitacao(scanner, (Administrador) usuarioAtivo,
                                    adocaoService, notificacaoService);
                            case 5 -> registrarAtendimento(scanner, animalService,
                                    (Veterinario) usuarioAtivo, saudeService);
                            case 6 -> cadastrarUsuario(scanner, usuarioService, autenticacaoService,
                                    (Administrador) usuarioAtivo, armazenamentoContas);
                            case 7 -> listarUsuarios(usuarioService);
                            case 8 -> cadastrarAtividade(scanner, (Administrador) usuarioAtivo, atividadeService);
                            case 9 -> inscreverVoluntario(scanner, (Voluntario) usuarioAtivo, atividadeService);
                            case 10 -> listarSolicitacoes(adocaoService);
                            case 11 -> listarAtividades(atividadeService);
                            case 12 -> buscarAnimais(scanner, animalService);
                            case 13 -> editarAnimal(scanner, animalService, usuarioService, usuarioAtivo);
                            case 14 -> removerAnimal(scanner, animalService);
                            case 15 -> registrarDevolucao(scanner, (Administrador) usuarioAtivo,
                                    animalService, adocaoService, notificacaoService);
                            case 16 -> listarHistoricoAdocoes(adocaoService);
                            case 17 -> listarNotificacoes(usuarioAtivo, atividadeService,
                                    saudeService, notificacaoService);
                            case 18 -> publicarAviso(scanner, (Administrador) usuarioAtivo, notificacaoService);
                            case 19 -> exibirRelatorioEstatistico((Administrador) usuarioAtivo, relatorioService);
                            case 20 -> aprovarVeterinario(scanner, usuarioService,
                                    autenticacaoService, armazenamentoContas);
                            default -> System.out.println("Opção inválida. Tente novamente.");
                        }
                    } catch (AnimalIndisponivelException | VacinaDuplicadaException
                            | IllegalArgumentException | IllegalStateException | NoSuchElementException
                            | AutenticacaoException exception) {
                        System.out.println("Não foi possível concluir a operação: " + exception.getMessage());
                    } catch (EntradaEncerradaException exception) {
                        autenticacaoService.sair(sessao.token());
                        sessaoAtiva = false;
                        executando = false;
                    }
                }
            }
            } catch (IOException exception) {
                System.err.println("Não foi possível salvar as contas: " + exception.getMessage());
            }

        System.out.println("Sistema encerrado.");
    }

    private static AutenticacaoService.Sessao menuAcesso(Scanner scanner, UsuarioService usuarioService,
            AutenticacaoService autenticacaoService, ArmazenamentoContas armazenamentoContas)
            throws IOException {
        while (true) {
            System.out.println("""

                    --- ABRIGO DE ANIMAIS ---
                    1. Fazer login
                    2. Cadastrar usuário
                    0. Sair
                    """);

            try {
                int opcao = lerInteiro(scanner, "Escolha uma opção: ");
                if (opcao == 0) {
                    return null;
                }
                if (opcao == 1) {
                    String email = lerTexto(scanner, "E-mail: ");
                    String senha = lerTexto(scanner, "Senha: ");
                    return autenticacaoService.entrar(email, senha);
                }
                if (opcao == 2) {
                    cadastrarConta(scanner, usuarioService, autenticacaoService, armazenamentoContas);
                    continue;
                }
                System.out.println("Opção inválida. Tente novamente.");
            } catch (AutenticacaoException | IllegalArgumentException | IllegalStateException
                    | NoSuchElementException exception) {
                System.out.println("Não foi possível concluir a operação: " + exception.getMessage());
            }
        }
    }

    private static void cadastrarPrimeiroAdministrador(Scanner scanner,
            UsuarioService usuarioService, AutenticacaoService autenticacaoService,
            ArmazenamentoContas armazenamentoContas) throws IOException {
        Usuario administrador = cadastrarContaPorTipo(
                scanner, usuarioService, autenticacaoService, "Administrador", true);
        armazenamentoContas.salvar(usuarioService, autenticacaoService);
        System.out.println("Primeiro administrador principal criado com sucesso! ID: "
                + administrador.getId() + ". Agora você já pode fazer login.");
    }

    private static void cadastrarConta(Scanner scanner, UsuarioService usuarioService,
            AutenticacaoService autenticacaoService, ArmazenamentoContas armazenamentoContas)
            throws IOException {
        System.out.println("\n--- Criar conta ---");
        boolean administradorConfigurado = usuarioService.listarTodos().stream()
                .anyMatch(Administrador.class::isInstance);
        String promptTipo = administradorConfigurado
                ? "Tipo (1 - voluntário, 2 - veterinário): "
                : "Tipo (1 - voluntário, 2 - veterinário, 3 - administrador principal): ";
        int tipoOpcao = lerInteiro(scanner, promptTipo);
        String tipo = switch (tipoOpcao) {
            case 1 -> "Voluntário";
            case 2 -> "Veterinário";
            case 3 -> {
                if (administradorConfigurado) {
                    throw new IllegalArgumentException(
                            "Para cadastrar outro administrador, entre com a conta do administrador principal.");
                }
                yield "Administrador";
            }
            default -> throw new IllegalArgumentException("Escolha um dos tipos de usuário disponíveis.");
        };
        if ("Administrador".equals(tipo) && !administradorConfigurado) {
            cadastrarPrimeiroAdministrador(scanner, usuarioService, autenticacaoService, armazenamentoContas);
            return;
        }
        Usuario usuario = cadastrarContaPorTipo(scanner, usuarioService, autenticacaoService, tipo, false);
        armazenamentoContas.salvar(usuarioService, autenticacaoService);
        exibirMensagemCadastro(usuario);
    }

    private static Usuario cadastrarContaPorTipo(Scanner scanner, UsuarioService usuarioService,
            AutenticacaoService autenticacaoService, String tipo, boolean administradorPrincipal) {
        String nome = lerTexto(scanner, "Nome: ");
        String email = lerTexto(scanner, "E-mail: ");
        String telefone = lerTexto(scanner, "Telefone: ");
        String crmv = tipo.equals("Veterinário") ? lerTexto(scanner, "CRMV: ") : "";
        if (tipo.equals("Veterinário") && crmv.isBlank()) {
            throw new IllegalArgumentException("Informe o CRMV do veterinário.");
        }
        String promptSenha = administradorPrincipal
                ? "Senha do primeiro administrador principal (mínimo 6 caracteres): "
                : switch (tipo) {
                    case "Administrador" -> "Senha do novo administrador (mínimo 6 caracteres): ";
                    case "Veterinário" -> "Senha do novo veterinário (mínimo 6 caracteres): ";
                    default -> "Senha do novo voluntário (mínimo 6 caracteres): ";
                };
        String senha = lerTexto(scanner, promptSenha);
        AutenticacaoService.validarSenha(senha);

        Usuario usuario = administradorPrincipal
                ? usuarioService.cadastrarAdministradorInicial(nome, email, telefone)
                : usuarioService.cadastrar(tipo, nome, email, telefone, crmv, "");
        if (usuario instanceof Veterinario veterinario) {
            veterinario.setAprovado(false);
        }
        autenticacaoService.registrarSenha(usuario, senha);
        return usuario;
    }

    private static void exibirMensagemCadastro(Usuario usuario) {
        if (usuario instanceof Administrador administrador) {
            if (administrador.isPrincipal()) {
                System.out.println("Primeiro administrador principal criado com sucesso! ID: "
                        + administrador.getId() + ".");
            } else {
                System.out.println("Novo administrador criado com sucesso! ID: "
                        + administrador.getId() + ".");
            }
        } else if (usuario instanceof Veterinario) {
            System.out.println("Cadastro de veterinário recebido com sucesso! ID: "
                    + usuario.getId() + ". Aguarde a aprovação do administrador para entrar.");
        } else {
            System.out.println("Cadastro de voluntário concluído com sucesso! ID: "
                    + usuario.getId() + ". Você já pode fazer login.");
        }
    }

    private static String tipoUsuario(Usuario usuario) {
        if (usuario instanceof Administrador) {
            return ((Administrador) usuario).isPrincipal()
                    ? "Administrador principal" : "Administrador";
        }
        if (usuario instanceof Veterinario) {
            return "Veterinário";
        }
        return "Voluntário";
    }

    private static boolean podeExecutar(Usuario usuario, int opcao) {
        if (usuario instanceof Administrador) {
            return opcao >= 1 && opcao <= 20 && opcao != 5;
        }
        if (usuario instanceof Veterinario) {
            return List.of(1, 2, 3, 5, 11, 12, 13, 17).contains(opcao);
        }
        if (usuario instanceof Voluntario) {
            return List.of(2, 3, 9, 11, 12, 17).contains(opcao);
        }
        return false;
    }

    private static void exibirMenu(Usuario usuario) {
        System.out.println("\n--- MENU DO ABRIGO (" + tipoUsuario(usuario) + ") ---");
        if (usuario instanceof Administrador) {
            System.out.println("""
                    1. Cadastrar animal
                    2. Listar animais
                    3. Solicitar adoção
                    4. Analisar solicitação de adoção
                    6. Cadastrar usuário
                    7. Listar usuários
                    8. Cadastrar atividade
                    9. Inscrever voluntário em atividade
                    10. Listar solicitações de adoção
                    11. Listar atividades
                    12. Buscar e filtrar animais
                    13. Editar animal
                    14. Remover animal
                    15. Registrar devolução de adoção
                    16. Consultar histórico de adoções
                    17. Consultar notificações
                    18. Publicar aviso do abrigo
                    19. Relatórios e estatísticas
                    20. Aprovar cadastro de veterinário
                    0. Sair da conta
                    """);
        } else if (usuario instanceof Veterinario) {
            System.out.println("""
                    1. Cadastrar animal
                    2. Listar animais
                    3. Solicitar adoção
                    5. Registrar atendimento médico
                    11. Listar atividades
                    12. Buscar e filtrar animais
                    13. Editar animal
                    17. Consultar notificações
                    0. Sair da conta
                    """);
        } else {
            System.out.println("""
                    2. Listar animais
                    3. Solicitar adoção
                    9. Inscrever-se em atividade
                    11. Listar atividades
                    12. Buscar e filtrar animais
                    17. Consultar notificações
                    0. Sair da conta
                    """);
        }
    }

    private static void cadastrarAnimal(Scanner scanner, AnimalService animalService,
            NotificacaoService notificacaoService, UsuarioService usuarioService, Usuario usuarioAtivo) {
        System.out.println("\n--- Cadastro de animal ---");
        String nome = lerTexto(scanner, "Nome: ");
        String especie = lerTexto(scanner, "Espécie: ");
        String raca = lerTexto(scanner, "Raça: ");
        int idade = lerInteiro(scanner, "Idade: ");
        String porte = lerTexto(scanner, "Porte: ");
        String estadoSaude = lerTexto(scanner, "Estado de saúde: ");
        if (!"saudavel".equals(normalizar(estadoSaude))) {
            if (!(usuarioAtivo instanceof Veterinario)) {
                String veterinarioId = lerTexto(scanner, "ID do veterinário responsável pelo estado de saúde: ");
                usuarioService.buscarVeterinario(veterinarioId);
            }
        }
        LocalDate dataResgate = lerDataObrigatoria(scanner, "Data do resgate");
        String statusAdocao = lerStatusAdocao(scanner);

        Animal animal = animalService.cadastrarAnimal(nome, especie, raca, idade, porte, estadoSaude,
                dataResgate, null, null, statusAdocao);
        notificacaoService.notificarNovoAnimal(animal);
        System.out.println("Animal cadastrado com sucesso! ID: " + animal.getId() + ".");
    }

    private static void listarAnimais(AnimalService animalService) {
        System.out.println("\n--- Animais cadastrados ---");
        exibirAnimais(animalService.listarTodos());
    }

    private static void exibirAnimais(List<Animal> animais) {
        if (animais.isEmpty()) {
            System.out.println("Nenhum animal cadastrado.");
            return;
        }
        for (Animal animal : animais) {
            String dataResgate = animal.getDataResgate() == null
                    ? "—" : animal.getDataResgate().format(FORMATO_DATA);
            System.out.printf("%s | %s | %s | %s | Idade: %d | Porte: %s | Saúde: %s | Resgate: %s | Adoção: %s%n",
                    animal.getId(), animal.getNome(), animal.getEspecie(), animal.getRaca(),
                    animal.getIdade(), animal.getPorte(), animal.getEstadoSaude(),
                    dataResgate, animal.getStatusAdocao());
        }
    }

    private static void buscarAnimais(Scanner scanner, AnimalService animalService) {
        System.out.println("\n--- Buscar e filtrar animais (Enter ignora o filtro) ---");
        String termo = lerTexto(scanner, "Nome ou ID: ");
        String especie = lerTexto(scanner, "Espécie: ");
        String raca = lerTexto(scanner, "Raça: ");
        String porte = lerTexto(scanner, "Porte: ");
        String estadoSaude = lerTexto(scanner, "Estado de saúde: ");
        String statusAdocao = lerTexto(scanner, "Status de adoção (DISPONIVEL/INDISPONIVEL): ");
        exibirAnimais(animalService.buscar(termo, especie, raca, porte, estadoSaude, statusAdocao));
    }

    private static void editarAnimal(Scanner scanner, AnimalService animalService,
            UsuarioService usuarioService, Usuario usuarioAtivo) {
        System.out.println("\n--- Editar animal ---");
        String id = lerTexto(scanner, "ID do animal: ");
        Animal animal = animalService.buscarPorId(id);
        if (animal.isArquivado()) {
            throw new IllegalStateException("Animais removidos não podem ser editados.");
        }

        String nome = lerTexto(scanner, "Nome [" + animal.getNome() + "]: ");
        String especie = lerTexto(scanner, "Espécie [" + animal.getEspecie() + "]: ");
        String raca = lerTexto(scanner, "Raça [" + animal.getRaca() + "]: ");
        int idade = lerInteiroOpcional(scanner, "Idade [" + animal.getIdade() + "] (Enter mantém): ",
                animal.getIdade());
        String porte = lerTexto(scanner, "Porte [" + animal.getPorte() + "]: ");
        String estadoSaude = lerTexto(scanner, "Estado de saúde [" + animal.getEstadoSaude() + "]: ");
        if (usuarioAtivo instanceof Administrador && !estadoSaude.isBlank()
                && !normalizar(estadoSaude).equals(normalizar(animal.getEstadoSaude()))) {
            String veterinarioId = lerTexto(scanner, "ID do veterinário responsável pela alteração médica: ");
            usuarioService.buscarVeterinario(veterinarioId);
        }
        LocalDate dataResgate = lerDataOpcional(scanner, "Data do resgate", animal.getDataResgate());
        String statusAdocao = "ADOTADO".equalsIgnoreCase(animal.getStatusAdocao())
                ? animal.getStatusAdocao() : lerStatusAdocao(scanner);

        animalService.atualizarAnimal(id,
                nome.isBlank() ? animal.getNome() : nome,
                especie.isBlank() ? animal.getEspecie() : especie,
                raca.isBlank() ? animal.getRaca() : raca,
                idade,
                porte.isBlank() ? animal.getPorte() : porte,
                estadoSaude.isBlank() ? animal.getEstadoSaude() : estadoSaude,
                dataResgate, animal.getImagem(), animal.getVoluntarioResponsavelId(), statusAdocao);
        System.out.println("Animal " + id + " atualizado com sucesso.");
    }

    private static void removerAnimal(Scanner scanner, AnimalService animalService) {
        System.out.println("\n--- Remover animal ---");
        String id = lerTexto(scanner, "ID do animal: ");
        animalService.removerAnimal(id);
        System.out.println("Animal " + id
                + " removido da lista ativa com sucesso. O histórico foi preservado.");
    }

    private static String lerStatusAdocao(Scanner scanner) {
        while (true) {
            String status = lerTexto(scanner, "Status de adoção (DISPONIVEL/INDISPONIVEL): ")
                    .toUpperCase(java.util.Locale.ROOT);
            if ("DISPONIVEL".equals(status) || "INDISPONIVEL".equals(status)) {
                return status;
            }
            System.out.println("Escolha DISPONIVEL ou INDISPONIVEL. ADOTADO só é definido após aprovação.");
        }
    }

    private static void solicitarAdocao(Scanner scanner, AnimalService animalService,
            AdocaoService adocaoService, NotificacaoService notificacaoService) {
        System.out.println("\n--- Solicitação de adoção ---");
        String animalId = lerTexto(scanner, "ID do animal: ");
        Animal animal = animalService.buscarPorId(animalId);
        String nome = lerTexto(scanner, "Nome do cliente/adotante: ");
        String contato = lerTexto(scanner, "Contato do cliente/adotante: ");
        SolicitacaoAdocao solicitacao = adocaoService.solicitarAdocao(animal, nome, contato);
        notificacaoService.notificarNovaSolicitacao(solicitacao);
        System.out.println("Solicitação de adoção registrada com sucesso! ID: "
                + solicitacao.getId() + ". Status: " + solicitacao.getStatus() + ".");
    }

    private static void analisarSolicitacao(Scanner scanner, Administrador administrador,
            AdocaoService adocaoService, NotificacaoService notificacaoService) {
        listarSolicitacoes(adocaoService);
        String solicitacaoId = lerTexto(scanner, "ID da solicitação para analisar: ");
        int decisao = lerInteiro(scanner, "Decisão (1 - aprovar, 2 - rejeitar): ");
        String status = switch (decisao) {
            case 1 -> "APROVADO";
            case 2 -> "REJEITADO";
            default -> throw new IllegalArgumentException("Escolha 1 para aprovar ou 2 para rejeitar.");
        };
        adocaoService.atualizarSolicitacao(solicitacaoId, status, administrador)
                .forEach(notificacaoService::notificarAtualizacaoSolicitacao);
        System.out.println("Solicitação " + solicitacaoId
                + " analisada com sucesso. Novo status: " + status + ".");
    }

    private static void registrarDevolucao(Scanner scanner, Administrador administrador,
            AnimalService animalService, AdocaoService adocaoService, NotificacaoService notificacaoService) {
        System.out.println("\n--- Registrar devolução de adoção ---");
        listarHistoricoAdocoes(adocaoService);
        String solicitacaoId = lerTexto(scanner, "ID da adoção ativa: ");
        String motivo = lerTexto(scanner, "Motivo da devolução: ");
        SolicitacaoAdocao solicitacao = adocaoService.registrarDevolucao(
                solicitacaoId, motivo, administrador, animalService);
        notificacaoService.notificarAtualizacaoSolicitacao(solicitacao);
        System.out.println("Devolução da adoção " + solicitacao.getId() + " registrada com sucesso.");
    }

    private static void listarHistoricoAdocoes(AdocaoService adocaoService) {
        System.out.println("\n--- Histórico de adoções ---");
        List<SolicitacaoAdocao> historico = adocaoService.getHistoricoAdocoes();
        if (historico.isEmpty()) {
            System.out.println("Nenhuma adoção aprovada registrada.");
            return;
        }
        for (SolicitacaoAdocao solicitacao : historico) {
            System.out.printf("%s | Animal: %s (%s) | Adotante: %s | Contato: %s | Adoção: %s | Status: %s",
                    solicitacao.getId(), solicitacao.getAnimal().getNome(), solicitacao.getAnimal().getId(),
                    solicitacao.getAdotanteNome(), solicitacao.getAdotanteContato(),
                    solicitacao.getDataAdocao().format(FORMATO_DATA), solicitacao.getStatus());
            if (solicitacao.getDataDevolucao() != null) {
                System.out.printf(" | Devolução: %s | Motivo: %s",
                        solicitacao.getDataDevolucao().format(FORMATO_DATA), solicitacao.getMotivoDevolucao());
            }
            System.out.println();
        }
    }

    private static void registrarAtendimento(Scanner scanner, AnimalService animalService,
            Usuario usuarioAtivo, SaudeService saudeService) {
        System.out.println("\n--- Registro de atendimento médico ---");
        String animalId = lerTexto(scanner, "ID do animal: ");
        animalService.buscarPorId(animalId);
        Veterinario veterinario;
        if (usuarioAtivo instanceof Veterinario veterinarioAutenticado) {
            veterinario = veterinarioAutenticado;
        } else {
            throw new IllegalStateException("Somente veterinários podem registrar atendimento médico.");
        }
        String descricao = lerTexto(scanner, "Descrição do atendimento: ");
        int tipoOpcao = lerInteiro(scanner, "Tipo (1 - vacina, 2 - consulta, 3 - tratamento): ");
        String tipo = switch (tipoOpcao) {
            case 1 -> "VACINA";
            case 2 -> "CONSULTA";
            case 3 -> "TRATAMENTO";
            default -> throw new IllegalArgumentException("Tipo de atendimento inválido.");
        };
        LocalDate data = lerData(scanner, "Data do atendimento");
        LocalDate dataVencimento = "VACINA".equals(tipo)
                ? lerDataObrigatoria(scanner, "Data de vencimento da vacina") : null;
        RegistroMedico registro = saudeService.registrarAtendimento(
                animalId, veterinario, descricao, tipo, data, dataVencimento);
        System.out.println("Atendimento veterinário registrado com sucesso! ID: "
                + registro.getId() + ".");
    }

    private static void listarNotificacoes(Usuario usuario,
            AtividadeService atividadeService, SaudeService saudeService, NotificacaoService notificacaoService) {
        List<Notificacao> notificacoes = notificacaoService.listar(usuario, atividadeService, saudeService);
        System.out.println("\n--- Notificações ---");
        if (notificacoes.isEmpty()) {
            System.out.println("Nenhuma notificação disponível.");
            return;
        }
        for (Notificacao notificacao : notificacoes) {
            System.out.printf("%s | %s | %s | %s | %s%n",
                    notificacao.getData().format(FORMATO_DATA), notificacao.getTipo(),
                    notificacao.getTitulo(), notificacao.getMensagem(), notificacao.getReferenciaId());
        }
    }

    private static void publicarAviso(Scanner scanner, Administrador administrador,
            NotificacaoService notificacaoService) {
        String titulo = lerTexto(scanner, "Título do aviso: ");
        String mensagem = lerTexto(scanner, "Mensagem: ");
        notificacaoService.publicarAviso(titulo, mensagem, administrador);
        System.out.println("Aviso publicado com sucesso.");
    }

    private static void exibirRelatorioEstatistico(Administrador administrador,
            RelatorioService relatorioService) {
        RelatorioService.Estatisticas estatisticas = relatorioService.gerarEstatisticas();

        System.out.println("\n--- Relatórios e estatísticas ---");
        System.out.println("Animais resgatados: " + estatisticas.animaisResgatados());
        System.out.println("Adoções realizadas: " + estatisticas.adocoesRealizadas());
        System.out.println("Voluntários ativos: " + estatisticas.voluntariosAtivos());

        System.out.println("\nEspécies mais resgatadas:");
        if (estatisticas.especiesMaisResgatadas().isEmpty()) {
            System.out.println("Nenhum resgate registrado.");
        } else {
            estatisticas.especiesMaisResgatadas().forEach(item ->
                    System.out.printf("%s: %d%n", item.especie(), item.quantidade()));
        }

        System.out.println("\nTaxa de adoção mensal (adoções / resgates do mês):");
        if (estatisticas.taxaAdocaoMensal().isEmpty()) {
            System.out.println("Ainda não há dados mensais.");
        } else {
            estatisticas.taxaAdocaoMensal().forEach(item -> System.out.printf(
                    "%s | Resgates: %d | Adoções: %d | Taxa: %s%n",
                    item.mes(), item.animaisResgatados(), item.adocoesRealizadas(),
                    item.taxaPercentual() == null ? "N/A" : item.taxaPercentual() + "%"));
        }

        System.out.println("\nAnimais em tratamento:");
        if (estatisticas.animaisEmTratamento().isEmpty()) {
            System.out.println("Nenhum animal em tratamento.");
        } else {
            estatisticas.animaisEmTratamento().forEach(animal -> System.out.printf(
                    "%s | %s | %s | Saúde: %s%n",
                    animal.getId(), animal.getNome(), animal.getEspecie(), animal.getEstadoSaude()));
        }
    }

    private static void cadastrarUsuario(Scanner scanner, UsuarioService usuarioService,
            AutenticacaoService autenticacaoService, Administrador administradorResponsavel,
            ArmazenamentoContas armazenamentoContas) throws IOException {
        System.out.println("\n--- Cadastro de usuário ---");
        String tiposDisponiveis = administradorResponsavel.isPrincipal()
                ? "Tipo (1 - voluntário, 2 - veterinário, 3 - administrador): "
                : "Tipo (1 - voluntário, 2 - veterinário): ";
        int tipoOpcao = lerInteiro(scanner, tiposDisponiveis);
        String tipo = switch (tipoOpcao) {
            case 1 -> "Voluntário";
            case 2 -> "Veterinário";
            case 3 -> "Administrador";
            default -> throw new IllegalArgumentException("Tipo de usuário inválido.");
        };
        if ("Administrador".equals(tipo) && !administradorResponsavel.isPrincipal()) {
            throw new IllegalStateException(
                    "Somente o administrador principal pode criar outros administradores.");
        }
        Usuario usuario = cadastrarContaPorTipo(scanner, usuarioService, autenticacaoService, tipo, false);
        armazenamentoContas.salvar(usuarioService, autenticacaoService);
        exibirMensagemCadastro(usuario);
    }

    private static void aprovarVeterinario(Scanner scanner, UsuarioService usuarioService,
            AutenticacaoService autenticacaoService, ArmazenamentoContas armazenamentoContas)
            throws IOException {
        List<Veterinario> pendentes = usuarioService.listarTodos().stream()
                .filter(Veterinario.class::isInstance)
                .map(Veterinario.class::cast)
                .filter(veterinario -> !veterinario.isAprovado())
                .toList();
        if (pendentes.isEmpty()) {
            System.out.println("Não há cadastros de veterinários aguardando aprovação.");
            return;
        }
        System.out.println("\n--- Veterinários aguardando aprovação ---");
        for (Veterinario veterinario : pendentes) {
            System.out.printf("%s | %s | %s | CRMV: %s%n",
                    veterinario.getId(), veterinario.getNome(), veterinario.getEmail(), veterinario.getCrmv());
        }
        String id = lerTexto(scanner, "ID do veterinário a aprovar: ");
        Usuario usuario = usuarioService.buscarPorId(id);
        if (!(usuario instanceof Veterinario veterinario)) {
            throw new IllegalArgumentException("O usuário selecionado não é um veterinário.");
        }
        if (veterinario.isAprovado()) {
            throw new IllegalStateException("Este veterinário já foi aprovado.");
        }
        veterinario.setAprovado(true);
        armazenamentoContas.salvar(usuarioService, autenticacaoService);
        System.out.println("Veterinário " + veterinario.getNome()
                + " aprovado com sucesso. Agora já pode fazer login.");
    }

    private static void listarUsuarios(UsuarioService usuarioService) {
        System.out.println("\n--- Usuários cadastrados ---");
        if (usuarioService.listarTodos().isEmpty()) {
            System.out.println("Nenhum usuário cadastrado.");
            return;
        }
        for (Usuario usuario : usuarioService.listarTodos()) {
            String perfil = usuario instanceof Administrador administrador && administrador.isPrincipal()
                    ? "Administrador principal" : usuario.getClass().getSimpleName();
            System.out.printf("%s | %s | %s | %s%n",
                    usuario.getId(), perfil,
                    usuario.getNome(), usuario.getEmail());
        }
    }

    private static void cadastrarAtividade(Scanner scanner, Administrador administrador,
            AtividadeService atividadeService) {
        System.out.println("\n--- Cadastro de atividade ---");
        Atividade.Tipo tipo = lerTipoAtividade(scanner);
        String titulo = lerTexto(scanner, "Título: ");
        String descricao = lerTexto(scanner, "Descrição: ");
        LocalDate data = lerData(scanner, "Data da atividade");
        Atividade atividade = atividadeService.cadastrarAtividade(tipo, titulo, descricao, data, administrador);
        System.out.println("Atividade cadastrada com sucesso! ID: " + atividade.getId() + ".");
    }

    private static Atividade.Tipo lerTipoAtividade(Scanner scanner) {
        System.out.println("""
                Tipo de atividade:
                1. Mutirão de limpeza
                2. Campanha de vacinação
                3. Feira de adoção
                4. Evento beneficente
                """);
        return switch (lerInteiro(scanner, "Escolha o tipo: ")) {
            case 1 -> Atividade.Tipo.MUTIRAO_LIMPEZA;
            case 2 -> Atividade.Tipo.CAMPANHA_VACINACAO;
            case 3 -> Atividade.Tipo.FEIRA_ADOCAO;
            case 4 -> Atividade.Tipo.EVENTO_BENEFICENTE;
            default -> throw new IllegalArgumentException("Escolha um dos tipos de atividade listados.");
        };
    }

    private static void inscreverVoluntario(Scanner scanner, Voluntario voluntario,
            AtividadeService atividadeService) {
        System.out.println("\n--- Inscrição em atividade ---");
        String atividadeId = lerTexto(scanner, "ID da atividade: ");
        atividadeService.inscreverVoluntario(atividadeId, voluntario);
        System.out.println("Inscrição na atividade concluída com sucesso.");
    }

    private static void listarSolicitacoes(AdocaoService adocaoService) {
        System.out.println("\n--- Solicitações de adoção ---");
        if (adocaoService.listarSolicitacoes().isEmpty()) {
            System.out.println("Nenhuma solicitação cadastrada.");
            return;
        }
        for (SolicitacaoAdocao solicitacao : adocaoService.listarSolicitacoes()) {
            System.out.printf("%s | Solicitada em: %s | Animal: %s - %s | Cliente: %s | Contato: %s | Status: %s%n",
                    solicitacao.getId(), solicitacao.getDataSolicitacao().format(FORMATO_DATA),
                    solicitacao.getAnimal().getId(), solicitacao.getAnimal().getNome(), solicitacao.getAdotanteNome(),
                    solicitacao.getAdotanteContato(), solicitacao.getStatus());
        }
    }

    private static void listarAtividades(AtividadeService atividadeService) {
        System.out.println("\n--- Atividades cadastradas ---");
        if (atividadeService.listarAtividades().isEmpty()) {
            System.out.println("Nenhuma atividade cadastrada.");
            return;
        }
        for (Atividade atividade : atividadeService.listarAtividades()) {
            System.out.printf("%s | %s | %s | %s | %s | Inscritos: %d | %s%n",
                    atividade.getId(), nomeTipoAtividade(atividade.getTipo()), atividade.getTitulo(), atividade.getDescricao(),
                    atividade.getData().format(FORMATO_DATA), atividade.getVoluntariosInscritosId().size(),
                    atividade.getData().isBefore(LocalDate.now()) ? "Inscrições encerradas" : "Inscrições abertas");
        }
    }

    private static String nomeTipoAtividade(Atividade.Tipo tipo) {
        return switch (tipo) {
            case MUTIRAO_LIMPEZA -> "Mutirão de limpeza";
            case CAMPANHA_VACINACAO -> "Campanha de vacinação";
            case FEIRA_ADOCAO -> "Feira de adoção";
            case EVENTO_BENEFICENTE -> "Evento beneficente";
        };
    }

    private static String lerTexto(Scanner scanner, String mensagem) {
        System.out.print(mensagem);
        if (!scanner.hasNextLine()) {
            throw new EntradaEncerradaException();
        }
        return scanner.nextLine().trim();
    }

    private static String normalizar(String valor) {
        return java.text.Normalizer.normalize(valor == null ? "" : valor,
                java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toLowerCase(java.util.Locale.ROOT);
    }

    private static int lerInteiro(Scanner scanner, String mensagem) {
        while (true) {
            String valor = lerTexto(scanner, mensagem);
            try {
                return Integer.parseInt(valor);
            } catch (NumberFormatException exception) {
                System.out.println("Digite um número inteiro válido.");
            }
        }
    }

    private static int lerInteiroOpcional(Scanner scanner, String mensagem, int atual) {
        while (true) {
            String valor = lerTexto(scanner, mensagem);
            if (valor.isBlank()) {
                return atual;
            }
            try {
                return Integer.parseInt(valor);
            } catch (NumberFormatException exception) {
                System.out.println("Digite um número inteiro válido ou pressione Enter para manter o atual.");
            }
        }
    }

    private static LocalDate lerDataObrigatoria(Scanner scanner, String campo) {
        while (true) {
            String valor = lerTexto(scanner, campo + " (DD/MM/AAAA; obrigatória): ");
            if (valor.isBlank()) {
                System.out.println("Informe " + campo.toLowerCase(java.util.Locale.ROOT) + ".");
                continue;
            }
            try {
                return LocalDate.parse(valor, FORMATO_DATA);
            } catch (DateTimeParseException exception) {
                System.out.println("Data inválida. Digite no formato DD/MM/AAAA.");
            }
        }
    }

    private static LocalDate lerDataOpcional(Scanner scanner, String campo, LocalDate atual) {
        String dataAtual = atual == null ? "" : atual.format(FORMATO_DATA);
        while (true) {
            String valor = lerTexto(scanner, campo + " [" + dataAtual + "] (Enter mantém): ");
            if (valor.isBlank() && atual != null) {
                return atual;
            }
            if (valor.isBlank()) {
                System.out.println("Informe a data de resgate.");
                continue;
            }
            try {
                return LocalDate.parse(valor, FORMATO_DATA);
            } catch (DateTimeParseException exception) {
                System.out.println("Data inválida. Digite no formato DD/MM/AAAA.");
            }
        }
    }

    private static LocalDate lerData(Scanner scanner, String campo) {
        while (true) {
            String valor = lerTexto(scanner,
                    campo + " (DD/MM/AAAA; deixe em branco para usar a data de hoje): ");
            if (valor.isBlank()) {
                return LocalDate.now();
            }
            try {
                return LocalDate.parse(valor, FORMATO_DATA);
            } catch (DateTimeParseException exception) {
                System.out.println("Data inválida. Digite no formato DD/MM/AAAA.");
            }
        }
    }

    private static class EntradaEncerradaException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
