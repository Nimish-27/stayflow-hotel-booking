import { formatMoney, pluralize } from '../utils/format';

// Shows the quote returned by the server. The browser never calculates a price itself.
export default function PriceSummary({ pricePerNight, quote, loading, hasDates }) {
  if (!hasDates) {
    return <div className="price-summary muted">Choose valid dates to see the total price.</div>;
  }
  if (loading) {
    return <div className="price-summary muted" role="status">Calculating price…</div>;
  }
  if (!quote) {
    return <div className="price-summary muted">Price unavailable right now.</div>;
  }

  const soldOut = quote.roomsAvailable === 0;
  return (
    <div className="price-summary">
      <dl>
        <div className="row">
          <dt>{formatMoney(pricePerNight)} × {pluralize(quote.nights, 'night')}</dt>
          <dd>{formatMoney(quote.totalPrice)}</dd>
        </div>
        <div className="row total">
          <dt>Total</dt>
          <dd>{formatMoney(quote.totalPrice)}</dd>
        </div>
      </dl>
      {soldOut ? (
        <span className="badge badge-danger">Sold out for at least one night of this stay</span>
      ) : (
        <span className="badge badge-ok">{pluralize(quote.roomsAvailable, 'room')} left</span>
      )}
    </div>
  );
}