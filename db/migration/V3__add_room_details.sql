-- V3: add the room category and the maximum number of guests per room.

ALTER TABLE hotels
    ADD COLUMN room_type VARCHAR(100) NOT NULL DEFAULT 'Standard Room',
    ADD COLUMN max_occupancy INT NOT NULL DEFAULT 2,
    ADD CONSTRAINT chk_hotels_max_occupancy_positive CHECK (max_occupancy > 0);

UPDATE hotels
SET room_type = CASE name
    WHEN 'Seaside Breeze Resort' THEN 'Deluxe Sea View Room'
    WHEN 'Pink City Haveli' THEN 'Heritage Courtyard Room'
    WHEN 'Harbour View Suites' THEN 'Executive Harbour Suite'
    WHEN 'Garden Tech Inn' THEN 'Garden King Room'
    WHEN 'Pine Hill Lodge' THEN 'Mountain View Cabin'
    WHEN 'Backwater Retreat' THEN 'Backwater Deluxe Room'
    ELSE room_type
END,
max_occupancy = CASE name
    WHEN 'Harbour View Suites' THEN 4
    ELSE 2
END;
