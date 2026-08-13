package fr.gesta.resa.booking;

import fr.gesta.resa.room.Room;
import fr.gesta.resa.room.RoomRepository;
import fr.gesta.squelette.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static fr.gesta.resa.ErrorConst.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

        BookingResponse result = bookingService.create(roomId, "username", start, end);

        assertEquals("username", result.user());
        assertEquals(start, result.startAt());
        assertEquals(end, result.endAt());
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void createBookingTestReturnNotFound() {
        Long roomId = 1L;
        Instant start = Instant.now().plusSeconds(3600);
        Instant end = start.plusSeconds(3600);

        when(roomRepository.findById(roomId)).thenReturn(Optional.empty());


        assertThatThrownBy(() -> bookingService.create(roomId, "username", start, end))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", ROOM_NOT_FOUND);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBookingTestReturnBadRequest() {
        Long roomId = 1L;
        Instant start = Instant.now().plusSeconds(3600);
        Instant end = start.plusSeconds(3600);

        assertThatThrownBy(() -> bookingService.create(roomId, "username", end, start))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", TIMING_ERROR);

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBookingTestReturnConflict() {
        Long roomId = 1L;
        Instant start = Instant.now().plusSeconds(3600);
        Instant end = start.plusSeconds(3600);
        Room room = new Room();

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(bookingRepository.hasOverlap(roomId, start, end, BookingState.CONFIRMED)).thenReturn(true);

        assertThatThrownBy(() -> bookingService.create(roomId, "username", start, end))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", OVERLAP_ERROR);

        verify(bookingRepository, never()).save(any());
    }
}
