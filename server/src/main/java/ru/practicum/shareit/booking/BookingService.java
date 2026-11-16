package ru.practicum.shareit.booking;

import java.util.List;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;

public interface BookingService {

    BookingDto create(long userId, BookingCreateDto dto);

    BookingDto approve(long userId, long bookingId, boolean approved);

    BookingDto getById(long userId, long bookingId);

    List<BookingDto> getByBooker(long userId, BookingState state);

    List<BookingDto> getByOwner(long userId, BookingState state);
}
