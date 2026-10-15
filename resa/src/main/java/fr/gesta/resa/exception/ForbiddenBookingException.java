package fr.gesta.resa.exception;

import fr.gesta.squelette.exception.ForbiddenException;

import static fr.gesta.resa.ErrorConst.FORBIDDEN_BOOKING;

public class ForbiddenBookingException extends ForbiddenException {
    public ForbiddenBookingException() {
        super("Vous ne pouvez annuler que vos propres réservations.", FORBIDDEN_BOOKING);
    }
}
