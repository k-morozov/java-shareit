package ru.practicum.shareit.item;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.CommentCreateDto;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemWithDetailsDto;
import ru.practicum.shareit.request.ItemRequestService;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;
import ru.practicum.shareit.user.UserRepository;
import ru.practicum.shareit.user.UserService;
import ru.practicum.shareit.user.dto.UserDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ItemServiceImplTest {

    @Autowired
    private ItemService itemService;
    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private ItemRequestService itemRequestService;
    private long ownerId;
    private long otherId;

    @BeforeEach
    void setUp() {
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
    void createWithRequestIdLinksItemToRequest() {
        long requestId = itemRequestService.create(otherId, new ItemRequestCreateDto("Нужна дрель")).getId();
        ItemDto dto = dto("Дрель", "Ударная", true);
        dto.setRequestId(requestId);

        ItemDto created = itemService.create(ownerId, dto);

        assertThat(created.getRequestId()).isEqualTo(requestId);
        assertThat(itemService.getById(ownerId, created.getId()).getRequestId()).isEqualTo(requestId);
    }

    @Test
    void createWithUnknownRequestIdIs404() {
        ItemDto dto = dto("Дрель", "Ударная", true);
        dto.setRequestId(999_999L);

        assertThatThrownBy(() -> itemService.create(ownerId, dto)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void createSetsOwnerAndId() {
        ItemDto created = itemService.create(ownerId, dto("Дрель", "Ударная", true));

        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("Дрель");
        assertThat(created.getDescription()).isEqualTo("Ударная");
        assertThat(created.getAvailable()).isTrue();
        assertThat(itemService.getOwnerItems(ownerId)).extracting(ItemWithDetailsDto::getId)
                .containsExactly(created.getId());
    }

    @Test
    void updateByNonOwnerThrowsForbidden() {
        ItemDto item = itemService.create(ownerId, dto("Дрель", "Ударная", true));

        assertThatThrownBy(() -> itemService.update(otherId, item.getId(), dto("Чужая", null, null)))
                .isInstanceOf(ForbiddenException.class);
        assertThat(itemService.getById(ownerId, item.getId()).getName()).isEqualTo("Дрель");
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
    void updateIgnoresBlankNameAndDescription() {
        ItemDto item = itemService.create(ownerId, dto("Дрель", "Ударная", true));

        ItemDto updated = itemService.update(ownerId, item.getId(), dto("   ", "", null));

        assertThat(updated.getName()).isEqualTo("Дрель");
        assertThat(updated.getDescription()).isEqualTo("Ударная");
    }

    @Test
    void getByIdUnknownThrowsNotFound() {
        assertThatThrownBy(() -> itemService.getById(ownerId, 999_999L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getOwnerItemsReturnsOnlyOwn() {
        itemService.create(ownerId, dto("Дрель", "Ударная", true));
        itemService.create(ownerId, dto("Пила", "Ручная", true));
        itemService.create(otherId, dto("Молоток", "Тяжёлый", true));

        assertThat(itemService.getOwnerItems(ownerId)).extracting(ItemWithDetailsDto::getName)
                .containsExactlyInAnyOrder("Дрель", "Пила");
    }

    @Test
    void getOwnerItemsOfUnknownUser404() {
        assertThatThrownBy(() -> itemService.getOwnerItems(999L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void searchCaseInsensitiveByNameAndDescription() {
        String token = "tok" + UUID.randomUUID();
        itemService.create(ownerId, dto("Дрель " + token.toUpperCase(), "Мощная", true));
        itemService.create(ownerId, dto("Набор", "Подходит к " + token, true));
        itemService.create(ownerId, dto("Пила", "Ручная", true));

        assertThat(itemService.search(token)).extracting(ItemDto::getName)
                .containsExactlyInAnyOrder("Дрель " + token.toUpperCase(), "Набор");
    }

    @Test
    void searchSkipsUnavailable() {
        String token = "tok" + UUID.randomUUID();
        itemService.create(ownerId, dto(token, "Ударная", false));

        assertThat(itemService.search(token)).isEmpty();
    }

    @Test
    void searchBlankReturnsEmpty() {
        itemService.create(ownerId, dto("Дрель", "Ударная", true));

        assertThat(itemService.search("")).isEmpty();
        assertThat(itemService.search("   ")).isEmpty();
        assertThat(itemService.search(null)).isEmpty();
    }

    private long booker() {
        return userService.create(UserDto.builder().name("Booker").email(UUID.randomUUID() + "@mail.ru").build())
                .getId();
    }

    private Booking booking(long itemId, long bookerId, long startDays, long endDays, BookingStatus status) {
        LocalDateTime now = LocalDateTime.now();
        return bookingRepository.save(Booking.builder()
                .item(itemRepository.getReferenceById(itemId))
                .booker(userRepository.getReferenceById(bookerId))
                .start(now.plusDays(startDays))
                .end(now.plusDays(endDays))
                .status(status)
                .build());
    }

    private CommentDto comment(long authorId, long itemId, String text) {
        return itemService.addComment(authorId, itemId, CommentCreateDto.builder().text(text).build());
    }

    @Test
    void ownerSeesLastAndNextBooking() {
        long itemId = itemService.create(ownerId, dto("Дрель", "Ударная", true)).getId();
        long bookerId = booker();
        booking(itemId, bookerId, -10, -9, BookingStatus.APPROVED);
        Booking last = booking(itemId, bookerId, -5, -4, BookingStatus.APPROVED);
        Booking next = booking(itemId, bookerId, 3, 4, BookingStatus.APPROVED);
        booking(itemId, bookerId, 7, 8, BookingStatus.APPROVED);

        ItemWithDetailsDto item = itemService.getById(ownerId, itemId);

        assertThat(item.getLastBooking().getId()).isEqualTo(last.getId());
        assertThat(item.getLastBooking().getBookerId()).isEqualTo(bookerId);
        assertThat(item.getLastBooking().getStart()).isEqualTo(last.getStart());
        assertThat(item.getNextBooking().getId()).isEqualTo(next.getId());
        assertThat(item.getNextBooking().getEnd()).isEqualTo(next.getEnd());
    }

    @Test
    void itemDatesIgnoreNotApprovedAndCurrentIsLast() {
        long itemId = itemService.create(ownerId, dto("Дрель", "Ударная", true)).getId();
        long bookerId = booker();
        booking(itemId, bookerId, -10, -9, BookingStatus.APPROVED);
        Booking current = booking(itemId, bookerId, -1, 1, BookingStatus.APPROVED);
        booking(itemId, bookerId, 2, 3, BookingStatus.WAITING);
        booking(itemId, bookerId, 4, 5, BookingStatus.REJECTED);
        booking(itemId, bookerId, -3, -2, BookingStatus.REJECTED);

        ItemWithDetailsDto item = itemService.getById(ownerId, itemId);

        assertThat(item.getLastBooking().getId()).isEqualTo(current.getId());
        assertThat(item.getNextBooking()).isNull();
    }

    @Test
    void nonOwnerSeesNullDates() {
        long itemId = itemService.create(ownerId, dto("Дрель", "Ударная", true)).getId();
        long bookerId = booker();
        booking(itemId, bookerId, -5, -4, BookingStatus.APPROVED);
        booking(itemId, bookerId, 3, 4, BookingStatus.APPROVED);

        ItemWithDetailsDto byOther = itemService.getById(otherId, itemId);
        ItemWithDetailsDto anonymous = itemService.getById(null, itemId);

        assertThat(byOther.getLastBooking()).isNull();
        assertThat(byOther.getNextBooking()).isNull();
        assertThat(anonymous.getLastBooking()).isNull();
        assertThat(anonymous.getNextBooking()).isNull();
    }

    @Test
    void ownerItemsHaveDatesAndCommentsPerItem() {
        long first = itemService.create(ownerId, dto("Дрель", "Ударная", true)).getId();
        long second = itemService.create(ownerId, dto("Пила", "Ручная", true)).getId();
        long bookerId = booker();
        Booking firstLast = booking(first, bookerId, -5, -4, BookingStatus.APPROVED);
        Booking secondNext = booking(second, bookerId, 3, 4, BookingStatus.APPROVED);
        comment(bookerId, first, "Отличная дрель");

        List<ItemWithDetailsDto> items = itemService.getOwnerItems(ownerId);

        assertThat(items).extracting(ItemWithDetailsDto::getId).containsExactly(first, second);
        assertThat(items.get(0).getLastBooking().getId()).isEqualTo(firstLast.getId());
        assertThat(items.get(0).getNextBooking()).isNull();
        assertThat(items.get(0).getComments()).extracting(CommentDto::getText).containsExactly("Отличная дрель");
        assertThat(items.get(1).getLastBooking()).isNull();
        assertThat(items.get(1).getNextBooking().getId()).isEqualTo(secondNext.getId());
        assertThat(items.get(1).getComments()).isEmpty();
    }

    @Test
    void commentAfterFinishedApprovedBooking() {
        long itemId = itemService.create(ownerId, dto("Дрель", "Ударная", true)).getId();
        long bookerId = booker();
        booking(itemId, bookerId, -5, -4, BookingStatus.APPROVED);

        CommentDto created = comment(bookerId, itemId, "Отличная дрель");

        assertThat(created.getId()).isNotNull();
        assertThat(created.getText()).isEqualTo("Отличная дрель");
        assertThat(created.getAuthorName()).isEqualTo("Booker");
        assertThat(created.getCreated()).isNotNull();
        assertThat(itemService.getById(otherId, itemId).getComments())
                .extracting(CommentDto::getId).containsExactly(created.getId());
    }

    @Test
    void commentWithoutBookingIs400() {
        long itemId = itemService.create(ownerId, dto("Дрель", "Ударная", true)).getId();

        assertThatThrownBy(() -> comment(otherId, itemId, "Не брал"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void commentDuringBookingIs400() {
        long itemId = itemService.create(ownerId, dto("Дрель", "Ударная", true)).getId();
        long bookerId = booker();
        booking(itemId, bookerId, -1, 1, BookingStatus.APPROVED);

        assertThatThrownBy(() -> comment(bookerId, itemId, "Пока пользуюсь"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void commentOnRejectedBookingIs400() {
        long itemId = itemService.create(ownerId, dto("Дрель", "Ударная", true)).getId();
        long bookerId = booker();
        booking(itemId, bookerId, -5, -4, BookingStatus.REJECTED);

        assertThatThrownBy(() -> comment(bookerId, itemId, "Не дали"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void commentUnknownItemIs404() {
        assertThatThrownBy(() -> comment(otherId, 999_999L, "Нет вещи"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void commentUnknownUserIs404() {
        long itemId = itemService.create(ownerId, dto("Дрель", "Ударная", true)).getId();

        assertThatThrownBy(() -> comment(999_999L, itemId, "Нет автора"))
                .isInstanceOf(NotFoundException.class);
    }
}
