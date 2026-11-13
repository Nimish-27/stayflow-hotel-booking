import { Link } from 'react-router-dom';
import { formatMoney, pluralize } from '../utils/format';
import { getHotelImage } from '../utils/hotelImages';
import { buildSearch } from '../utils/query';

export default function HotelCard({ hotel, occupancy }) {
  const { quote } = hotel;
  const maxOccupancy = hotel.maxOccupancy ?? 2;
  const soldOut = quote !== null && quote.roomsAvailable === 0;
  const limited = quote !== null
    && quote.roomsAvailable <= Math.max(1, Math.ceil((hotel.totalRooms ?? 1) * 0.25));
  // Carry the searched dates over to the details page.
  const search = buildSearch({
    ...(quote ? { checkIn: quote.checkIn, checkOut: quote.checkOut } : {}),
    ...occupancy,
  });

  return (
    <article className="card hotel-card">
      {getHotelImage(hotel) && <img className="hotel-image" src={getHotelImage(hotel)} alt={`${hotel.name} exterior`} loading="lazy" decoding="async" />}
      <div>
        <h3>{hotel.name}</h3>
        <p className="muted">{hotel.location}</p>
        <p className="room-type">{hotel.roomType || 'Standard Room'} · Up to {maxOccupancy} guests</p>
        <p className="description">{hotel.description}</p>
        <ul className="chips">
          {hotel.amenities.map((amenity) => (
            <li key={amenity}>{amenity}</li>
          ))}
        </ul>
      </div>

      <div className="hotel-card-footer">
        <div>
          <p className="price">
            {formatMoney(hotel.pricePerNight)} <span className="muted">/ night</span>
          </p>
          {quote && (
            <p className="muted">
              {pluralize(quote.nights, 'night')}: <strong>{formatMoney(quote.totalPrice)}</strong>
            </p>
          )}
          {quote && soldOut && <span className="badge badge-danger">Sold out for these dates</span>}
          {quote && !soldOut && limited && (
            <span className="badge badge-ok">
              Only {quote.roomsAvailable} {quote.roomsAvailable === 1 ? 'room' : 'rooms'} left
            </span>
          )}
        </div>
        <Link className="button" to={`/hotels/${hotel.id}${search}`}>
          {soldOut ? 'See alternatives' : 'View & book'}
        </Link>
      </div>
    </article>
  );
}