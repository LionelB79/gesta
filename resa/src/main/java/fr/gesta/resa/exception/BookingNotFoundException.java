package fr.gesta.resa.exception;

import fr.gesta.squelette.exception.NotFoundException;

import static fr.gesta.resa.ErrorConst.BOOKING_NOT_FOUND;

public class BookingNotFoundException extends NotFoundException {
    public BookingNotFoundException() {
        super("Réservation introuvable.", BOOKING_NOT_FOUND);
    }
}
