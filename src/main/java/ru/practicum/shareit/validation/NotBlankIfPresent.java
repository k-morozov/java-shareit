package ru.practicum.shareit.validation;

public final class NotBlankIfPresent {

    public static final String REGEXP = "(?s).*\\S.*";
    public static final String MESSAGE = "must not be blank";

    private NotBlankIfPresent() {
    }
}
