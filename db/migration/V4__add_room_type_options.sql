-- V4: store the room categories available at each hotel.

ALTER TABLE hotels
    ADD COLUMN room_types VARCHAR(500) NOT NULL DEFAULT 'Standard Room';

UPDATE hotels
SET room_types = CASE name
    WHEN 'Seaside Breeze Resort' THEN 'Deluxe Sea View Room,Standard Room'
    WHEN 'Pink City Haveli' THEN 'Heritage Courtyard Room,Standard Room'
    WHEN 'Harbour View Suites' THEN 'Executive Harbour Suite,Deluxe Suite,Standard Room'
    WHEN 'Garden Tech Inn' THEN 'Garden King Room,Standard Room'
    WHEN 'Pine Hill Lodge' THEN 'Mountain View Cabin,Deluxe Cabin'
    WHEN 'Backwater Retreat' THEN 'Backwater Deluxe Room,Standard Room'
    ELSE room_types
END;
