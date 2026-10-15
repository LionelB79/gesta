package fr.gesta.squelette.exception;

public abstract class ForbiddenException extends BusinessException {

    protected ForbiddenException(String message, String code) {

        super(message, code);
    }
}