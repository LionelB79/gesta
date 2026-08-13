package fr.gesta.resa.booking;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CreateBookingRequest(@NotNull Long roomId, @NotNull @Future Instant start, @NotNull  Instant end) {
}
