package ru.practicum.shareit.request;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ItemRequestTest {

    @Test
    void builderSetsCreatedToNowByDefault() {
        LocalDateTime before = LocalDateTime.now();

        LocalDateTime created = ItemRequest.builder().description("Нужна дрель").build().getCreated();

        assertThat(created).isBetween(before, LocalDateTime.now());
    }

    @Test
    void noArgsConstructorSetsCreatedToNow() {
        LocalDateTime before = LocalDateTime.now();

        LocalDateTime created = new ItemRequest().getCreated();

        assertThat(created).isBetween(before, LocalDateTime.now());
    }

    @Test
    void explicitCreatedIsKept() {
        LocalDateTime created = LocalDateTime.of(2030, 1, 1, 10, 0);

        assertThat(ItemRequest.builder().created(created).build().getCreated()).isEqualTo(created);
    }
}
