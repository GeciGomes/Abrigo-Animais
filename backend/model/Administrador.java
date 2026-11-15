package backend.model;

public class Administrador extends Usuario {
    private String nivelAcesso;
    private final boolean principal;

    public Administrador(String id, String nome, String email, String telefone, String nivelAcesso) {
        this(id, nome, email, telefone, nivelAcesso, false);
    }

    public Administrador(String id, String nome, String email, String telefone,
            String nivelAcesso, boolean principal) {
        super(id, nome, email, telefone);
        this.nivelAcesso = nivelAcesso;
        this.principal = principal;
    }

    public String getNivelAcesso() { return nivelAcesso; }
    public void setNivelAcesso(String nivelAcesso) { this.nivelAcesso = nivelAcesso; }

    public boolean isPrincipal() {
        return principal;
    }
}