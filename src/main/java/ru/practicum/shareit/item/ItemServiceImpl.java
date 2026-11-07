package ru.practicum.shareit.item;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserService;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final UserService userService;

    @Override
    public ItemDto create(long userId, ItemDto itemDto) {
        User owner = userService.getEntity(userId);
        Item item = ItemMapper.toItem(itemDto, owner);
        item.setId(null);
        Item saved = itemRepository.save(item);
        log.info("Created item id={} by owner id={}", saved.getId(), userId);
        return ItemMapper.toItemDto(saved);
    }

    @Override
    public ItemDto update(long userId, long itemId, ItemDto itemDto) {
        userService.getEntity(userId);
        Item item = getEntity(itemId);
        if (item.getOwner().getId() != userId) {
            throw new ForbiddenException("User id=" + userId + " is not the owner of item id=" + itemId);
        }
        if (itemDto.getName() != null) {
            item.setName(itemDto.getName());
        }
        if (itemDto.getDescription() != null) {
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
    public ItemDto getById(long itemId) {
        return ItemMapper.toItemDto(getEntity(itemId));
    }

    @Override
    public List<ItemDto> getOwnerItems(long userId) {
        userService.getEntity(userId);
        return itemRepository.findByOwnerId(userId).stream()
                .map(ItemMapper::toItemDto)
                .toList();
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

    private Item getEntity(long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item with id=" + itemId + " not found"));
    }
}
