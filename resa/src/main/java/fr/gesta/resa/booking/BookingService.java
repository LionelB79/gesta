package fr.gesta.resa.booking;

import fr.gesta.resa.room.Room;
import fr.gesta.resa.room.RoomRepository;
import fr.gesta.squelette.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static fr.gesta.resa.ErrorConst.*;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;

    public BookingService(BookingRepository bookingRepository, RoomRepository roomRepository) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
    }

    @Transactional
    public BookingResponse create(Long roomId, String user, Instant start, Instant end) {
        if (!end.isAfter(start)) {
            throw new ApiException("L'heure de début de la réservation doit précéder l'heure de fin", HttpStatus.BAD_REQUEST, TIMING_ERROR);
        }

        Room room = roomRepository.findById(roomId).orElseThrow(() -> new ApiException("Salle introuvable", HttpStatus.NOT_FOUND, ROOM_NOT_FOUND));
        if (bookingRepository.hasOverlap(roomId, start, end, BookingState.CONFIRMED)) {
            throw new ApiException("le crénau de la réservation ne doit pas chevaucher une autre réservation", HttpStatus.CONFLICT, OVERLAP_ERROR);
        }
        Booking booking = new Booking(room, user, start, end);


        bookingRepository.save(booking);

        return new BookingResponse(booking.getId(), booking.getUsername(), start, end, room.getName());
    }
}
