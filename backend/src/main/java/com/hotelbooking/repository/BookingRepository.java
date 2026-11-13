package com.hotelbooking.repository;

import com.hotelbooking.entity.Booking;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    boolean existsByReference(String reference);

    /** Plain read for "retrieve booking". Reference and email must both match. */
    Optional<Booking> findByReferenceAndGuestEmail(String reference, String guestEmail);

    /**
     * Locks the booking row. Cancel uses it so two simultaneous cancels are serialized:
     * the second one waits, then sees status CANCELLED and releases nothing.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.reference = :reference and b.guestEmail = :guestEmail")
    Optional<Booking> findForUpdate(@Param("reference") String reference,
                                    @Param("guestEmail") String guestEmail);
}