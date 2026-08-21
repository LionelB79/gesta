package fr.gesta.resa.booking;

import fr.gesta.resa.room.Room;
import fr.gesta.resa.room.RoomRepository;
import fr.gesta.squelette.ApiException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
            throw new ApiException("le créneau de la réservation ne doit pas chevaucher une autre réservation", HttpStatus.CONFLICT, OVERLAP_ERROR);
        }
        Booking booking = new Booking(room, user, start, end);

        try {
            bookingRepository.saveAndFlush(booking);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException("le créneau de la réservation ne doit pas chevaucher une autre réservation", HttpStatus.CONFLICT, OVERLAP_ERROR);
        }


        return toResponse(booking);
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> list(Long roomId, Pageable pageable, BookingState state, Instant start) {

        Page<Booking> bookings = bookingRepository.findFilteredBookings(roomId, start, state, pageable);

        return bookings.map(this::toResponse);
    }


    @Transactional(readOnly = true)
    public Page<BookingResponse> listAll(Pageable pageable) {

        return bookingRepository.findAll(pageable).map(this::toResponse);
    }

    private BookingResponse toResponse(Booking booking) {
        return new BookingResponse(booking.getId(), booking.getUsername(), booking.getStartAt(), booking.getEndAt(), booking.getRoom().getName(), booking.getState());
    }

    @Transactional(readOnly = true)
    public List<BookingStats> countBookings() {

        Map<String, Long> bookings = bookingRepository.findByState(BookingState.CONFIRMED)
                .stream()
                .collect(Collectors.groupingBy(
                        booking -> booking.getRoom().getName(),
                        Collectors.counting()));

        return bookings.entrySet().stream()
                .map(entry -> new BookingStats(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Transactional
    public BookingResponse cancel(Long id, String user) {

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ApiException("Réservation introuvable", HttpStatus.NOT_FOUND, BOOKING_NOT_FOUND));

        if (!Objects.equals(booking.getUsername(), user)) {
            throw new ApiException("Impossible de supprimer la réservation d'un autre", HttpStatus.FORBIDDEN, FORBIDDEN_BOOKING);
        }

        if (booking.getState() == BookingState.CANCELED) {
            throw new ApiException("La réservation a déjà été annulée", HttpStatus.CONFLICT, ALREADY_CANCELED);

        }

        booking.setState(BookingState.CANCELED);

        return toResponse(booking);

    }
}
