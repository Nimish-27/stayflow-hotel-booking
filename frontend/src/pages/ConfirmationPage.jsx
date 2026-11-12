import { Link, useLocation } from 'react-router-dom';
import BookingDetails from '../components/BookingDetails.jsx';

export default function ConfirmationPage() {
  const booking = useLocation().state?.booking;

  if (!booking) {
    return (
      <section>
        <h1>No booking to show</h1>
        <p>This page only appears right after booking. To look up a booking, use your reference and email.</p>
        <Link className="button" to="/manage">Manage a booking</Link>
      </section>
    );
  }

  return (
    <section className="narrow">
      <h1>Booking confirmed</h1>
      <p>
        Keep your reference and the email you used. You need both to view or cancel the booking.
        No confirmation email is sent by this project.
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