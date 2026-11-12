package com.hotelbooking.service;

import static com.hotelbooking.service.TestFixtures.CLOCK;
import static com.hotelbooking.service.TestFixtures.HOTEL_ID;
import static com.hotelbooking.service.TestFixtures.RULES;
import static com.hotelbooking.service.TestFixtures.TODAY;
import static com.hotelbooking.service.TestFixtures.hotel;
import static com.hotelbooking.service.TestFixtures.inventory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.hotelbooking.dto.BookingResponse;
import com.hotelbooking.dto.CreateBookingRequest;
import com.hotelbooking.entity.Booking;
import com.hotelbooking.entity.BookingStatus;
import com.hotelbooking.entity.RoomInventory;
import com.hotelbooking.exception.BookingNotFoundException;
import com.hotelbooking.exception.HotelNotFoundException;
import com.hotelbooking.exception.InvalidStayException;
import com.hotelbooking.exception.SoldOutException;
import com.hotelbooking.mapper.BookingMapper;
import com.hotelbooking.repository.BookingRepository;
import com.hotelbooking.repository.HotelRepository;
import com.hotelbooking.repository.RoomInventoryRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    private static final LocalDate CHECK_IN = TODAY.plusDays(10);
    private static final LocalDate CHECK_OUT = CHECK_IN.plusDays(3);
    private static final String EMAIL = "asha@example.com";
    private static final String REFERENCE = "HB-ABCD2345";

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private HotelRepository hotelRepository;
    @Mock
    private RoomInventoryRepository inventoryRepository;
    @Mock
    private BookingReferenceGenerator referenceGenerator;

    private BookingService service;

    @BeforeEach
    void setUp() {
        // Pure classes are real; only the database and the random generator are mocked.
        service = new BookingService(bookingRepository, hotelRepository, inventoryRepository,
                new StayValidator(RULES, CLOCK), new PriceCalculator(), new AvailabilityCalculator(),
                referenceGenerator, new BookingMapper(), CLOCK);
    }

    // ---- create ----

    @Test
    void createsBookingAndTakesOneRoomOnEveryNight() {
        List<RoomInventory> nights = inventory(CHECK_IN, CHECK_OUT, 1);
        givenHotelAndLockedNights(3, nights);
        givenFreeReference(REFERENCE);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(call -> call.getArgument(0));

        BookingResponse response = service.createBooking(request());

        assertThat(response.reference()).isEqualTo(REFERENCE);
        assertThat(response.nights()).isEqualTo(3);
        assertThat(response.totalAmount()).isEqualByComparingTo("9000.00");
        assertThat(response.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(response.guestName()).isEqualTo("Asha");
        assertThat(response.guestEmail()).isEqualTo(EMAIL);
        assertThat(nights).extracting(RoomInventory::getBookedRooms).containsExactly(2, 2, 2);
    }

    @Test
    void rejectsTheBookingWhenAnySingleNightIsFull() {
        List<RoomInventory> nights = List.of(
                new RoomInventory(HOTEL_ID, CHECK_IN, 0),
                new RoomInventory(HOTEL_ID, CHECK_IN.plusDays(1), 3), // full
                new RoomInventory(HOTEL_ID, CHECK_IN.plusDays(2), 0));
        givenHotelAndLockedNights(3, nights);

        assertThatThrownBy(() -> service.createBooking(request())).isInstanceOf(SoldOutException.class);

        assertThat(nights).extracting(RoomInventory::getBookedRooms).containsExactly(0, 3, 0);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void failsLoudlyWhenInventoryRowsAreMissing() {
        List<RoomInventory> twoOfThreeNights = inventory(CHECK_IN, CHECK_OUT.minusDays(1), 0);
        givenHotelAndLockedNights(3, twoOfThreeNights);

        assertThatThrownBy(() -> service.createBooking(request())).isInstanceOf(IllegalStateException.class);

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void rejectsUnknownHotelBeforeLockingAnything() {
        when(hotelRepository.findById(HOTEL_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createBooking(request())).isInstanceOf(HotelNotFoundException.class);

        verifyNoInteractions(inventoryRepository, bookingRepository);
    }

    @Test
    void rejectsInvalidDatesBeforeTouchingTheDatabase() {
        CreateBookingRequest sameDay = new CreateBookingRequest("Asha", EMAIL, HOTEL_ID, CHECK_IN, CHECK_IN);

        assertThatThrownBy(() -> service.createBooking(sameDay)).isInstanceOf(InvalidStayException.class);

        verifyNoInteractions(hotelRepository, inventoryRepository, bookingRepository);
    }

    @Test
    void retriesWhenTheGeneratedReferenceAlreadyExists() {
        givenHotelAndLockedNights(3, inventory(CHECK_IN, CHECK_OUT, 0));
        when(referenceGenerator.generate()).thenReturn("HB-AAAAAAAA", "HB-BBBBBBBB");
        when(bookingRepository.existsByReference("HB-AAAAAAAA")).thenReturn(true);
        when(bookingRepository.existsByReference("HB-BBBBBBBB")).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(call -> call.getArgument(0));

        BookingResponse response = service.createBooking(request());

        assertThat(response.reference()).isEqualTo("HB-BBBBBBBB");
    }

    @Test
    void givesUpAfterTooManyReferenceCollisions() {
        givenHotelAndLockedNights(3, inventory(CHECK_IN, CHECK_OUT, 0));
        when(referenceGenerator.generate()).thenReturn("HB-AAAAAAAA");
        when(bookingRepository.existsByReference(anyString())).thenReturn(true);

        assertThatThrownBy(() -> service.createBooking(request())).isInstanceOf(IllegalStateException.class);

        verify(referenceGenerator, times(BookingService.MAX_REFERENCE_ATTEMPTS)).generate();
        verify(bookingRepository, never()).save(any());
    }

    // ---- retrieve ----

    @Test
    void retrievalIgnoresCaseAndWhitespaceInReferenceAndEmail() {
        when(bookingRepository.findByReferenceAndGuestEmail(REFERENCE, EMAIL))
                .thenReturn(Optional.of(confirmedBooking()));

        BookingResponse response = service.getBooking("  hb-abcd2345 ", "  ASHA@Example.COM ");

        assertThat(response.reference()).isEqualTo(REFERENCE);
    }

    @Test
    void unknownReferenceOrWrongEmailIsReportedAsNotFound() {
        when(bookingRepository.findByReferenceAndGuestEmail(REFERENCE, "other@example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getBooking(REFERENCE, "other@example.com"))
                .isInstanceOf(BookingNotFoundException.class);
    }

    // ---- cancel ----

    @Test
    void cancellingReleasesEveryNightAndMarksTheBookingCancelled() {
        Booking booking = confirmedBooking();
        List<RoomInventory> nights = inventory(CHECK_IN, CHECK_OUT, 2);
        when(bookingRepository.findForUpdate(REFERENCE, EMAIL)).thenReturn(Optional.of(booking));
        when(inventoryRepository.findStayForUpdate(HOTEL_ID, CHECK_IN, CHECK_OUT)).thenReturn(nights);

        BookingResponse response = service.cancelBooking(REFERENCE, EMAIL);

        assertThat(response.status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(response.cancelledAt()).isEqualTo(Instant.now(CLOCK));
        assertThat(nights).extracting(RoomInventory::getBookedRooms).containsExactly(1, 1, 1);
    }

    @Test
    void cancellingTwiceIsHarmlessAndReleasesNothingTheSecondTime() {
        Booking booking = confirmedBooking();
        booking.cancel(Instant.now(CLOCK));
        when(bookingRepository.findForUpdate(REFERENCE, EMAIL)).thenReturn(Optional.of(booking));

        BookingResponse response = service.cancelBooking(REFERENCE, EMAIL);

        assertThat(response.status()).isEqualTo(BookingStatus.CANCELLED);
        verifyNoInteractions(inventoryRepository);
    }

    @Test
    void cancellingWithTheWrongEmailIsNotFoundAndReleasesNothing() {
        when(bookingRepository.findForUpdate(REFERENCE, "other@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelBooking(REFERENCE, "other@example.com"))
                .isInstanceOf(BookingNotFoundException.class);

        verifyNoInteractions(inventoryRepository);
    }

    // ---- helpers ----

    private CreateBookingRequest request() {
        return new CreateBookingRequest("  Asha  ", " ASHA@Example.com ", HOTEL_ID, CHECK_IN, CHECK_OUT);
    }

    private Booking confirmedBooking() {
        return new Booking(REFERENCE, hotel(3), "Asha", EMAIL, CHECK_IN, CHECK_OUT, 3,
                new BigDecimal("9000.00"), Instant.now(CLOCK));
    }

    private void givenHotelAndLockedNights(int totalRooms, List<RoomInventory> nights) {
        when(hotelRepository.findById(HOTEL_ID)).thenReturn(Optional.of(hotel(totalRooms)));
        when(inventoryRepository.findStayForUpdate(HOTEL_ID, CHECK_IN, CHECK_OUT)).thenReturn(nights);
    }

    private void givenFreeReference(String reference) {
        when(referenceGenerator.generate()).thenReturn(reference);
        when(bookingRepository.existsByReference(reference)).thenReturn(false);
    }
}
