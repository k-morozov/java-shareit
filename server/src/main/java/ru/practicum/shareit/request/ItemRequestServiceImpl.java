package ru.practicum.shareit.request;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemRequestServiceImpl implements ItemRequestService {

    private final ItemRequestRepository requestRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Override
    @Transactional
    public ItemRequestDto create(long userId, ItemRequestCreateDto dto) {
        User requestor = getUser(userId);
        ItemRequest saved = requestRepository.save(ItemRequest.builder()
                .description(dto.getDescription())
                .requestor(requestor)
                .created(LocalDateTime.now(clock))
                .build());
        log.info("Created request id={} by user id={}", saved.getId(), userId);
        return ItemRequestMapper.toDto(saved, List.of());
    }

    @Override
    public List<ItemRequestDto> getOwn(long userId) {
        getUser(userId);
        return withAnswers(requestRepository.findByRequestorIdOrderByCreatedDescIdDesc(userId));
    }

    @Override
    public List<ItemRequestDto> getOthers(long userId) {
        getUser(userId);
        return withAnswers(requestRepository.findByRequestorIdNotOrderByCreatedDescIdDesc(userId));
    }

    @Override
    public ItemRequestDto getById(long userId, long requestId) {
        getUser(userId);
        ItemRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request with id=" + requestId + " not found"));
        return withAnswers(List.of(request)).get(0);
    }

    private List<ItemRequestDto> withAnswers(List<ItemRequest> requests) {
        List<Long> ids = requests.stream().map(ItemRequest::getId).toList();
        Map<Long, List<Item>> answers = ids.isEmpty() ? Map.of() : itemRepository.findByRequestIdIn(ids).stream()
                .collect(Collectors.groupingBy(item -> item.getRequest().getId()));
        return requests.stream()
                .map(request -> ItemRequestMapper.toDto(request, answers.getOrDefault(request.getId(), List.of())))
                .toList();
    }

    private User getUser(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " not found"));
    }
}
