package fr.gesta.resa;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

final public class TestDates {
    private final static ZoneId ZONE_EUROPE = ZoneId.of("Europe/Paris");

    private TestDates() {
    }

    public static Instant paris(int mois, int jour, int heure, int minutes) {
        return LocalDateTime.of(2026, mois, jour, heure, minutes).atZone(ZONE_EUROPE).toInstant();
    }
}
