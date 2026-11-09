package backend.service;

import backend.exception.AnimalIndisponivelException;
import backend.model.Animal;
import backend.model.SolicitacaoAdocao;
import java.util.ArrayList;
import java.util.List;

public class AdocaoService {
    private List<SolicitacaoAdocao> solicitacoes = new ArrayList<>();
    private List<SolicitacaoAdocao> historicoAdocoes = new ArrayList<>();

    public void solicitarAdocao(String idSolicitacao, Animal animal, String adotanteNome, String adotanteContato) {
        if (!animal.estaDisponivelParaAdocao()) {
            throw new AnimalIndisponivelException("O animal " + animal.getNome() + " não está disponível para adoção.");
        }

        boolean jaAdotado = solicitacoes.stream()
                .anyMatch(s -> s.getAnimal().getId().equals(animal.getId()) && "APROVADO".equalsIgnoreCase(s.getStatus()));
        
        if (jaAdotado) {
            throw new AnimalIndisponivelException("Este animal já foi adotado e não aceita novas solicitações.");
        }

        SolicitacaoAdocao novaSolicitacao = new SolicitacaoAdocao(idSolicitacao, animal, adotanteNome, adotanteContato);
        solicitacoes.add(novaSolicitacao);
    }

    public void aprovarAdocao(String idSolicitacao) {
        SolicitacaoAdocao solicitacao = solicitacoes.stream()
                .filter(s -> s.getId().equals(idSolicitacao))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Solicitação não encontrada."));

        solicitacao.setStatus("APROVADO");
        solicitacao.getAnimal().setStatusAdocao("ADOTADO");
        
        historicoAdocoes.add(solicitacao);
    }

    public List<SolicitacaoAdocao> getHistoricoAdocoes() {
        return historicoAdocoes;
    }
}
