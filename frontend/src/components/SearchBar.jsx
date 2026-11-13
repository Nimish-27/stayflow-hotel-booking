import { useState } from 'react';
import { validateSearchDates } from '../utils/dates';
import DateRangeFields from './DateRangeFields.jsx';
import OccupancySelector from './OccupancySelector.jsx';
import TextField from './TextField.jsx';

const HOTEL_LOCATIONS = ['Goa', 'Jaipur', 'Mumbai', 'Bengaluru', 'Manali', 'Kochi'];
export default function SearchBar({ initialValues = {}, onSearch }) {
  const [location, setLocation] = useState(initialValues.location ?? '');
  const [dates, setDates] = useState({
    checkIn: initialValues.checkIn ?? '',
    checkOut: initialValues.checkOut ?? '',
  });
  const [occupancy, setOccupancy] = useState({
    adults: initialValues.adults ?? 2,
    children: initialValues.children ?? 0,
    rooms: initialValues.rooms ?? 1,
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
      onSearch({ location: location.trim(), ...dates, ...occupancy });
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
          list="hotel-locations"
        />
        <datalist id="hotel-locations">
          {HOTEL_LOCATIONS.map((item) => <option key={item} value={item} />)}
        </datalist>
        <DateRangeFields
          idPrefix="search"
          checkIn={dates.checkIn}
          checkOut={dates.checkOut}
          onChange={handleDatesChange}
        />
        <OccupancySelector value={occupancy} onChange={setOccupancy} />
        <button type="submit" className="button">Search</button>
      </div>
      {dateError && <p className="field-error" role="alert">{dateError}</p>}
    </form>
  );
}