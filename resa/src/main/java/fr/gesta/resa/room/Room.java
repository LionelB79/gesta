package fr.gesta.resa.room;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "rooms", schema = "resa")
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "capacity", nullable = false)
    private int capacity;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;


    public Room() {
    }



    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "Room{ id='" + id + '\'' +
                ", enabled=" + enabled +
                ", createdAt=" + createdAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Room room)) return false;
        return id != null && id.equals(room.id);
    }

    @Override
    public int hashCode() {
        return Room.class.hashCode();
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }


}
