package fr.gesta.resa.booking;

import fr.gesta.resa.room.Room;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "bookings", schema = "resa")
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 20)
    private BookingState state;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;


    public Booking() {
    }

    public Booking(Room room, String username, Instant startAt, Instant endAt) {
        this.room = room;
        this.username = username;
        this.startAt = startAt;
        this.endAt = endAt;
        this.state = BookingState.CONFIRMED;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }


    public BookingState getState() {
        return state;
    }

    public void setState(BookingState state) {
        this.state = state;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public void setEndAt(Instant endAt) {
        this.endAt = endAt;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public void setStartAt(Instant startAt) {
        this.startAt = startAt;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public Long getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Booking{ id='" + id + '\'' +
                ", username=" + username +
                ", createdAt=" + createdAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Booking booking)) return false;
        return id != null && id.equals(booking.id);}

    @Override
    public int hashCode() {
        return Booking.class.hashCode();
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }


}
