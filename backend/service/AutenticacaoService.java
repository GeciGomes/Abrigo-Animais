package backend.service;

import backend.exception.AutenticacaoException;
import backend.model.Usuario;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class AutenticacaoService {
    private static final int ITERATIONS = 120_000;
    private static final int SALT_LENGTH = 16;
    private static final int KEY_LENGTH = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UsuarioService usuarioService;
    private final Map<String, Credencial> credenciais = new ConcurrentHashMap<>();
    private final Map<String, String> sessoes = new ConcurrentHashMap<>();

    public AutenticacaoService(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    public static void validarSenha(String senha) {
        if (senha == null || senha.length() < 12) {
            throw new IllegalArgumentException("A senha deve ter pelo menos 12 caracteres.");
        }
    }

    public void registrarSenha(Usuario usuario, String senha) {
        validarSenha(senha);
        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);
        credenciais.put(usuario.getId(), new Credencial(salt, derivar(senha, salt)));
    }

    public Sessao entrar(String email, String senha) {
        Usuario usuario;
        try {
            usuario = usuarioService.buscarPorEmail(email);
        } catch (java.util.NoSuchElementException exception) {
            throw new AutenticacaoException("E-mail ou senha inválidos.");
        }
        Credencial credencial = credenciais.get(usuario.getId());
        if (credencial == null || !MessageDigest.isEqual(credencial.hash, derivar(senha, credencial.salt))) {
            throw new AutenticacaoException("E-mail ou senha inválidos.");
        }

        byte[] tokenBytes = new byte[32];
        RANDOM.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        sessoes.put(token, usuario.getId());
        return new Sessao(token, usuario);
    }

    public Usuario usuarioDaSessao(String token) {
        String usuarioId = sessoes.get(token);
        if (usuarioId == null) {
            throw new AutenticacaoException("Sessão inválida ou expirada.");
        }
        return usuarioService.buscarPorId(usuarioId);
    }

    public void sair(String token) {
        sessoes.remove(token);
    }

    public void revogarSessoes(String usuarioId, String tokenPreservado) {
        sessoes.entrySet().removeIf(sessao -> sessao.getValue().equals(usuarioId)
                && !sessao.getKey().equals(tokenPreservado));
    }

    private byte[] derivar(String senha, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(senha.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Não foi possível validar a senha.", exception);
        } finally {
            spec.clearPassword();
        }
    }

    private record Credencial(byte[] salt, byte[] hash) {
    }

    public record Sessao(String token, Usuario usuario) {
    }
}
