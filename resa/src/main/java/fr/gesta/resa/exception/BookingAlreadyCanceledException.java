package fr.gesta.resa.exception;

import fr.gesta.squelette.exception.ConflictException;

import static fr.gesta.resa.ErrorConst.ALREADY_CANCELED;

public class BookingAlreadyCanceledException extends ConflictException {
    public BookingAlreadyCanceledException() {
        super("La réservation a déjà été annulée.", ALREADY_CANCELED);
    }
}
