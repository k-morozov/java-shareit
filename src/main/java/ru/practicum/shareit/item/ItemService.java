package ru.practicum.shareit.item;

import java.util.List;
import ru.practicum.shareit.item.dto.CommentCreateDto;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemWithDetailsDto;

public interface ItemService {

    ItemDto create(long userId, ItemDto itemDto);

    ItemDto update(long userId, long itemId, ItemDto itemDto);

    ItemWithDetailsDto getById(Long userId, long itemId);

    List<ItemWithDetailsDto> getOwnerItems(long userId);

    CommentDto addComment(long userId, long itemId, CommentCreateDto dto);

    List<ItemDto> search(String text);
}
