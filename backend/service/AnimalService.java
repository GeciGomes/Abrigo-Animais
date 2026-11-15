package backend.service;

import backend.exception.AnimalIndisponivelException;
import backend.exception.AnimalAdotadoException;
import backend.model.Animal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.time.LocalDate;

public class AnimalService {
    private List<Animal> animais = new ArrayList<>();

    public Animal cadastrarAnimal(String nome, String especie, String raca, int idade, String porte,
            String estadoSaude, LocalDate dataResgate, String imagem) {
        return cadastrarAnimal(nome, especie, raca, idade, porte, estadoSaude, dataResgate, imagem, null);
    }

    public Animal cadastrarAnimal(String nome, String especie, String raca, int idade, String porte,
            String estadoSaude, LocalDate dataResgate, String imagem, String voluntarioResponsavelId) {
        String statusAdocao = "CRITICO".equalsIgnoreCase(normalizar(estadoSaude))
                ? "INDISPONIVEL" : "DISPONIVEL";
        return cadastrarAnimal(nome, especie, raca, idade, porte, estadoSaude, dataResgate, imagem,
                voluntarioResponsavelId, statusAdocao);
    }

    public Animal cadastrarAnimal(String nome, String especie, String raca, int idade, String porte,
            String estadoSaude, LocalDate dataResgate, String imagem, String voluntarioResponsavelId,
            String statusAdocao) {
        String statusValidado = validarStatusNovo(statusAdocao, estadoSaude);
        Animal animal = new Animal(proximoId(), nome, especie, raca, idade, porte, estadoSaude,
                dataResgate, statusValidado, imagem);
        animal.setVoluntarioResponsavelId(voluntarioResponsavelId);
        animais.add(animal);
        return animal;
    }

    public void cadastrarAnimal(Animal animal) {
        if (animais.stream().anyMatch(cadastrado -> cadastrado.getId().equals(animal.getId()))) {
            throw new IllegalArgumentException("Já existe um animal com o ID " + animal.getId() + ".");
        }
        if (animal.estaEmTratamentoCritico() && !"ADOTADO".equalsIgnoreCase(animal.getStatusAdocao())) {
            animal.setStatusAdocao("INDISPONIVEL");
        }
        animais.add(animal);
    }

    public List<Animal> listarTodos() {
        return animais.stream()
                .filter(animal -> !animal.isArquivado())
                .collect(Collectors.toList());
    }

    public List<Animal> listarTodosIncluindoArquivados() {
        return animais;
    }

