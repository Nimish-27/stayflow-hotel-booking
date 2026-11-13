const HOTEL_IMAGES = {
  'Backwater Retreat': '/images/backwater-retreat.webp',
  'Garden Tech Inn': '/images/garden-tech-inn.webp',
  'Harbour View Suites': '/images/harbour-view-suites.webp',
  'Pine Hill Lodge': '/images/pine-hill-lodge.webp',
  'Pink City Haveli': '/images/pink-city-haveli.webp',
  'Seaside Breeze Resort': '/images/seaside-breeze-resort.webp',
};

export function getHotelImage(hotel) {
  return HOTEL_IMAGES[hotel.name] ?? null;
}
