package backend.exception;

public class AutenticacaoException extends RuntimeException {
    public AutenticacaoException(String mensagem) {
        super(mensagem);
    }
}
