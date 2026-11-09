package backend.model;

public class Administrador extends Usuario {
    private String nivelAcesso;

    public Administrador(String id, String nome, String email, String telefone, String nivelAcesso) {
        super(id, nome, email, telefone);
        this.nivelAcesso = nivelAcesso;
    }

    public String getNivelAcesso() { return nivelAcesso; }
    public void setNivelAcesso(String nivelAcesso) { this.nivelAcesso = nivelAcesso; }
}