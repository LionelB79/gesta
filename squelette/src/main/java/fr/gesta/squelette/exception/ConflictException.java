package fr.gesta.squelette.exception;

public abstract class ConflictException extends BusinessException {

    protected ConflictException(String message, String code) {

        super(message, code);
    }
}