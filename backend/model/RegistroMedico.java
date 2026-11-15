package backend.model;
import java.time.LocalDate;

public class RegistroMedico implements Identificavel {
    private String id;
    private String animalId;
    private String veterinarioCrmv;
    private String descricao;
    private String tipo; 
    private LocalDate data;
    private LocalDate dataVencimento;

    public RegistroMedico(String id, String animalId, String veterinarioCrmv, String descricao, String tipo, LocalDate data) {
        this(id, animalId, veterinarioCrmv, descricao, tipo, data, null);
    }

    public RegistroMedico(String id, String animalId, String veterinarioCrmv, String descricao, String tipo,
            LocalDate data, LocalDate dataVencimento) {
        this.id = id;
        this.animalId = animalId;
        this.veterinarioCrmv = veterinarioCrmv;
        this.descricao = descricao;
        this.tipo = tipo;
        this.data = data;
        this.dataVencimento = dataVencimento;
    }

    public String getId() { 
        return id; 
    }
    public String getAnimalId() { 
        return animalId; 
    }
    public String getVeterinarioCrmv() { 
        return veterinarioCrmv; 
    }
    public String getDescricao() { 
        return descricao; 
    }
    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }
    public String getTipo() { 
        return tipo; 
    }
    public void setTipo(String tipo) {
        this.tipo = tipo;
    }
    public LocalDate getData() { 
        return data; 
    }
    public void setData(LocalDate data) {
        this.data = data;
    }
    public LocalDate getDataVencimento() {
        return dataVencimento;
    }
    public void setDataVencimento(LocalDate dataVencimento) {
        this.dataVencimento = dataVencimento;
    }
}