# Database scripts

| Item | Location | Purpose |
|---|---|---|
| `create-database.sql` | `db/setup/` | One-time: creates `hotel_booking`, `hotel_booking_test` and the `hotel_app` user |
| `V1__create_schema.sql` | `db/migration/` | Schema (Flyway) |
| `V3__` to `V11__` | `db/migration/` | Room metadata, room-type inventory, booking fields, GST, sample hotels, and legacy cleanup |

Flyway runs the migrations automatically when the backend starts. Never edit a migration that
has already been applied: add a new `V3__...` file instead.

## Setup

1. Edit the password in `create-database.sql` (do not commit it), then run:
   `mysql -u root -p < db/setup/create-database.sql`
2. Export the connection settings (see `.env.example`). Spring Boot does not read `.env`
   files by itself, so export them in your shell or IDE run configuration.

## Sample data on or off

Sample hotels are included in the main migration chain by V10. The legacy `db/sample-data`
folder is no longer part of the default runtime locations. V2 remains there as an archival
assignment-era seed script; V10 is the idempotent replacement because deployed databases cannot
have an already-applied migration rewritten or renumbered.

## Room-type inventory (365-day rolling window)

`room_type_inventory` is filled by `InventorySeeder` (backend), not by a migration, because a one-time
migration would run out of dates as time passes. It runs at startup and once a day, and it only
inserts missing rows, so running it again changes nothing. The statement it runs, with literal
dates for manual checks (the app passes the dates from its Asia/Kolkata `Clock`):

The seeder inserts one row for every configured hotel/room-type/date combination, covering 372
days per room type and hotel (365-day booking window plus a 7-day safety buffer), using an
idempotent `INSERT ... ON DUPLICATE KEY UPDATE` statement.
The former `room_inventory` table was dropped by V11 because no runtime path uses it.

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
SELECT hotel_id, room_type, COUNT(*), MIN(stay_date), MAX(stay_date)
FROM room_type_inventory GROUP BY hotel_id, room_type; -- 372 per room type
-- run the seeder again: "0 rows added" proves it is idempotent

-- This must SUCCEED, and the same statement run a second time must FAIL (1062 duplicate reference):
INSERT INTO bookings (reference, hotel_id, guest_name, guest_email, check_in, check_out,
                      nights, total_amount, status, created_at, cancelled_at)
VALUES ('HB-TEST2222', 1, 'Asha', 'asha@example.com', '2030-01-10', '2030-01-12',
        2, 13000.00, 'CONFIRMED', UTC_TIMESTAMP(3), NULL);

-- Each of these must FAIL (3819 CHECK): change nights to 5 | check_out before check_in |
-- status 'CANCELLED' with cancelled_at NULL. Use a new reference each time.
```

Clean up: `mysql -u root -p -e "DROP DATABASE hotel_booking_scratch"`