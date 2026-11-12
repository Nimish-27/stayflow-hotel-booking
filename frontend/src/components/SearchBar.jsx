import { useState } from 'react';
import { validateSearchDates } from '../utils/dates';
import DateRangeFields from './DateRangeFields.jsx';
import TextField from './TextField.jsx';

export default function SearchBar({ initialValues = {}, onSearch }) {
  const [location, setLocation] = useState(initialValues.location ?? '');
  const [dates, setDates] = useState({
    checkIn: initialValues.checkIn ?? '',
    checkOut: initialValues.checkOut ?? '',
  });
  const [dateError, setDateError] = useState('');

  function handleDatesChange(newDates) {
    setDates(newDates);
    setDateError('');
  }

  function handleSubmit(event) {
    event.preventDefault();
    const message = validateSearchDates(dates.checkIn, dates.checkOut);
    setDateError(message);
    if (!message) {
      onSearch({ location: location.trim(), ...dates });
    }
  }

  return (
    <form className="card" onSubmit={handleSubmit} noValidate>
      <div className="search-bar">
        <TextField
          label="Location"
          name="location"
          placeholder="e.g. Goa (leave empty for all)"
          value={location}
          onChange={(event) => setLocation(event.target.value)}
        />
        <DateRangeFields
          idPrefix="search"
          checkIn={dates.checkIn}
          checkOut={dates.checkOut}
          onChange={handleDatesChange}
        />
        <button type="submit" className="button">Search</button>
      </div>
      {dateError && <p className="field-error" role="alert">{dateError}</p>}
    </form>
  );
}