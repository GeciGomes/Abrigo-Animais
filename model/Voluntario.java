package backend.model;
import java.util.ArrayList;
import java.util.List;

public class Voluntario extends Usuario {
    
    private List<String> atividadesInscritas;
    private List<String> historicoParticipacoes;

    public Voluntario(String id, String nome, String email, String telefone) {
        super(id, nome, email, telefone);
        this.atividadesInscritas = new ArrayList<>();
        this.historicoParticipacoes = new ArrayList<>();
    }

    public List<String> getAtividadesInscritas() { return atividadesInscritas; }
    public List<String> getHistoricoParticipacoes() { return historicoParticipacoes; }
}