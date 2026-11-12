# Database scripts

| Item | Location | Purpose |
|---|---|---|
| `create-database.sql` | `db/` | One-time: creates `hotel_booking`, `hotel_booking_test` and the `hotel_app` user |
| `V1__create_schema.sql` | `backend/src/main/resources/db/migration/` | Schema (Flyway) |
| `V2__sample_hotels.sql` | `backend/src/main/resources/db/sample-data/` | Sample hotels (Flyway) |

Flyway runs the migrations automatically when the backend starts. Never edit a migration that
has already been applied: add a new `V3__...` file instead.

## Setup

1. Edit the password in `create-database.sql` (do not commit it), then run:
   `mysql -u root -p < db/create-database.sql`
2. Export the connection settings (see `.env.example`). Spring Boot does not read `.env`
   files by itself, so export them in your shell or IDE run configuration.

## Sample data on or off

`spring.flyway.locations` defaults to `classpath:db/migration,classpath:db/sample-data`.
To start with an empty hotel table, set `FLYWAY_LOCATIONS=classpath:db/migration` **before the
first run**. Removing a location after its migrations were applied makes Flyway's validation fail.

## Room inventory (365-day rolling window)

`room_inventory` is filled by `InventorySeeder` (backend), not by a migration, because a one-time
migration would run out of dates as time passes. It runs at startup and once a day, and it only
inserts missing rows, so running it again changes nothing. The statement it runs, with literal
dates for manual checks (the app passes the dates from its Asia/Kolkata `Clock`):

```sql
INSERT INTO room_inventory (hotel_id, stay_date, booked_rooms)
WITH RECURSIVE days (stay_date) AS (
    SELECT DATE '2025-01-01'
    UNION ALL
    SELECT stay_date + INTERVAL 1 DAY FROM days
    WHERE stay_date < DATE '2025-01-01' + INTERVAL 371 DAY
)
SELECT h.id, days.stay_date, 0
FROM hotels h
CROSS JOIN days
WHERE NOT EXISTS (
    SELECT 1 FROM room_inventory ri
    WHERE ri.hotel_id = h.id AND ri.stay_date = days.stay_date
);
```

This inserts 372 days per hotel (365-day booking window plus a 7-day safety buffer).

## Verify the scripts on a scratch database

Do **not** apply the SQL by hand to `hotel_booking` or `hotel_booking_test`. Flyway would later
refuse a non-empty schema that has no history table. Use a throwaway database:

```bash
mysql -u root -p -e "CREATE DATABASE hotel_booking_scratch CHARACTER SET utf8mb4"
mysql -u root -p hotel_booking_scratch < backend/src/main/resources/db/migration/V1__create_schema.sql
mysql -u root -p hotel_booking_scratch < backend/src/main/resources/db/sample-data/V2__sample_hotels.sql
mysql -u root -p hotel_booking_scratch
```

Inside the `mysql` prompt:

```sql
SELECT VERSION();                                   -- must be >= 8.0.16
SELECT id, name, location, price_per_night, total_rooms FROM hotels;   -- 6 rows

-- paste the INSERT ... WITH RECURSIVE statement above (use today's date for both literals)
SELECT hotel_id, COUNT(*), MIN(stay_date), MAX(stay_date)
FROM room_inventory GROUP BY hotel_id;              -- 372 per hotel (2232 rows total)
-- run the same INSERT again: "0 rows affected" proves it is idempotent

-- Each of these must FAIL:
INSERT INTO room_inventory VALUES (1, CURDATE() + INTERVAL 2000 DAY, -1);   -- 3819 CHECK
INSERT INTO room_inventory VALUES (999, CURDATE() + INTERVAL 2000 DAY, 0);  -- 1452 foreign key

-- This must SUCCEED, and the same statement run a second time must FAIL (1062 duplicate reference):
INSERT INTO bookings (reference, hotel_id, guest_name, guest_email, check_in, check_out,
                      nights, total_amount, status, created_at, cancelled_at)
VALUES ('HB-TEST2222', 1, 'Asha', 'asha@example.com', '2030-01-10', '2030-01-12',
        2, 13000.00, 'CONFIRMED', UTC_TIMESTAMP(3), NULL);

-- Each of these must FAIL (3819 CHECK): change nights to 5 | check_out before check_in |
-- status 'CANCELLED' with cancelled_at NULL. Use a new reference each time.
```

Clean up: `mysql -u root -p -e "DROP DATABASE hotel_booking_scratch"`