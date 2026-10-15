package fr.gesta.resa.booking;

import fr.gesta.resa.TestApplication;
import fr.gesta.resa.exception.BookingOverlapException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.concurrent.*;

import static fr.gesta.resa.ErrorConst.OVERLAP_ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = TestApplication.class)
public class BookingCallableTest {

    @Autowired
    BookingService bookingService;

    @Autowired
    BookingRepository bookingRepository;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
    }

    @Test
    void createTestWithTwoBookings() throws InterruptedException {
        Long roomId = 1L;
        Instant start = Instant.now().plusSeconds(3600);
        Instant end = start.plusSeconds(3600);

        CountDownLatch latch = new CountDownLatch(1);

        ExecutorService executorService = Executors.newFixedThreadPool(2);

        Callable<BookingResponse> call1 = () -> {
            latch.await();
            return bookingService.create(roomId, "username", start, end);
        };

        Callable<BookingResponse> call2 = () -> {
            latch.await();
            return bookingService.create(roomId, "username", start, end);
        };

        Future<BookingResponse> resp = executorService.submit(call1);
        Future<BookingResponse> resp2 = executorService.submit(call2);


        latch.countDown();

        int succes = 0;
        BookingOverlapException fail = null;


        try {
            BookingResponse result = resp.get();
            succes += 1;
        } catch (ExecutionException e) {
            fail = (BookingOverlapException) e.getCause();
        }

        try {
            BookingResponse result2 = resp2.get();
            succes += 1;
        } catch (ExecutionException e) {
            fail = (BookingOverlapException) e.getCause();
        }

        executorService.shutdown();

        assertEquals(1, succes);
        assertNotNull(fail);
        assertEquals(OVERLAP_ERROR, fail.getCode());
    }
}
