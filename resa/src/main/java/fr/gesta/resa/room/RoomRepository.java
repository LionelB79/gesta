package fr.gesta.resa.room;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    Optional<Room> findByName(String name);

    List<Room> findByEnabledTrueOrderByNameAsc();
}
