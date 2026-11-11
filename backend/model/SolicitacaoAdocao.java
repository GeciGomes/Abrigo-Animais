package backend.model;
import java.time.LocalDate;

public class SolicitacaoAdocao {
    private String id;
    private Animal animal;
    private String adotanteNome;
    private String adotanteContato;
    private LocalDate dataSolicitacao;
    private String status; 

    public SolicitacaoAdocao(String id, Animal animal, String adotanteNome, String adotanteContato) {
        this.id = id;
        this.animal = animal;
        this.adotanteNome = adotanteNome;
        this.adotanteContato = adotanteContato;
        this.dataSolicitacao = LocalDate.now();
        this.status = "PENDENTE";
    }

    public String getId() { 
        return id; 
    }
    public Animal getAnimal() { 
        return animal; 
    }
    public String getAdotanteNome() { 
        return adotanteNome; 
    }
    public String getAdotanteContato() { 
        return adotanteContato; 
    }
    public LocalDate getDataSolicitacao() { 
        return dataSolicitacao; 
    }
    public String getStatus() { 
        return status; 
    }
    public void setStatus(String status) { 
        this.status = status; 
    }
}