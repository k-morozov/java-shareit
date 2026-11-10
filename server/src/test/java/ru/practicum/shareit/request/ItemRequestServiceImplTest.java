package ru.practicum.shareit.request;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.ItemService;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.request.dto.ItemAnswerDto;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.user.UserService;
import ru.practicum.shareit.user.dto.UserDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest
@Transactional
class ItemRequestServiceImplTest {

    private static final long UNKNOWN = 999_999L;

    @Autowired
    private ItemRequestService requestService;
    @Autowired
    private ItemService itemService;
    @Autowired
    private UserService userService;

    private long userA;
    private long userB;
    private long userC;

    @BeforeEach
    void setUp() {
        userA = user("A");
        userB = user("B");
        userC = user("C");
    }

    private long user(String name) {
        return userService.create(UserDto.builder().name(name).email(UUID.randomUUID() + "@mail.ru").build()).getId();
    }

    private long request(long userId, String description) {
        return requestService.create(userId, new ItemRequestCreateDto(description)).getId();
    }

    private long answer(long ownerId, long requestId, String name) {
        return itemService.create(ownerId, ItemDto.builder()
                .name(name).description("Ударная").available(true).requestId(requestId).build()).getId();
    }

    private static List<Long> ids(List<ItemRequestDto> requests) {
        return requests.stream().map(ItemRequestDto::getId).toList();
    }

    @Test
    void createReturnsDtoWithEmptyItems() {
        ItemRequestDto created = requestService.create(userA, new ItemRequestCreateDto("Нужна дрель"));

        assertThat(created.getId()).isNotNull();
        assertThat(created.getDescription()).isEqualTo("Нужна дрель");
        assertThat(created.getCreated()).isNotNull();
        assertThat(created.getItems()).isEmpty();
    }

    @Test
    void createByUnknownUserIs404() {
        assertThatThrownBy(() -> requestService.create(UNKNOWN, new ItemRequestCreateDto("Нужна дрель")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getOwnReturnsNewestFirstWithAnswers() {
        long r1 = request(userA, "Нужна дрель");
        long r2 = request(userA, "Нужна пила");
        long itemId = answer(userB, r1, "Дрель");

        List<ItemRequestDto> own = requestService.getOwn(userA);

        assertThat(ids(own)).containsExactly(r2, r1);
        assertThat(own.get(1).getItems()).extracting(ItemAnswerDto::getId, ItemAnswerDto::getName,
                ItemAnswerDto::getOwnerId).containsExactly(tuple(itemId, "Дрель", userB));
        assertThat(own.get(0).getItems()).isEmpty();
    }

    @Test
    void getOwnExcludesOthersRequests() {
        long foreign = request(userB, "Нужна пила");

        assertThat(ids(requestService.getOwn(userA))).doesNotContain(foreign);
    }

    @Test
    void getOthersExcludesOwnNewestFirst() {
        long a1 = request(userA, "Нужна дрель");
        long b1 = request(userB, "Нужна пила");
        long b2 = request(userB, "Нужен молоток");

        List<Long> others = ids(requestService.getOthers(userA));

        assertThat(others).containsSubsequence(b2, b1).doesNotContain(a1);
    }

    @Test
    void getByIdIsVisibleToAnyUserWithAnswers() {
        long r1 = request(userA, "Нужна дрель");
        answer(userB, r1, "Дрель");

        ItemRequestDto found = requestService.getById(userC, r1);

        assertThat(found.getDescription()).isEqualTo("Нужна дрель");
        assertThat(found.getItems().get(0).getName()).isEqualTo("Дрель");
    }

    @Test
    void getByIdUnknownRequestIs404() {
        assertThatThrownBy(() -> requestService.getById(userA, UNKNOWN)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getByIdUnknownUserIs404() {
        long r1 = request(userA, "Нужна дрель");

        assertThatThrownBy(() -> requestService.getById(UNKNOWN, r1)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getOwnUnknownUserIs404() {
        assertThatThrownBy(() -> requestService.getOwn(UNKNOWN)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getOthersUnknownUserIs404() {
        assertThatThrownBy(() -> requestService.getOthers(UNKNOWN)).isInstanceOf(NotFoundException.class);
    }
}
