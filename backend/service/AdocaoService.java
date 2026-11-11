package backend.service;

import backend.exception.AnimalIndisponivelException;
import backend.exception.AcessoNegadoException;
import backend.model.Administrador;
import backend.model.Animal;
import backend.model.SolicitacaoAdocao;
import java.util.ArrayList;
import java.util.List;

public class AdocaoService {
    private List<SolicitacaoAdocao> solicitacoes = new ArrayList<>();
    private List<SolicitacaoAdocao> historicoAdocoes = new ArrayList<>();

    public SolicitacaoAdocao solicitarAdocao(Animal animal, String adotanteNome, String adotanteContato) {
        if (!animal.estaDisponivelParaAdocao()) {
            throw new AnimalIndisponivelException("O animal " + animal.getNome() + " não está disponível para adoção.");
        }

        boolean jaAdotado = solicitacoes.stream()
                .anyMatch(s -> s.getAnimal().getId().equals(animal.getId()) && "APROVADO".equalsIgnoreCase(s.getStatus()));
        
        if (jaAdotado) {
            throw new AnimalIndisponivelException("Este animal já foi adotado e não aceita novas solicitações.");
        }

        SolicitacaoAdocao novaSolicitacao = new SolicitacaoAdocao(proximoId(), animal, adotanteNome, adotanteContato);
        solicitacoes.add(novaSolicitacao);
        return novaSolicitacao;
    }

    public void atualizarSolicitacao(String idSolicitacao, String decisao, Administrador administrador) {
        if (administrador == null) {
            throw new AcessoNegadoException("Apenas um administrador cadastrado pode analisar solicitações.");
        }
        SolicitacaoAdocao solicitacao = solicitacoes.stream()
                .filter(s -> s.getId().equals(idSolicitacao))
                .findFirst()
                .orElseThrow(() -> new java.util.NoSuchElementException("Solicitação não encontrada."));

        if (!"PENDENTE".equals(solicitacao.getStatus())) {
            throw new IllegalArgumentException("Esta solicitação já foi analisada.");
        }
        if ("APROVADO".equals(decisao)) {
            if (!solicitacao.getAnimal().estaDisponivelParaAdocao()) {
                throw new AnimalIndisponivelException("O animal não está mais disponível para adoção.");
            }
            solicitacao.setStatus("APROVADO");
            solicitacao.getAnimal().setStatusAdocao("ADOTADO");
            solicitacoes.stream()
                    .filter(outra -> !outra.getId().equals(idSolicitacao)
                            && outra.getAnimal().getId().equals(solicitacao.getAnimal().getId())
                            && "PENDENTE".equals(outra.getStatus()))
                    .forEach(outra -> outra.setStatus("REJEITADO"));
            historicoAdocoes.add(solicitacao);
        } else if ("REJEITADO".equals(decisao)) {
            solicitacao.setStatus("REJEITADO");
        } else {
            throw new IllegalArgumentException("A decisão deve ser APROVADO ou REJEITADO.");
        }
    }

    public List<SolicitacaoAdocao> listarSolicitacoes() {
        return solicitacoes;
    }

    public List<SolicitacaoAdocao> getHistoricoAdocoes() {
        return historicoAdocoes;
    }

    private String proximoId() {
        int maiorId = solicitacoes.stream()
                .map(SolicitacaoAdocao::getId)
                .filter(id -> id.matches("S\\d+"))
                .mapToInt(id -> Integer.parseInt(id.substring(1)))
                .max()
                .orElse(0);
        return "S" + String.format("%03d", maiorId + 1);
    }
}
