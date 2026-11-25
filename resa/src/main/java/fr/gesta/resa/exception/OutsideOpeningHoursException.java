package fr.gesta.resa.exception;

public class OutsideOpeningHoursException extends RuntimeException {
  public OutsideOpeningHoursException(String message) {
    super(message);
  }
}
