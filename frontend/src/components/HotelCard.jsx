import { Link } from 'react-router-dom';
import { formatMoney, pluralize } from '../utils/format';
import { buildSearch } from '../utils/query';

export default function HotelCard({ hotel }) {
  const { quote } = hotel;
  const soldOut = quote !== null && quote.roomsAvailable === 0;
  // Carry the searched dates over to the details page.
  const search = quote ? buildSearch({ checkIn: quote.checkIn, checkOut: quote.checkOut }) : '';

  return (
    <article className="card hotel-card">
      <div>
        <h3>{hotel.name}</h3>
        <p className="muted">{hotel.location}</p>
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
          {quote && !soldOut && (
            <span className="badge badge-ok">{pluralize(quote.roomsAvailable, 'room')} left</span>
          )}
        </div>
        <Link className="button" to={`/hotels/${hotel.id}${search}`}>
          {soldOut ? 'See alternatives' : 'View & book'}
        </Link>
      </div>
    </article>
  );
}