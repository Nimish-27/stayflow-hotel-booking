import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { createBooking } from '../api/bookings';
import { ApiError } from '../api/client';
import { getAlternatives, getAvailability, getHotel } from '../api/hotels';
import AlternativeDates from '../components/AlternativeDates.jsx';
import DateRangeFields from '../components/DateRangeFields.jsx';
import ErrorBanner from '../components/ErrorBanner.jsx';
import Loader from '../components/Loader.jsx';
import PriceSummary from '../components/PriceSummary.jsx';
import OccupancySelector from '../components/OccupancySelector.jsx';
import TextField from '../components/TextField.jsx';
import { addDays, todayIso, validateStay } from '../utils/dates';
import { formatMoney } from '../utils/format';
import { getHotelImage } from '../utils/hotelImages';
import { buildSearch } from '../utils/query';
import { validateGuest } from '../utils/validation';

const OCCUPANCY_CAPACITY = {
  Single: 1,
  Double: 2,
  Triple: 3,
  Quad: 4,
};

export default function HotelDetailsPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  // The chosen dates live in the URL so the page can be refreshed or shared.
  const checkIn = searchParams.get('checkIn') ?? '';
  const checkOut = searchParams.get('checkOut') ?? '';
  const adults = searchParams.get('adults') ?? '2';
  const children = searchParams.get('children') ?? '0';
  const rooms = searchParams.get('rooms') ?? '1';
  const selectedRoomType = searchParams.get('roomType') ?? '';

  const stayError = checkIn && checkOut ? validateStay(checkIn, checkOut) : '';
  const hasValidDates = Boolean(checkIn && checkOut && !stayError);

  const [hotel, setHotel] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [refreshKey, setRefreshKey] = useState(0);
  const [availability, setAvailability] = useState([]);
  const [availabilityLoading, setAvailabilityLoading] = useState(false);

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
  const currentAvailability = availability.find((day) => day.date === todayIso());
  const roomOptions = hotel?.roomOptions?.length
    ? hotel.roomOptions
    : [{ name: hotel?.roomType || 'Standard Room', pricePerNight: hotel?.pricePerNight ?? 0, totalRooms: hotel?.totalRooms ?? 1 }];
  const availableRoomTypes = roomOptions.map((option) => option.name);
  const roomType = availableRoomTypes.includes(selectedRoomType)
    ? selectedRoomType
    : availableRoomTypes[0];
  const selectedRoom = roomOptions.find((option) => option.name === roomType) ?? roomOptions[0];
  const maxOccupancy = OCCUPANCY_CAPACITY[selectedRoom.occupancyType] ?? hotel?.maxOccupancy ?? 2;
  const selectedRoomInventory = selectedRoom.totalRooms ?? hotel?.totalRooms ?? 1;
  const limited = quote !== null
    && quote.roomsAvailable <= Math.max(1, Math.ceil(selectedRoomInventory * 0.25));
  const availableRooms = Math.min(quote?.roomsAvailable ?? hotel?.totalRooms ?? 1, selectedRoomInventory);
  const roomCount = Number(rooms);
  const guestCount = Number(adults) + Number(children);
  const maxGuests = maxOccupancy * roomCount;
  const occupancyError = roomCount > availableRooms
    ? `Only ${availableRooms} room${availableRooms === 1 ? '' : 's'} are available for these dates.`
    : guestCount > maxGuests
      ? `${selectedRoom.name} sleeps up to ${maxGuests} guest${maxGuests === 1 ? '' : 's'} for ${roomCount} room${roomCount === 1 ? '' : 's'}. Reduce guests or add another room.`
      : '';
  const canBook = quote !== null && !soldOut && !occupancyError && !loading && !submitting;

  const soldOutCheckIn = soldOut ? quote.checkIn : '';
  const soldOutCheckOut = soldOut ? quote.checkOut : '';

  useEffect(() => {
    let ignore = false;
    const from = todayIso();
    setAvailabilityLoading(true);
    getAvailability(id, from, addDays(from, 365), roomType, roomCount)
      .then((data) => { if (!ignore) setAvailability(data); })
      .catch(() => { if (!ignore) setAvailability([]); })
      .finally(() => { if (!ignore) setAvailabilityLoading(false); });
    return () => { ignore = true; };
  }, [id, refreshKey, roomType, roomCount]);

  // Load the hotel plus the server quote for the selected dates.
  useEffect(() => {
    let ignore = false;
    setLoading(true);
    setLoadError('');
    getHotel(id, hasValidDates ? { checkIn, checkOut, roomType, rooms } : { roomType, rooms })
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
  }, [id, checkIn, checkOut, hasValidDates, refreshKey, roomType, rooms]);

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

  function handleRoomTypeChange(event) {
    const next = Object.fromEntries(searchParams.entries());
    if (event.target.value) {
      next.roomType = event.target.value;
    } else {
      delete next.roomType;
    }
    setSearchParams(next, { replace: true });
  }

  function handleOccupancyChange(nextOccupancy) {
    const next = Object.fromEntries(searchParams.entries());
    next.adults = String(nextOccupancy.adults);
    next.children = String(nextOccupancy.children);
    next.rooms = String(nextOccupancy.rooms);
    setSearchParams(next, { replace: true });
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
      const booking = await createBooking({
        ...guest, hotelId: Number(id), checkIn, checkOut, roomType,
        adults: Number(adults), children: Number(children), rooms: Number(rooms),
      });
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
      <Link to={`/hotels${buildSearch({
        ...(hasValidDates ? { checkIn, checkOut } : {}),
        adults,
        children,
        rooms,
      })}`}>← Back to results</Link>

      <div className="details-layout">
        <div>
          {getHotelImage(hotel) && <img className="details-image" src={getHotelImage(hotel)} alt={`${hotel.name} exterior`} decoding="async" />}
          <h1>{hotel.name}</h1>
          <p className="muted">{hotel.location}</p>
          <div className="room-summary">
            <label htmlFor="room-type">Room type</label>
            <select id="room-type" value={roomType} onChange={handleRoomTypeChange}>
              {roomOptions.map((option) => (
                <option key={option.name} value={option.name}>
                  {option.name} · {formatMoney(option.pricePerNight)} / night
                </option>
              ))}
            </select>
            <span>{selectedRoom.occupancyType} occupancy · {selectedRoom.bedType} bed · Sleeps up to {maxOccupancy} guests</span>
            <small>{selectedRoom.description} · {selectedRoomInventory} rooms in this category</small>
          </div>
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
          {quote && !soldOut && limited && (
            <p className="badge badge-ok" role="status">
              Only {quote.roomsAvailable} {quote.roomsAvailable === 1 ? 'room' : 'rooms'} left for these dates
            </p>
          )}
          {quote && soldOut && (
            <p className="badge badge-danger" role="status">Sold out for these dates</p>
          )}
        </div>

        <div className="booking-column">
          <form className="card" onSubmit={handleSubmit} noValidate>
            <h2>Book your stay</h2>
            <ErrorBanner message={loadError} />
            <OccupancySelector
              value={{ adults, children, rooms }}
              maxRooms={availableRooms}
              onChange={handleOccupancyChange}
            />
            {occupancyError && <p className="field-error" role="alert">{occupancyError}</p>}
            {!hasValidDates && currentAvailability?.level === 'limited' && (
              <p className="badge badge-ok">
                Only {currentAvailability.roomsAvailable} {currentAvailability.roomsAvailable === 1 ? 'room' : 'rooms'} left
              </p>
            )}

            <p className="selected-room-summary">Selected: <strong>{roomType}</strong></p>
            <DateRangeFields
              idPrefix="booking"
              hotelId={id}
              checkIn={checkIn}
              checkOut={checkOut}
              availability={availability}
              availabilityLoading={availabilityLoading}
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