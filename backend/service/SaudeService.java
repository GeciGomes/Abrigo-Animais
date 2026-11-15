package backend.service;

import backend.exception.VacinaDuplicadaException;
import backend.model.RegistroMedico;
import backend.model.Veterinario;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

public class SaudeService {
    private final List<RegistroMedico> registrosMedicos = new ArrayList<>();

    public RegistroMedico registrarAtendimento(String animalId, Veterinario veterinario, String descricao,
            String tipo, LocalDate data) {
        return registrarAtendimento(animalId, veterinario, descricao, tipo, data, null);
    }

    public synchronized RegistroMedico registrarAtendimento(String animalId, Veterinario veterinario,
            String descricao, String tipo, LocalDate data, LocalDate dataVencimento) {
        if (veterinario == null) {
            throw new IllegalArgumentException("É necessário selecionar um veterinário cadastrado.");
        }
        String tipoValidado = validarTipo(tipo, descricao, data, dataVencimento);
        if ("VACINA".equals(tipoValidado)) {
            boolean vacinaDuplicada = registrosMedicos.stream()
                    .anyMatch(r -> r.getAnimalId().equals(animalId)
                            && "VACINA".equalsIgnoreCase(r.getTipo())
                            && normalizar(r.getDescricao()).equals(normalizar(descricao))
                            && r.getData().equals(data));

            if (vacinaDuplicada) {
                throw new VacinaDuplicadaException("Já existe um registro desta mesma vacina para este animal na data de " + data);
            }
        }

        RegistroMedico registro = new RegistroMedico(proximoId(), animalId, veterinario.getCrmv(),
                descricao.trim(), tipoValidado, data, dataVencimento);
        registrosMedicos.add(registro);
        return registro;
    }

    public RegistroMedico atualizarAtendimento(String registroId, Veterinario veterinario, String descricao,
            String tipo, LocalDate data) {
        RegistroMedico existente = buscarRegistro(registroId);
        LocalDate vencimento = "VACINA".equalsIgnoreCase(tipo) ? existente.getDataVencimento() : null;
        return atualizarAtendimento(registroId, veterinario, descricao, tipo, data, vencimento);
    }

    public synchronized RegistroMedico atualizarAtendimento(String registroId, Veterinario veterinario,
            String descricao, String tipo, LocalDate data, LocalDate dataVencimento) {
        if (veterinario == null) {
            throw new IllegalArgumentException("É necessário selecionar um veterinário cadastrado.");
        }
        RegistroMedico registro = buscarRegistro(registroId);
        String tipoValidado = validarTipo(tipo, descricao, data, dataVencimento);

        if ("VACINA".equals(tipoValidado)) {
            boolean vacinaDuplicada = registrosMedicos.stream()
                    .anyMatch(outro -> !outro.getId().equals(registroId)
                            && outro.getAnimalId().equals(registro.getAnimalId())
                            && "VACINA".equalsIgnoreCase(outro.getTipo())
                            && normalizar(outro.getDescricao()).equals(normalizar(descricao))
                            && outro.getData().equals(data));
            if (vacinaDuplicada) {
                throw new VacinaDuplicadaException(
                        "Já existe um registro desta mesma vacina para este animal na data de " + data);
            }
        }

        registro.setDescricao(descricao.trim());
        registro.setTipo(tipoValidado);
        registro.setData(data);
        registro.setDataVencimento(dataVencimento);
        return registro;
    }

    public synchronized List<RegistroMedico> listarRegistros() {
        return List.copyOf(registrosMedicos);
    }

    public synchronized List<RegistroMedico> consultarHistoricoAnimal(String animalId) {
        return registrosMedicos.stream()
                .filter(r -> r.getAnimalId().equals(animalId))
                .toList();
    }

    private RegistroMedico buscarRegistro(String registroId) {
        return registrosMedicos.stream()
                .filter(item -> item.getId().equals(registroId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Registro médico não encontrado."));
    }

    private String validarTipo(String tipo, String descricao, LocalDate data, LocalDate dataVencimento) {
        if (tipo == null || descricao == null || descricao.isBlank() || data == null) {
            throw new IllegalArgumentException("Tipo, descrição e data do atendimento são obrigatórios.");
        }
        String tipoValidado = tipo.trim().toUpperCase(Locale.ROOT);
        if (!List.of("VACINA", "CONSULTA", "TRATAMENTO").contains(tipoValidado)) {
            throw new IllegalArgumentException("Tipo de atendimento inválido.");
        }
        if ("VACINA".equals(tipoValidado)) {
            if (dataVencimento == null) {
                throw new IllegalArgumentException("Informe a data de vencimento da vacina.");
            }
            if (dataVencimento.isBefore(data)) {
                throw new IllegalArgumentException("O vencimento da vacina não pode ser anterior à aplicação.");
            }
        } else if (dataVencimento != null) {
            throw new IllegalArgumentException("A data de vencimento só deve ser informada para vacinas.");
        }
        return tipoValidado;
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

    private String normalizar(String valor) {
        return valor == null ? "" : valor.trim().toLowerCase(Locale.ROOT);
    }
}
