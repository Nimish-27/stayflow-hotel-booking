-- V7: divide each hotel's inventory among its room categories.

ALTER TABLE hotels
    ADD COLUMN room_type_counts VARCHAR(1000) NOT NULL DEFAULT 'Standard Room=1';

UPDATE hotels
SET room_type_counts = CASE name
    WHEN 'Seaside Breeze Resort' THEN 'Deluxe Sea View Room=3,Standard Room=5'
    WHEN 'Pink City Haveli' THEN 'Heritage Courtyard Room=2,Standard Room=3'
    WHEN 'Harbour View Suites' THEN 'Executive Harbour Suite=2,Deluxe Suite=3,Standard Room=5'
    WHEN 'Garden Tech Inn' THEN 'Garden King Room=2,Standard Room=4'
    WHEN 'Pine Hill Lodge' THEN 'Mountain View Cabin=1,Deluxe Cabin=1,Standard Room=1'
    WHEN 'Backwater Retreat' THEN 'Backwater Deluxe Room=2,Standard Room=2'
    ELSE CONCAT(room_type, '=', total_rooms)
END;
