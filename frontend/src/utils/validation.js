import { MAX_EMAIL_LENGTH, MAX_NAME_LENGTH } from '../config';

// Same pattern as the backend CreateBookingRequest: a dot is required in the domain.
const EMAIL_PATTERN = /^[^@\s]+@[^@\s.]+(\.[^@\s.]+)+$/;

function validateEmail(value) {
  const email = value.trim();
  if (!email) {
    return 'Email is required';
  }
  if (email.length > MAX_EMAIL_LENGTH || !EMAIL_PATTERN.test(email)) {
    return 'Enter a valid email address';
  }
  return '';
}

// Each validator returns an object that only contains the invalid fields.
export function validateGuest({ guestName, guestEmail }) {
  const errors = {};
  const name = guestName.trim();
  if (!name) {
    errors.guestName = 'Name is required';
  } else if (name.length > MAX_NAME_LENGTH) {
    errors.guestName = `Name must be at most ${MAX_NAME_LENGTH} characters`;
  }
  const emailError = validateEmail(guestEmail);
  if (emailError) {
    errors.guestEmail = emailError;
  }
  return errors;
}

export function validateLookup({ reference, email }) {
  const errors = {};
  if (!reference.trim()) {
    errors.reference = 'Booking reference is required';
  }
  const emailError = validateEmail(email);
  if (emailError) {
    errors.email = emailError;
  }
  return errors;
}