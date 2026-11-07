package ru.practicum.shareit.item;

import java.util.List;
import ru.practicum.shareit.item.dto.ItemDto;

public interface ItemService {

    ItemDto create(long userId, ItemDto itemDto);

    ItemDto update(long userId, long itemId, ItemDto itemDto);

    ItemDto getById(long itemId);

    List<ItemDto> getOwnerItems(long userId);

    List<ItemDto> search(String text);
}
