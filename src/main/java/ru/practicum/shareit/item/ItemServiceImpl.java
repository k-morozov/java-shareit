package ru.practicum.shareit.item;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingMapper;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.booking.dto.BookingShortDto;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.CommentCreateDto;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemWithDetailsDto;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemServiceImpl implements ItemService {

    private static final Sort BY_START = Sort.by(Sort.Direction.ASC, "start");

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final CommentRepository commentRepository;
    private final Clock clock;

    @Override
    @Transactional
    public ItemDto create(long userId, ItemDto itemDto) {
        User owner = getUser(userId);
        Item item = ItemMapper.toItem(itemDto, owner);
        item.setId(null);
        Item saved = itemRepository.save(item);
        log.info("Created item id={} by owner id={}", saved.getId(), userId);
        return ItemMapper.toItemDto(saved);
    }

    @Override
    @Transactional
    public ItemDto update(long userId, long itemId, ItemDto itemDto) {
        getUser(userId);
        Item item = getEntity(itemId);
        if (item.getOwner().getId() != userId) {
            throw new ForbiddenException("User id=" + userId + " is not the owner of item id=" + itemId);
        }
        if (itemDto.getName() != null && !itemDto.getName().isBlank()) {
            item.setName(itemDto.getName());
        }
        if (itemDto.getDescription() != null && !itemDto.getDescription().isBlank()) {
            item.setDescription(itemDto.getDescription());
        }
        if (itemDto.getAvailable() != null) {
            item.setAvailable(itemDto.getAvailable());
        }
        Item saved = itemRepository.save(item);
        log.info("Updated item id={}", itemId);
        return ItemMapper.toItemDto(saved);
    }

    @Override
    public ItemWithDetailsDto getById(Long userId, long itemId) {
        Item item = getEntity(itemId);
        boolean isOwner = userId != null && userId.equals(item.getOwner().getId());
        return withDetails(List.of(item), isOwner).getFirst();
    }

    @Override
    public List<ItemWithDetailsDto> getOwnerItems(long userId) {
        getUser(userId);
        return withDetails(itemRepository.findByOwnerIdOrderById(userId), true);
    }

    @Override
    @Transactional
    public CommentDto addComment(long userId, long itemId, CommentCreateDto dto) {
        User author = getUser(userId);
        Item item = getEntity(itemId);
        LocalDateTime now = LocalDateTime.now(clock);
        if (!bookingRepository.existsByBookerIdAndItemIdAndStatusAndEndLessThanEqual(
                userId, itemId, BookingStatus.APPROVED, now)) {
            throw new ValidationException("User id=" + userId + " has no finished booking of item id=" + itemId);
        }
        Comment comment = Comment.builder()
                .text(dto.getText())
                .item(item)
                .author(author)
                .created(now)
                .build();
        Comment saved = commentRepository.save(comment);
        log.info("Created comment id={} for item id={} by user id={}", saved.getId(), itemId, userId);
        return CommentMapper.toCommentDto(saved);
    }

    @Override
    public List<ItemDto> search(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return itemRepository.search(text).stream()
                .map(ItemMapper::toItemDto)
                .toList();
    }

    private List<ItemWithDetailsDto> withDetails(List<Item> items, boolean withBookings) {
        if (items.isEmpty()) {
            return List.of();
        }
        List<Long> itemIds = items.stream().map(Item::getId).toList();
        Map<Long, List<CommentDto>> comments = commentRepository.findByItemIdInOrderByCreated(itemIds).stream()
                .collect(Collectors.groupingBy(comment -> comment.getItem().getId(),
                        Collectors.mapping(CommentMapper::toCommentDto, Collectors.toList())));
        Map<Long, List<Booking>> bookings = withBookings
                ? bookingRepository.findByItemIdInAndStatus(itemIds, BookingStatus.APPROVED, BY_START).stream()
                        .collect(Collectors.groupingBy(booking -> booking.getItem().getId()))
                : Map.of();
        LocalDateTime now = LocalDateTime.now(clock);
        return items.stream()
                .map(item -> {
                    Booking last = null;
                    Booking next = null;
                    for (Booking booking : bookings.getOrDefault(item.getId(), List.of())) {
                        if (!booking.getStart().isAfter(now)) {
                            last = booking;
                        } else if (next == null) {
                            next = booking;
                        }
                    }
                    return ItemMapper.toItemWithDetailsDto(item, toShort(last), toShort(next),
                            comments.getOrDefault(item.getId(), List.of()));
                })
                .toList();
    }

    private static BookingShortDto toShort(Booking booking) {
        return booking == null ? null : BookingMapper.toBookingShortDto(booking);
    }

    private User getUser(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " not found"));
    }

    private Item getEntity(long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item with id=" + itemId + " not found"));
    }
}
