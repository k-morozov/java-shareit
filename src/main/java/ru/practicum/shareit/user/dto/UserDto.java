package ru.practicum.shareit.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
public class UserDto {
    private Long id;

    @NotBlank(groups = Create.class)
    @Pattern(regexp = NotBlankIfPresent.REGEXP, message = NotBlankIfPresent.MESSAGE, groups = Update.class)
    private String name;

    @NotBlank(groups = Create.class)
    @Pattern(regexp = NotBlankIfPresent.REGEXP, message = NotBlankIfPresent.MESSAGE, groups = Update.class)
    @Email(groups = {Create.class, Update.class})
    private String email;
}
