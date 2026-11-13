-- V6: add classification details for each selectable room category.

ALTER TABLE hotels
    ADD COLUMN room_type_details VARCHAR(2000) NOT NULL DEFAULT
    'Standard Room|Double|Queen|Comfortable room with essential amenities';

UPDATE hotels
SET room_type_details = CASE name
    WHEN 'Seaside Breeze Resort' THEN
        'Deluxe Sea View Room|Double|King|Upgraded room with sea views and enhanced furnishings;Standard Room|Double|Queen|Baseline room with essential amenities'
    WHEN 'Pink City Haveli' THEN
        'Heritage Courtyard Room|Double|King|Upgraded room around the restored courtyard;Standard Room|Double|Queen|Baseline room with essential amenities'
    WHEN 'Harbour View Suites' THEN
        'Executive Harbour Suite|Quad|King|Separate living area with a work space and harbour views;Deluxe Suite|Triple|King|Spacious upgraded suite with a sitting area;Standard Room|Double|Queen|Baseline room with essential amenities'
    WHEN 'Garden Tech Inn' THEN
        'Garden King Room|Double|King|Room overlooking the garden with a dedicated work desk;Standard Room|Double|Queen|Baseline room with essential amenities'
    WHEN 'Pine Hill Lodge' THEN
        'Mountain View Cabin|Triple|King|Larger cabin with valley views and a fireplace;Deluxe Cabin|Double|Queen|Upgraded cabin with enhanced furnishings;Standard Room|Double|Queen|Baseline room with essential amenities'
    WHEN 'Backwater Retreat' THEN
        'Backwater Deluxe Room|Double|King|Upgraded room with backwater views;Standard Room|Double|Queen|Baseline room with essential amenities'
    ELSE room_type_details
END;
