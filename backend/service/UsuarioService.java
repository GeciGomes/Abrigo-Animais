package backend.service;

import backend.model.Administrador;
import backend.model.Usuario;
import backend.model.Veterinario;
import backend.model.Voluntario;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Locale;

public class UsuarioService {
    public static final String ID_ADMINISTRADOR_PRINCIPAL = "ADM001";

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

    public Usuario buscarPorEmail(String email) {
        String emailNormalizado = normalizarEmail(email);
        return usuarios.stream()
                .filter(usuario -> normalizarEmail(usuario.getEmail()).equals(emailNormalizado))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Usuário não encontrado."));
    }

    public Usuario cadastrar(String tipo, String nome, String email, String telefone, String crmv,
            String nivelAcesso) {
        return cadastrar(tipo, nome, email, telefone, crmv, nivelAcesso, false);
    }

    private Usuario cadastrar(String tipo, String nome, String email, String telefone, String crmv,
            String nivelAcesso, boolean administradorPrincipal) {
        validarEmailUnico(email, null);
        String prefixo = switch (tipo) {
            case "Voluntário" -> "V";
            case "Veterinário" -> "VT";
            case "Administrador" -> "ADM";
            default -> throw new IllegalArgumentException("Tipo de usuário inválido.");
        };
        String id;
        if (administradorPrincipal) {
            id = ID_ADMINISTRADOR_PRINCIPAL;
        } else {
            int maiorId = usuarios.stream()
                    .filter(usuario -> correspondeAoTipo(usuario, tipo))
                    .map(Usuario::getId)
                    .filter(usuarioId -> usuarioId.matches(
                            java.util.regex.Pattern.quote(prefixo) + "\\d+"))
                    .mapToInt(usuarioId -> Integer.parseInt(usuarioId.substring(prefixo.length())))
                    .max()
                    .orElse(0);
            id = prefixo + String.format("%03d", maiorId + 1);
        }
        Usuario usuario = criarInstancia(
                tipo, id, nome, email, telefone, crmv, nivelAcesso, administradorPrincipal);
        usuarios.add(usuario);
        return usuario;
    }

    public Administrador cadastrarAdministradorInicial(String nome, String email, String telefone) {
        if (usuarios.stream().anyMatch(Administrador.class::isInstance)) {
            throw new IllegalStateException("O administrador principal já foi configurado.");
        }
        return (Administrador) cadastrar("Administrador", nome, email, telefone,
                "", "Total", true);
    }

    public Usuario restaurar(String tipo, String id, String nome, String email, String telefone,
            String crmv, String nivelAcesso, boolean administradorPrincipal, boolean aprovado) {
        if (id == null || id.isBlank()
                || usuarios.stream().anyMatch(usuario -> usuario.getId().equals(id))) {
            throw new IllegalArgumentException("O arquivo contém um ID de usuário inválido ou duplicado.");
        }
        if (administradorPrincipal && !"Administrador".equals(tipo)) {
            throw new IllegalArgumentException("O arquivo contém um administrador principal inválido.");
        }
        if (administradorPrincipal && !ID_ADMINISTRADOR_PRINCIPAL.equals(id)) {
            throw new IllegalArgumentException("O administrador principal deve usar o ID "
                    + ID_ADMINISTRADOR_PRINCIPAL + ".");
        }
        if (administradorPrincipal && usuarios.stream().anyMatch(usuario ->
                usuario instanceof Administrador administrador && administrador.isPrincipal())) {
            throw new IllegalArgumentException("O arquivo contém mais de um administrador principal.");
        }

        validarEmailUnico(email, null);
        Usuario usuario = criarInstancia(tipo, id, nome, email, telefone, crmv,
                nivelAcesso, administradorPrincipal);
        usuario.setAprovado(aprovado);
        usuarios.add(usuario);
        return usuario;
    }

    public Usuario atualizar(String id, String tipo, String nome, String email, String telefone,
            String crmv, String nivelAcesso) {
        Usuario anterior = buscarPorId(id);
        validarEmailUnico(email, id);
        if (!correspondeAoTipo(anterior, tipo)) {
            usuarios.remove(anterior);
            Usuario atualizado = criarInstancia(tipo, id, nome, email, telefone, crmv,
                    nivelAcesso, false);
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
            String crmv, String nivelAcesso, boolean administradorPrincipal) {
        return switch (tipo) {
            case "Voluntário" -> new Voluntario(id, nome, email, telefone);
            case "Veterinário" -> new Veterinario(id, nome, email, telefone, crmv);
            case "Administrador" -> new Administrador(
                    id, nome, email, telefone, nivelAcesso, administradorPrincipal);
            default -> throw new IllegalArgumentException("Tipo de usuário inválido.");
        };
    }

    private void validarEmailUnico(String email, String idIgnorado) {
        String emailNormalizado = normalizarEmail(email);
        if (emailNormalizado.isBlank() || usuarios.stream().anyMatch(usuario ->
                !usuario.getId().equals(idIgnorado)
                        && normalizarEmail(usuario.getEmail()).equals(emailNormalizado))) {
            throw new IllegalArgumentException("Informe um e-mail válido e ainda não cadastrado.");
        }
    }

    private String normalizarEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
