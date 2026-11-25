package fr.gesta.resa.booking;


import fr.gesta.resa.exception.OutsideOpeningHoursException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static fr.gesta.resa.TestDates.paris;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpeningHoursTest {

    @Test
    void acceptsWeekdayFromOpening() {
        Instant start = paris(11, 24, 8, 0);
        Instant end = paris(11, 24, 10, 0);

        assertThatCode(() -> OpeningHours.check(start, end)).doesNotThrowAnyException();
    }

    @Test
    void acceptsEndingExactlyAtClosing() {
        Instant start = paris(11, 24, 18, 0);
        Instant end = paris(11, 24, 19, 0);

        assertThatCode(() -> OpeningHours.check(start, end)).doesNotThrowAnyException();
    }

    @Test
    void acceptsSummerOpening() {
        Instant start = paris(7, 6, 8, 0);
        Instant end = paris(7, 6, 9, 0);

        assertThatCode(() -> OpeningHours.check(start, end)).doesNotThrowAnyException();
    }

    @Test
    void rejectsStartBeforeOpening() {
        Instant start = paris(11, 24, 7, 0);
        Instant end = paris(11, 24, 8, 0);

        assertThatThrownBy(() -> OpeningHours.check(start, end))
                .isInstanceOf(OutsideOpeningHoursException.class);
    }

    @Test
    void rejectsEndAfterClosing() {
        Instant start = paris(11, 24, 18, 30);
        Instant end = paris(11, 24, 20, 0);

        assertThatThrownBy(() -> OpeningHours.check(start, end))
                .isInstanceOf(OutsideOpeningHoursException.class);
    }

    @Test
    void rejectsOvernight() {
        Instant start = paris(11, 24, 8, 30);
        Instant end = paris(11, 25, 10, 0);

        assertThatThrownBy(() -> OpeningHours.check(start, end))
                .isInstanceOf(OutsideOpeningHoursException.class);
    }

    @Test
    void rejectsOneWeekLater() {
        Instant start = paris(11, 24, 8, 30);
        Instant end = paris(12, 1, 10, 0);

        assertThatThrownBy(() -> OpeningHours.check(start, end))
                .isInstanceOf(OutsideOpeningHoursException.class);
    }

    @Test
    void rejectsSaturday() {
        Instant start = paris(11, 28, 8, 30);
        Instant end = paris(11, 28, 10, 0);

        assertThatThrownBy(() -> OpeningHours.check(start, end))
                .isInstanceOf(OutsideOpeningHoursException.class);
    }

    @Test
    void rejectsSunday() {
        Instant start = paris(11, 29, 8, 30);
        Instant end = paris(11, 29, 10, 0);

        assertThatThrownBy(() -> OpeningHours.check(start, end))
                .isInstanceOf(OutsideOpeningHoursException.class);
    }

}
