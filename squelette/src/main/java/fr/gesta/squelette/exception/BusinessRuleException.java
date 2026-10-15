package fr.gesta.squelette.exception;

public abstract class BusinessRuleException extends BusinessException {

    protected BusinessRuleException(String message, String code) {

        super(message, code);
    }
}