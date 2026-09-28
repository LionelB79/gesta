package fr.gesta.resa.booking;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }


    @PostMapping("/bookings")
    @ResponseStatus(code = HttpStatus.CREATED)
    public BookingResponse createBooking(@Valid @RequestBody CreateBookingRequest request, Authentication authentication) {
        return bookingService.create(request.roomId(), authentication.getName(), request.start(), request.end());

    }
    @GetMapping("/bookings")
    public Page<BookingResponse> findFilteredBookings(@RequestParam(required = false) Long roomId, Pageable pageable, @RequestParam(required = false) BookingState state,  @RequestParam(required = false) Instant start) {
        return roomId != null ? bookingService.list(roomId, pageable, state, start) : bookingService.listAll(pageable);
    }


    @GetMapping("/bookings/stats")
    public List<BookingStats> countBookings() {
        return  bookingService.countBookings();
    }


    @PostMapping("/bookings/{id}")
    public BookingResponse cancelBooking(@PathVariable Long id, Authentication authentication) {
        return bookingService.cancel(id, authentication.getName());
    }

}
