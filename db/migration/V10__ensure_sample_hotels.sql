-- V10: keep the development sample hotels in the main migration chain.
-- The existence checks make this safe for databases that already contain them.

INSERT INTO hotels (
    name, location, description, amenities, room_type, room_types,
    room_type_prices, room_type_details, room_type_counts,
    max_occupancy, price_per_night, total_rooms
)
SELECT 'Seaside Breeze Resort', 'Goa',
       'Beachfront resort a short walk from the sand, with a pool and an open-air restaurant.',
       'WiFi,Pool,Breakfast,Beach Access,Air Conditioning',
       'Deluxe Sea View Room', 'Deluxe Sea View Room,Standard Room',
       'Deluxe Sea View Room=7500,Standard Room=6500',
       'Deluxe Sea View Room|Double|King|Upgraded room with sea views and enhanced furnishings;Standard Room|Double|Queen|Baseline room with essential amenities',
       'Deluxe Sea View Room=3,Standard Room=5', 2, 6500.00, 8
WHERE NOT EXISTS (SELECT 1 FROM hotels WHERE name = 'Seaside Breeze Resort');

INSERT INTO hotels (
    name, location, description, amenities, room_type, room_types,
    room_type_prices, room_type_details, room_type_counts,
    max_occupancy, price_per_night, total_rooms
)
SELECT 'Pink City Haveli', 'Jaipur',
       'Restored haveli in the old city with courtyard dining and rooftop views of the forts.',
       'WiFi,Breakfast,Courtyard,Airport Shuttle',
       'Heritage Courtyard Room', 'Heritage Courtyard Room,Standard Room',
       'Heritage Courtyard Room=5000,Standard Room=4200',
       'Heritage Courtyard Room|Double|King|Upgraded room around the restored courtyard;Standard Room|Double|Queen|Baseline room with essential amenities',
       'Heritage Courtyard Room=2,Standard Room=3', 2, 4200.00, 5
WHERE NOT EXISTS (SELECT 1 FROM hotels WHERE name = 'Pink City Haveli');

INSERT INTO hotels (
    name, location, description, amenities, room_type, room_types,
    room_type_prices, room_type_details, room_type_counts,
    max_occupancy, price_per_night, total_rooms
)
SELECT 'Harbour View Suites', 'Mumbai',
       'Modern business hotel near the waterfront with a gym and 24-hour room service.',
       'WiFi,Gym,Room Service,Air Conditioning,Parking',
       'Executive Harbour Suite', 'Executive Harbour Suite,Deluxe Suite,Standard Room',
       'Executive Harbour Suite=12500,Deluxe Suite=11000,Standard Room=9800',
       'Executive Harbour Suite|Quad|King|Separate living area with a work space and harbour views;Deluxe Suite|Triple|King|Spacious upgraded suite with a sitting area;Standard Room|Double|Queen|Baseline room with essential amenities',
       'Executive Harbour Suite=2,Deluxe Suite=3,Standard Room=5', 4, 9800.00, 10
WHERE NOT EXISTS (SELECT 1 FROM hotels WHERE name = 'Harbour View Suites');

INSERT INTO hotels (
    name, location, description, amenities, room_type, room_types,
    room_type_prices, room_type_details, room_type_counts,
    max_occupancy, price_per_night, total_rooms
)
SELECT 'Garden Tech Inn', 'Bengaluru',
       'Quiet inn close to the tech parks, with a leafy garden and a co-working lounge.',
       'WiFi,Co-working Lounge,Breakfast,Parking',
       'Garden King Room', 'Garden King Room,Standard Room',
       'Garden King Room=4200,Standard Room=3600',
       'Garden King Room|Double|King|Room overlooking the garden with a dedicated work desk;Standard Room|Double|Queen|Baseline room with essential amenities',
       'Garden King Room=2,Standard Room=4', 2, 3600.00, 6
WHERE NOT EXISTS (SELECT 1 FROM hotels WHERE name = 'Garden Tech Inn');

INSERT INTO hotels (
    name, location, description, amenities, room_type, room_types,
    room_type_prices, room_type_details, room_type_counts,
    max_occupancy, price_per_night, total_rooms
)
SELECT 'Pine Hill Lodge', 'Manali',
       'Wooden mountain lodge with valley views, a fireplace lounge and guided trek bookings.',
       'WiFi,Fireplace,Breakfast,Heating,Mountain View',
       'Mountain View Cabin', 'Mountain View Cabin,Deluxe Cabin,Standard Room',
       'Mountain View Cabin=3600,Deluxe Cabin=3300,Standard Room=3000',
       'Mountain View Cabin|Triple|King|Larger cabin with valley views and a fireplace;Deluxe Cabin|Double|Queen|Upgraded cabin with enhanced furnishings;Standard Room|Double|Queen|Baseline room with essential amenities',
       'Mountain View Cabin=1,Deluxe Cabin=1,Standard Room=1', 2, 3000.00, 3
WHERE NOT EXISTS (SELECT 1 FROM hotels WHERE name = 'Pine Hill Lodge');

INSERT INTO hotels (
    name, location, description, amenities, room_type, room_types,
    room_type_prices, room_type_details, room_type_counts,
    max_occupancy, price_per_night, total_rooms
)
SELECT 'Backwater Retreat', 'Kochi',
       'Peaceful stay on the backwaters with houseboat trips and Kerala-style meals.',
       'WiFi,Houseboat Trips,Breakfast,Air Conditioning',
       'Backwater Deluxe Room', 'Backwater Deluxe Room,Standard Room',
       'Backwater Deluxe Room=6200,Standard Room=5400',
       'Backwater Deluxe Room|Double|King|Upgraded room with backwater views;Standard Room|Double|Queen|Baseline room with essential amenities',
       'Backwater Deluxe Room=2,Standard Room=2', 2, 5400.00, 4
WHERE NOT EXISTS (SELECT 1 FROM hotels WHERE name = 'Backwater Retreat');
