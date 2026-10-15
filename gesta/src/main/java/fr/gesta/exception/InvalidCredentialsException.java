package fr.gesta.exception;

import fr.gesta.squelette.exception.UnauthenticatedException;

import static fr.gesta.ErrorConst.INVALID_CREDENTIALS;

public class InvalidCredentialsException extends UnauthenticatedException {
    public InvalidCredentialsException() {
        super("Identifiants invalides", INVALID_CREDENTIALS);
    }
}
