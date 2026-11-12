-- V2: sample hotels (one room category each). Kept in a separate folder so it can be excluded
-- by changing spring.flyway.locations (see db/README.md). Prices are in INR.

INSERT INTO hotels (name, location, description, amenities, price_per_night, total_rooms) VALUES
('Seaside Breeze Resort', 'Goa',
 'Beachfront resort a short walk from the sand, with a pool and an open-air restaurant.',
 'WiFi,Pool,Breakfast,Beach Access,Air Conditioning', 6500.00, 8),

('Pink City Haveli', 'Jaipur',
 'Restored haveli in the old city with courtyard dining and rooftop views of the forts.',
 'WiFi,Breakfast,Courtyard,Airport Shuttle', 4200.00, 5),

('Harbour View Suites', 'Mumbai',
 'Modern business hotel near the waterfront with a gym and 24-hour room service.',
 'WiFi,Gym,Room Service,Air Conditioning,Parking', 9800.00, 10),

('Garden Tech Inn', 'Bengaluru',
 'Quiet inn close to the tech parks, with a leafy garden and a co-working lounge.',
 'WiFi,Co-working Lounge,Breakfast,Parking', 3600.00, 6),

('Pine Hill Lodge', 'Manali',
 'Wooden mountain lodge with valley views, a fireplace lounge and guided trek bookings.',
 'WiFi,Fireplace,Breakfast,Heating,Mountain View', 3000.00, 3),

('Backwater Retreat', 'Kochi',
 'Peaceful stay on the backwaters with houseboat trips and Kerala-style meals.',
 'WiFi,Houseboat Trips,Breakfast,Air Conditioning', 5400.00, 4);