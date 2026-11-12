package com.hotelbooking.service;

import com.hotelbooking.dto.BookingResponse;
import com.hotelbooking.dto.CreateBookingRequest;
import com.hotelbooking.entity.Booking;
import com.hotelbooking.entity.Hotel;
import com.hotelbooking.entity.RoomInventory;
import com.hotelbooking.exception.BookingNotFoundException;
import com.hotelbooking.exception.HotelNotFoundException;
import com.hotelbooking.exception.SoldOutException;
import com.hotelbooking.mapper.BookingMapper;
import com.hotelbooking.repository.BookingRepository;
import com.hotelbooking.repository.HotelRepository;
import com.hotelbooking.repository.RoomInventoryRepository;
import com.hotelbooking.util.InputNormalizer;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Create, retrieve and cancel bookings.
 * Lock order (prevents deadlocks): inventory rows are always locked in stay_date order.
 * Logs contain the reference and hotel id, never the guest's name or email.
 */
@Service
public class BookingService {

    static final int MAX_REFERENCE_ATTEMPTS = 5;

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final HotelRepository hotelRepository;
    private final RoomInventoryRepository inventoryRepository;
    private final StayValidator stayValidator;
    private final PriceCalculator priceCalculator;
    private final AvailabilityCalculator availabilityCalculator;
    private final BookingReferenceGenerator referenceGenerator;
    private final BookingMapper bookingMapper;
    private final Clock clock;

    public BookingService(BookingRepository bookingRepository, HotelRepository hotelRepository,
                          RoomInventoryRepository inventoryRepository, StayValidator stayValidator,
                          PriceCalculator priceCalculator, AvailabilityCalculator availabilityCalculator,
                          BookingReferenceGenerator referenceGenerator, BookingMapper bookingMapper,
                          Clock clock) {
        this.bookingRepository = bookingRepository;
        this.hotelRepository = hotelRepository;
        this.inventoryRepository = inventoryRepository;
        this.stayValidator = stayValidator;
        this.priceCalculator = priceCalculator;
        this.availabilityCalculator = availabilityCalculator;
        this.referenceGenerator = referenceGenerator;
        this.bookingMapper = bookingMapper;
        this.clock = clock;
    }

    /** One transaction: lock the nights, check every night, increment every night, insert the booking. */
    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request) {
        stayValidator.validate(request.checkIn(), request.checkOut());
        Hotel hotel = hotelRepository.findById(request.hotelId())
                .orElseThrow(() -> new HotelNotFoundException(request.hotelId()));
        int nights = priceCalculator.nights(request.checkIn(), request.checkOut());

        List<RoomInventory> lockedNights = inventoryRepository
                .findStayForUpdate(request.hotelId(), request.checkIn(), request.checkOut());
        requireOneRowPerNight(lockedNights, nights, request.hotelId());
        requireFreeRoomOnEveryNight(hotel, lockedNights, request);

        lockedNights.forEach(RoomInventory::increment);

        Booking booking = new Booking(generateUniqueReference(), hotel, request.guestName(),
                request.guestEmail(), request.checkIn(), request.checkOut(), nights,
                priceCalculator.total(hotel.getPricePerNight(), nights), Instant.now(clock));
        bookingRepository.save(booking);

        log.info("Booking created: reference={} hotelId={} nights={}",
                booking.getReference(), request.hotelId(), nights);
        return bookingMapper.toResponse(booking);
    }

    /** Wrong email and unknown reference are the same 404, so references cannot be guessed. */
    @Transactional(readOnly = true)
    public BookingResponse getBooking(String reference, String email) {
        Booking booking = bookingRepository
                .findByReferenceAndGuestEmail(InputNormalizer.reference(reference), InputNormalizer.email(email))
                .orElseThrow(BookingNotFoundException::new);
        return bookingMapper.toResponse(booking);
    }

    /**
     * Idempotent: cancelling a cancelled booking returns it unchanged and releases nothing.
     * The booking row is locked first, so two simultaneous cancels cannot both release the nights.
     */
    @Transactional
    public BookingResponse cancelBooking(String reference, String email) {
        Booking booking = bookingRepository
                .findForUpdate(InputNormalizer.reference(reference), InputNormalizer.email(email))
                .orElseThrow(BookingNotFoundException::new);

        if (booking.isCancelled()) {
            log.info("Booking {} was already cancelled, nothing to release", booking.getReference());
            return bookingMapper.toResponse(booking);
        }

        Long hotelId = booking.getHotel().getId();
        List<RoomInventory> lockedNights =
                inventoryRepository.findStayForUpdate(hotelId, booking.getCheckIn(), booking.getCheckOut());
        requireOneRowPerNight(lockedNights, booking.getNights(), hotelId);

        lockedNights.forEach(RoomInventory::decrement);
        booking.cancel(Instant.now(clock));

        log.info("Booking cancelled: reference={} hotelId={}", booking.getReference(), hotelId);
        return bookingMapper.toResponse(booking);
    }

    /** Safety net: validation should make this impossible, so fail loudly instead of booking silently. */
    private void requireOneRowPerNight(List<RoomInventory> rows, int nights, Long hotelId) {
        if (rows.size() != nights) {
            log.error("Inventory rows missing: hotelId={} expectedNights={} foundRows={}",
                    hotelId, nights, rows.size());
            throw new IllegalStateException("Inventory is missing for some nights of hotel " + hotelId);
        }
    }

    private void requireFreeRoomOnEveryNight(Hotel hotel, List<RoomInventory> lockedNights,
                                             CreateBookingRequest request) {
        boolean available = availabilityCalculator.isAvailable(hotel.getTotalRooms(),
                availabilityCalculator.bookedByNight(lockedNights), request.checkIn(), request.checkOut());
        if (!available) {
            log.info("Sold out: hotelId={} checkIn={} checkOut={}",
                    request.hotelId(), request.checkIn(), request.checkOut());
            throw new SoldOutException("No room is available for all nights of the selected stay");
        }
    }

    /** A collision is practically impossible; the UNIQUE constraint remains the final guard. */
    private String generateUniqueReference() {
        for (int attempt = 1; attempt <= MAX_REFERENCE_ATTEMPTS; attempt++) {
            String reference = referenceGenerator.generate();
            if (!bookingRepository.existsByReference(reference)) {
                return reference;
            }
            log.warn("Booking reference collision on attempt {}", attempt);
        }
        throw new IllegalStateException("Could not generate a unique booking reference");
    }
}
