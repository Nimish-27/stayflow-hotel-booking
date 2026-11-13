package com.hotelbooking.service;

import com.hotelbooking.dto.BookingResponse;
import com.hotelbooking.dto.CreateBookingRequest;
import com.hotelbooking.entity.Booking;
import com.hotelbooking.entity.Hotel;
import com.hotelbooking.entity.RoomTypeInventory;
import com.hotelbooking.exception.BookingNotFoundException;
import com.hotelbooking.exception.CancellationNotAllowedException;
import com.hotelbooking.exception.HotelNotFoundException;
import com.hotelbooking.exception.InvalidStayException;
import com.hotelbooking.exception.SoldOutException;
import com.hotelbooking.mapper.BookingMapper;
import com.hotelbooking.repository.BookingRepository;
import com.hotelbooking.repository.HotelRepository;
import com.hotelbooking.repository.RoomTypeInventoryRepository;
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
    private final RoomTypeInventoryRepository roomTypeInventoryRepository;
    private final StayValidator stayValidator;
    private final PriceCalculator priceCalculator;
    private final BookingReferenceGenerator referenceGenerator;
    private final BookingMapper bookingMapper;
    private final Clock clock;
    private final CancellationPolicy cancellationPolicy;

    public BookingService(BookingRepository bookingRepository, HotelRepository hotelRepository,
                          RoomTypeInventoryRepository roomTypeInventoryRepository, StayValidator stayValidator,
                          PriceCalculator priceCalculator,
                          BookingReferenceGenerator referenceGenerator, BookingMapper bookingMapper,
                          Clock clock, CancellationPolicy cancellationPolicy) {
        this.bookingRepository = bookingRepository;
        this.hotelRepository = hotelRepository;
        this.roomTypeInventoryRepository = roomTypeInventoryRepository;
        this.stayValidator = stayValidator;
        this.priceCalculator = priceCalculator;
        this.referenceGenerator = referenceGenerator;
        this.bookingMapper = bookingMapper;
        this.clock = clock;
        this.cancellationPolicy = cancellationPolicy;
    }

    /** One transaction: lock the nights, check every night, increment every night, insert the booking. */
    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request) {
        stayValidator.validate(request.checkIn(), request.checkOut());
        Hotel hotel = hotelRepository.findById(request.hotelId())
                .orElseThrow(() -> new HotelNotFoundException(request.hotelId()));
        String roomType = hotel.resolveRoomType(request.roomType());
        int rooms = request.rooms();
        int guests = request.adults() + request.children();
        if (request.adults() < 1 || request.children() < 0 || rooms < 1
                || guests > hotel.maxOccupancyForRoomType(roomType) * rooms) {
            throw new InvalidStayException("The selected room type cannot accommodate this occupancy");
        }
        int nights = priceCalculator.nights(request.checkIn(), request.checkOut());

        var roomPrice = hotel.priceForRoomType(roomType);
        List<RoomTypeInventory> lockedNights = roomTypeInventoryRepository.findStayForUpdate(
                request.hotelId(), roomType, request.checkIn(), request.checkOut());
        requireOneTypeRowPerNight(lockedNights, nights, request.hotelId(), roomType);
        requireFreeRoomOnEveryNight(hotel, lockedNights, roomType, rooms);
        lockedNights.forEach(row -> row.increment(rooms));

        var subtotal = priceCalculator.subtotal(roomPrice, nights)
                .multiply(java.math.BigDecimal.valueOf(rooms))
                .setScale(2, java.math.RoundingMode.HALF_UP);
        var gstRate = priceCalculator.gstRate();
        var gstAmount = priceCalculator.gst(subtotal);
        var total = subtotal.add(gstAmount).setScale(2, java.math.RoundingMode.HALF_UP);
        Booking booking = new Booking(generateUniqueReference(), hotel, request.guestName(),
                request.guestEmail(), request.checkIn(), request.checkOut(), nights,
                subtotal, gstRate, gstAmount, total,
                Instant.now(clock), roomType, request.adults(), request.children(), rooms);
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
        if (!cancellationPolicy.isCancellable(booking, Instant.now(clock))) {
            throw new CancellationNotAllowedException(
                    "Cancellation is not allowed after the cancellation deadline");
        }

        Long hotelId = booking.getHotel().getId();
        List<RoomTypeInventory> lockedNights = roomTypeInventoryRepository.findStayForUpdate(
                hotelId, booking.getRoomType(), booking.getCheckIn(), booking.getCheckOut());
        requireOneTypeRowPerNight(lockedNights, booking.getNights(), hotelId, booking.getRoomType());
        lockedNights.forEach(row -> row.decrement(booking.getRooms()));
        booking.cancel(Instant.now(clock));

        log.info("Booking cancelled: reference={} hotelId={}", booking.getReference(), hotelId);
        return bookingMapper.toResponse(booking);
    }

    private void requireFreeRoomOnEveryNight(Hotel hotel, List<RoomTypeInventory> rows, String roomType, int rooms) {
        int capacity = hotel.roomsForRoomType(roomType);
        for (RoomTypeInventory row : rows) {
            if (row.getBookedRooms() + rooms > capacity) throw new SoldOutException("No room is available for all nights of the selected stay");
        }
    }

    private void requireOneTypeRowPerNight(List<RoomTypeInventory> rows, int nights, Long hotelId, String roomType) {
        if (rows.size() != nights) throw new IllegalStateException("Inventory is missing for room type " + roomType + " at hotel " + hotelId);
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
