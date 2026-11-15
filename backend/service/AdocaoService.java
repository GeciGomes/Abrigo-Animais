package backend.service;

import backend.exception.AnimalIndisponivelException;
import backend.exception.AcessoNegadoException;
import backend.model.Administrador;
import backend.model.Animal;
import backend.model.SolicitacaoAdocao;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.util.Locale;

public class AdocaoService {
    private final List<SolicitacaoAdocao> solicitacoes = new ArrayList<>();
    private final List<SolicitacaoAdocao> historicoAdocoes = new ArrayList<>();

    public synchronized SolicitacaoAdocao solicitarAdocao(Animal animal, String adotanteNome, String adotanteContato) {
        if (animal == null) {
            throw new IllegalArgumentException("Informe um animal válido para a solicitação.");
        }
        if (!animal.estaDisponivelParaAdocao()) {
            throw new AnimalIndisponivelException("O animal " + animal.getNome() + " não está disponível para adoção.");
        }

        boolean jaAdotado = solicitacoes.stream()
                .anyMatch(s -> s.getAnimal().getId().equals(animal.getId())
                        && "APROVADO".equalsIgnoreCase(s.getStatus()));
        if (jaAdotado) {
            throw new AnimalIndisponivelException("Este animal já foi adotado e não aceita novas solicitações.");
        }

        if (adotanteNome == null || adotanteNome.isBlank()
                || adotanteContato == null || adotanteContato.isBlank()) {
            throw new IllegalArgumentException("Informe o nome e o contato do candidato à adoção.");
        }
        SolicitacaoAdocao novaSolicitacao = new SolicitacaoAdocao(proximoId(), animal,
                adotanteNome.trim(), adotanteContato.trim());
        solicitacoes.add(novaSolicitacao);
        return novaSolicitacao;
    }

    public synchronized List<SolicitacaoAdocao> atualizarSolicitacao(
            String idSolicitacao, String decisao, Administrador administrador) {
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
        String decisaoNormalizada = decisao == null ? "" : decisao.trim().toUpperCase(Locale.ROOT);
        List<SolicitacaoAdocao> atualizadas = new ArrayList<>();
        if ("APROVADO".equals(decisaoNormalizada)) {
            if (!solicitacao.getAnimal().estaDisponivelParaAdocao()) {
                throw new AnimalIndisponivelException("O animal não está mais disponível para adoção.");
            }
            LocalDate dataDecisao = LocalDate.now();
            solicitacao.setDataDecisao(dataDecisao);
            solicitacao.setDataAdocao(dataDecisao);
            solicitacao.setStatus("APROVADO");
            solicitacao.getAnimal().setStatusAdocao("ADOTADO");
            atualizadas.add(solicitacao);
            solicitacoes.stream()
                    .filter(outra -> !outra.getId().equals(idSolicitacao)
                            && outra.getAnimal().getId().equals(solicitacao.getAnimal().getId())
                            && "PENDENTE".equals(outra.getStatus()))
                    .forEach(outra -> {
                        outra.setStatus("REJEITADO");
                        outra.setDataDecisao(dataDecisao);
                        atualizadas.add(outra);
                    });
            historicoAdocoes.add(solicitacao);
        } else if ("REJEITADO".equals(decisaoNormalizada)) {
            solicitacao.setStatus("REJEITADO");
            solicitacao.setDataDecisao(LocalDate.now());
            atualizadas.add(solicitacao);
        } else {
            throw new IllegalArgumentException("A decisão deve ser APROVADO ou REJEITADO.");
        }
        return List.copyOf(atualizadas);
    }

    public synchronized SolicitacaoAdocao registrarDevolucao(String idSolicitacao, String motivo,
            Administrador administrador, AnimalService animalService) {
        if (administrador == null) {
            throw new AcessoNegadoException("Apenas um administrador pode registrar uma devolução.");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("Informe o motivo da devolução.");
        }
        SolicitacaoAdocao solicitacao = buscarSolicitacao(idSolicitacao);
        if (!"APROVADO".equals(solicitacao.getStatus())) {
            throw new IllegalStateException("Somente uma adoção ativa pode ser devolvida.");
        }

        animalService.registrarDevolucao(solicitacao.getAnimal().getId());
        solicitacao.setStatus("DEVOLVIDO");
        solicitacao.setDataDevolucao(LocalDate.now());
        solicitacao.setMotivoDevolucao(motivo.trim());
        return solicitacao;
    }

    public synchronized List<SolicitacaoAdocao> listarSolicitacoes() {
        return List.copyOf(solicitacoes);
    }

    public synchronized List<SolicitacaoAdocao> getHistoricoAdocoes() {
        return List.copyOf(historicoAdocoes);
    }

    private SolicitacaoAdocao buscarSolicitacao(String idSolicitacao) {
        return solicitacoes.stream()
                .filter(s -> s.getId().equals(idSolicitacao))
                .findFirst()
                .orElseThrow(() -> new java.util.NoSuchElementException("Solicitação não encontrada."));
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
