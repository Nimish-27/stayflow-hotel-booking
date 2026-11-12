import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { createBooking } from '../api/bookings';
import { ApiError } from '../api/client';
import { getAlternatives, getHotel } from '../api/hotels';
import AlternativeDates from '../components/AlternativeDates.jsx';
import DateRangeFields from '../components/DateRangeFields.jsx';
import ErrorBanner from '../components/ErrorBanner.jsx';
import Loader from '../components/Loader.jsx';
import PriceSummary from '../components/PriceSummary.jsx';
import TextField from '../components/TextField.jsx';
import { validateStay } from '../utils/dates';
import { formatMoney } from '../utils/format';
import { buildSearch } from '../utils/query';
import { validateGuest } from '../utils/validation';

export default function HotelDetailsPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  // The chosen dates live in the URL so the page can be refreshed or shared.
  const checkIn = searchParams.get('checkIn') ?? '';
  const checkOut = searchParams.get('checkOut') ?? '';

  const stayError = checkIn && checkOut ? validateStay(checkIn, checkOut) : '';
  const hasValidDates = Boolean(checkIn && checkOut && !stayError);

  const [hotel, setHotel] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [refreshKey, setRefreshKey] = useState(0);

  const [alternatives, setAlternatives] = useState([]);
  const [alternativesLoading, setAlternativesLoading] = useState(false);
  const [alternativesError, setAlternativesError] = useState('');

  const [guest, setGuest] = useState({ guestName: '', guestEmail: '' });
  const [fieldErrors, setFieldErrors] = useState({});
  const [submitError, setSubmitError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  // Only trust a quote that belongs to the dates currently selected.
  const quote =
    hotel?.quote && hotel.quote.checkIn === checkIn && hotel.quote.checkOut === checkOut
      ? hotel.quote
      : null;
  const soldOut = quote !== null && quote.roomsAvailable === 0;
  const canBook = quote !== null && !soldOut && !loading && !submitting;

  const soldOutCheckIn = soldOut ? quote.checkIn : '';
  const soldOutCheckOut = soldOut ? quote.checkOut : '';

  // Load the hotel plus the server quote for the selected dates.
  useEffect(() => {
    let ignore = false;
    setLoading(true);
    setLoadError('');
    getHotel(id, hasValidDates ? { checkIn, checkOut } : {})
      .then((data) => {
        if (!ignore) setHotel(data);
      })
      .catch((err) => {
        if (!ignore) setLoadError(err.message);
      })
      .finally(() => {
        if (!ignore) setLoading(false);
      });
    return () => {
      ignore = true;
    };
  }, [id, checkIn, checkOut, hasValidDates, refreshKey]);

  // Smart alternative dates: only asked for when the selected stay is sold out.
  useEffect(() => {
    if (!soldOutCheckIn) {
      return undefined;
    }
    let ignore = false;
    setAlternativesLoading(true);
    setAlternativesError('');
    getAlternatives(id, soldOutCheckIn, soldOutCheckOut)
      .then((data) => {
        if (!ignore) setAlternatives(data);
      })
      .catch((err) => {
        if (!ignore) {
          setAlternatives([]);
          setAlternativesError(err.message);
        }
      })
      .finally(() => {
        if (!ignore) setAlternativesLoading(false);
      });
    return () => {
      ignore = true;
    };
  }, [id, soldOutCheckIn, soldOutCheckOut]);

  function handleDatesChange(dates) {
    const next = {};
    if (dates.checkIn) next.checkIn = dates.checkIn;
    if (dates.checkOut) next.checkOut = dates.checkOut;
    setSearchParams(next, { replace: true });
    setSubmitError('');
  }

  function handleGuestChange(event) {
    const { name, value } = event.target;
    setGuest((previous) => ({ ...previous, [name]: value }));
    setFieldErrors((previous) => ({ ...previous, [name]: undefined }));
  }

  function handleGuestBlur(event) {
    const { name } = event.target;
    setFieldErrors((previous) => ({ ...previous, [name]: validateGuest(guest)[name] }));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    const errors = validateGuest(guest);
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0 || !canBook) {
      return;
    }

    setSubmitting(true);
    setSubmitError('');
    try {
      const booking = await createBooking({ ...guest, hotelId: Number(id), checkIn, checkOut });
      navigate('/confirmation', { state: { booking } });
    } catch (err) {
      setSubmitError(err.message);
      if (err instanceof ApiError) {
        setFieldErrors(err.fieldMessages());
      }
      if (err.status === 409) {
        // Someone took the last room: reload the quote, which also loads alternatives.
        setRefreshKey((key) => key + 1);
      }
      setSubmitting(false);
    }
  }

  if (!hotel && loading) {
    return <Loader label="Loading hotel…" />;
  }
  if (!hotel) {
    return (
      <section>
        <ErrorBanner message={loadError} />
        <Link to="/hotels">← Back to hotels</Link>
      </section>
    );
  }

  return (
    <section>
      <Link to={`/hotels${hasValidDates ? buildSearch({ checkIn, checkOut }) : ''}`}>← Back to results</Link>

      <div className="details-layout">
        <div>
          <h1>{hotel.name}</h1>
          <p className="muted">{hotel.location}</p>
          <p>{hotel.description}</p>
          <h3>Amenities</h3>
          <ul className="chips">
            {hotel.amenities.map((amenity) => (
              <li key={amenity}>{amenity}</li>
            ))}
          </ul>
          <p className="price">
            {formatMoney(hotel.pricePerNight)} <span className="muted">/ night</span>
          </p>
        </div>

        <div className="booking-column">
          <form className="card" onSubmit={handleSubmit} noValidate>
            <h2>Book your stay</h2>
            <ErrorBanner message={loadError} />

            <DateRangeFields
              idPrefix="booking"
              checkIn={checkIn}
              checkOut={checkOut}
              onChange={handleDatesChange}
            />
            {stayError && <p className="field-error" role="alert">{stayError}</p>}

            <PriceSummary
              pricePerNight={hotel.pricePerNight}
              quote={quote}
              loading={loading}
              hasDates={hasValidDates}
            />

            <TextField
              label="Full name"
              name="guestName"
              autoComplete="name"
              value={guest.guestName}
              error={fieldErrors.guestName}
              onChange={handleGuestChange}
              onBlur={handleGuestBlur}
            />
            <TextField
              label="Email"
              name="guestEmail"
              type="email"
              autoComplete="email"
              value={guest.guestEmail}
              error={fieldErrors.guestEmail}
              onChange={handleGuestChange}
              onBlur={handleGuestBlur}
            />

            <ErrorBanner message={submitError} />
            <button type="submit" className="button" disabled={!canBook}>
              {submitting ? 'Booking…' : 'Confirm booking'}
            </button>
            <p className="muted small">
              The total shown comes from the server. You need your reference and this email to manage the booking.
            </p>
          </form>

          {soldOut && (
            <AlternativeDates
              alternatives={alternatives}
              loading={alternativesLoading}
              error={alternativesError}
              onSelect={(option) => handleDatesChange({ checkIn: option.checkIn, checkOut: option.checkOut })}
            />
          )}
        </div>
      </div>
    </section>
  );
}