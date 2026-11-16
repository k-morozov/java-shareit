package ru.practicum.shareit.item;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ItemRepositoryTest {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UserRepository userRepository;

    private User user(String name) {
        return userRepository.save(User.builder().name(name).email(UUID.randomUUID() + "@mail.ru").build());
    }

    private Item item(User owner, String name, String description, boolean available) {
        return itemRepository.save(Item.builder()
                .name(name)
                .description(description)
                .available(available)
                .owner(owner)
                .build());
    }

    @Test
    void searchMatchesNameOrDescriptionIgnoringCaseOnlyAvailable() {
        User owner = user("Owner");
        String token = "tok" + UUID.randomUUID();
        Item byName = item(owner, "Drill " + token.toUpperCase(), "Strong", true);
        Item byDescription = item(owner, "Set", "Fits " + token, true);
        item(owner, token, token, false);
        item(owner, "Saw", "Manual", true);

        assertThat(itemRepository.search(token.substring(0, 10).toUpperCase() + token.substring(10)))
                .extracting(Item::getId)
                .containsExactlyInAnyOrder(byName.getId(), byDescription.getId());
    }

    @Test
    void findByOwnerIdOrderByIdReturnsOnlyOwnInIdOrder() {
        User owner = user("Owner");
        User other = user("Other");
        Item first = item(owner, "First", "A", true);
        item(other, "Foreign", "B", true);
        Item second = item(owner, "Second", "C", false);

        assertThat(itemRepository.findByOwnerIdOrderById(owner.getId()))
                .extracting(Item::getId)
                .containsExactly(first.getId(), second.getId());
    }
}
