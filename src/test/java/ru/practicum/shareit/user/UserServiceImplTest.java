package ru.practicum.shareit.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserServiceImplTest {

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(new InMemoryUserRepository());
    }

    private UserDto dto(String name, String email) {
        return UserDto.builder().name(name).email(email).build();
    }

    @Test
    void createAssignsId() {
        UserDto created = service.create(dto("Ann", "ann@mail.ru"));

        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("Ann");
        assertThat(created.getEmail()).isEqualTo("ann@mail.ru");
    }

    @Test
    void createDuplicateEmailThrowsConflict() {
        service.create(dto("Ann", "ann@mail.ru"));

        assertThatThrownBy(() -> service.create(dto("Bob", "ann@mail.ru")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updatePartialKeepsOtherFields() {
        UserDto created = service.create(dto("Ann", "ann@mail.ru"));

        UserDto updated = service.update(created.getId(), dto("Anna", null));

        assertThat(updated.getName()).isEqualTo("Anna");
        assertThat(updated.getEmail()).isEqualTo("ann@mail.ru");
        assertThat(service.getById(created.getId()).getName()).isEqualTo("Anna");
    }

    @Test
    void updateToForeignEmailThrowsConflict() {
        service.create(dto("Ann", "ann@mail.ru"));
        UserDto bob = service.create(dto("Bob", "bob@mail.ru"));

        assertThatThrownBy(() -> service.update(bob.getId(), dto(null, "ann@mail.ru")))
                .isInstanceOf(ConflictException.class);
        assertThat(service.getById(bob.getId()).getEmail()).isEqualTo("bob@mail.ru");
    }

    @Test
    void updateWithOwnEmailIsAllowed() {
        UserDto ann = service.create(dto("Ann", "ann@mail.ru"));

        UserDto updated = service.update(ann.getId(), dto("Anna", "ann@mail.ru"));

        assertThat(updated.getEmail()).isEqualTo("ann@mail.ru");
    }

    @Test
    void oldEmailBecomesFree() {
        UserDto ann = service.create(dto("Ann", "ann@mail.ru"));
        service.update(ann.getId(), dto(null, "anna@mail.ru"));

        UserDto bob = service.create(dto("Bob", "ann@mail.ru"));

        assertThat(bob.getEmail()).isEqualTo("ann@mail.ru");
    }

    @Test
    void getUnknownThrowsNotFound() {
        assertThatThrownBy(() -> service.getById(42L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateUnknownThrowsNotFound() {
        assertThatThrownBy(() -> service.update(42L, dto("X", null))).isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteRemovesUser() {
        UserDto ann = service.create(dto("Ann", "ann@mail.ru"));

        service.delete(ann.getId());

        assertThatThrownBy(() -> service.getById(ann.getId())).isInstanceOf(NotFoundException.class);
        assertThat(service.getAll()).isEmpty();
    }

    @Test
    void getAllReturnsEveryUser() {
        service.create(dto("Ann", "ann@mail.ru"));
        service.create(dto("Bob", "bob@mail.ru"));

        assertThat(service.getAll()).extracting(UserDto::getName).containsExactlyInAnyOrder("Ann", "Bob");
    }
}
