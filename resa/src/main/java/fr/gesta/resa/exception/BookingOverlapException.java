package fr.gesta.resa.exception;

import fr.gesta.squelette.exception.ConflictException;

import static fr.gesta.resa.ErrorConst.OVERLAP_ERROR;

public class BookingOverlapException extends ConflictException {
    public BookingOverlapException() {
        super("le créneau de la réservation ne doit pas chevaucher une autre réservation.", OVERLAP_ERROR);
    }
}
