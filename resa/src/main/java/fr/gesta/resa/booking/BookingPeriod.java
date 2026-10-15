package fr.gesta.resa.booking;

import fr.gesta.resa.exception.InvalidBookingPeriodException;
import jakarta.persistence.Embeddable;

import java.time.Instant;

@Embeddable
public record BookingPeriod(Instant start, Instant end) {

    public BookingPeriod {
        if (start == null || end == null) {
            throw new InvalidBookingPeriodException("Les dates de début et de fin de la réservation sont obligatoires.");

        }
        if (!end.isAfter(start)) {
            throw new InvalidBookingPeriodException("La fin de la réservation doit être postérieure à son début.");
        }
    }
}
