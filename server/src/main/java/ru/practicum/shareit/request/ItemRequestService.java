package ru.practicum.shareit.request;

import java.util.List;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;

public interface ItemRequestService {
    ItemRequestDto create(long userId, ItemRequestCreateDto dto);

    List<ItemRequestDto> getOwn(long userId);

    List<ItemRequestDto> getOthers(long userId);

    ItemRequestDto getById(long userId, long requestId);
}
