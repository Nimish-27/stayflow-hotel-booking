-- V8: inventory and booking occupancy are tracked for the selected room type.
ALTER TABLE bookings
    ADD COLUMN room_type VARCHAR(150) NOT NULL DEFAULT 'Standard Room',
    ADD COLUMN adults INT NOT NULL DEFAULT 2,
    ADD COLUMN children INT NOT NULL DEFAULT 0,
    ADD COLUMN rooms INT NOT NULL DEFAULT 1;

CREATE TABLE room_type_inventory (
    hotel_id     BIGINT       NOT NULL,
    room_type    VARCHAR(150) NOT NULL,
    stay_date    DATE         NOT NULL,
    booked_rooms INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (hotel_id, room_type, stay_date),
    CONSTRAINT fk_type_inventory_hotel FOREIGN KEY (hotel_id) REFERENCES hotels (id),
    CONSTRAINT chk_type_inventory_booked_non_negative CHECK (booked_rooms >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

INSERT INTO room_type_inventory (hotel_id, room_type, stay_date, booked_rooms)
SELECT id, room_type, CURDATE(), 0 FROM hotels;
