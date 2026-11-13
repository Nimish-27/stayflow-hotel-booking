-- V1: schema. Requires MySQL 8.0.16+ (older versions parse CHECK constraints but silently ignore them).

CREATE TABLE hotels (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    name            VARCHAR(150)  NOT NULL,
    location        VARCHAR(100)  NOT NULL,
    description     VARCHAR(1000) NOT NULL,
    amenities       VARCHAR(500)  NOT NULL,  -- comma-separated, e.g. 'WiFi,Pool,Breakfast'
    price_per_night DECIMAL(10,2) NOT NULL,
    total_rooms     INT           NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_hotels_price_positive       CHECK (price_per_night > 0),
    CONSTRAINT chk_hotels_total_rooms_positive CHECK (total_rooms > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- One row per hotel per night. booked_rooms <= hotels.total_rooms is enforced in the
-- service layer while the rows are locked (a CHECK cannot read another table).
CREATE TABLE room_inventory (
    hotel_id     BIGINT NOT NULL,
    stay_date    DATE   NOT NULL,
    booked_rooms INT    NOT NULL DEFAULT 0,
    PRIMARY KEY (hotel_id, stay_date),
    CONSTRAINT fk_inventory_hotel FOREIGN KEY (hotel_id) REFERENCES hotels (id),
    CONSTRAINT chk_inventory_booked_non_negative CHECK (booked_rooms >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- check_out is exclusive: the stay holds the nights [check_in, check_out).
-- Timestamps are stored in UTC.
CREATE TABLE bookings (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    reference    VARCHAR(20)   NOT NULL,
    hotel_id     BIGINT        NOT NULL,
    guest_name   VARCHAR(100)  NOT NULL,
    guest_email  VARCHAR(254)  NOT NULL,  -- stored trimmed + lowercase
    check_in     DATE          NOT NULL,
    check_out    DATE          NOT NULL,
    nights       INT           NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    status       ENUM('CONFIRMED', 'CANCELLED') NOT NULL DEFAULT 'CONFIRMED',
    created_at   DATETIME(3)   NOT NULL,
    cancelled_at DATETIME(3)   NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_bookings_reference UNIQUE (reference),
    CONSTRAINT fk_bookings_hotel FOREIGN KEY (hotel_id) REFERENCES hotels (id),
    CONSTRAINT chk_bookings_dates_order  CHECK (check_out > check_in),
    CONSTRAINT chk_bookings_nights_match CHECK (nights = DATEDIFF(check_out, check_in)),
    CONSTRAINT chk_bookings_total_non_negative CHECK (total_amount >= 0),
    CONSTRAINT chk_bookings_cancelled_at CHECK (
        (status = 'CONFIRMED' AND cancelled_at IS NULL)
        OR (status = 'CANCELLED' AND cancelled_at IS NOT NULL)
    ),
    INDEX idx_bookings_reference_email (reference, guest_email),
    INDEX idx_bookings_hotel_dates (hotel_id, check_in, check_out)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;