package ru.practicum.shareit.booking.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.Duration;
import java.time.LocalDateTime;

public class BookingDatesValidator implements ConstraintValidator<BookingDates, BookingCreateDto> {

    private static final Duration TIME_TOLERANCE = Duration.ofSeconds(5);

    @Override
    public boolean isValid(BookingCreateDto dto, ConstraintValidatorContext context) {
        if (dto.getStart() == null || dto.getEnd() == null) {
            return true;
        }
        LocalDateTime earliest = LocalDateTime.now().minus(TIME_TOLERANCE);
        return !dto.getStart().isBefore(earliest)
                && !dto.getEnd().isBefore(earliest)
                && dto.getEnd().isAfter(dto.getStart());
    }
}
