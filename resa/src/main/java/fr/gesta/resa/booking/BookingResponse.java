package fr.gesta.resa.booking;

import java.time.Instant;

public record BookingResponse(Long id, String user, Instant startAt, Instant endAt, String roomName) {
}
