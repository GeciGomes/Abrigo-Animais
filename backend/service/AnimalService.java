package backend.service;

import backend.exception.AnimalIndisponivelException;
import backend.model.Animal;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDate;

public class AnimalService {
    private List<Animal> animais = new ArrayList<>();

    public Animal cadastrarAnimal(String nome, String especie, String raca, int idade, String porte,
            String estadoSaude, LocalDate dataResgate, String imagem) {
        Animal animal = new Animal(proximoId(), nome, especie, raca, idade, porte, estadoSaude,
                dataResgate, "DISPONIVEL", imagem);
        animais.add(animal);
        return animal;
    }

    public void cadastrarAnimal(Animal animal) {
        if (animais.stream().anyMatch(cadastrado -> cadastrado.getId().equals(animal.getId()))) {
            throw new IllegalArgumentException("Já existe um animal com o ID " + animal.getId() + ".");
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
        animal.setDataResgate(dataResgate);
        animal.setImagem(imagem);
        return animal;
    }

    public void removerAnimal(String id) {
        animais.remove(buscarPorId(id));
    }

    public void disponibilizarParaAdocao(String id) {
        Animal animal = buscarPorId(id);
        
        if ("CRITICO".equalsIgnoreCase(animal.getEstadoSaude())) {
            throw new AnimalIndisponivelException("Animais em tratamento crítico não podem ser disponibilizados para adoção.");
        }
        
        animal.setStatusAdocao("DISPONIVEL");
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