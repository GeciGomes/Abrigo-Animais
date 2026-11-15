package backend.service;

import backend.exception.AcessoNegadoException;
import backend.model.Administrador;
import backend.model.Atividade;
import backend.model.Animal;
import backend.model.Notificacao;
import backend.model.RegistroMedico;
import backend.model.SolicitacaoAdocao;
import backend.model.Usuario;
import backend.model.Veterinario;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class NotificacaoService {
    private static final int DIAS_PROXIMO_EVENTO = 7;
    private static final int DIAS_AVISO_VACINA = 30;
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/uuuu");
    private final List<Notificacao> notificacoes = new ArrayList<>();
    private int proximoId = 1;

    public synchronized void notificarNovoAnimal(Animal animal) {
        adicionar(Notificacao.Tipo.NOVO_ANIMAL, "Novo animal cadastrado",
                animal.getNome() + " (" + animal.getEspecie() + ") está no abrigo.",
                LocalDate.now(), animal.getId(), false);
    }

    public synchronized void notificarNovaSolicitacao(SolicitacaoAdocao solicitacao) {
        adicionar(Notificacao.Tipo.SOLICITACAO_ADOCAO, "Nova solicitação de adoção",
                "A solicitação " + solicitacao.getId() + " para o animal "
                        + solicitacao.getAnimal().getNome() + " aguarda análise.",
                LocalDate.now(), solicitacao.getId(), true);
    }

    public synchronized void notificarAtualizacaoSolicitacao(SolicitacaoAdocao solicitacao) {
        adicionar(Notificacao.Tipo.SOLICITACAO_ADOCAO, "Solicitação de adoção atualizada",
                "A solicitação " + solicitacao.getId() + " do animal "
                        + solicitacao.getAnimal().getNome() + " foi atualizada para "
                        + solicitacao.getStatus() + ".",
                LocalDate.now(), solicitacao.getId(), false);
    }

    public synchronized void publicarAviso(String titulo, String mensagem, Administrador administrador) {
        if (administrador == null) {
            throw new AcessoNegadoException("Apenas um administrador pode publicar avisos.");
        }
        if (titulo == null || titulo.isBlank() || mensagem == null || mensagem.isBlank()) {
            throw new IllegalArgumentException("Informe o título e a mensagem do aviso.");
        }
        adicionar(Notificacao.Tipo.AVISO_GERAL, titulo.trim(), mensagem.trim(), LocalDate.now(), null, false);
    }

    public synchronized List<Notificacao> listar(Usuario usuario, AtividadeService atividadeService,
            SaudeService saudeService) {
        if (usuario == null) {
            throw new AcessoNegadoException("Autenticação obrigatória para consultar notificações.");
        }
        boolean administrador = usuario instanceof Administrador;
        boolean equipeVeterinaria = administrador || usuario instanceof Veterinario;
        List<Notificacao> resultado = new ArrayList<>(notificacoes.stream()
                .filter(notificacao -> !notificacao.isSomenteAdministradores() || administrador)
                .toList());

        LocalDate hoje = LocalDate.now();
        LocalDate limiteEventos = hoje.plusDays(DIAS_PROXIMO_EVENTO);
        for (Atividade atividade : atividadeService.listarAtividades()) {
            if (!atividade.getData().isBefore(hoje) && !atividade.getData().isAfter(limiteEventos)) {
                resultado.add(new Notificacao(
                        "EVENTO-" + atividade.getId(),
                        Notificacao.Tipo.PROXIMO_EVENTO,
                        "Próximo evento: " + atividade.getTitulo(),
                        atividade.getTipo().name() + " em " + atividade.getData().format(FORMATO_DATA) + ".",
                        atividade.getData(),
                        atividade.getId(),
                        false));
            }
        }

        if (equipeVeterinaria) {
            LocalDate limiteVacinas = hoje.plusDays(DIAS_AVISO_VACINA);
            for (RegistroMedico registro : saudeService.listarRegistros()) {
                LocalDate vencimento = registro.getDataVencimento();
                if ("VACINA".equalsIgnoreCase(registro.getTipo())
                        && vencimento != null
                        && !vencimento.isBefore(hoje)
                        && !vencimento.isAfter(limiteVacinas)) {
                    resultado.add(new Notificacao(
                            "VACINA-" + registro.getId(),
                            Notificacao.Tipo.VACINA_PROXIMA_VENCIMENTO,
                            "Vacina próxima do vencimento",
                            registro.getDescricao() + " do animal " + registro.getAnimalId()
                                    + " vence em " + vencimento.format(FORMATO_DATA) + ".",
                            vencimento,
                            registro.getId(),
                            true));
                }
            }
        }

        resultado.sort(Comparator.comparing(Notificacao::getData).reversed()
                .thenComparing(Notificacao::getId));
        return List.copyOf(resultado);
    }

    private void adicionar(Notificacao.Tipo tipo, String titulo, String mensagem, LocalDate data,
            String referenciaId, boolean somenteAdministradores) {
        notificacoes.add(new Notificacao("N" + String.format("%05d", proximoId++), tipo, titulo,
                mensagem, data, referenciaId, somenteAdministradores));
    }
}
