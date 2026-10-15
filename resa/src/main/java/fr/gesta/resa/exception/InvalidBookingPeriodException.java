package fr.gesta.resa.exception;

import fr.gesta.squelette.exception.BusinessRuleException;

import static fr.gesta.resa.ErrorConst.TIMING_ERROR;

public class InvalidBookingPeriodException extends BusinessRuleException {
    public InvalidBookingPeriodException(String message) {
        super(message, TIMING_ERROR);

    }
}
