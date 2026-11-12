package com.hotelbooking.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** One booking for one room. The nights it holds are [checkIn, checkOut): checkOut is not a held night. */
@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String reference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hotel_id")
    private Hotel hotel;

    private String guestName;
    private String guestEmail; // stored trimmed and lowercase
    private LocalDate checkIn;
    private LocalDate checkOut;
    private int nights;
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    private Instant createdAt;
    private Instant cancelledAt;

    protected Booking() {
        // required by JPA
    }

    public Booking(String reference, Hotel hotel, String guestName, String guestEmail,
                   LocalDate checkIn, LocalDate checkOut, int nights, BigDecimal totalAmount,
                   Instant createdAt) {
        this.reference = reference;
        this.hotel = hotel;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.nights = nights;
        this.totalAmount = totalAmount;
        this.status = BookingStatus.CONFIRMED;
        this.createdAt = createdAt;
    }

    public boolean isCancelled() {
        return status == BookingStatus.CANCELLED;
    }

    /** The service checks isCancelled() first, so a second cancel never reaches this method. */
    public void cancel(Instant cancelledAt) {
        if (isCancelled()) {
            throw new IllegalStateException("Booking " + reference + " is already cancelled");
        }
        this.status = BookingStatus.CANCELLED;
        this.cancelledAt = cancelledAt;
    }

    public Long getId() { return id; }
    public String getReference() { return reference; }
    public Hotel getHotel() { return hotel; }
    public String getGuestName() { return guestName; }
    public String getGuestEmail() { return guestEmail; }
    public LocalDate getCheckIn() { return checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public int getNights() { return nights; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BookingStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCancelledAt() { return cancelledAt; }
}