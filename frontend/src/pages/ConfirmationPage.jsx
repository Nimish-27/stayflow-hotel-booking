import { Link, useLocation } from 'react-router-dom';
import BookingDetails from '../components/BookingDetails.jsx';

export default function ConfirmationPage() {
  const booking = useLocation().state?.booking;

  if (!booking) {
    return (
      <section>
        <h1>No booking to show</h1>
        <p>Use your booking reference and email to look up your reservation.</p>
        <Link className="button" to="/manage">Manage a booking</Link>
      </section>
    );
  }

  return (
    <section className="narrow">
      <h1>Booking confirmed</h1>
      <p>
        Keep your reference and the email you used. You need both to view or cancel the booking.
        Free cancellation is available until 48 hours before check-in.
      </p>
      <BookingDetails booking={booking} />
      <div className="actions">
        <Link
          className="button"
          to="/manage"
          state={{ reference: booking.reference, email: booking.guestEmail }}
        >
          Manage this booking
        </Link>
        <Link className="button button-secondary" to="/hotels">Book another stay</Link>
      </div>
    </section>
  );
}