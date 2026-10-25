package fr.gesta.resa.room;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoomService {

    final private RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> list() {
        List<Room> rooms = roomRepository.findByEnabledTrueOrderByNameAsc();
        return rooms.stream().map(this::toResponse).toList();
    }


    private RoomResponse toResponse(Room room) {
        return new RoomResponse(
                room.getId(),
                room.getName(),
                room.getCapacity());
    }

}
