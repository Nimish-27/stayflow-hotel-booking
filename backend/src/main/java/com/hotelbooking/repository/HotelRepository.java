package com.hotelbooking.repository;

import com.hotelbooking.entity.Hotel;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HotelRepository extends JpaRepository<Hotel, Long> {

    List<Hotel> findAllByOrderByNameAsc();

    /** Case-insensitive "contains" search on location. */
    List<Hotel> findByLocationContainingIgnoreCaseOrderByNameAsc(String location);
}