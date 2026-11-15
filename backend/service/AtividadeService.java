package backend.service;

import backend.exception.AcessoNegadoException;
import backend.model.Atividade;
import backend.model.Administrador;
import backend.model.Voluntario;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

public class AtividadeService {
    private final List<Atividade> atividades = new ArrayList<>();

    public synchronized Atividade cadastrarAtividade(Atividade.Tipo tipo, String titulo, String descricao,
            LocalDate data, Administrador administrador) {
        if (administrador == null) {
            throw new AcessoNegadoException("Apenas um administrador cadastrado pode criar atividades.");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("Selecione o tipo da atividade.");
        }
        if (titulo == null || titulo.isBlank() || descricao == null || descricao.isBlank() || data == null) {
            throw new IllegalArgumentException("Título, descrição e data da atividade são obrigatórios.");
        }
        int maiorId = atividades.stream()
                .map(Atividade::getId)
                .filter(id -> id.matches("AT\\d+"))
                .mapToInt(id -> Integer.parseInt(id.substring(2)))
                .max()
                .orElse(0);
        Atividade atividade = new Atividade("AT" + String.format("%03d", maiorId + 1),
                tipo, titulo.trim(), descricao.trim(), data);
        atividades.add(atividade);
        return atividade;
    }

    public synchronized void inscreverVoluntario(String atividadeId, Voluntario voluntario) {
        Atividade atividade = atividades.stream()
                .filter(a -> a.getId().equals(atividadeId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Atividade não encontrada."));

        if (atividade.getData().isBefore(LocalDate.now())) {
            throw new IllegalStateException("As inscrições para esta atividade já foram encerradas.");
        }
        if (voluntario == null) {
            throw new IllegalArgumentException("É necessário selecionar um voluntário cadastrado.");
        }
        if (atividade.getVoluntariosInscritosId().contains(voluntario.getId())) {
            throw new IllegalArgumentException("Este voluntário já está inscrito nesta atividade.");
        }
        atividade.getVoluntariosInscritosId().add(voluntario.getId());
        voluntario.getAtividadesInscritas().add(atividade.getTitulo());
    }

    public synchronized void marcarPresenca(String atividadeId, Voluntario voluntario, Administrador administrador) {
        if (administrador == null) {
            throw new AcessoNegadoException("Apenas um administrador pode registrar presenças.");
        }
        Atividade atividade = atividades.stream()
                .filter(item -> item.getId().equals(atividadeId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Atividade não encontrada."));
        if (atividade.getData().isAfter(LocalDate.now())) {
            throw new IllegalStateException("A presença só pode ser registrada após a data da atividade.");
        }
        if (voluntario == null) {
            throw new IllegalArgumentException("É necessário selecionar um voluntário cadastrado.");
        }
        String voluntarioId = voluntario.getId();
        if (!atividade.getVoluntariosInscritosId().contains(voluntarioId)) {
            throw new IllegalArgumentException("O voluntário não está inscrito nesta atividade.");
        }
        if (atividade.getVoluntariosPresentesId().contains(voluntarioId)) {
            throw new IllegalArgumentException("A presença deste voluntário já foi registrada.");
        }
        atividade.getVoluntariosPresentesId().add(voluntarioId);
        voluntario.getHistoricoParticipacoes().add(atividade.getTitulo());
    }

    public synchronized List<Atividade> listarAtividades() {
        return List.copyOf(atividades);
    }
}