package fr.gesta.resa.exception;

import fr.gesta.squelette.exception.BusinessRuleException;

import static fr.gesta.resa.ErrorConst.OUTSIDE_OPENING_HOURS;

public class OutsideOpeningHoursException extends BusinessRuleException {
    public OutsideOpeningHoursException() {
        super("Les réservations sont possibles du lundi au vendredi, de 8 h à 19 h.", OUTSIDE_OPENING_HOURS);
    }
}
