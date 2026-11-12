import { formatDate, formatDateTime, formatMoney, pluralize } from '../utils/format';
import { isFreeCancellationClosed } from '../utils/dates';

export default function BookingDetails({ booking }) {
  const cancelled = booking.status === 'CANCELLED';

  return (
    <div className="card">
      <div className="booking-header">
        <p className="reference">{booking.reference}</p>
        <span className={`badge ${cancelled ? 'badge-danger' : 'badge-ok'}`}>
          {cancelled ? 'Cancelled' : 'Confirmed'}
        </span>
      </div>

      <dl className="details-list">
        <dt>Hotel</dt>
        <dd>{booking.hotelName}</dd>
        <dt>Guest</dt>
        <dd>{booking.guestName} ({booking.guestEmail})</dd>
        <dt>Check-in</dt>
        <dd>{formatDate(booking.checkIn)}</dd>
        <dt>Check-out</dt>
        <dd>{formatDate(booking.checkOut)}</dd>
        <dt>Stay</dt>
        <dd>{pluralize(booking.nights, 'night')}</dd>
        <dt>Total</dt>
        <dd><strong>{formatMoney(booking.totalAmount)}</strong></dd>
        <dt>Tax</dt>
        <dd>Includes 18% GST</dd>
        <dt>Booked on</dt>
        <dd>{formatDateTime(booking.createdAt)}</dd>
        {booking.cancelledAt && (
          <>
            <dt>Cancelled on</dt>
            <dd>{formatDateTime(booking.cancelledAt)}</dd>
          </>
        )}
      </dl>
      {!cancelled && isFreeCancellationClosed(booking.checkIn) && (
        <div className="banner banner-error" role="note">
          Free cancellation is no longer available because check-in is within 48 hours.
        </div>
      )}
    </div>
  );
}