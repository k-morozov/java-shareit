package ru.practicum.shareit.item;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import ru.practicum.shareit.item.model.Item;

@Repository
public class InMemoryItemRepository implements ItemRepository {

    private final Map<Long, Item> items = new LinkedHashMap<>();
    private long nextId = 1;

    @Override
    public Item save(Item item) {
        if (item.getId() == null) {
            item.setId(nextId++);
        }
        items.put(item.getId(), item);
        return item;
    }

    @Override
    public Optional<Item> findById(long id) {
        return Optional.ofNullable(items.get(id));
    }

    @Override
    public List<Item> findByOwnerId(long ownerId) {
        return items.values().stream()
                .filter(item -> item.getOwner().getId() == ownerId)
                .toList();
    }

    @Override
    public List<Item> search(String text) {
        String query = text.toLowerCase();
        return items.values().stream()
                .filter(item -> Boolean.TRUE.equals(item.getAvailable()))
                .filter(item -> containsIgnoreCase(item.getName(), query)
                        || containsIgnoreCase(item.getDescription(), query))
                .toList();
    }

    private static boolean containsIgnoreCase(String value, String lowerQuery) {
        return value != null && value.toLowerCase().contains(lowerQuery);
    }
}
