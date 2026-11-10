package ru.practicum.shareit.booking;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Sort;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BookingRepositoryTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2030, 1, 10, 12, 0);
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "start");

    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private UserRepository userRepository;

    private User owner;
    private User booker;
    private Item item;
    private Item foreignItem;

    @BeforeEach
    void setUp() {
        owner = user();
        booker = user();
        User other = user();
        item = item(owner);
        foreignItem = item(other);
    }

    private User user() {
        return userRepository.save(User.builder().name("U").email(UUID.randomUUID() + "@mail.ru").build());
    }

    private Item item(User itemOwner) {
        return itemRepository.save(Item.builder()
                .name("Дрель").description("Ударная").available(true).owner(itemOwner).build());
    }

    private Booking booking(Item bookedItem, LocalDateTime start, LocalDateTime end, BookingStatus status) {
        return bookingRepository.save(Booking.builder()
                .item(bookedItem).booker(booker).start(start).end(end).status(status).build());
    }

    private static LocalDateTime jan(int day) {
        return LocalDateTime.of(2030, 1, day, 12, 0);
    }

    @Test
    void bookerQueriesPerState() {
        Booking past = booking(item, jan(1), jan(5), BookingStatus.APPROVED);
        Booking current = booking(item, jan(9), jan(11), BookingStatus.APPROVED);
        Booking rejected = booking(item, jan(15), jan(16), BookingStatus.REJECTED);
        Booking future = booking(item, jan(20), jan(21), BookingStatus.APPROVED);
        Booking waiting = booking(item, jan(25), jan(26), BookingStatus.WAITING);
        Booking foreign = booking(foreignItem, jan(30), jan(31), BookingStatus.APPROVED);
        long id = booker.getId();

        assertThat(bookingRepository.findByBookerId(id, NEWEST_FIRST)).extracting(Booking::getId)
                .containsExactly(foreign.getId(), waiting.getId(), future.getId(), rejected.getId(),
                        current.getId(), past.getId());
        assertThat(bookingRepository.findByBookerIdAndStartLessThanEqualAndEndAfter(id, NOW, NOW, NEWEST_FIRST))
                .extracting(Booking::getId).containsExactly(current.getId());
        assertThat(bookingRepository.findByBookerIdAndEndLessThanEqual(id, NOW, NEWEST_FIRST))
                .extracting(Booking::getId).containsExactly(past.getId());
        assertThat(bookingRepository.findByBookerIdAndStartAfter(id, NOW, NEWEST_FIRST))
                .extracting(Booking::getId)
                .containsExactly(foreign.getId(), waiting.getId(), future.getId(), rejected.getId());
        assertThat(bookingRepository.findByBookerIdAndStatus(id, BookingStatus.WAITING, NEWEST_FIRST))
                .extracting(Booking::getId).containsExactly(waiting.getId());
        assertThat(bookingRepository.findByBookerIdAndStatus(id, BookingStatus.REJECTED, NEWEST_FIRST))
                .extracting(Booking::getId).containsExactly(rejected.getId());
    }

    @Test
    void ownerQueriesPerState() {
        Booking past = booking(item, jan(1), jan(5), BookingStatus.APPROVED);
        Booking current = booking(item, jan(9), jan(11), BookingStatus.APPROVED);
        Booking rejected = booking(item, jan(15), jan(16), BookingStatus.REJECTED);
        Booking future = booking(item, jan(20), jan(21), BookingStatus.APPROVED);
        Booking waiting = booking(item, jan(25), jan(26), BookingStatus.WAITING);
        booking(foreignItem, jan(30), jan(31), BookingStatus.APPROVED);
        long id = owner.getId();

        assertThat(bookingRepository.findByItemOwnerId(id, NEWEST_FIRST)).extracting(Booking::getId)
                .containsExactly(waiting.getId(), future.getId(), rejected.getId(), current.getId(), past.getId());
        assertThat(bookingRepository.findByItemOwnerIdAndStartLessThanEqualAndEndAfter(id, NOW, NOW, NEWEST_FIRST))
                .extracting(Booking::getId).containsExactly(current.getId());
        assertThat(bookingRepository.findByItemOwnerIdAndEndLessThanEqual(id, NOW, NEWEST_FIRST))
                .extracting(Booking::getId).containsExactly(past.getId());
        assertThat(bookingRepository.findByItemOwnerIdAndStartAfter(id, NOW, NEWEST_FIRST))
                .extracting(Booking::getId).containsExactly(waiting.getId(), future.getId(), rejected.getId());
        assertThat(bookingRepository.findByItemOwnerIdAndStatus(id, BookingStatus.WAITING, NEWEST_FIRST))
                .extracting(Booking::getId).containsExactly(waiting.getId());
        assertThat(bookingRepository.findByItemOwnerIdAndStatus(id, BookingStatus.REJECTED, NEWEST_FIRST))
                .extracting(Booking::getId).containsExactly(rejected.getId());
    }

    @Test
    void stateBoundariesAtExactNow() {
        Booking startsNow = booking(item, NOW, NOW.plusDays(1), BookingStatus.APPROVED);
        Booking endsNow = booking(item, NOW.minusDays(1), NOW, BookingStatus.APPROVED);
        long id = booker.getId();

        assertThat(bookingRepository.findByBookerIdAndStartLessThanEqualAndEndAfter(id, NOW, NOW, NEWEST_FIRST))
                .extracting(Booking::getId).containsExactly(startsNow.getId());
        assertThat(bookingRepository.findByBookerIdAndEndLessThanEqual(id, NOW, NEWEST_FIRST))
                .extracting(Booking::getId).containsExactly(endsNow.getId());
        assertThat(bookingRepository.findByBookerIdAndStartAfter(id, NOW, NEWEST_FIRST)).isEmpty();
    }

    @Test
    void finishedBookingExistsWhenEndEqualsNow() {
        Booking ended = booking(item, NOW.minusDays(1), NOW, BookingStatus.APPROVED);

        assertThat(bookingRepository.existsByBookerIdAndItemIdAndStatusAndEndLessThanEqual(
                booker.getId(), item.getId(), BookingStatus.APPROVED, ended.getEnd())).isTrue();
        assertThat(bookingRepository.existsByBookerIdAndItemIdAndStatusAndEndLessThanEqual(
                booker.getId(), item.getId(), BookingStatus.APPROVED, NOW.minusSeconds(1))).isFalse();
    }
}
