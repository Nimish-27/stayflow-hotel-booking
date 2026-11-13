-- V9: preserve the GST breakdown used to calculate each booking total.
ALTER TABLE bookings
    ADD COLUMN subtotal_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    ADD COLUMN gst_rate DECIMAL(5,4) NOT NULL DEFAULT 0.1800,
    ADD COLUMN gst_amount DECIMAL(12,2) NOT NULL DEFAULT 0;

UPDATE bookings
SET subtotal_amount = ROUND(total_amount / 1.18, 2),
    gst_amount = total_amount - ROUND(total_amount / 1.18, 2)
WHERE subtotal_amount = 0 AND total_amount > 0;
