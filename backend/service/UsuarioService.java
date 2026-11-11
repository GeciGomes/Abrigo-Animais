package backend.service;

import backend.model.Administrador;
import backend.model.Usuario;
import backend.model.Veterinario;
import backend.model.Voluntario;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class UsuarioService {
    private final List<Usuario> usuarios = new ArrayList<>();

    public List<Usuario> listarTodos() {
        return usuarios;
    }

    public Usuario buscarPorId(String id) {
        return usuarios.stream()
                .filter(usuario -> usuario.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Usuário não encontrado."));
    }

    public Usuario cadastrar(String tipo, String nome, String email, String telefone, String crmv,
            String nivelAcesso) {
        String prefixo = switch (tipo) {
            case "Voluntário" -> "V";
            case "Veterinário" -> "VT";
            case "Administrador" -> "ADM";
            default -> throw new IllegalArgumentException("Tipo de usuário inválido.");
        };
        int maiorId = usuarios.stream()
                .filter(usuario -> correspondeAoTipo(usuario, tipo))
                .map(Usuario::getId)
                .filter(id -> id.matches(java.util.regex.Pattern.quote(prefixo) + "\\d+"))
                .mapToInt(id -> Integer.parseInt(id.substring(prefixo.length())))
                .max()
                .orElse(0);
        String id = prefixo + String.format("%03d", maiorId + 1);
        Usuario usuario = criarInstancia(tipo, id, nome, email, telefone, crmv, nivelAcesso);
        usuarios.add(usuario);
        return usuario;
    }

    public Usuario atualizar(String id, String tipo, String nome, String email, String telefone,
            String crmv, String nivelAcesso) {
        Usuario anterior = buscarPorId(id);
        if (!correspondeAoTipo(anterior, tipo)) {
            usuarios.remove(anterior);
            Usuario atualizado = criarInstancia(tipo, id, nome, email, telefone, crmv, nivelAcesso);
            usuarios.add(atualizado);
            return atualizado;
        }
        anterior.setNome(nome);
        anterior.setEmail(email);
        anterior.setTelefone(telefone);
        if (anterior instanceof Veterinario veterinario) {
            veterinario.setCrmv(crmv);
        } else if (anterior instanceof Administrador administrador) {
            administrador.setNivelAcesso(nivelAcesso);
        }
        return anterior;
    }

    public void remover(String id) {
        usuarios.remove(buscarPorId(id));
    }

    public Administrador buscarAdministrador(String id) {
        Usuario usuario = buscarPorId(id);
        if (!(usuario instanceof Administrador administrador)) {
            throw new IllegalArgumentException("O usuário selecionado não é um administrador.");
        }
        return administrador;
    }

    public Veterinario buscarVeterinario(String id) {
        Usuario usuario = buscarPorId(id);
        if (!(usuario instanceof Veterinario veterinario)) {
            throw new IllegalArgumentException("O usuário selecionado não é um veterinário.");
        }
        return veterinario;
    }

    public Voluntario buscarVoluntario(String id) {
        Usuario usuario = buscarPorId(id);
        if (!(usuario instanceof Voluntario voluntario)) {
            throw new IllegalArgumentException("O usuário selecionado não é um voluntário.");
        }
        return voluntario;
    }

    private boolean correspondeAoTipo(Usuario usuario, String tipo) {
        return switch (tipo) {
            case "Voluntário" -> usuario instanceof Voluntario;
            case "Veterinário" -> usuario instanceof Veterinario;
            case "Administrador" -> usuario instanceof Administrador;
            default -> false;
        };
    }

    private Usuario criarInstancia(String tipo, String id, String nome, String email, String telefone,
            String crmv, String nivelAcesso) {
        return switch (tipo) {
            case "Voluntário" -> new Voluntario(id, nome, email, telefone);
            case "Veterinário" -> new Veterinario(id, nome, email, telefone, crmv);
            case "Administrador" -> new Administrador(id, nome, email, telefone, nivelAcesso);
            default -> throw new IllegalArgumentException("Tipo de usuário inválido.");
        };
    }
}
