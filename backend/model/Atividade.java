package backend.model;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Atividade implements Identificavel {
    public enum Tipo {
        MUTIRAO_LIMPEZA,
        CAMPANHA_VACINACAO,
        FEIRA_ADOCAO,
        EVENTO_BENEFICENTE
    }

    private String id;
    private Tipo tipo;
    private String titulo;
    private String descricao;
    private LocalDate data;
    private List<String> voluntariosInscritosId;
    private List<String> voluntariosPresentesId;

    public Atividade(String id, Tipo tipo, String titulo, String descricao, LocalDate data) {
        this.id = id;
        this.tipo = java.util.Objects.requireNonNull(tipo, "O tipo da atividade é obrigatório.");
        this.titulo = titulo;
        this.descricao = descricao;
        this.data = data;
        this.voluntariosInscritosId = new ArrayList<>();
        this.voluntariosPresentesId = new ArrayList<>();
    }

    public String getId() { 
        return id; 
    }
    public Tipo getTipo() {
        return tipo;
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
    public List<String> getVoluntariosPresentesId() {
        return voluntariosPresentesId;
    }
}