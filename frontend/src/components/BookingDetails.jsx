import { formatDate, formatDateTime, formatMoney, pluralize } from '../utils/format';

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
        <dt>Room</dt>
        <dd>{booking.roomType || 'Standard Room'} · {booking.rooms ?? 1} room{(booking.rooms ?? 1) === 1 ? '' : 's'} · {booking.adults ?? 2} adults · {booking.children ?? 0} children</dd>
        <dt>Subtotal</dt>
        <dd>{formatMoney(booking.subtotalAmount ?? booking.totalAmount)}</dd>
        <dt>GST ({Math.round((booking.gstRate ?? 0.18) * 100)}%)</dt>
        <dd>{formatMoney(booking.gstAmount ?? 0)}</dd>
        <dt>Total</dt>
        <dd><strong>{formatMoney(booking.totalAmount)}</strong></dd>
        <dt>Booked on</dt>
        <dd>{formatDateTime(booking.createdAt)}</dd>
        {booking.cancelledAt && (
          <>
            <dt>Cancelled on</dt>
            <dd>{formatDateTime(booking.cancelledAt)}</dd>
          </>
        )}
      </dl>
      {!cancelled && (
        <div className={`banner ${booking.cancellable ? 'banner-success' : 'banner-error'}`} role="note">
          {booking.cancellable
            ? `Free cancellation available until ${formatDateTime(booking.cancellableUntil)}.`
            : 'Free cancellation is no longer available for this booking.'}
        </div>
      )}
    </div>
  );
}