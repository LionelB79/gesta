package fr.gesta.resa.booking;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BookingControllerTest {

    @InjectMocks
    private BookingController bookingController;

    @Mock
    private BookingService bookingService;

    @Test
    public void createBookingTest() {
        Long roomId = 1L;
        String userName = "test";
        Instant start = Instant.now().plusSeconds(3600);
        Instant end = start.plusSeconds(3600);
        String roomName = "test";

        Authentication authentication = Mockito.mock(Authentication.class);
        CreateBookingRequest createBookingRequest = new CreateBookingRequest(roomId,start,end);
        BookingResponse bookingResponse = new BookingResponse(roomId,userName,start,end,roomName);

        when(authentication.getName()).thenReturn(userName);

        when(bookingService.create(roomId, userName, start, end)).thenReturn(bookingResponse);

        BookingResponse response = bookingController.createBooking(createBookingRequest, authentication);

        assertEquals(createBookingRequest.start(), response.startAt());
        assertEquals(createBookingRequest.end(), response.endAt());

    }
}
