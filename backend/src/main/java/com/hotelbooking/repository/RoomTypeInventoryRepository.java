package com.hotelbooking.repository;

import com.hotelbooking.entity.RoomTypeInventory;
import com.hotelbooking.entity.RoomTypeInventoryId;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

public interface RoomTypeInventoryRepository extends JpaRepository<RoomTypeInventory, RoomTypeInventoryId> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ri from RoomTypeInventory ri where ri.id.hotelId = :hotelId and ri.id.roomType = :roomType "
            + "and ri.id.stayDate >= :startDate and ri.id.stayDate < :endDate order by ri.id.stayDate")
    List<RoomTypeInventory> findStayForUpdate(@Param("hotelId") Long hotelId, @Param("roomType") String roomType,
                                               @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("select ri from RoomTypeInventory ri where ri.id.hotelId = :hotelId and ri.id.roomType = :roomType "
            + "and ri.id.stayDate >= :startDate and ri.id.stayDate < :endDate order by ri.id.stayDate")
    List<RoomTypeInventory> findStay(@Param("hotelId") Long hotelId, @Param("roomType") String roomType,
                                      @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("select ri from RoomTypeInventory ri where ri.id.stayDate >= :startDate and ri.id.stayDate < :endDate order by ri.id.hotelId, ri.id.stayDate")
    List<RoomTypeInventory> findAllStay(@Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate);

    @Query("select max(ri.id.stayDate) from RoomTypeInventory ri where ri.id.hotelId = :hotelId and ri.id.roomType = :roomType")
    Optional<LocalDate> findLastStayDate(@Param("hotelId") Long hotelId, @Param("roomType") String roomType);

    @Modifying
    @Query(value = "INSERT INTO room_type_inventory (hotel_id, room_type, stay_date, booked_rooms) "
            + "VALUES (:hotelId, :roomType, :stayDate, 0) ON DUPLICATE KEY UPDATE booked_rooms = booked_rooms",
            nativeQuery = true)
    int insertNight(@Param("hotelId") Long hotelId, @Param("roomType") String roomType,
                    @Param("stayDate") LocalDate stayDate);
}
