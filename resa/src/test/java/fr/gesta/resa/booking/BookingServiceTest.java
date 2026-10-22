package fr.gesta.resa.booking;

import fr.gesta.resa.exception.*;
import fr.gesta.resa.room.Room;
import fr.gesta.resa.room.RoomRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

import static fr.gesta.resa.ErrorConst.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookingServiceTest {

    @InjectMocks
    private BookingService bookingService;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private RoomRepository roomRepository;

    @Test
    void createBookingTest() {
        Long roomId = 1L;
        Instant start = Instant.now().plusSeconds(3600);
        Instant end = start.plusSeconds(3600);
        Room room = new Room();

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(bookingRepository.hasOverlap(roomId, start, end, BookingState.CONFIRMED)).thenReturn(false);

        BookingResponse result = bookingService.create(roomId, "username",new BookingPeriod(start, end));

        assertEquals("username", result.user());
        assertEquals(start, result.startAt());
        assertEquals(end, result.endAt());
        verify(bookingRepository).saveAndFlush(any(Booking.class));
    }

    @Test
    void createBookingTestReturnNotFound() {
        Long roomId = 1L;
        Instant start = Instant.now().plusSeconds(3600);
        Instant end = start.plusSeconds(3600);

        when(roomRepository.findById(roomId)).thenReturn(Optional.empty());


        assertThatThrownBy(() -> bookingService.create(roomId, "username", new BookingPeriod(start, end)))
                .isInstanceOf(RoomNotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ROOM_NOT_FOUND);
        verifyNoInteractions(bookingRepository);
    }


    @Test
    void createBookingTestReturnConflict() {
        Long roomId = 1L;
        Instant start = Instant.now().plusSeconds(3600);
        Instant end = start.plusSeconds(3600);
        Room room = new Room();

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(bookingRepository.hasOverlap(roomId, start, end, BookingState.CONFIRMED)).thenReturn(true);

        assertThatThrownBy(() -> bookingService.create(roomId, "username", new BookingPeriod(start, end)))
                .isInstanceOf(BookingOverlapException.class)
                .hasFieldOrPropertyWithValue("code", OVERLAP_ERROR);

        verify(bookingRepository).hasOverlap(roomId, start, end, BookingState.CONFIRMED);
        verifyNoMoreInteractions(bookingRepository);
    }

    @Test
    void listTest() {
        Long roomId = 1L;
        BookingState state = BookingState.CONFIRMED;
        Instant start = Instant.now().plusSeconds(3600);
        Instant end = start.plusSeconds(3600);
        Room room = new Room();
        room.setName("room1");
        Pageable pageable = PageRequest.of(0, 10);

        Booking booking1 = new Booking(room, "username", new BookingPeriod(start, end));
        Booking booking2 = new Booking(room, "username",  new BookingPeriod(start, end));

        PageImpl<Booking> bookings = new PageImpl<>(List.of(booking1, booking2), PageRequest.of(0, 2), 5);

        when(bookingRepository.findFilteredBookings(roomId, start, state, pageable)).thenReturn(bookings);

        Page<BookingResponse> result = bookingService.list(roomId, pageable, state, start);

        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.getNumber()).isZero();
        assertThat(result.getContent())
                .extracting(BookingResponse::roomName)
                .containsExactly("room1", "room1");
    }

    @Test
    void listTestWithEmptyList() {
        Long roomId = 1L;
        Instant start = Instant.now().plusSeconds(3600);
        BookingState state = BookingState.CONFIRMED;
        Pageable pageable = PageRequest.of(0, 10);

        when(bookingRepository.findFilteredBookings(roomId, start, state, pageable)).thenReturn(Page.empty());

        Page<BookingResponse> result = bookingService.list(roomId, pageable, state, start);
        assertThat(result).isEmpty();
    }

    @Test
    void cancelTestWithBooking() {
        Instant start = Instant.now().plusSeconds(3600);
        Instant end = start.plusSeconds(3600);
        Room room = new Room();
        room.setName("room1");

        Booking booking = new Booking(room, "username", new BookingPeriod(start, end));


        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        BookingResponse result = bookingService.cancel(booking.getId(), "username");

        assertEquals(BookingState.CANCELED, result.state());
    }

    @Test
    void cancelTestReturnNotFound() {
        Instant start = Instant.now().plusSeconds(3600);
        Instant end = start.plusSeconds(3600);
        Room room = new Room();
        room.setName("room1");

        Booking booking = new Booking(room, "username",new BookingPeriod(start, end));


        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.cancel(booking.getId(), "username"))
                .isInstanceOf(BookingNotFoundException.class)
                .hasFieldOrPropertyWithValue("code", BOOKING_NOT_FOUND);

        assertEquals(BookingState.CONFIRMED, booking.getState());
    }

    @Test
    void cancelTestReturnForbidden() {
        Instant start = Instant.now().plusSeconds(3600);
        Instant end = start.plusSeconds(3600);
        Room room = new Room();
        room.setName("room1");

        Booking booking = new Booking(room, "username",new BookingPeriod(start, end));


        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancel(booking.getId(), "username1"))
                .isInstanceOf(ForbiddenBookingException.class)
                .hasFieldOrPropertyWithValue("code", FORBIDDEN_BOOKING);

        assertEquals(BookingState.CONFIRMED, booking.getState());
    }

    @Test
    void cancelRejectsAlreadyCanceledBooking() {
        Instant start = Instant.now().plusSeconds(3600);
        Instant end = start.plusSeconds(3600);
        Room room = new Room();
        room.setName("room1");

        Booking booking = new Booking(room, "username", new BookingPeriod(start, end));
        booking.setState(BookingState.CANCELED);

        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancel(booking.getId(), "username"))
                .isInstanceOf(BookingAlreadyCanceledException.class)
                .hasFieldOrPropertyWithValue("code", ALREADY_CANCELED);

        assertEquals(BookingState.CANCELED, booking.getState());
    }
    @Test
    void listWithoutRoomIdStillAppliesOtherFiltersTest() {
        Pageable pageable = PageRequest.of(0, 10);
        BookingState state = BookingState.CANCELED;

        when(bookingRepository.findFilteredBookings(null, null, state, pageable)).thenReturn(Page.empty());

        Page<BookingResponse> result = bookingService.list(null, pageable, state, null);

        assertThat(result).isEmpty();
        verify(bookingRepository).findFilteredBookings(null, null, state, pageable);
        verifyNoMoreInteractions(bookingRepository);
    }
}
