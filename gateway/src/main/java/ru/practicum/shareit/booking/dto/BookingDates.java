package ru.practicum.shareit.booking.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BookingDatesValidator.class)
public @interface BookingDates {
    String message() default "Booking dates must not be in the past and end must be after start";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
