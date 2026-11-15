package backend.model;
import java.time.LocalDate;

public class SolicitacaoAdocao implements Identificavel {
    private String id;
    private Animal animal;
    private String adotanteNome;
    private String adotanteContato;
    private LocalDate dataSolicitacao;
    private LocalDate dataDecisao;
    private LocalDate dataAdocao;
    private LocalDate dataDevolucao;
    private String motivoDevolucao;
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
    public LocalDate getDataDecisao() {
        return dataDecisao;
    }
    public void setDataDecisao(LocalDate dataDecisao) {
        this.dataDecisao = dataDecisao;
    }
    public LocalDate getDataAdocao() {
        return dataAdocao;
    }
    public void setDataAdocao(LocalDate dataAdocao) {
        this.dataAdocao = dataAdocao;
    }
    public LocalDate getDataDevolucao() {
        return dataDevolucao;
    }
    public void setDataDevolucao(LocalDate dataDevolucao) {
        this.dataDevolucao = dataDevolucao;
    }
    public String getMotivoDevolucao() {
        return motivoDevolucao;
    }
    public void setMotivoDevolucao(String motivoDevolucao) {
        this.motivoDevolucao = motivoDevolucao;
    }
    public String getStatus() { 
        return status; 
    }
    public void setStatus(String status) { 
        this.status = status; 
    }
}