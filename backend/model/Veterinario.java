package backend.model;

public class Veterinario extends Usuario {
    private String crmv;

    public Veterinario(String id, String nome, String email, String telefone, String crmv) {
        super(id, nome, email, telefone);
        this.crmv = crmv;
    }

    public String getCrmv() { return crmv; }
    public void setCrmv(String crmv) { this.crmv = crmv; }
}