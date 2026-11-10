package ru.practicum.shareit.booking;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import ru.practicum.shareit.booking.dto.BookingCreateDto;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class BookingCreateDtoJsonTest {

    @Autowired
    private JacksonTester<BookingCreateDto> json;

    @Test
    void parsesDatesInPattern() throws Exception {
        BookingCreateDto dto = json.parseObject(
                "{\"itemId\":1,\"start\":\"2030-01-01T10:00:00\",\"end\":\"2030-01-02T10:00:00\"}");

        assertThat(dto.getItemId()).isEqualTo(1L);
        assertThat(dto.getStart()).isEqualTo(LocalDateTime.of(2030, 1, 1, 10, 0));
        assertThat(dto.getEnd()).isEqualTo(LocalDateTime.of(2030, 1, 2, 10, 0));
    }

    @Test
    void parsesIsoDatesWithoutSecondsAndWithFractions() throws Exception {
        BookingCreateDto dto = json.parseObject(
                "{\"itemId\":1,\"start\":\"2030-01-01T10:00\",\"end\":\"2030-01-02T10:00:00.123\"}");

        assertThat(dto.getStart()).isEqualTo(LocalDateTime.of(2030, 1, 1, 10, 0));
        assertThat(dto.getEnd()).isEqualTo(LocalDateTime.of(2030, 1, 2, 10, 0, 0, 123_000_000));
    }

    @Test
    void serializesIsoDatesForServer() throws Exception {
        BookingCreateDto dto = new BookingCreateDto(1L, LocalDateTime.of(2030, 1, 1, 10, 0, 0, 123_000_000),
                LocalDateTime.of(2030, 1, 2, 10, 0));

        assertThat(json.write(dto)).extractingJsonPathStringValue("$.start").isEqualTo("2030-01-01T10:00:00.123");
        assertThat(json.write(dto)).extractingJsonPathStringValue("$.end").isEqualTo("2030-01-02T10:00:00");
    }
}
