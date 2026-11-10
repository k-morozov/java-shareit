package ru.practicum.shareit.item.dto;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class CommentDtoJsonTest {

    @Autowired
    private JacksonTester<CommentDto> json;

    @Test
    void createdHasPattern() throws Exception {
        CommentDto dto = CommentDto.builder().id(1L).text("Ок").authorName("Ann")
                .created(LocalDateTime.of(2030, 1, 1, 10, 0, 0, 555_000_000)).build();

        JsonContent<CommentDto> content = json.write(dto);

        assertThat(content).extractingJsonPathStringValue("$.created").isEqualTo("2030-01-01T10:00:00");
        assertThat(content).extractingJsonPathStringValue("$.authorName").isEqualTo("Ann");
        assertThat(content).extractingJsonPathStringValue("$.text").isEqualTo("Ок");
    }
}
