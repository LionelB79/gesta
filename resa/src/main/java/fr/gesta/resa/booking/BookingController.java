package fr.gesta.resa.booking;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }


    @PostMapping("/bookings")
    @ResponseStatus(code = HttpStatus.CREATED)
    public BookingResponse createBooking(@Valid @RequestBody CreateBookingRequest request, Authentication authentication) {
        return bookingService.create(request.roomId(),authentication.getName(), request.start(),request.end());

    }
}
