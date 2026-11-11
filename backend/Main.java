package backend;

import backend.exception.AnimalIndisponivelException;
import backend.exception.VacinaDuplicadaException;
import backend.model.*;
import backend.service.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        AnimalService animalService = new AnimalService();
        UsuarioService usuarioService = new UsuarioService();
        AdocaoService adocaoService = new AdocaoService();
        SaudeService saudeService = new SaudeService();
        AtividadeService atividadeService = new AtividadeService();

        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("=== SISTEMA DE GERENCIAMENTO DE ABRIGOS E RESGATES ===");
            boolean executando = true;

            while (executando) {
                exibirMenu();
                try {
                    int opcao = lerInteiro(scanner, "Escolha uma opção: ");
                    switch (opcao) {
                        case 1 -> cadastrarAnimal(scanner, animalService);
                        case 2 -> listarAnimais(animalService);
                        case 3 -> solicitarAdocao(scanner, animalService, adocaoService);
                        case 4 -> analisarSolicitacao(scanner, usuarioService, adocaoService);
                        case 5 -> registrarAtendimento(scanner, animalService, usuarioService, saudeService);
                        case 6 -> cadastrarUsuario(scanner, usuarioService);
                        case 7 -> listarUsuarios(usuarioService);
                        case 8 -> cadastrarAtividade(scanner, usuarioService, atividadeService);
                        case 9 -> inscreverVoluntario(scanner, usuarioService, atividadeService);
                        case 10 -> listarSolicitacoes(adocaoService);
                        case 11 -> listarAtividades(atividadeService);
                        case 0 -> executando = false;
                        default -> System.out.println("Opção inválida. Tente novamente.");
                    }
                } catch (AnimalIndisponivelException | VacinaDuplicadaException
                        | IllegalArgumentException | NoSuchElementException exception) {
                    System.out.println("Não foi possível concluir a operação: " + exception.getMessage());
                } catch (EntradaEncerradaException exception) {
                    executando = false;
                }
            }
        }

        System.out.println("Sistema encerrado.");
    }

    private static void exibirMenu() {
        System.out.println("""

                --- MENU DO ABRIGO ---
                1. Cadastrar animal
                2. Listar animais
                3. Solicitar adoção
                4. Analisar solicitação de adoção
                5. Registrar atendimento médico
                6. Cadastrar usuário
                7. Listar usuários
                8. Cadastrar atividade
                9. Inscrever voluntário em atividade
                10. Listar solicitações de adoção
                11. Listar atividades
                0. Sair
                """);
    }

    private static void cadastrarAnimal(Scanner scanner, AnimalService animalService) {
        System.out.println("\n--- Cadastro de animal ---");
        String nome = lerTexto(scanner, "Nome: ");
        String especie = lerTexto(scanner, "Espécie: ");
        String raca = lerTexto(scanner, "Raça: ");
        int idade = lerInteiro(scanner, "Idade: ");
        String porte = lerTexto(scanner, "Porte: ");
        String estadoSaude = lerTexto(scanner, "Estado de saúde: ");
        LocalDate dataResgate = lerData(scanner, "Data do resgate");
        String imagem = lerTexto(scanner, "Imagem (opcional): ");

        Animal animal = animalService.cadastrarAnimal(nome, especie, raca, idade, porte, estadoSaude,
                dataResgate, imagem.isBlank() ? null : imagem);
        System.out.println("Animal cadastrado com o ID " + animal.getId() + ".");
    }

    private static void listarAnimais(AnimalService animalService) {
        System.out.println("\n--- Animais cadastrados ---");
        if (animalService.listarTodos().isEmpty()) {
            System.out.println("Nenhum animal cadastrado.");
            return;
        }
        for (Animal animal : animalService.listarTodos()) {
            System.out.printf("%s | %s | %s | %s | Saúde: %s | Adoção: %s%n",
                    animal.getId(), animal.getNome(), animal.getEspecie(), animal.getRaca(),
                    animal.getEstadoSaude(), animal.getStatusAdocao());
        }
    }

    private static void solicitarAdocao(Scanner scanner, AnimalService animalService,
            AdocaoService adocaoService) {
        System.out.println("\n--- Solicitação de adoção ---");
        String animalId = lerTexto(scanner, "ID do animal: ");
        Animal animal = animalService.buscarPorId(animalId);
        String nome = lerTexto(scanner, "Nome do cliente/adotante: ");
        String contato = lerTexto(scanner, "Contato do cliente/adotante: ");
        SolicitacaoAdocao solicitacao = adocaoService.solicitarAdocao(animal, nome, contato);
        System.out.println("Solicitação " + solicitacao.getId() + " registrada com status "
                + solicitacao.getStatus() + ".");
    }

    private static void analisarSolicitacao(Scanner scanner, UsuarioService usuarioService,
            AdocaoService adocaoService) {
        listarSolicitacoes(adocaoService);
        String solicitacaoId = lerTexto(scanner, "ID da solicitação para analisar: ");
        int decisao = lerInteiro(scanner, "Decisão (1 - aprovar, 2 - rejeitar): ");
        String status = switch (decisao) {
            case 1 -> "APROVADO";
            case 2 -> "REJEITADO";
            default -> throw new IllegalArgumentException("Escolha 1 para aprovar ou 2 para rejeitar.");
        };
        String administradorId = lerTexto(scanner, "ID do administrador responsável: ");
        Administrador administrador = usuarioService.buscarAdministrador(administradorId);
        adocaoService.atualizarSolicitacao(solicitacaoId, status, administrador);
        System.out.println("Solicitação " + solicitacaoId + " atualizada para " + status + ".");
    }

    private static void registrarAtendimento(Scanner scanner, AnimalService animalService,
            UsuarioService usuarioService, SaudeService saudeService) {
        System.out.println("\n--- Registro de atendimento médico ---");
        String animalId = lerTexto(scanner, "ID do animal: ");
        animalService.buscarPorId(animalId);
        String veterinarioId = lerTexto(scanner, "ID do veterinário: ");
        Veterinario veterinario = usuarioService.buscarVeterinario(veterinarioId);
        String descricao = lerTexto(scanner, "Descrição do atendimento: ");
        int tipoOpcao = lerInteiro(scanner, "Tipo (1 - vacina, 2 - consulta, 3 - tratamento): ");
        String tipo = switch (tipoOpcao) {
            case 1 -> "VACINA";
            case 2 -> "CONSULTA";
            case 3 -> "TRATAMENTO";
            default -> throw new IllegalArgumentException("Tipo de atendimento inválido.");
        };
        LocalDate data = lerData(scanner, "Data do atendimento");
        RegistroMedico registro = saudeService.registrarAtendimento(
                animalId, veterinario, descricao, tipo, data);
        System.out.println("Atendimento registrado com o ID " + registro.getId() + ".");
    }

    private static void cadastrarUsuario(Scanner scanner, UsuarioService usuarioService) {
        System.out.println("\n--- Cadastro de usuário ---");
        int tipoOpcao = lerInteiro(scanner, "Tipo (1 - voluntário, 2 - veterinário, 3 - administrador): ");
        String tipo = switch (tipoOpcao) {
            case 1 -> "Voluntário";
            case 2 -> "Veterinário";
            case 3 -> "Administrador";
            default -> throw new IllegalArgumentException("Tipo de usuário inválido.");
        };
        String nome = lerTexto(scanner, "Nome: ");
        String email = lerTexto(scanner, "E-mail: ");
        String telefone = lerTexto(scanner, "Telefone: ");
        String crmv = tipo.equals("Veterinário") ? lerTexto(scanner, "CRMV: ") : "";
        String nivelAcesso = tipo.equals("Administrador") ? lerTexto(scanner, "Nível de acesso: ") : "";

        Usuario usuario = usuarioService.cadastrar(tipo, nome, email, telefone, crmv, nivelAcesso);
        System.out.println("Usuário cadastrado com o ID " + usuario.getId() + ".");
    }

    private static void listarUsuarios(UsuarioService usuarioService) {
        System.out.println("\n--- Usuários cadastrados ---");
        if (usuarioService.listarTodos().isEmpty()) {
            System.out.println("Nenhum usuário cadastrado.");
            return;
        }
        for (Usuario usuario : usuarioService.listarTodos()) {
            System.out.printf("%s | %s | %s | %s%n",
                    usuario.getId(), usuario.getClass().getSimpleName(),
                    usuario.getNome(), usuario.getEmail());
        }
    }

    private static void cadastrarAtividade(Scanner scanner, UsuarioService usuarioService,
            AtividadeService atividadeService) {
        System.out.println("\n--- Cadastro de atividade ---");
        String administradorId = lerTexto(scanner, "ID do administrador responsável: ");
        Administrador administrador = usuarioService.buscarAdministrador(administradorId);
        String titulo = lerTexto(scanner, "Título: ");
        String descricao = lerTexto(scanner, "Descrição: ");
        LocalDate data = lerData(scanner, "Data da atividade");
        Atividade atividade = atividadeService.cadastrarAtividade(titulo, descricao, data, administrador);
        System.out.println("Atividade cadastrada com o ID " + atividade.getId() + ".");
    }

    private static void inscreverVoluntario(Scanner scanner, UsuarioService usuarioService,
            AtividadeService atividadeService) {
        System.out.println("\n--- Inscrição em atividade ---");
        String atividadeId = lerTexto(scanner, "ID da atividade: ");
        String voluntarioId = lerTexto(scanner, "ID do voluntário: ");
        Voluntario voluntario = usuarioService.buscarVoluntario(voluntarioId);
        atividadeService.inscreverVoluntario(atividadeId, voluntario);
        System.out.println("Voluntário inscrito na atividade.");
    }

    private static void listarSolicitacoes(AdocaoService adocaoService) {
        System.out.println("\n--- Solicitações de adoção ---");
        if (adocaoService.listarSolicitacoes().isEmpty()) {
            System.out.println("Nenhuma solicitação cadastrada.");
            return;
        }
        for (SolicitacaoAdocao solicitacao : adocaoService.listarSolicitacoes()) {
            System.out.printf("%s | Animal: %s - %s | Cliente: %s | Contato: %s | Status: %s%n",
                    solicitacao.getId(), solicitacao.getAnimal().getId(),
                    solicitacao.getAnimal().getNome(), solicitacao.getAdotanteNome(),
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
            System.out.printf("%s | %s | %s | %s | Inscritos: %d%n",
                    atividade.getId(), atividade.getTitulo(), atividade.getDescricao(),
                    atividade.getData(), atividade.getVoluntariosInscritosId().size());
        }
    }

    private static String lerTexto(Scanner scanner, String mensagem) {
        System.out.print(mensagem);
        if (!scanner.hasNextLine()) {
            throw new EntradaEncerradaException();
        }
        return scanner.nextLine().trim();
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

    private static LocalDate lerData(Scanner scanner, String campo) {
        while (true) {
            String valor = lerTexto(scanner, campo + " (AAAA-MM-DD; Enter usa a data atual): ");
            if (valor.isBlank()) {
                return LocalDate.now();
            }
            try {
                return LocalDate.parse(valor);
            } catch (DateTimeParseException exception) {
                System.out.println("Data inválida. Use o formato AAAA-MM-DD.");
            }
        }
    }

    private static class EntradaEncerradaException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
