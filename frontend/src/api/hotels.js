import { request } from './client';

export function searchHotels({ location, checkIn, checkOut }) {
  return request('/hotels', { params: { location, checkIn, checkOut } });
}

export function getHotel(id, { checkIn, checkOut } = {}) {
  return request(`/hotels/${encodeURIComponent(id)}`, { params: { checkIn, checkOut } });
}

export function getAlternatives(id, checkIn, checkOut) {
  return request(`/hotels/${encodeURIComponent(id)}/alternatives`, { params: { checkIn, checkOut } });
}