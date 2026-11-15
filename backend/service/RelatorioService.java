package backend.service;

import backend.model.Animal;
import backend.model.SolicitacaoAdocao;
import backend.model.Usuario;
import backend.model.Voluntario;
import java.text.Normalizer;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class RelatorioService {
    private final AnimalService animalService;
    private final AdocaoService adocaoService;
    private final UsuarioService usuarioService;

    public RelatorioService(AnimalService animalService, AdocaoService adocaoService,
            UsuarioService usuarioService) {
        this.animalService = animalService;
        this.adocaoService = adocaoService;
        this.usuarioService = usuarioService;
    }

    public Estatisticas gerarEstatisticas() {
        List<Animal> resgates = animalService.listarTodosIncluindoArquivados().stream()
                .filter(animal -> animal.getDataResgate() != null)
                .toList();
        List<SolicitacaoAdocao> adocoesRealizadas = adocaoService.getHistoricoAdocoes();

        Map<String, Long> resgatesPorEspecie = resgates.stream()
                .collect(Collectors.groupingBy(Animal::getEspecie, Collectors.counting()));
        List<EspecieResgatada> especies = resgatesPorEspecie.entrySet().stream()
                .map(entrada -> new EspecieResgatada(entrada.getKey(), entrada.getValue()))
                .sorted(Comparator.comparingLong(EspecieResgatada::quantidade).reversed()
                        .thenComparing(EspecieResgatada::especie, String.CASE_INSENSITIVE_ORDER))
                .toList();

        Map<YearMonth, long[]> totaisMensais = new TreeMap<>();
        for (Animal animal : resgates) {
            long[] totais = totaisMensais.computeIfAbsent(YearMonth.from(animal.getDataResgate()),
                    ignorado -> new long[2]);
            totais[0]++;
        }
        for (SolicitacaoAdocao adocao : adocoesRealizadas) {
            if (adocao.getDataAdocao() == null) {
                continue;
            }
            long[] totais = totaisMensais.computeIfAbsent(YearMonth.from(adocao.getDataAdocao()),
                    ignorado -> new long[2]);
            totais[1]++;
        }
        List<TaxaAdocaoMensal> taxasMensais = totaisMensais.entrySet().stream()
                .map(entrada -> {
                    long resgatados = entrada.getValue()[0];
                    long adotados = entrada.getValue()[1];
                    Double taxa = resgatados == 0 ? null
                            : Math.round((double) adotados / resgatados * 10_000.0) / 100.0;
                    return new TaxaAdocaoMensal(entrada.getKey(), resgatados, adotados, taxa);
                })
                .toList();

        List<Animal> emTratamento = animalService.listarTodos().stream()
                .filter(RelatorioService::estaEmTratamento)
                .sorted(Comparator.comparing(Animal::getNome, String.CASE_INSENSITIVE_ORDER))
                .toList();
        long voluntariosAtivos = usuarioService.listarTodos().stream()
                .filter(Voluntario.class::isInstance)
                .filter(Usuario::isAprovado)
                .count();

        return new Estatisticas(resgates.size(), adocoesRealizadas.size(), especies, taxasMensais,
                voluntariosAtivos, emTratamento);
    }

    private static boolean estaEmTratamento(Animal animal) {
        String estado = Normalizer.normalize(animal.getEstadoSaude() == null ? "" : animal.getEstadoSaude(),
                Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toLowerCase(Locale.ROOT);
        return estado.contains("tratamento") || estado.contains("critico");
    }

    public record Estatisticas(
            long animaisResgatados,
            long adocoesRealizadas,
            List<EspecieResgatada> especiesMaisResgatadas,
            List<TaxaAdocaoMensal> taxaAdocaoMensal,
            long voluntariosAtivos,
            List<Animal> animaisEmTratamento) {
        public Estatisticas {
            especiesMaisResgatadas = List.copyOf(especiesMaisResgatadas);
            taxaAdocaoMensal = List.copyOf(taxaAdocaoMensal);
            animaisEmTratamento = List.copyOf(animaisEmTratamento);
        }
    }

    public record EspecieResgatada(String especie, long quantidade) {
    }

    public record TaxaAdocaoMensal(YearMonth mes, long animaisResgatados, long adocoesRealizadas,
            Double taxaPercentual) {
    }
}
