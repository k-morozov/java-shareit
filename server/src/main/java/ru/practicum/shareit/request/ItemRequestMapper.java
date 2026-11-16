package ru.practicum.shareit.request;

import java.util.List;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.dto.ItemAnswerDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;

public final class ItemRequestMapper {

    private ItemRequestMapper() {
    }

    public static ItemRequestDto toDto(ItemRequest request, List<Item> answers) {
        return ItemRequestDto.builder()
                .id(request.getId())
                .description(request.getDescription())
                .created(request.getCreated())
                .items(answers.stream().map(ItemRequestMapper::toAnswer).toList())
                .build();
    }

    private static ItemAnswerDto toAnswer(Item item) {
        return new ItemAnswerDto(item.getId(), item.getName(), item.getOwner().getId());
    }
}
