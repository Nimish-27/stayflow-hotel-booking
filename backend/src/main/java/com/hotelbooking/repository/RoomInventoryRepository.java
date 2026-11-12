package com.hotelbooking.repository;

import com.hotelbooking.entity.RoomInventory;
import com.hotelbooking.entity.RoomInventoryId;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** In every range method, endDate is exclusive: the range is [startDate, endDate). */
public interface RoomInventoryRepository extends JpaRepository<RoomInventory, RoomInventoryId> {

    /** Plain read, used for search, hotel details and alternatives. */
    @Query("""
            select ri from RoomInventory ri
            where ri.id.hotelId = :hotelId
              and ri.id.stayDate >= :startDate and ri.id.stayDate < :endDate
            order by ri.id.stayDate
            """)
    List<RoomInventory> findStay(@Param("hotelId") Long hotelId,
                                 @Param("startDate") LocalDate startDate,
                                 @Param("endDate") LocalDate endDate);

    /** Same range for all hotels in ONE query, so the hotel list does not run one query per hotel. */
    @Query("""
            select ri from RoomInventory ri
            where ri.id.stayDate >= :startDate and ri.id.stayDate < :endDate
            order by ri.id.hotelId, ri.id.stayDate
            """)
    List<RoomInventory> findAllHotelsStay(@Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);

    /**
     * SELECT ... FOR UPDATE. Rows are returned and locked in stay_date order, which is the order
     * every transaction uses, so two bookings can never wait on each other in a cycle (deadlock).
     * Must be called inside a transaction.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select ri from RoomInventory ri
            where ri.id.hotelId = :hotelId
              and ri.id.stayDate >= :startDate and ri.id.stayDate < :endDate
            order by ri.id.stayDate
            """)
    List<RoomInventory> findStayForUpdate(@Param("hotelId") Long hotelId,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);

    /** Latest night that already has an inventory row for this hotel (empty if none). */
    @Query("select max(ri.id.stayDate) from RoomInventory ri where ri.id.hotelId = :hotelId")
    Optional<LocalDate> findLastStayDate(@Param("hotelId") Long hotelId);

    /**
     * Adds one night with 0 booked rooms. If the row already exists (two app instances starting
     * together) the no-op update keeps the existing count: it never overwrites real bookings.
     * Returns 1 when a row was inserted, 0 when it already existed.
     */
    @Modifying
    @Query(value = """
            INSERT INTO room_inventory (hotel_id, stay_date, booked_rooms)
            VALUES (:hotelId, :stayDate, 0)
            ON DUPLICATE KEY UPDATE booked_rooms = booked_rooms
            """, nativeQuery = true)
    int insertNight(@Param("hotelId") Long hotelId, @Param("stayDate") LocalDate stayDate);
}
