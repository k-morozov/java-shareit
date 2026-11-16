package ru.practicum.shareit.booking;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingState;

import static ru.practicum.shareit.item.ItemController.USER_ID_HEADER;

@Slf4j
@RestController
@RequestMapping(path = "/bookings")
@RequiredArgsConstructor
public class BookingController {
    private final BookingClient bookingClient;

    @PostMapping
    public ResponseEntity<Object> create(@RequestHeader(USER_ID_HEADER) long userId,
                                         @RequestBody @Valid BookingCreateDto dto) {
        log.info("Create booking {} by user id={}", dto, userId);
        return bookingClient.create(userId, dto);
    }

    @PatchMapping("/{bookingId}")
    public ResponseEntity<Object> approve(@RequestHeader(USER_ID_HEADER) long userId,
                                          @PathVariable long bookingId,
                                          @RequestParam boolean approved) {
        log.info("Approve booking id={} by user id={}: {}", bookingId, userId, approved);
        return bookingClient.approve(userId, bookingId, approved);
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<Object> getById(@RequestHeader(USER_ID_HEADER) long userId,
                                          @PathVariable long bookingId) {
        return bookingClient.getById(userId, bookingId);
    }

    @GetMapping
    public ResponseEntity<Object> getByBooker(@RequestHeader(USER_ID_HEADER) long userId,
                                              @RequestParam(defaultValue = "ALL") String state) {
        return bookingClient.getByBooker(userId, parse(state));
    }

    @GetMapping("/owner")
    public ResponseEntity<Object> getByOwner(@RequestHeader(USER_ID_HEADER) long userId,
                                             @RequestParam(defaultValue = "ALL") String state) {
        return bookingClient.getByOwner(userId, parse(state));
    }

    private static BookingState parse(String state) {
        return BookingState.from(state)
                .orElseThrow(() -> new IllegalArgumentException("Unknown state: " + state));
    }
}
