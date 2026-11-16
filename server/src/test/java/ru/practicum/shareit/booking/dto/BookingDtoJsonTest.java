package ru.practicum.shareit.booking.dto;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import ru.practicum.shareit.booking.BookingStatus;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class BookingDtoJsonTest {

    private static final LocalDateTime START = LocalDateTime.of(2030, 1, 1, 10, 0, 0, 123_456_789);
    private static final LocalDateTime END = LocalDateTime.of(2030, 1, 2, 10, 0, 0, 987_000_000);

    @Autowired
    private JacksonTester<BookingDto> bookingJson;

    @Autowired
    private JacksonTester<BookingShortDto> shortJson;

    @Test
    void datesHaveNoFractionalSeconds() throws Exception {
        BookingDto dto = BookingDto.builder().id(1L).start(START).end(END).status(BookingStatus.WAITING)
                .item(new BookingDto.ItemRef(5L, "Дрель")).booker(new BookingDto.BookerRef(7L)).build();

        JsonContent<BookingDto> json = bookingJson.write(dto);

        assertThat(json).extractingJsonPathStringValue("$.start").isEqualTo("2030-01-01T10:00:00");
        assertThat(json).extractingJsonPathStringValue("$.end").isEqualTo("2030-01-02T10:00:00");
        assertThat(json).extractingJsonPathStringValue("$.item.name").isEqualTo("Дрель");
        assertThat(json).extractingJsonPathNumberValue("$.booker.id").isEqualTo(7);
        assertThat(json).extractingJsonPathStringValue("$.status").isEqualTo("WAITING");
    }

    @Test
    void shortDtoDates() throws Exception {
        JsonContent<BookingShortDto> json = shortJson.write(new BookingShortDto(1L, 7L, START, END));

        assertThat(json).extractingJsonPathNumberValue("$.bookerId").isEqualTo(7);
        assertThat(json).extractingJsonPathStringValue("$.start").isEqualTo("2030-01-01T10:00:00");
        assertThat(json).extractingJsonPathStringValue("$.end").isEqualTo("2030-01-02T10:00:00");
    }
}
