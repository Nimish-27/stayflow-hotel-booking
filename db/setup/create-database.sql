-- One-time setup. Run as a MySQL admin user (e.g. root).
-- 1. Replace CHANGE_ME with a real password BEFORE running (do not commit the change).
-- 2. mysql -u root -p < db/create-database.sql

CREATE DATABASE IF NOT EXISTS hotel_booking
    CHARACTER SET utf8mb4;

CREATE DATABASE IF NOT EXISTS hotel_booking_test
    CHARACTER SET utf8mb4;

CREATE USER IF NOT EXISTS 'hotel_app'@'localhost' IDENTIFIED BY 'CHANGE_ME';

GRANT ALL PRIVILEGES ON hotel_booking.*      TO 'hotel_app'@'localhost';
GRANT ALL PRIVILEGES ON hotel_booking_test.* TO 'hotel_app'@'localhost';