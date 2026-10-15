package fr.gesta.resa.booking;

import fr.gesta.resa.exception.InvalidBookingPeriodException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static fr.gesta.resa.ErrorConst.TIMING_ERROR;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

public class BookingPeriodTest {

    @Test
    void bookingPeriodAcceptsEndAfterStart() {
        Instant start = Instant.parse("2026-11-01T09:00:00Z");
        Instant end = Instant.parse("2026-11-01T10:00:00Z");

        BookingPeriod period = new BookingPeriod(start, end);

        assertThat(period.start()).isEqualTo(start);
        assertThat(period.end()).isEqualTo(end);
    }
    @Test
    void bookingPeriodRejectsEndBeforeStart() {
        Instant start = Instant.parse("2026-11-01T10:00:00Z");
        Instant end = Instant.parse("2026-11-01T09:00:00Z");


        assertThatThrownBy(() -> new BookingPeriod(start, end))
                .isInstanceOf(InvalidBookingPeriodException.class)
                .hasFieldOrPropertyWithValue("code", TIMING_ERROR)
                .hasFieldOrPropertyWithValue("message", "La fin de la réservation doit être postérieure à son début.");
    }

    @Test
    void bookingPeriodRejectsEqualDates() {
        Instant start = Instant.parse("2026-11-01T10:00:00Z");
        Instant end = Instant.parse("2026-11-01T10:00:00Z");


        assertThatThrownBy(() -> new BookingPeriod(start, end))
                .isInstanceOf(InvalidBookingPeriodException.class)
                .hasFieldOrPropertyWithValue("code", TIMING_ERROR)
                .hasFieldOrPropertyWithValue("message", "La fin de la réservation doit être postérieure à son début.");
    }
    @Test
    void bookingPeriodRejectsNullStart() {
        Instant start = null;
        Instant end = Instant.parse("2026-11-01T10:00:00Z");

        assertThatThrownBy(() -> new BookingPeriod(start, end))
                .isInstanceOf(InvalidBookingPeriodException.class)
                .hasFieldOrPropertyWithValue("code", TIMING_ERROR)
                .hasFieldOrPropertyWithValue("message", "Les dates de début et de fin de la réservation sont obligatoires.");
    }
}
