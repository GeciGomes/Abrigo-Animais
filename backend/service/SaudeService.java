package backend.service;

import backend.exception.VacinaDuplicadaException;
import backend.model.RegistroMedico;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SaudeService {
    private List<RegistroMedico> registrosMedicos = new ArrayList<>();

    public void registrarAtendimento(String id, String animalId, String veterinarioCrmv, String descricao, String tipo, LocalDate data) {
        
        if ("VACINA".equalsIgnoreCase(tipo)) {
            boolean vacinaDuplicada = registrosMedicos.stream()
                    .anyMatch(r -> r.getAnimalId().equals(animalId)
                            && "VACINA".equalsIgnoreCase(r.getTipo())
                            && r.getDescricao().equalsIgnoreCase(descricao)
                            && r.getData().equals(data));

            if (vacinaDuplicada) {
                throw new VacinaDuplicadaException("Já existe um registro desta mesma vacina para este animal na data de " + data);
            }
        }

        RegistroMedico registro = new RegistroMedico(id, animalId, veterinarioCrmv, descricao, tipo, data);
        registrosMedicos.add(registro);
    }

    public List<RegistroMedico> consultarHistoricoAnimal(String animalId) {
        return registrosMedicos.stream()
                .filter(r -> r.getAnimalId().equals(animalId))
                .collect(Collectors.toList());
    }
}
