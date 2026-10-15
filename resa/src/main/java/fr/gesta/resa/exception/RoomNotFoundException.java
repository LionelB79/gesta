package fr.gesta.resa.exception;

import fr.gesta.squelette.exception.NotFoundException;

import static fr.gesta.resa.ErrorConst.ROOM_NOT_FOUND;

public class RoomNotFoundException extends NotFoundException {
    public RoomNotFoundException() {
        super("Salle introuvable.", ROOM_NOT_FOUND);
    }
}
