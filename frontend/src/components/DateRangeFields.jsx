import { BOOKING_RULES } from '../config';
import { addDays, todayIso } from '../utils/dates';
import TextField from './TextField.jsx';

// Check-in / check-out inputs. The parent owns the values and shows any error message.
export default function DateRangeFields({ checkIn, checkOut, onChange, idPrefix }) {
  const today = todayIso();
  const lastCheckOut = addDays(today, BOOKING_RULES.windowDays);
  const longestStayEnd = checkIn ? addDays(checkIn, BOOKING_RULES.maxNights) : lastCheckOut;
  const maxCheckOut = longestStayEnd < lastCheckOut ? longestStayEnd : lastCheckOut;

  function handleCheckInChange(event) {
    const newCheckIn = event.target.value;
    // Keep the old check-out only while it is still after the new check-in.
    const keepCheckOut = checkOut && (!newCheckIn || checkOut > newCheckIn);
    onChange({ checkIn: newCheckIn, checkOut: keepCheckOut ? checkOut : '' });
  }

  function handleCheckOutChange(event) {
    onChange({ checkIn, checkOut: event.target.value });
  }

  return (
    <div className="date-range">
      <TextField
        label="Check-in"
        name={`${idPrefix}-check-in`}
        type="date"
        value={checkIn}
        min={today}
        max={addDays(lastCheckOut, -1)}
        onChange={handleCheckInChange}
      />
      <TextField
        label="Check-out"
        name={`${idPrefix}-check-out`}
        type="date"
        value={checkOut}
        min={addDays(checkIn || today, 1)}
        max={maxCheckOut}
        onChange={handleCheckOutChange}
      />
    </div>
  );
}