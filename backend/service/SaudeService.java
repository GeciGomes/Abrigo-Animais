package backend.service;

import backend.exception.VacinaDuplicadaException;
import backend.model.RegistroMedico;
import backend.model.Veterinario;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SaudeService {
    private List<RegistroMedico> registrosMedicos = new ArrayList<>();

    public RegistroMedico registrarAtendimento(String animalId, Veterinario veterinario, String descricao,
            String tipo, LocalDate data) {
        if (veterinario == null) {
            throw new IllegalArgumentException("É necessário selecionar um veterinário cadastrado.");
        }
        
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

        RegistroMedico registro = new RegistroMedico(proximoId(), animalId, veterinario.getCrmv(), descricao, tipo, data);
        registrosMedicos.add(registro);
        return registro;
    }

    public List<RegistroMedico> listarRegistros() {
        return registrosMedicos;
    }

    public List<RegistroMedico> consultarHistoricoAnimal(String animalId) {
        return registrosMedicos.stream()
                .filter(r -> r.getAnimalId().equals(animalId))
                .collect(Collectors.toList());
    }

    private String proximoId() {
        int maiorId = registrosMedicos.stream()
                .map(RegistroMedico::getId)
                .filter(id -> id.matches("R\\d+"))
                .mapToInt(id -> Integer.parseInt(id.substring(1)))
                .max()
                .orElse(0);
        return "R" + String.format("%03d", maiorId + 1);
    }
}