    public Animal buscarPorId(String id) {
        return animais.stream()
                .filter(a -> a.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Animal não encontrado com o ID: " + id));
    }

    public List<Animal> filtrarPorEspecie(String especie) {
        return buscar("", especie, "", "", "", "");
    }

    public List<Animal> buscar(String termo, String especie, String raca, String porte, String estadoSaude,
            String statusAdocao) {
        return listarTodos().stream()
                .filter(animal -> correspondeAlgum(termo, animal.getId(), animal.getNome()))
                .filter(animal -> corresponde(animal.getEspecie(), especie))
                .filter(animal -> corresponde(animal.getRaca(), raca))
                .filter(animal -> corresponde(animal.getPorte(), porte))
                .filter(animal -> corresponde(animal.getEstadoSaude(), estadoSaude))
                .filter(animal -> correspondeExato(animal.getStatusAdocao(), statusAdocao))
                .collect(Collectors.toList());
    }

    public Animal atualizarAnimal(String id, String nome, String especie, String raca, int idade, String porte,
            String estadoSaude, LocalDate dataResgate, String imagem) {
        Animal existente = buscarPorId(id);
        return atualizarAnimal(id, nome, especie, raca, idade, porte, estadoSaude, dataResgate, imagem,
                existente.getVoluntarioResponsavelId());
    }

    public Animal atualizarAnimal(String id, String nome, String especie, String raca, int idade, String porte,
            String estadoSaude, LocalDate dataResgate, String imagem, String voluntarioResponsavelId) {
        Animal existente = buscarPorId(id);
        return atualizarAnimal(id, nome, especie, raca, idade, porte, estadoSaude, dataResgate, imagem,
                voluntarioResponsavelId, existente.getStatusAdocao());
    }

    public Animal atualizarAnimal(String id, String nome, String especie, String raca, int idade, String porte,
            String estadoSaude, LocalDate dataResgate, String imagem, String voluntarioResponsavelId,
            String statusAdocao) {
        Animal animal = buscarPorId(id);
        if (animal.isArquivado()) {
            throw new IllegalStateException("Não é possível editar um animal removido.");
        }
        String statusValidado = validarStatusAtualizacao(animal, estadoSaude, statusAdocao);
        animal.setNome(nome);
        animal.setEspecie(especie);
        animal.setRaca(raca);
        animal.setIdade(idade);
        animal.setPorte(porte);
        animal.setEstadoSaude(estadoSaude);
        if (animal.estaEmTratamentoCritico() && !"ADOTADO".equalsIgnoreCase(animal.getStatusAdocao())) {
            animal.setStatusAdocao("INDISPONIVEL");
        }
        animal.setDataResgate(dataResgate);
        animal.setImagem(imagem);
        animal.setVoluntarioResponsavelId(voluntarioResponsavelId);
        animal.setStatusAdocao(statusValidado);
        return animal;
    }

    public void removerAnimal(String id) {
        Animal animal = buscarPorId(id);
        if (animal.isArquivado()) {
            throw new IllegalStateException("Este animal já foi removido.");
        }
        animal.setArquivado(true);
    }

    public void removerAtribuicoesDoVoluntario(String voluntarioId) {
        animais.stream()
                .filter(animal -> voluntarioId.equals(animal.getVoluntarioResponsavelId()))
                .forEach(animal -> animal.setVoluntarioResponsavelId(null));
    }

    public void disponibilizarParaAdocao(String id) {
        Animal animal = buscarPorId(id);

        if (animal.isArquivado()) {
            throw new IllegalStateException("Animais removidos não podem ser disponibilizados para adoção.");
        }
        
        if (animal.estaEmTratamentoCritico()) {
            throw new AnimalIndisponivelException("Animais em tratamento crítico não podem ser disponibilizados para adoção.");
        }
        if ("ADOTADO".equalsIgnoreCase(animal.getStatusAdocao())) {
            throw new AnimalAdotadoException("Um animal adotado não pode voltar a ser disponibilizado para adoção.");
        }
        
        animal.setStatusAdocao("DISPONIVEL");
    }

    public void registrarDevolucao(String id) {
        Animal animal = buscarPorId(id);
        if (animal.isArquivado()) {
            throw new IllegalStateException("Animais removidos não podem ser recebidos de volta.");
        }
        if (!"ADOTADO".equalsIgnoreCase(animal.getStatusAdocao())) {
            throw new IllegalStateException("O animal não possui uma adoção ativa para devolver.");
        }
        animal.setStatusAdocao(animal.estaEmTratamentoCritico() ? "INDISPONIVEL" : "DISPONIVEL");
    }

    private String normalizar(String valor) {
        return valor == null ? "" : java.text.Normalizer.normalize(valor, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").trim();
    }

    private String validarStatusNovo(String statusAdocao, String estadoSaude) {
        String status = normalizar(statusAdocao).toUpperCase(Locale.ROOT);
        if (!"DISPONIVEL".equals(status) && !"INDISPONIVEL".equals(status)) {
            throw new IllegalArgumentException(
                    "O status inicial deve ser DISPONIVEL ou INDISPONIVEL; ADOTADO só pode resultar de uma adoção aprovada.");
        }
        if ("DISPONIVEL".equals(status) && "CRITICO".equalsIgnoreCase(normalizar(estadoSaude))) {
            throw new IllegalArgumentException("Animais em estado crítico não podem ficar disponíveis para adoção.");
        }
        return status;
    }

    private String validarStatusAtualizacao(Animal animal, String estadoSaude, String statusAdocao) {
        String status = normalizar(statusAdocao).toUpperCase(Locale.ROOT);
        String atual = normalizar(animal.getStatusAdocao()).toUpperCase(Locale.ROOT);
        if ("ADOTADO".equals(atual)) {
            if (!"ADOTADO".equals(status)) {
                throw new IllegalArgumentException("O status ADOTADO só pode ser alterado por uma nova decisão de adoção.");
            }
            return atual;
        }
        if ("ADOTADO".equals(status)) {
            throw new IllegalArgumentException("O status ADOTADO só pode resultar de uma adoção aprovada.");
        }
        if ("DISPONIVEL".equals(status) && "CRITICO".equalsIgnoreCase(normalizar(estadoSaude))) {
            return "INDISPONIVEL";
        }
        return validarStatusNovo(status, estadoSaude);
    }

    private boolean corresponde(String valor, String filtro) {
        return filtro == null || filtro.isBlank()
                || normalizarBusca(valor).contains(normalizarBusca(filtro));
    }

    private boolean correspondeExato(String valor, String filtro) {
        return filtro == null || filtro.isBlank()
                || normalizarBusca(valor).equals(normalizarBusca(filtro));
    }

    private boolean correspondeAlgum(String filtro, String... valores) {
        return filtro == null || filtro.isBlank()
                || java.util.Arrays.stream(valores).anyMatch(valor -> corresponde(valor, filtro));
    }

    private String normalizarBusca(String valor) {
        return normalizar(valor).toLowerCase(Locale.ROOT);
    }

    private String proximoId() {
        int maiorId = animais.stream()
                .map(Animal::getId)
                .filter(id -> id.matches("A\\d+"))
                .mapToInt(id -> Integer.parseInt(id.substring(1)))
                .max()
                .orElse(0);
        return "A" + String.format("%03d", maiorId + 1);
    }
}