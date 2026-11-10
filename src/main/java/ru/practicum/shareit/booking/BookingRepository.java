package ru.practicum.shareit.booking;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("select count(b) > 0 from Booking b "
            + "where b.item.id = :itemId "
            + "and b.status = ru.practicum.shareit.booking.BookingStatus.APPROVED "
            + "and b.start < :to and b.end > :from")
    boolean existsApprovedOverlap(@Param("itemId") long itemId,
                                  @Param("from") LocalDateTime start,
                                  @Param("to") LocalDateTime end);

    @EntityGraph(attributePaths = {"item", "booker"})
    List<Booking> findByBookerId(long bookerId, Sort sort);

    @EntityGraph(attributePaths = {"item", "booker"})
    List<Booking> findByBookerIdAndStartLessThanEqualAndEndAfter(long bookerId,
            LocalDateTime start, LocalDateTime end, Sort sort);

    @EntityGraph(attributePaths = {"item", "booker"})
    List<Booking> findByBookerIdAndEndLessThanEqual(long bookerId, LocalDateTime end, Sort sort);

    @EntityGraph(attributePaths = {"item", "booker"})
    List<Booking> findByBookerIdAndStartAfter(long bookerId, LocalDateTime start, Sort sort);

    @EntityGraph(attributePaths = {"item", "booker"})
    List<Booking> findByBookerIdAndStatus(long bookerId, BookingStatus status, Sort sort);

    @EntityGraph(attributePaths = {"item", "booker"})
    List<Booking> findByItemOwnerId(long ownerId, Sort sort);

    @EntityGraph(attributePaths = {"item", "booker"})
    List<Booking> findByItemOwnerIdAndStartLessThanEqualAndEndAfter(long ownerId,
            LocalDateTime start, LocalDateTime end, Sort sort);

    @EntityGraph(attributePaths = {"item", "booker"})
    List<Booking> findByItemOwnerIdAndEndLessThanEqual(long ownerId, LocalDateTime end, Sort sort);

    @EntityGraph(attributePaths = {"item", "booker"})
    List<Booking> findByItemOwnerIdAndStartAfter(long ownerId, LocalDateTime start, Sort sort);

    @EntityGraph(attributePaths = {"item", "booker"})
    List<Booking> findByItemOwnerIdAndStatus(long ownerId, BookingStatus status, Sort sort);

    List<Booking> findByItemIdInAndStatus(Collection<Long> itemIds, BookingStatus status, Sort sort);

    boolean existsByBookerIdAndItemIdAndStatusAndEndLessThanEqual(long bookerId, long itemId,
                                                                    BookingStatus status, LocalDateTime now);
}
