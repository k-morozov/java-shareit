package ru.practicum.shareit.item;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.user.InMemoryUserRepository;
import ru.practicum.shareit.user.UserService;
import ru.practicum.shareit.user.UserServiceImpl;
import ru.practicum.shareit.user.dto.UserDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ItemServiceImplTest {

    private ItemService itemService;
    private UserService userService;
    private long ownerId;
    private long otherId;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(new InMemoryUserRepository());
        itemService = new ItemServiceImpl(new InMemoryItemRepository(), userService);
        ownerId = userService.create(UserDto.builder().name("Owner").email("owner@mail.ru").build()).getId();
        otherId = userService.create(UserDto.builder().name("Other").email("other@mail.ru").build()).getId();
    }

    private ItemDto dto(String name, String description, Boolean available) {
        return ItemDto.builder().name(name).description(description).available(available).build();
    }

    @Test
    void createByUnknownUserThrowsNotFound() {
        assertThatThrownBy(() -> itemService.create(999L, dto("Дрель", "Ударная", true)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void createSetsOwnerAndId() {
        ItemDto created = itemService.create(ownerId, dto("Дрель", "Ударная", true));

        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("Дрель");
        assertThat(created.getDescription()).isEqualTo("Ударная");
        assertThat(created.getAvailable()).isTrue();
        assertThat(itemService.getOwnerItems(ownerId)).extracting(ItemDto::getId).containsExactly(created.getId());
    }

    @Test
    void updateByNonOwnerThrowsForbidden() {
        ItemDto item = itemService.create(ownerId, dto("Дрель", "Ударная", true));

        assertThatThrownBy(() -> itemService.update(otherId, item.getId(), dto("Чужая", null, null)))
                .isInstanceOf(ForbiddenException.class);
        assertThat(itemService.getById(item.getId()).getName()).isEqualTo("Дрель");
    }

    @Test
    void updateByUnknownUserThrowsNotFound() {
        ItemDto item = itemService.create(ownerId, dto("Дрель", "Ударная", true));

        assertThatThrownBy(() -> itemService.update(999L, item.getId(), dto("X", null, null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateUnknownItemThrowsNotFound() {
        assertThatThrownBy(() -> itemService.update(ownerId, 999L, dto("X", null, null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateOnlyAvailable() {
        ItemDto item = itemService.create(ownerId, dto("Дрель", "Ударная", true));

        ItemDto updated = itemService.update(ownerId, item.getId(), dto(null, null, false));

        assertThat(updated.getAvailable()).isFalse();
        assertThat(updated.getName()).isEqualTo("Дрель");
        assertThat(updated.getDescription()).isEqualTo("Ударная");
    }

    @Test
    void updateNameAndDescription() {
        ItemDto item = itemService.create(ownerId, dto("Дрель", "Ударная", true));

        ItemDto updated = itemService.update(ownerId, item.getId(), dto("Дрель 2", "Новая", null));

        assertThat(updated.getName()).isEqualTo("Дрель 2");
        assertThat(updated.getDescription()).isEqualTo("Новая");
        assertThat(updated.getAvailable()).isTrue();
    }

    @Test
    void getByIdUnknownThrowsNotFound() {
        assertThatThrownBy(() -> itemService.getById(999L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getOwnerItemsReturnsOnlyOwn() {
        itemService.create(ownerId, dto("Дрель", "Ударная", true));
        itemService.create(ownerId, dto("Пила", "Ручная", true));
        itemService.create(otherId, dto("Молоток", "Тяжёлый", true));

        assertThat(itemService.getOwnerItems(ownerId)).extracting(ItemDto::getName)
                .containsExactlyInAnyOrder("Дрель", "Пила");
    }

    @Test
    void getOwnerItemsOfUnknownUser404() {
        assertThatThrownBy(() -> itemService.getOwnerItems(999L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void searchCaseInsensitiveByNameAndDescription() {
        itemService.create(ownerId, dto("Дрель Салют", "Мощная", true));
        itemService.create(ownerId, dto("Набор", "Подходит к дрели и шуруповёрту", true));
        itemService.create(ownerId, dto("Пила", "Ручная", true));

        assertThat(itemService.search("дРЕЛ")).extracting(ItemDto::getName)
                .containsExactlyInAnyOrder("Дрель Салют", "Набор");
    }

    @Test
    void searchSkipsUnavailable() {
        itemService.create(ownerId, dto("Дрель", "Ударная", false));

        assertThat(itemService.search("дрель")).isEmpty();
    }

    @Test
    void searchBlankReturnsEmpty() {
        itemService.create(ownerId, dto("Дрель", "Ударная", true));

        assertThat(itemService.search("")).isEmpty();
        assertThat(itemService.search("   ")).isEmpty();
        assertThat(itemService.search(null)).isEmpty();
    }
}
