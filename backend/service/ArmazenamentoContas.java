package backend.service;

import backend.model.Administrador;
import backend.model.Usuario;
import backend.model.Veterinario;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class ArmazenamentoContas {
    private static final TypeReference<List<ContaPersistida>> TIPO_CONTAS = new TypeReference<>() { };

    private final Path arquivo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ArmazenamentoContas() {
        this(Path.of(System.getProperty("user.home"), ".abrigo-animais", "contas.json"));
    }

    public ArmazenamentoContas(Path arquivo) {
        this.arquivo = arquivo;
    }

    public void carregar(UsuarioService usuarioService, AutenticacaoService autenticacaoService)
            throws IOException {
        if (!Files.exists(arquivo)) {
            return;
        }

        List<ContaPersistida> contas = objectMapper.readValue(arquivo.toFile(), TIPO_CONTAS);
        if (contas == null) {
            throw new IOException("O arquivo de contas está vazio ou inválido.");
        }
        for (ContaPersistida conta : contas) {
            Usuario usuario = usuarioService.restaurar(
                    conta.tipo(), conta.id(), conta.nome(), conta.email(), conta.telefone(),
                    conta.crmv(), conta.nivelAcesso(), conta.administradorPrincipal(), conta.aprovado());
            autenticacaoService.restaurarCredencial(usuario.getId(),
                    new AutenticacaoService.CredencialPersistida(conta.salt(), conta.hash()));
        }
    }

    public void salvar(UsuarioService usuarioService, AutenticacaoService autenticacaoService)
            throws IOException {
        List<ContaPersistida> contas = usuarioService.listarTodos().stream()
                .map(usuario -> {
                    String tipo = usuario instanceof Administrador ? "Administrador"
                            : usuario instanceof Veterinario ? "Veterinário" : "Voluntário";
                    String crmv = usuario instanceof Veterinario veterinario ? veterinario.getCrmv() : "";
                    String nivelAcesso = usuario instanceof Administrador administrador
                            ? administrador.getNivelAcesso() : "";
                    boolean principal = usuario instanceof Administrador administrador
                            && administrador.isPrincipal();
                    AutenticacaoService.CredencialPersistida credencial =
                            autenticacaoService.obterCredencialPersistida(usuario.getId());
                    return new ContaPersistida(tipo, usuario.getId(), usuario.getNome(), usuario.getEmail(),
                            usuario.getTelefone(), crmv, nivelAcesso, principal, usuario.isAprovado(),
                            credencial.salt(), credencial.hash());
                })
                .toList();

        Path diretorio = arquivo.toAbsolutePath().getParent();
        Files.createDirectories(diretorio);
        Path temporario = Files.createTempFile(diretorio, "contas-", ".tmp");
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(temporario.toFile(), contas);
            try {
                Files.move(temporario, arquivo, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporario, arquivo, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporario);
        }
    }

    private record ContaPersistida(
            String tipo,
            String id,
            String nome,
            String email,
            String telefone,
            String crmv,
            String nivelAcesso,
            boolean administradorPrincipal,
            boolean aprovado,
            String salt,
            String hash) {
    }
}
