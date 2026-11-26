package fr.gesta.resa.booking;

import fr.gesta.resa.exception.OutsideOpeningHoursException;

import java.time.*;

public final class OpeningHours {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private static final LocalTime START_TIME = LocalTime.of(8, 0);
    private static final LocalTime END_TIME = LocalTime.of(19, 0);

    private OpeningHours() {
    }

    public static void check(Instant start, Instant end) {
        ZonedDateTime bookingStart = start.atZone(PARIS);
        ZonedDateTime bookingEnd = end.atZone(PARIS);

        if (bookingStart.toLocalTime().isBefore(START_TIME)
                || bookingEnd.toLocalTime().isAfter(END_TIME)
                || !bookingStart.toLocalDate().isEqual(bookingEnd.toLocalDate())
                || bookingStart.getDayOfWeek() == DayOfWeek.SATURDAY || bookingStart.getDayOfWeek() == DayOfWeek.SUNDAY
        ) {
            throw new OutsideOpeningHoursException();

        }
    }
}
