import { useState } from 'react';
import { BOOKING_RULES } from '../config';
import { addDays, todayIso } from '../utils/dates';
import TextField from './TextField.jsx';
import AvailabilityCalendar from './AvailabilityCalendar.jsx';

// Check-in / check-out inputs. The parent owns the values and shows any error message.
export default function DateRangeFields({ checkIn, checkOut, onChange, idPrefix, hotelId, availability = [], availabilityLoading = false }) {
  const [activeCalendar, setActiveCalendar] = useState('checkIn');
  const today = todayIso();
  const lastCheckOut = addDays(today, BOOKING_RULES.windowDays);
  const longestStayEnd = checkIn ? addDays(checkIn, BOOKING_RULES.maxNights) : lastCheckOut;
  const maxCheckOut = longestStayEnd < lastCheckOut ? longestStayEnd : lastCheckOut;

  function handleCheckInChange(event) {
    const newCheckIn = event.target.value;
    // Keep the old check-out only while it is still after the new check-in.
    const keepCheckOut = checkOut && (!newCheckIn || checkOut > newCheckIn);
    onChange({ checkIn: newCheckIn, checkOut: keepCheckOut ? checkOut : '' });
    setActiveCalendar('checkOut');
  }

  function handleCheckOutChange(event) {
    onChange({ checkIn, checkOut: event.target.value });
    setActiveCalendar(null);
  }

  return (
    <div className="date-range">
      {hotelId && idPrefix === 'booking' ? (
        <>
          <AvailabilityCalendar
            value={checkIn}
            availability={availability}
            loading={availabilityLoading}
            min={today}
            max={lastCheckOut}
            label="Check-in"
            id="booking-check-in"
            placeholder="Select a check-in date"
            open={activeCalendar === 'checkIn'}
            onToggle={() => setActiveCalendar(activeCalendar === 'checkIn' ? null : 'checkIn')}
            onChange={(newCheckIn) => handleCheckInChange({ target: { value: newCheckIn } })}
          />
          <AvailabilityCalendar
            value={checkOut}
            availability={availability}
            loading={availabilityLoading}
            min={addDays(checkIn || today, 1)}
            max={maxCheckOut}
            label="Check-out"
            id="booking-check-out"
            placeholder="Select a check-out date"
            open={activeCalendar === 'checkOut'}
            onToggle={() => setActiveCalendar(activeCalendar === 'checkOut' ? null : 'checkOut')}
            onChange={(newCheckOut) => handleCheckOutChange({ target: { value: newCheckOut } })}
          />
        </>
      ) : (
        <TextField
          label="Check-in"
          name={`${idPrefix}-check-in`}
          type="date"
          value={checkIn}
          min={today}
          max={addDays(lastCheckOut, -1)}
          onChange={handleCheckInChange}
        />
      )}
      {!(hotelId && idPrefix === 'booking') && (
        <TextField
          label="Check-out"
          name={`${idPrefix}-check-out`}
          type="date"
          value={checkOut}
          min={addDays(checkIn || today, 1)}
          max={maxCheckOut}
          onChange={handleCheckOutChange}
        />
      )}
    </div>
  );
}