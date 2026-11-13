import { request } from './client';

export function createBooking(payload) {
  return request('/bookings', { method: 'POST', body: payload });
}

export function getBooking(reference, email) {
  return request(`/bookings/${encodeURIComponent(reference.trim())}`, { params: { email } });
}

export function cancelBooking(reference, email) {
  return request(`/bookings/${encodeURIComponent(reference.trim())}/cancel`, {
    method: 'POST',
    params: { email },
  });
}