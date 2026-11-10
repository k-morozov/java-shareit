package ru.practicum.shareit.request;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRequestRepository extends JpaRepository<ItemRequest, Long> {
    List<ItemRequest> findByRequestorIdOrderByCreatedDescIdDesc(long requestorId);

    List<ItemRequest> findByRequestorIdNotOrderByCreatedDescIdDesc(long requestorId);
}
