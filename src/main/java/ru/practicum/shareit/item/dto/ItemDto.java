package ru.practicum.shareit.item.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.shareit.validation.Create;
import ru.practicum.shareit.validation.NotBlankIfPresent;
import ru.practicum.shareit.validation.Update;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemDto {
    private Long id;

    @NotBlank(groups = Create.class)
    @Pattern(regexp = NotBlankIfPresent.REGEXP, message = NotBlankIfPresent.MESSAGE, groups = Update.class)
    private String name;

    @NotBlank(groups = Create.class)
    @Pattern(regexp = NotBlankIfPresent.REGEXP, message = NotBlankIfPresent.MESSAGE, groups = Update.class)
    private String description;

    @NotNull(groups = Create.class)
    private Boolean available;

    private Long requestId;
}
