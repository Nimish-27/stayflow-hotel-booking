# StayFlow: Hotel Booking Portal

A full-stack hotel booking application: browse hotels, check availability for your dates, see
the price before you commit, book a room, and manage the booking with its reference and email.

**Stack:** React (Vite) · Spring Boot 3 (Java 17) · MySQL 8 · Flyway

> Built for a full-stack interview assignment. For how and why it is built this way, read
> [DESIGN.md](DESIGN.md).

## Contents

- [Features](#features)
- [Quick start](#quick-start)
- [Configuration](#configuration)
- [Running the tests](#running-the-tests)
- [API reference](#api-reference)
- [Business rules and assumptions](#business-rules-and-assumptions)
- [Surprise feature](#surprise-feature-smart-alternative-dates)
- [Screenshots](#screenshots)
- [Project structure](#project-structure)
- [Submission notes](#submission-notes)
- [Known limitations](#known-limitations)

## Features

**Required by the assignment**

- Browse hotels with name, location, description, amenities and price per night
- Search by location and check-in/check-out dates
- See availability and the total price for the selected stay before confirming
- Book a room with name and email; receive a unique booking reference
- Retrieve a booking with its reference and email, and cancel it
- Cancelling releases the held availability
- Price calculated on the backend; two users cannot book the last room at the same time

**Added on top**

- Room categories per hotel, each with its own price, stock and capacity
- Guests and rooms selection, validated and stored with the booking
- 18% GST breakdown (subtotal, GST, total), stored with each booking
- Cancellation window of 48 hours before check-in, with a short grace period
- Availability calendar and "rooms left" indicator
- Smart alternative dates when a stay is sold out (the surprise feature)

## Quick start

### Prerequisites

| Tool | Version |
|---|---|
| JDK | 17 or newer |
| Maven | 3.9+ |
| Node.js + npm | current LTS |
| MySQL | **8.0.16 or newer** (older versions ignore `CHECK` constraints) |

### 1. Create the database

Open `db/setup/create-database.sql` and **replace `CHANGE_ME` with a real password** (do not
commit that change). Then run it as a MySQL administrator:

```bash
mysql -u root -p < db/setup/create-database.sql
```

This creates the `hotel_booking` and `hotel_booking_test` databases and the `hotel_app` user.
The schema and sample hotels are applied automatically by Flyway when the backend starts.

### 2. Start the backend

macOS / Linux:

```bash
export DB_URL='jdbc:mysql://localhost:3306/hotel_booking?useSSL=false&allowPublicKeyRetrieval=true&connectionTimeZone=UTC'
export DB_USER='hotel_app'
export DB_PASSWORD='your_password'

cd backend
mvn spring-boot:run
```

Windows (PowerShell):

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/hotel_booking?useSSL=false&allowPublicKeyRetrieval=true&connectionTimeZone=UTC'
$env:DB_USER = 'hotel_app'
$env:DB_PASSWORD = 'your_password'

Set-Location backend
mvn spring-boot:run
```

The API starts at `http://localhost:8080`.

For manual API testing, import [postman/Stayflow Hotel Booking.postman_collection.json](G:/hotel-booking-portal/postman/Stayflow%20Hotel%20Booking.postman_collection.json)
into Postman. Run **Create booking** first; it stores the returned reference for the retrieve
and cancel requests.

### 3. Start the frontend

In a second terminal:

```bash
cd frontend
cp .env.example .env        # PowerShell: Copy-Item .env.example .env
npm install
npm run dev
```

Open `http://localhost:5173`.

Other frontend scripts: `npm run build`, `npm run lint`, `npm run preview`.

### 4. Try it

1. Search for a location (for example "Goa") with check-in and check-out dates.
2. Open a hotel, pick a room type, and review the price breakdown.
3. Enter a name and email and confirm. Note the booking reference.
4. Open **Manage booking**, enter the reference and email, then view or cancel it.

## Configuration

Spring Boot does not read `.env` files. Set these as environment variables or in your IDE run
configuration. `.env.example` lists them; never commit real values.

| Variable | Required | Default | Purpose |
|---|---|---|---|
| `DB_URL` | yes | none | JDBC URL of the application database |
| `DB_USER` | yes | none | Database user |
| `DB_PASSWORD` | yes | none | Database password |
| `TEST_DB_URL` | for tests | none | JDBC URL of the **separate** test database |
| `APP_TIMEZONE` | no | `Asia/Kolkata` | Zone used to decide "today" and cancellation deadlines |
| `CORS_ALLOWED_ORIGIN` | no | `http://localhost:5173` | Allowed frontend origin |
| `VITE_API_URL` | frontend | `http://localhost:8080/api` | API base URL used by the frontend |

Business limits live in `backend/src/main/resources/application.yml` under `app.booking`:

| Property | Value | Meaning |
|---|---|---|
| `max-nights` | 30 | Longest allowed stay |
| `window-days` | 365 | How far ahead a booking can be made |
| `free-cancellation-hours` | 48 | Cancellation closes this long before check-in |
| `cancellation-grace-hours` | 2 | Grace period for bookings made inside that window |
| `alternative-shift-days` | 7 | How far alternative dates may shift |
| `max-alternatives` | 3 | Number of alternatives returned |

## Running the tests

The integration tests **wipe data**, so they use a separate schema. Set `TEST_DB_URL` to
`hotel_booking_test` (created by the setup script) in addition to `DB_USER` and `DB_PASSWORD`:

```bash
export TEST_DB_URL='jdbc:mysql://localhost:3306/hotel_booking_test?useSSL=false&allowPublicKeyRetrieval=true&connectionTimeZone=UTC'

cd backend
mvn test
```

What is covered:

- **Unit tests:** night counting, price and GST calculation, date validation, overlap and
  back-to-back stays, sold-out detection, cancellation, repeated cancellation, reference
  generation, alternative dates
- **Integration tests:** the full book, retrieve and cancel flow; concurrent requests for the
  last room (exactly one must succeed); idempotent inventory seeding

## API reference

All endpoints are under `/api`. Dates use `YYYY-MM-DD`.

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/hotels?location=&checkIn=&checkOut=` | Search hotels; with dates, includes availability and a quote |
| `GET` | `/hotels/{id}?checkIn=&checkOut=&roomType=&rooms=` | Hotel details, room options and quote |
| `GET` | `/hotels/{id}/availability?from=&to=&roomType=` | Per-night availability for the calendar |
| `GET` | `/hotels/{id}/alternatives?checkIn=&checkOut=` | Nearby available stays when the dates are sold out |
| `POST` | `/bookings` | Create a booking |
| `GET` | `/bookings/{reference}?email=` | Retrieve a booking |
| `POST` | `/bookings/{reference}/cancel?email=` | Cancel a booking |

**Create a booking**

```bash
curl -X POST http://localhost:8080/api/bookings \
  -H 'Content-Type: application/json' \
  -d '{
    "hotelId": 1,
    "guestName": "Asha Sharma",
    "guestEmail": "asha@example.com",
    "checkIn": "2026-12-10",
    "checkOut": "2026-12-13",
    "roomType": "Deluxe Sea View Room",
    "adults": 2,
    "children": 0,
    "rooms": 1
  }'
```

The response includes the reference plus `subtotalAmount`, `gstRate`, `gstAmount`,
`totalAmount`, `cancellable` and `cancellableUntil`.

**Retrieve and cancel**

```bash
curl 'http://localhost:8080/api/bookings/HB-XXXXXXXX?email=asha@example.com'
curl -X POST 'http://localhost:8080/api/bookings/HB-XXXXXXXX/cancel?email=asha@example.com'
```

**Errors** always use one shape: `{ timestamp, status, error, message, path, fieldErrors[] }`.

| Status | Meaning |
|---|---|
| `400` | Invalid input (email, dates, stay length, guests) |
| `404` | Hotel not found, or reference and email do not match |
| `409` | Sold out, or cancellation no longer allowed |
| `500` | Unexpected error (no stack trace is returned) |

## Business rules and assumptions

- **Overlap rule:** a stay occupies the nights `[check-in, check-out)`. The check-out day is
  not a booked night, so back-to-back stays are allowed and a sold-out check-out day can still
  be chosen.
- **Stay limits:** check-in cannot be in the past; maximum 30 nights; bookings up to 365 days
  ahead.
- **Pricing:** `subtotal = nights × nightly rate × rooms`; `GST = 18% of subtotal`;
  `total = subtotal + GST`. Calculated on the backend only, and stored with the booking.
- **GST:** room prices are treated as GST-exclusive, and 18% is a flat assumption (real hotel
  GST depends on the room tariff). This is not tax advice.
- **Cancellation:** allowed until 48 hours before 00:00 on the check-in date (in
  `APP_TIMEZONE`). A booking made inside that window can be cancelled for 2 hours after it is
  created, but never after check-in begins. There are no fees or refunds because there are no
  payments. Cancelling twice is safe.
- **Guests:** adults and children each count as one guest toward room capacity.
- **Booking reference:** random and unique (`HB-XXXXXXXX`), not sequential.
- **Privacy:** a wrong email and an unknown reference return the same `404`, so references
  cannot be guessed.
- **Timezone:** "today" follows `APP_TIMEZONE` (default `Asia/Kolkata`); stored timestamps are
  UTC.

## Surprise feature: smart alternative dates

**What:** when the chosen dates are sold out, the app suggests the nearest available stays with
the same number of nights, up to 7 days earlier or later, each with its total price.

**Why:** a "sold out" message ends a guest's search. Suggesting nearby dates keeps them in the
booking flow.

**How:** one query loads the inventory for the surrounding window, and each candidate stay is
checked in memory with the same availability and pricing rules as a real booking. The 3 nearest
bookable stays are returned. See [DESIGN.md](DESIGN.md#8-surprise-feature-smart-alternative-dates).

## Screenshots


[Search results]docs/screenshots/Search Hotel.jpg
[Hotel details and price breakdown](docs/screenshots/Booking Page.jpg)
[Booking confirmation](docs/screenshots/Booking Confirmation.jpg)
[Manage booking](docs/screenshots/Find Booking.jpg)


## Project structure

```text
backend/    Spring Boot API: controller, service, repository, entity, dto, config, tests
frontend/   React (Vite) application: pages, components, API layer
db/         Database setup script and Flyway migrations
docs/       Screenshots
DESIGN.md   Architecture, database design, concurrency, trade-offs
```


## Git workflow

- `main` contains the stable branch.
- `development` is the active development branch.

```powershell
git checkout development
git pull
# make changes
git add .
git commit -m "feat: describe your change"
git push
```

## Submission notes

- **Time spent:** approximately 14 hours, including the booking flow, room inventory, UI,
  migrations, validation, tests and documentation.
- **AI assistance:** AI coding assistance was used for code review, refactoring, and test and
  build troubleshooting. All code and design decisions have been reviewed and can be explained
  by the author.
- **Not implemented (out of scope):** authentication, payments, an admin panel and cloud
  deployment.

## Known limitations

- Room-type names, prices and counts are stored as delimited text on `hotels`; a normalised
  `room_types` table is the planned improvement.
- GST is a flat 18%, not a tariff-slab tax engine.
- The booking lookup puts the email in the query string, which can appear in server logs.
- Booking has no idempotency key; the UI disables the button while submitting.
- Hotel images are project assets: confirm usage rights or replace them before publishing.

The full list, with reasons and future improvements, is in [DESIGN.md](DESIGN.md#11-limitations-and-future-improvements).



