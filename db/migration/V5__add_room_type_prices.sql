-- V5: assign a nightly price to every room category.

ALTER TABLE hotels
    ADD COLUMN room_type_prices VARCHAR(1000) NOT NULL DEFAULT 'Standard Room=0';

UPDATE hotels
SET room_type_prices = CASE name
    WHEN 'Seaside Breeze Resort' THEN 'Deluxe Sea View Room=7500,Standard Room=6500'
    WHEN 'Pink City Haveli' THEN 'Heritage Courtyard Room=5000,Standard Room=4200'
    WHEN 'Harbour View Suites' THEN 'Executive Harbour Suite=12500,Deluxe Suite=11000,Standard Room=9800'
    WHEN 'Garden Tech Inn' THEN 'Garden King Room=4200,Standard Room=3600'
    WHEN 'Pine Hill Lodge' THEN 'Mountain View Cabin=3600,Deluxe Cabin=3300,Standard Room=3000'
    WHEN 'Backwater Retreat' THEN 'Backwater Deluxe Room=6200,Standard Room=5400'
    ELSE CONCAT(room_type, '=', price_per_night)
END;
