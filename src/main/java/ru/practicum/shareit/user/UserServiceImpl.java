package ru.practicum.shareit.user;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserDto;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserDto create(UserDto userDto) {
        checkEmailIsFree(userDto.getEmail());
        User user = UserMapper.toUser(userDto);
        user.setId(null);
        User saved = userRepository.save(user);
        log.info("Created user id={}", saved.getId());
        return UserMapper.toUserDto(saved);
    }

    @Override
    public UserDto update(long id, UserDto userDto) {
        User user = getEntity(id);
        String newEmail = userDto.getEmail();
        if (newEmail != null && !newEmail.equals(user.getEmail())) {
            checkEmailIsFree(newEmail);
            user.setEmail(newEmail);
        }
        if (userDto.getName() != null && !userDto.getName().isBlank()) {
            user.setName(userDto.getName());
        }
        User saved = userRepository.save(user);
        log.info("Updated user id={}", id);
        return UserMapper.toUserDto(saved);
    }

    @Override
    public UserDto getById(long id) {
        return UserMapper.toUserDto(getEntity(id));
    }

    @Override
    public List<UserDto> getAll() {
        return userRepository.findAll().stream()
                .map(UserMapper::toUserDto)
                .toList();
    }

    @Override
    public void delete(long id) {
        getEntity(id);
        userRepository.deleteById(id);
        log.info("Deleted user id={}", id);
    }

    private User getEntity(long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User with id=" + id + " not found"));
    }

    private void checkEmailIsFree(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email " + email + " is already in use");
        }
    }
}
