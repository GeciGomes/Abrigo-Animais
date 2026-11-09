package backend.service;

import backend.model.Atividade;
import backend.model.Voluntario;
import java.util.ArrayList;
import java.util.List;

public class AtividadeService {
    private List<Atividade> atividades = new ArrayList<>();

    public void cadastrarAtividade(Atividade atividade) {
        atividades.add(atividade);
    }

    public void inscreverVoluntario(String atividadeId, Voluntario voluntario) {
        Atividade atividade = atividades.stream()
                .filter(a -> a.getId().equals(atividadeId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Atividade não encontrada."));

        if (!atividade.getVoluntariosInscritosId().contains(voluntario.getId())) {
            atividade.getVoluntariosInscritosId().add(voluntario.getId());
            voluntario.getAtividadesInscritas().add(atividade.getTitulo());
        }
    }

    public List<Atividade> listarAtividades() {
        return atividades;
    }
}