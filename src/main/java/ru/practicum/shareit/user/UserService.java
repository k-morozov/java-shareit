package ru.practicum.shareit.user;

import java.util.List;
import ru.practicum.shareit.user.dto.UserDto;

public interface UserService {

    UserDto create(UserDto userDto);

    UserDto update(long id, UserDto userDto);

    UserDto getById(long id);

    List<UserDto> getAll();

    void delete(long id);
}
