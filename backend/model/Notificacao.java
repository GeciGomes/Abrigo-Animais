package backend.model;

import java.time.LocalDate;

public class Notificacao implements Identificavel {
    public enum Tipo {
        NOVO_ANIMAL,
        PROXIMO_EVENTO,
        VACINA_PROXIMA_VENCIMENTO,
        SOLICITACAO_ADOCAO,
        AVISO_GERAL
    }

    private final String id;
    private final Tipo tipo;
    private final String titulo;
    private final String mensagem;
    private final LocalDate data;
    private final String referenciaId;
    private final boolean somenteAdministradores;

    public Notificacao(String id, Tipo tipo, String titulo, String mensagem, LocalDate data,
            String referenciaId, boolean somenteAdministradores) {
        this.id = id;
        this.tipo = tipo;
        this.titulo = titulo;
        this.mensagem = mensagem;
        this.data = data;
        this.referenciaId = referenciaId;
        this.somenteAdministradores = somenteAdministradores;
    }

    public String getId() {
        return id;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getMensagem() {
        return mensagem;
    }

    public LocalDate getData() {
        return data;
    }

    public String getReferenciaId() {
        return referenciaId;
    }

    public boolean isSomenteAdministradores() {
        return somenteAdministradores;
    }
}
