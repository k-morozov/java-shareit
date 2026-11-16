package ru.practicum.shareit.booking;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.ItemService;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.user.UserRepository;
import ru.practicum.shareit.user.UserService;
import ru.practicum.shareit.user.dto.UserDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class BookingServiceImplTest {

    @Autowired
    private BookingService bookingService;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ItemService itemService;
    @Autowired
    private ItemRepository itemRepository;

    private long ownerId;
    private long bookerId;
    private long strangerId;
    private long itemId;
    private LocalDateTime base;

    @BeforeEach
    void setUp() {
        base = LocalDateTime.now().withNano(0);
        ownerId = user("Owner");
        bookerId = user("Booker");
        strangerId = user("Stranger");
        itemId = item(ownerId, true);
    }

    private long user(String name) {
        return userService.create(UserDto.builder().name(name).email(UUID.randomUUID() + "@mail.ru").build()).getId();
    }

    private long item(long owner, boolean available) {
        return itemService.create(owner, ItemDto.builder()
                .name("Дрель").description("Ударная").available(available).build()).getId();
    }

    private LocalDateTime inDays(long days) {
        return base.plusDays(days);
    }

    private static BookingCreateDto request(long itemId, LocalDateTime start, LocalDateTime end) {
        return BookingCreateDto.builder().itemId(itemId).start(start).end(end).build();
    }

    private Booking saved(long booker, LocalDateTime start, LocalDateTime end, BookingStatus status) {
        return bookingRepository.save(Booking.builder()
                .item(itemRepository.getReferenceById(itemId))
                .booker(userRepository.getReferenceById(booker))
                .start(start)
                .end(end)
                .status(status)
                .build());
    }

    @Test
    void createReturnsWaiting() {
        BookingDto created = bookingService.create(bookerId, request(itemId, inDays(1), inDays(2)));

        assertThat(created.getId()).isNotNull();
        assertThat(created.getStatus()).isEqualTo(BookingStatus.WAITING);
        assertThat(created.getStart()).isEqualTo(inDays(1));
        assertThat(created.getItem().getId()).isEqualTo(itemId);
        assertThat(created.getItem().getName()).isEqualTo("Дрель");
        assertThat(created.getBooker().getId()).isEqualTo(bookerId);
    }

    @Test
    void createByUnknownUserOnUnavailableItemIs404() {
        long unavailable = item(ownerId, false);

        assertThatThrownBy(() -> bookingService.create(999_999L, request(unavailable, inDays(1), inDays(2))))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void createUnknownItemIs404() {
        assertThatThrownBy(() -> bookingService.create(bookerId, request(999_999L, inDays(1), inDays(2))))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void createOwnItemIs400() {
        assertThatThrownBy(() -> bookingService.create(ownerId, request(itemId, inDays(1), inDays(2))))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Cannot book your own item");
    }

    @Test
    void createUnavailableItemIs400() {
        long unavailable = item(ownerId, false);

        assertThatThrownBy(() -> bookingService.create(bookerId, request(unavailable, inDays(1), inDays(2))))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createOverlappingApprovedIs409() {
        saved(strangerId, inDays(2), inDays(5), BookingStatus.APPROVED);

        assertThatThrownBy(() -> bookingService.create(bookerId, request(itemId, inDays(3), inDays(4))))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> bookingService.create(bookerId, request(itemId, inDays(1), inDays(3))))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> bookingService.create(bookerId, request(itemId, inDays(4), inDays(6))))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createAllowsTouchingApprovedBooking() {
        saved(strangerId, inDays(2), inDays(5), BookingStatus.APPROVED);

        assertThat(bookingService.create(bookerId, request(itemId, inDays(5), inDays(6))).getId()).isNotNull();
        assertThat(bookingService.create(bookerId, request(itemId, inDays(1), inDays(2))).getId()).isNotNull();
    }

    @Test
    void createAllowsOverlapWithWaitingOrRejected() {
        saved(strangerId, inDays(2), inDays(5), BookingStatus.WAITING);
        saved(strangerId, inDays(2), inDays(5), BookingStatus.REJECTED);

        assertThat(bookingService.create(bookerId, request(itemId, inDays(3), inDays(4))).getId()).isNotNull();
    }

    @Test
    void approveByOwnerSetsApprovedOrRejected() {
        long first = bookingService.create(bookerId, request(itemId, inDays(1), inDays(2))).getId();
        long second = bookingService.create(bookerId, request(itemId, inDays(3), inDays(4))).getId();

        assertThat(bookingService.approve(ownerId, first, true).getStatus()).isEqualTo(BookingStatus.APPROVED);
        assertThat(bookingService.approve(ownerId, second, false).getStatus()).isEqualTo(BookingStatus.REJECTED);
        assertThat(bookingService.getById(bookerId, first).getStatus()).isEqualTo(BookingStatus.APPROVED);
    }

    @Test
    void approveByNonOwnerIs403() {
        long booking = bookingService.create(bookerId, request(itemId, inDays(1), inDays(2))).getId();

        assertThatThrownBy(() -> bookingService.approve(bookerId, booking, true))
                .isInstanceOf(ForbiddenException.class);
        assertThat(bookingService.getById(bookerId, booking).getStatus()).isEqualTo(BookingStatus.WAITING);
    }

    @Test
    void approveByUnknownUserIs403() {
        long booking = bookingService.create(bookerId, request(itemId, inDays(1), inDays(2))).getId();

        assertThatThrownBy(() -> bookingService.approve(999_999L, booking, true))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void approveTwiceIs400() {
        long booking = bookingService.create(bookerId, request(itemId, inDays(1), inDays(2))).getId();
        bookingService.approve(ownerId, booking, false);

        assertThatThrownBy(() -> bookingService.approve(ownerId, booking, true))
                .isInstanceOf(ValidationException.class);
        assertThat(bookingService.getById(ownerId, booking).getStatus()).isEqualTo(BookingStatus.REJECTED);
    }

    @Test
    void approveUnknownBookingIs404() {
        assertThatThrownBy(() -> bookingService.approve(ownerId, 999_999L, true))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getByBookerAndOwnerWorksGetByStrangerIs404() {
        long booking = bookingService.create(bookerId, request(itemId, inDays(1), inDays(2))).getId();

        assertThat(bookingService.getById(bookerId, booking).getId()).isEqualTo(booking);
        assertThat(bookingService.getById(ownerId, booking).getId()).isEqualTo(booking);
        assertThatThrownBy(() -> bookingService.getById(strangerId, booking))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> bookingService.getById(bookerId, 999_999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getByBookerAllSortedNewestFirst() {
        long early = bookingService.create(bookerId, request(itemId, inDays(1), inDays(2))).getId();
        long late = bookingService.create(bookerId, request(itemId, inDays(5), inDays(6))).getId();
        long middle = bookingService.create(bookerId, request(itemId, inDays(3), inDays(4))).getId();

        List<BookingDto> all = bookingService.getByBooker(bookerId, BookingState.ALL);

        assertThat(all).extracting(BookingDto::getId).containsExactly(late, middle, early);
        assertThat(bookingService.getByOwner(ownerId, BookingState.WAITING))
                .extracting(BookingDto::getId).containsExactly(late, middle, early);
        assertThat(bookingService.getByBooker(bookerId, BookingState.CURRENT)).isEmpty();
    }

    @Test
    void getByOwnerWithoutItemsIsEmpty() {
        assertThat(bookingService.getByOwner(strangerId, BookingState.ALL)).isEmpty();
    }

    @Test
    void getByUnknownUserIs404() {
        assertThatThrownBy(() -> bookingService.getByBooker(999_999L, BookingState.ALL))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> bookingService.getByOwner(999_999L, BookingState.ALL))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void serviceMapsEveryStateToMatchingBookings() {
        Booking past = saved(bookerId, inDays(-5), inDays(-4), BookingStatus.APPROVED);
        Booking current = saved(bookerId, inDays(-1), inDays(1), BookingStatus.APPROVED);
        Booking future = saved(bookerId, inDays(3), inDays(4), BookingStatus.APPROVED);
        Booking waiting = saved(bookerId, inDays(5), inDays(6), BookingStatus.WAITING);
        Booking rejected = saved(bookerId, inDays(7), inDays(8), BookingStatus.REJECTED);

        for (boolean asOwner : new boolean[]{false, true}) {
            assertThat(list(asOwner, BookingState.ALL)).containsExactly(
                    rejected.getId(), waiting.getId(), future.getId(), current.getId(), past.getId());
            assertThat(list(asOwner, BookingState.CURRENT)).containsExactly(current.getId());
            assertThat(list(asOwner, BookingState.PAST)).containsExactly(past.getId());
            assertThat(list(asOwner, BookingState.FUTURE))
                    .containsExactly(rejected.getId(), waiting.getId(), future.getId());
            assertThat(list(asOwner, BookingState.WAITING)).containsExactly(waiting.getId());
            assertThat(list(asOwner, BookingState.REJECTED)).containsExactly(rejected.getId());
        }
        assertThat(bookingService.getByOwner(bookerId, BookingState.ALL)).isEmpty();
        assertThat(bookingService.getByBooker(ownerId, BookingState.ALL)).isEmpty();
    }

    private List<Long> list(boolean asOwner, BookingState state) {
        List<BookingDto> result = asOwner
                ? bookingService.getByOwner(ownerId, state)
                : bookingService.getByBooker(bookerId, state);
        return result.stream().map(BookingDto::getId).toList();
    }
}
