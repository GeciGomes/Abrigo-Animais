package backend.model;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Atividade {
    private String id;
    private String titulo;
    private String descricao;
    private LocalDate data;
    private List<String> voluntariosInscritosId;

    public Atividade(String id, String titulo, String descricao, LocalDate data) {
        this.id = id;
        this.titulo = titulo;
        this.descricao = descricao;
        this.data = data;
        this.voluntariosInscritosId = new ArrayList<>();
    }

    public String getId() { 
        return id; 
    }
    public String getTitulo() { 
        return titulo; 
    }
    public String getDescricao() { 
        return descricao; 
    }
    public LocalDate getData() { 
        return data; 
    }
    public List<String> getVoluntariosInscritosId() { 
        return voluntariosInscritosId; 
    }
}