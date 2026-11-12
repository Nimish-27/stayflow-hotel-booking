import { useState } from 'react';
import { useLocation } from 'react-router-dom';
import { cancelBooking, getBooking } from '../api/bookings';
import BookingDetails from '../components/BookingDetails.jsx';
import ErrorBanner from '../components/ErrorBanner.jsx';
import TextField from '../components/TextField.jsx';
import { validateLookup } from '../utils/validation';

export default function ManageBookingPage() {
  const prefill = useLocation().state; // set by the confirmation page, otherwise empty
  const [form, setForm] = useState({
    reference: prefill?.reference ?? '',
    email: prefill?.email ?? '',
  });
  const [fieldErrors, setFieldErrors] = useState({});
  const [booking, setBooking] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  function handleChange(event) {
    const { name, value } = event.target;
    setForm((previous) => ({ ...previous, [name]: value }));
    setFieldErrors((previous) => ({ ...previous, [name]: undefined }));
    // Editing the form invalidates whatever was shown for the old reference/email.
    setBooking(null);
    setNotice('');
    setError('');
  }

  async function handleFind(event) {
    event.preventDefault();
    const errors = validateLookup(form);
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) {
      return;
    }

    setBusy(true);
    setError('');
    setNotice('');
    setBooking(null);
    try {
      setBooking(await getBooking(form.reference, form.email));
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  async function handleCancel() {
    if (!window.confirm(`Cancel booking ${booking.reference}? This cannot be undone.`)) {
      return;
    }
    setBusy(true);
    setError('');
    try {
      // Use the values the server returned, not the (possibly edited) form fields.
      setBooking(await cancelBooking(booking.reference, booking.guestEmail));
      setNotice('Your booking has been cancelled.');
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="narrow">
      <h1>Manage your booking</h1>
      <form className="card" onSubmit={handleFind} noValidate>
        <TextField
          label="Booking reference"
          name="reference"
          placeholder="HB-XXXXXXXX"
          value={form.reference}
          error={fieldErrors.reference}
          onChange={handleChange}
        />
        <TextField
          label="Email used for the booking"
          name="email"
          type="email"
          autoComplete="email"
          value={form.email}
          error={fieldErrors.email}
          onChange={handleChange}
        />
        <button type="submit" className="button" disabled={busy}>
          {busy ? 'Please wait…' : 'Find booking'}
        </button>
      </form>

      <ErrorBanner message={error} />
      {notice && <div className="banner banner-success" role="status">{notice}</div>}

      {booking && (
        <>
          <BookingDetails booking={booking} />
          {booking.status === 'BOOKED' && (
            <div className="actions">
              <button type="button" className="button button-danger" disabled={busy} onClick={handleCancel}>
                Cancel booking
              </button>
            </div>
          )}
        </>
      )}
    </section>
  );
}