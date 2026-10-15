package fr.gesta.squelette.exception;

public abstract class BusinessException extends RuntimeException {

    private final String code;

    protected BusinessException(String message, String code) {
        super(message);
        this.code = code;
    }
    public String getCode() {
        return code;
    }
}
