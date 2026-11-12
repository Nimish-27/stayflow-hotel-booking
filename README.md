# Staywell Hotel Booking Portal

A full-stack hotel booking application with a Spring Boot API, MySQL persistence, Flyway migrations, and a React/Vite frontend.

## Features

- Search hotels by location and stay dates
- View availability, pricing, and alternative dates
- Create, retrieve, and cancel bookings
- Case-insensitive booking email lookup
- MySQL-backed inventory with transactional booking logic
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

Flyway automatically applies the schema and sample hotel data when the backend starts.

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
GET /api/hotels/{id}?checkIn=YYYY-MM-DD&checkOut=YYYY-MM-DD
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
  "checkOut": "2026-12-13"
}
```

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