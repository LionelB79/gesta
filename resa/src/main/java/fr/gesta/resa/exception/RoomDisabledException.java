package fr.gesta.resa.exception;

import fr.gesta.squelette.exception.BusinessRuleException;

import static fr.gesta.resa.ErrorConst.ROOM_DISABLED;

public class RoomDisabledException extends BusinessRuleException {
    public RoomDisabledException() {
        super("Cette salle n’est pas disponible à la réservation.", ROOM_DISABLED);
    }

}