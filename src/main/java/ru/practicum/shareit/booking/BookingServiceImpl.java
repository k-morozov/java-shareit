package ru.practicum.shareit.booking;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingServiceImpl implements BookingService {

    private static final Duration TIME_TOLERANCE = Duration.ofSeconds(5);
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "start");

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final Clock clock;

    @Override
    @Transactional
    public BookingDto create(long userId, BookingCreateDto dto) {
        User booker = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " not found"));
        Item item = itemRepository.findById(dto.getItemId())
                .orElseThrow(() -> new NotFoundException("Item with id=" + dto.getItemId() + " not found"));
        if (item.getOwner().getId() == userId) {
            throw new NotFoundException("Item with id=" + item.getId() + " not found");
        }
        if (!Boolean.TRUE.equals(item.getAvailable())) {
            throw new ValidationException("Item id=" + item.getId() + " is not available");
        }
        validateDates(dto.getStart(), dto.getEnd());
        if (bookingRepository.existsApprovedOverlap(item.getId(), dto.getStart(), dto.getEnd())) {
            throw new ConflictException("Item id=" + item.getId() + " is already booked for these dates");
        }
        Booking booking = Booking.builder()
                .item(item)
                .booker(booker)
                .start(dto.getStart())
                .end(dto.getEnd())
                .status(BookingStatus.WAITING)
                .build();
        Booking saved = bookingRepository.save(booking);
        log.info("Created booking id={} for item id={} by user id={}", saved.getId(), item.getId(), userId);
        return BookingMapper.toBookingDto(saved);
    }

    @Override
    @Transactional
    public BookingDto approve(long userId, long bookingId, boolean approved) {
        Booking booking = getBooking(bookingId);
        if (booking.getItem().getOwner().getId() != userId) {
            throw new ForbiddenException("User id=" + userId + " is not the owner of booking id=" + bookingId);
        }
        if (booking.getStatus() != BookingStatus.WAITING) {
            throw new ValidationException("Booking id=" + bookingId + " is already " + booking.getStatus());
        }
        booking.setStatus(approved ? BookingStatus.APPROVED : BookingStatus.REJECTED);
        log.info("Booking id={} set to {}", bookingId, booking.getStatus());
        return BookingMapper.toBookingDto(bookingRepository.save(booking));
    }

    @Override
    public BookingDto getById(long userId, long bookingId) {
        Booking booking = getBooking(bookingId);
        boolean isBooker = booking.getBooker().getId() == userId;
        boolean isOwner = booking.getItem().getOwner().getId() == userId;
        if (!isBooker && !isOwner) {
            throw new NotFoundException("Booking with id=" + bookingId + " not found");
        }
        return BookingMapper.toBookingDto(booking);
    }

    @Override
    public List<BookingDto> getByBooker(long userId, BookingState state) {
        checkUserExists(userId);
        LocalDateTime now = LocalDateTime.now(clock);
        List<Booking> bookings = switch (state) {
            case ALL -> bookingRepository.findByBookerId(userId, NEWEST_FIRST);
            case CURRENT -> bookingRepository.findByBookerIdAndStartLessThanEqualAndEndAfter(
                    userId, now, now, NEWEST_FIRST);
            case PAST -> bookingRepository.findByBookerIdAndEndLessThanEqual(userId, now, NEWEST_FIRST);
            case FUTURE -> bookingRepository.findByBookerIdAndStartAfter(userId, now, NEWEST_FIRST);
            case WAITING -> bookingRepository.findByBookerIdAndStatus(userId, BookingStatus.WAITING, NEWEST_FIRST);
            case REJECTED -> bookingRepository.findByBookerIdAndStatus(userId, BookingStatus.REJECTED, NEWEST_FIRST);
        };
        return toDtos(bookings);
    }

    @Override
    public List<BookingDto> getByOwner(long userId, BookingState state) {
        checkUserExists(userId);
        LocalDateTime now = LocalDateTime.now(clock);
        List<Booking> bookings = switch (state) {
            case ALL -> bookingRepository.findByItemOwnerId(userId, NEWEST_FIRST);
            case CURRENT -> bookingRepository.findByItemOwnerIdAndStartLessThanEqualAndEndAfter(
                    userId, now, now, NEWEST_FIRST);
            case PAST -> bookingRepository.findByItemOwnerIdAndEndLessThanEqual(userId, now, NEWEST_FIRST);
            case FUTURE -> bookingRepository.findByItemOwnerIdAndStartAfter(userId, now, NEWEST_FIRST);
            case WAITING -> bookingRepository.findByItemOwnerIdAndStatus(
                    userId, BookingStatus.WAITING, NEWEST_FIRST);
            case REJECTED -> bookingRepository.findByItemOwnerIdAndStatus(
                    userId, BookingStatus.REJECTED, NEWEST_FIRST);
        };
        return toDtos(bookings);
    }

    private static List<BookingDto> toDtos(List<Booking> bookings) {
        return bookings.stream()
                .map(BookingMapper::toBookingDto)
                .toList();
    }

    private void checkUserExists(long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User with id=" + userId + " not found");
        }
    }

    private Booking getBooking(long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking with id=" + bookingId + " not found"));
    }

    private void validateDates(LocalDateTime start, LocalDateTime end) {
        LocalDateTime earliest = LocalDateTime.now(clock).minus(TIME_TOLERANCE);
        if (start.isBefore(earliest) || end.isBefore(earliest)) {
            throw new ValidationException("Booking dates must not be in the past");
        }
        if (!end.isAfter(start)) {
            throw new ValidationException("Booking end must be after start");
        }
    }
}
