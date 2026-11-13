// Display-only constants. The backend is the source of truth for every rule here.
export const CURRENCY_SYMBOL = '₹';

// Mirrors app.booking.max-nights and app.booking.window-days in the backend application.yml.
export const BOOKING_RULES = {
  maxNights: 30,
  windowDays: 365,
};

export const MAX_NAME_LENGTH = 100;
export const MAX_EMAIL_LENGTH = 254;