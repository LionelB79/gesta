package fr.gesta.resa.booking;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("""
            SELECT COUNT(b) > 0 FROM Booking b WHERE
            b.room.id = :roomId AND b.state = :state AND b.period.start < :endAt AND :startAt < b.period.end
            """)
    boolean hasOverlap(@Param("roomId") Long roomId,
                       @Param("startAt") Instant startAt,
                       @Param("endAt") Instant endAt,
                       @Param("state") BookingState state);

    @EntityGraph(attributePaths = "room")
    Page<Booking> findAll(Pageable pageable);

    @Query("""
            SELECT b FROM Booking b WHERE (:roomId IS NULL OR b.room.id = :roomId)
            AND (:state IS NULL OR b.state = :state)
            AND (CAST(:startAt AS timestamp) IS NULL OR b.period.start >= :startAt)
            """)
    @EntityGraph(attributePaths = "room")
    Page<Booking> findFilteredBookings(
            @Param("roomId") Long roomId,
            @Param("startAt") Instant startAt,
            @Param("state") BookingState state,
            Pageable pageable);

    @EntityGraph(attributePaths = "room")
    List<Booking> findByState(BookingState state);
}