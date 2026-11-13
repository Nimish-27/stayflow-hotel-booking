# Staywell Hotel Booking Portal

A full-stack hotel booking application with a Spring Boot API, MySQL persistence, Flyway migrations, and a React/Vite frontend.

This repository implements the interview assignment in the supplied Word brief. The original
single-room-per-hotel flow is extended with room categories, occupancy selection, per-category
inventory, server-side GST breakdowns, and alternative-date suggestions.

## Features

- Search hotels by location and stay dates
- View availability, pricing, and alternative dates
- View an 18% GST breakdown before booking (the portal assumes the displayed nightly rate is GST-exclusive)
- Create, retrieve, and cancel bookings
- Case-insensitive booking email lookup
- MySQL-backed inventory with transactional booking logic
- Room-type inventory, occupancy enforcement, and server-persisted GST breakdowns
- Cancellations expose their exact deadline; bookings made inside the 48-hour window receive a two-hour grace period, capped at check-in
- Responsive, minimalist frontend

## Tech stack

- **Backend:** Java 17+, Spring Boot 3.3, Spring Data JPA, Flyway
- **Database:** MySQL 8+
- **Frontend:** React, Vite, React Router

## Prerequisites

- JDK 17 or newer
- Maven 3.9+
- Node.js and npm
- MySQL 8+

## Database setup

Run the setup script as a MySQL administrator. Update the placeholder password before running it:

```powershell
mysql -u root -p < db/setup/create-database.sql
```

The script creates:

- `hotel_booking`
- `hotel_booking_test`
- `hotel_app` with access to both databases

Flyway automatically applies the schema and development sample hotel data when the backend starts.

## Backend

From the repository root, configure the database variables in PowerShell:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.12.1'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:DB_URL = 'jdbc:mysql://localhost:3306/hotel_booking?useSSL=false&allowPublicKeyRetrieval=true&connectionTimeZone=UTC'
$env:DB_USER = 'hotel_app'
$env:DB_PASSWORD = 'your_password'
```

Start the API:

```powershell
Set-Location backend
mvn spring-boot:run
```

The API runs at `http://localhost:8080`.

Run backend tests:

```powershell
mvn test
```

## Frontend

In a second terminal:

```powershell
Set-Location frontend
Copy-Item .env.example .env
npm install
npm run dev
```

The frontend runs at `http://localhost:5173`.

Available frontend scripts:

```powershell
npm run dev
npm run build
npm run lint
npm run preview
```

## API endpoints

### Hotels

```text
GET /api/hotels?location=goa&checkIn=YYYY-MM-DD&checkOut=YYYY-MM-DD
GET /api/hotels/{id}?checkIn=YYYY-MM-DD&checkOut=YYYY-MM-DD&roomType=Deluxe&rooms=1
GET /api/hotels/{id}/availability?from=YYYY-MM-DD&to=YYYY-MM-DD&roomType=Deluxe&rooms=1
GET /api/hotels/{id}/alternatives?checkIn=YYYY-MM-DD&checkOut=YYYY-MM-DD
```

### Bookings

```text
POST /api/bookings
GET  /api/bookings/{reference}?email=guest@example.com
POST /api/bookings/{reference}/cancel?email=guest@example.com
```

Example booking request:

```json
{
  "hotelId": 1,
  "guestName": "Asha Sharma",
  "guestEmail": "asha@example.com",
  "checkIn": "2026-12-10",
  "checkOut": "2026-12-13",
  "roomType": "Deluxe Sea View Room",
  "adults": 2,
  "children": 0,
  "rooms": 1
}
```

Booking responses include `subtotalAmount`, `gstRate`, `gstAmount`, `cancellable`, and
`cancellableUntil`. The cancellation deadline is 48 hours before check-in for ordinary
bookings; a booking created inside that window can be cancelled for two hours after creation,
but never after check-in begins.

## GST assumption

GST is modelled as 18% of the GST-exclusive room subtotal. The backend calculates and stores
the subtotal, rate, GST amount, and total at booking time so later tax-rate changes do not
alter historical bookings. This is a product assumption, not tax advice; verify the applicable
rate and whether a listed rate should be tax-inclusive before production use.

## Image rights and delivery

The six hotel images in `frontend/public/images` were supplied for this project and are used
under the supplier's permission. They are converted to WebP for delivery, mapped only to their
matching hotel names, and rendered with descriptive alt text and lazy loading on result cards.
Replace them only with assets for which the project has documented usage rights.

## Additional product rules

These features extend the basic booking assignment:

- **Room types:** each category has its own price, capacity, allocation, and date inventory.
  The current database stores room metadata as delimited text in `hotels` for a small demo
  dataset; V8 adds normalized per-type inventory rows. A larger production system should move
  room definitions and prices into dedicated tables.
- **Occupancy:** adults, children, and rooms are sent to the API, validated against the selected
  room capacity, and stored with the booking. Children count as one guest because the product
  has no age-band rules.
- **Calendar:** availability covers nights in `[checkIn, checkOut)`, so check-out is exclusive.
  The calendar blocks sold-out check-in nights, but a sold-out check-out day may still be chosen.
  Availability is limited to the configured 365-day window and 30-night maximum stay.
- **Cancellation:** the API returns the authoritative `cancellableUntil` timestamp; the browser
  does not recalculate it. Cancellation is blocked during the final 48 hours before check-in,
  except for the short grace period documented above.

## Assignment design summary

### Architecture

The React/Vite frontend calls the Spring Boot REST API. Spring services validate dates,
occupancy, room type, pricing, and inventory before writing through Spring Data JPA to MySQL.
Flyway owns schema changes and seeds the development hotels; the frontend never calculates an
authoritative total.

### Database and booking logic

The main entities are `hotels`, `room_type_inventory`, and `bookings`.
Bookings reference a hotel and persist the selected room type, guest counts, room count,
subtotal, GST rate, GST amount, total, and status. Check-out is exclusive, so a stay owns
`[checkIn, checkOut)`. Cancellation changes the booking status and releases the same inventory
rows.

For simultaneous bookings, the selected room-type inventory rows are locked with a pessimistic
write lock inside the booking transaction. Each night is checked before incrementing inventory,
so two requests competing for the last room cannot both succeed.

### Surprise feature

The surprise feature is **smart alternative dates**. When the requested dates are unavailable,
the API searches nearby stays with the same length and returns bookable alternatives. This helps
guests keep the hotel while adjusting dates instead of restarting their search.

### Submission notes

- Approximate effort: 14 hours, including the booking flow, room inventory, UI, migrations,
  validation, tests, and documentation.
- Significant assistance: AI coding assistance was used for implementation review, refactoring,
  and test/build troubleshooting. The project code and design decisions should be reviewed and
  explained by the submitter during the interview.
- Screenshots or a demo recording are not stored in this repository yet; capture the search,
  room selection, booking confirmation, and manage-booking flow before submission.

## Project structure

```text
db/          MySQL setup, Flyway migrations, and sample data
backend/     Spring Boot API and tests
frontend/    React/Vite web application
docs/        Project screenshots and documentation assets
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

Never commit real credentials. Use the example environment files as templates.