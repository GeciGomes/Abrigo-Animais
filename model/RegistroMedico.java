package backend.model;
import java.time.LocalDate;

public class RegistroMedico {
    private String id;
    private String animalId;
    private String veterinarioCrmv;
    private String descricao;
    private String tipo; 
    private LocalDate data;

    public RegistroMedico(String id, String animalId, String veterinarioCrmv, String descricao, String tipo, LocalDate data) {
        this.id = id;
        this.animalId = animalId;
        this.veterinarioCrmv = veterinarioCrmv;
        this.descricao = descricao;
        this.tipo = tipo;
        this.data = data;
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
    public String getTipo() { 
        return tipo; 
    }
    public LocalDate getData() { 
        return data; 
    }
}