package backend.service;

import backend.exception.AnimalIndisponivelException;
import backend.exception.AnimalAdotadoException;
import backend.model.Animal;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDate;

public class AnimalService {
    private List<Animal> animais = new ArrayList<>();

    public Animal cadastrarAnimal(String nome, String especie, String raca, int idade, String porte,
            String estadoSaude, LocalDate dataResgate, String imagem) {
        String statusAdocao = "CRITICO".equalsIgnoreCase(normalizar(estadoSaude))
                ? "INDISPONIVEL" : "DISPONIVEL";
        Animal animal = new Animal(proximoId(), nome, especie, raca, idade, porte, estadoSaude,
                dataResgate, statusAdocao, imagem);
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
        return animais;
    }

    public Animal buscarPorId(String id) {
        return animais.stream()
                .filter(a -> a.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Animal não encontrado com o ID: " + id));
    }

    public List<Animal> filtrarPorEspecie(String especie) {
        return animais.stream()
                .filter(a -> a.getEspecie().equalsIgnoreCase(especie))
                .collect(Collectors.toList());
    }

    public Animal atualizarAnimal(String id, String nome, String especie, String raca, int idade, String porte,
            String estadoSaude, LocalDate dataResgate, String imagem) {
        Animal animal = buscarPorId(id);
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
        return animal;
    }

    public void removerAnimal(String id) {
        Animal animal = buscarPorId(id);
        if ("ADOTADO".equalsIgnoreCase(animal.getStatusAdocao())) {
            throw new AnimalAdotadoException("Animais adotados não podem ser removidos do histórico.");
        }
        animais.remove(animal);
    }

    public void disponibilizarParaAdocao(String id) {
        Animal animal = buscarPorId(id);
        
        if (animal.estaEmTratamentoCritico()) {
            throw new AnimalIndisponivelException("Animais em tratamento crítico não podem ser disponibilizados para adoção.");
        }
        if ("ADOTADO".equalsIgnoreCase(animal.getStatusAdocao())) {
            throw new AnimalAdotadoException("Um animal adotado não pode voltar a ser disponibilizado para adoção.");
        }
        
        animal.setStatusAdocao("DISPONIVEL");
    }

    private String normalizar(String valor) {
        return valor == null ? "" : java.text.Normalizer.normalize(valor, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").trim();
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