package fr.gesta.resa.booking;

import fr.gesta.resa.TestApplication;
import fr.gesta.resa.exception.BookingOverlapException;
import fr.gesta.resa.room.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.*;

import static fr.gesta.resa.ErrorConst.OVERLAP_ERROR;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = TestApplication.class)
class BookingCallableTest {

    @Autowired
    BookingService bookingService;

    @Autowired
    BookingRepository bookingRepository;

    @Autowired
    RoomRepository roomRepository;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
    }

    @Test
    void concurrentCreationsOnSameSlotOnlyOneSucceeds() throws InterruptedException {
        Long roomId = roomRepository.findByName("demo").orElseThrow().getId();
        Instant start = Instant.now().plusSeconds(3600);
        BookingPeriod period = new BookingPeriod(start, start.plusSeconds(3600));
        CountDownLatch latch = new CountDownLatch(1);

        Callable<BookingResponse> call = () -> {
            latch.await();
            return bookingService.create(roomId, "username", period);
        };

        List<Future<BookingResponse>> futures;
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            futures = List.of(executor.submit(call), executor.submit(call));
            latch.countDown();
        }

        int success = 0;
        BookingOverlapException failure = null;
        for (Future<BookingResponse> future : futures) {
            try {
                future.get();
                success++;
            } catch (ExecutionException e) {
                failure = assertInstanceOf(BookingOverlapException.class, e.getCause());
            }
        }

        assertEquals(1, success);
        assertNotNull(failure);
        assertEquals(OVERLAP_ERROR, failure.getCode());
    }
}