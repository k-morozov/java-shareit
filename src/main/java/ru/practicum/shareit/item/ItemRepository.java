package ru.practicum.shareit.item;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.shareit.item.model.Item;

public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findByOwnerIdOrderById(long ownerId);

    @Query("select i from Item i "
            + "where i.available = true "
            + "and (upper(i.name) like upper(concat('%', ?1, '%')) "
            + "or upper(i.description) like upper(concat('%', ?1, '%')))")
    List<Item> search(String text);
}
