package fr.gesta.squelette.exception;

public abstract class NotFoundException extends BusinessException {

    protected NotFoundException(String message, String code) {

       super(message, code);
    }
}
