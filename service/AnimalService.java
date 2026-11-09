package backend.service;

import backend.exception.AnimalIndisponivelException;
import backend.model.Animal;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AnimalService {
    private List<Animal> animais = new ArrayList<>();

    public void cadastrarAnimal(Animal animal) {
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

    public void disponibilizarParaAdocao(String id) {
        Animal animal = buscarPorId(id);
        
        if ("CRITICO".equalsIgnoreCase(animal.getEstadoSaude())) {
            throw new AnimalIndisponivelException("Animais em tratamento crítico não podem ser disponibilizados para adoção.");
        }
        
        animal.setStatusAdocao("DISPONIVEL");
    }
}