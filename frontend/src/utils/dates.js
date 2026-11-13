import { BOOKING_RULES } from '../config';

const MS_PER_DAY = 24 * 60 * 60 * 1000;

function pad(number) {
  return String(number).padStart(2, '0');
}

function parseIso(iso) {
  const [year, month, day] = iso.split('-').map(Number);
  return { year, month, day };
}

function nightsBetween(checkIn, checkOut) {
  const start = parseIso(checkIn);
  const end = parseIso(checkOut);
  // Date.UTC avoids daylight-saving shifts when subtracting dates.
  const diff = Date.UTC(end.year, end.month - 1, end.day) - Date.UTC(start.year, start.month - 1, start.day);
  return Math.round(diff / MS_PER_DAY);
}

export function toIsoDate(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

export function todayIso() {
  return toIsoDate(new Date());
}

export function addDays(iso, days) {
  const { year, month, day } = parseIso(iso);
  return toIsoDate(new Date(year, month - 1, day + days));
}

// Same rules and messages as the backend StayValidator. Returns '' when valid.
// ISO dates (yyyy-mm-dd) sort correctly as plain strings, so < and > compare dates.
export function validateStay(checkIn, checkOut) {
  const today = todayIso();
  if (checkOut <= checkIn) {
    return 'Check-out must be after check-in';
  }
  if (checkIn < today) {
    return 'Check-in cannot be in the past';
  }
  if (nightsBetween(checkIn, checkOut) > BOOKING_RULES.maxNights) {
    return `Stay cannot be longer than ${BOOKING_RULES.maxNights} nights`;
  }
  if (checkOut > addDays(today, BOOKING_RULES.windowDays)) {
    return `Bookings are limited to the next ${BOOKING_RULES.windowDays} days`;
  }
  return '';
}

// Search allows "no dates", but not just one of them.
export function validateSearchDates(checkIn, checkOut) {
  if (!checkIn && !checkOut) {
    return '';
  }
  if (!checkIn || !checkOut) {
    return 'Select both check-in and check-out dates, or leave both empty';
  }
  return validateStay(checkIn, checkOut);
}