package com.hotelbooking.controller;

import com.hotelbooking.dto.HotelResponse;
import com.hotelbooking.dto.StayQuote;
import com.hotelbooking.service.AlternativeDatesService;
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

    public HotelController(HotelService hotelService, AlternativeDatesService alternativeDatesService) {
        this.hotelService = hotelService;
        this.alternativeDatesService = alternativeDatesService;
    }

    @GetMapping
    public List<HotelResponse> searchHotels(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate checkIn,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate checkOut) {
        return hotelService.searchHotels(location, checkIn, checkOut);
    }

    @GetMapping("/{id}")
    public HotelResponse getHotel(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate checkIn,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate checkOut) {
        return hotelService.getHotel(id, checkIn, checkOut);
    }

    @GetMapping("/{id}/alternatives")
    public List<StayQuote> getAlternatives(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate checkOut) {
        return alternativeDatesService.findAlternatives(id, checkIn, checkOut);
    }
}