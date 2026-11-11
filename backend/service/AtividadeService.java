package backend.service;

import backend.model.Atividade;
import backend.model.Administrador;
import backend.model.Voluntario;
import java.util.ArrayList;
import java.util.List;

public class AtividadeService {
    private List<Atividade> atividades = new ArrayList<>();

    public void cadastrarAtividade(Atividade atividade) {
        atividades.add(atividade);
    }

    public Atividade cadastrarAtividade(String titulo, String descricao, java.time.LocalDate data,
            Administrador administrador) {
        if (administrador == null) {
            throw new backend.exception.AcessoNegadoException("Apenas um administrador cadastrado pode criar atividades.");
        }
        int maiorId = atividades.stream()
                .map(Atividade::getId)
                .filter(id -> id.matches("AT\\d+"))
                .mapToInt(id -> Integer.parseInt(id.substring(2)))
                .max()
                .orElse(0);
        Atividade atividade = new Atividade("AT" + String.format("%03d", maiorId + 1), titulo, descricao, data);
        atividades.add(atividade);
        return atividade;
    }

    public void inscreverVoluntario(String atividadeId, Voluntario voluntario) {
        Atividade atividade = atividades.stream()
                .filter(a -> a.getId().equals(atividadeId))
                .findFirst()
                .orElseThrow(() -> new java.util.NoSuchElementException("Atividade não encontrada."));

        if (voluntario == null) {
            throw new IllegalArgumentException("É necessário selecionar um voluntário cadastrado.");
        }
        if (atividade.getVoluntariosInscritosId().contains(voluntario.getId())) {
            throw new IllegalArgumentException("Este voluntário já está inscrito nesta atividade.");
        }
        atividade.getVoluntariosInscritosId().add(voluntario.getId());
        voluntario.getAtividadesInscritas().add(atividade.getTitulo());
    }

    public List<Atividade> listarAtividades() {
        return atividades;
    }
}