package fr.gesta.squelette;

import java.time.Instant;

public record ErrorResponse(Instant timestamp, int status, String code, String message, String path) {

}