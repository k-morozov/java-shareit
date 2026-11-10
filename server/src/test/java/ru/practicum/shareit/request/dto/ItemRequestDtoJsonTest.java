package ru.practicum.shareit.request.dto;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class ItemRequestDtoJsonTest {

    @Autowired
    private JacksonTester<ItemRequestDto> json;

    @Test
    void createdHasPatternAndItems() throws Exception {
        ItemRequestDto dto = ItemRequestDto.builder().id(5L).description("Нужна дрель")
                .created(LocalDateTime.of(2030, 1, 1, 10, 0, 0, 1))
                .items(List.of(new ItemAnswerDto(3L, "Дрель", 2L))).build();

        JsonContent<ItemRequestDto> content = json.write(dto);

        assertThat(content).extractingJsonPathStringValue("$.created").isEqualTo("2030-01-01T10:00:00");
        assertThat(content).extractingJsonPathNumberValue("$.items[0].ownerId").isEqualTo(2);
        assertThat(content).extractingJsonPathStringValue("$.items[0].name").isEqualTo("Дрель");
    }
}
