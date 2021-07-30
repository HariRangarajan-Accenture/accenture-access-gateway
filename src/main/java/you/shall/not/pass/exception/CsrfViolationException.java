package you.shall.not.pass.exception;

public class CsrfViolationException extends RuntimeException {

    public CsrfViolationException( String message) {
        super(message);
    }
}
