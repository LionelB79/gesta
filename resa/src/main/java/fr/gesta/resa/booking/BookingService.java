package fr.gesta.resa.booking;

import fr.gesta.resa.exception.*;
import fr.gesta.resa.room.Room;
import fr.gesta.resa.room.RoomRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class BookingService {

    // SQLState PostgreSQL d'une violation de contrainte d'exclusion.
    // bookings n'en a qu'une : excl_bookings_overlap (resa/002-threadsbookings.yaml)
    private static final String EXCLUSION_VIOLATION = "23P01";

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;

    public BookingService(BookingRepository bookingRepository, RoomRepository roomRepository) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
    }

    @Transactional
    public BookingResponse create(Long roomId, String user, BookingPeriod period) {

        Room room = roomRepository.findById(roomId).orElseThrow(RoomNotFoundException::new);
        if (bookingRepository.hasOverlap(roomId, period.start(), period.end(), BookingState.CONFIRMED)) {
            throw new BookingOverlapException();
        }
        Booking booking = new Booking(room, user, period);

        try {
            bookingRepository.saveAndFlush(booking);
        } catch (DataIntegrityViolationException e) {
            if (e.getCause() instanceof ConstraintViolationException cve
                    && EXCLUSION_VIOLATION.equals(cve.getSQLState())) {
                throw new BookingOverlapException();
            }
            throw e;
        }


        return toResponse(booking);
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> list(Long roomId, Pageable pageable, BookingState state, Instant start) {

        Page<Booking> bookings = bookingRepository.findFilteredBookings(roomId, start, state, pageable);

        return bookings.map(this::toResponse);
    }


    private BookingResponse toResponse(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getUsername(),
                booking.getPeriod().start(),
                booking.getPeriod().end(),
                booking.getRoom().getName(),
                booking.getState());
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
                .orElseThrow(BookingNotFoundException::new);

        if (!Objects.equals(booking.getUsername(), user)) {
            throw new ForbiddenBookingException();
        }

        if (booking.getState() == BookingState.CANCELED) {
            throw new BookingAlreadyCanceledException();
        }

        booking.setState(BookingState.CANCELED);

        return toResponse(booking);

    }
}
