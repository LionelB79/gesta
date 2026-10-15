package fr.gesta.squelette.exception;

public abstract class UnauthenticatedException extends BusinessException {

  protected UnauthenticatedException(String message, String code) {

    super(message, code);
  }
}