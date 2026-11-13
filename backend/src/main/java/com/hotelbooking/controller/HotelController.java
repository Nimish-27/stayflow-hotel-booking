package com.hotelbooking.controller;

import com.hotelbooking.dto.HotelResponse;
import com.hotelbooking.dto.StayQuote;
import com.hotelbooking.dto.AvailabilityDay;
import com.hotelbooking.service.AlternativeDatesService;
import com.hotelbooking.service.HotelAvailabilityService;
import com.hotelbooking.service.HotelService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/hotels")
public class HotelController {

    private final HotelService hotelService;
    private final AlternativeDatesService alternativeDatesService;
    private final HotelAvailabilityService hotelAvailabilityService;

    public HotelController(HotelService hotelService, AlternativeDatesService alternativeDatesService,
                           HotelAvailabilityService hotelAvailabilityService) {
        this.hotelService = hotelService;
        this.alternativeDatesService = alternativeDatesService;
        this.hotelAvailabilityService = hotelAvailabilityService;
    }

    @GetMapping("/{id}/availability")
    public List<AvailabilityDay> getAvailability(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate to,
            @RequestParam(required = false) String roomType,
            @RequestParam(defaultValue = "1") int rooms) {
        return hotelAvailabilityService.getAvailability(id, from, to, roomType, rooms);
    }

    @GetMapping
    public List<HotelResponse> searchHotels(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate checkIn,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate checkOut,
            @RequestParam(required = false) String roomType) {
        return hotelService.searchHotels(location, checkIn, checkOut, roomType);
    }

    @GetMapping("/{id}")
    public HotelResponse getHotel(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate checkIn,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate checkOut,
            @RequestParam(required = false) String roomType,
            @RequestParam(defaultValue = "1") int rooms) {
        return hotelService.getHotel(id, checkIn, checkOut, roomType, rooms);
    }

    @GetMapping("/{id}/alternatives")
    public List<StayQuote> getAlternatives(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate checkOut) {
        return alternativeDatesService.findAlternatives(id, checkIn, checkOut);
    }
}