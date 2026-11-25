package fr.gesta.resa.room;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @InjectMocks
    private RoomService roomService;

    @Mock
    private RoomRepository roomRepository;

    @Test
    void listReturnsEnabledRoomsMappedToResponseTest() {
        Room mars = new Room();
        mars.setName("mars");
        mars.setCapacity(3);
        when(roomRepository.findByEnabledTrueOrderByNameAsc()).thenReturn(List.of(mars));

        List<RoomResponse> result = roomService.list();

        assertThat(result)
                .extracting(RoomResponse::name, RoomResponse::capacity)
                .containsExactly(tuple("mars", 3));
    }
}