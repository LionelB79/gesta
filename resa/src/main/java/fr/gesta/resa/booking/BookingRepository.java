package fr.gesta.resa.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("""
            SELECT COUNT(b) > 0 FROM Booking b WHERE
            b.room.id = :roomId AND b.state = :state AND b.startAt < :endAt AND :startAt < b.endAt
            """)
    boolean hasOverlap(@Param("roomId") Long roomId,
                       @Param("startAt") Instant startAt,
                       @Param("endAt") Instant endAt,
                       @Param("state") BookingState state);
}