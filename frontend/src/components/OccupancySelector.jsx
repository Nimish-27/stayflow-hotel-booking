import { useState } from 'react';

const DEFAULT_OCCUPANCY = { adults: 2, children: 0, rooms: 1 };

function getInitialValue(value, fallback) {
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed >= 0 ? parsed : fallback;
}

export default function OccupancySelector({ value = {}, onChange, maxRooms = Number.POSITIVE_INFINITY }) {
  const [open, setOpen] = useState(false);
  const occupancy = {
    adults: getInitialValue(value.adults, DEFAULT_OCCUPANCY.adults),
    children: getInitialValue(value.children, DEFAULT_OCCUPANCY.children),
    rooms: Math.min(maxRooms, Math.max(1, getInitialValue(value.rooms, DEFAULT_OCCUPANCY.rooms))),
  };

  function update(field, amount) {
    const minimum = field === 'adults' ? 1 : 0;
    const upperBound = field === 'rooms' ? maxRooms : Number.POSITIVE_INFINITY;
    const nextValue = Math.min(upperBound, Math.max(minimum, occupancy[field] + amount));
    onChange({ ...occupancy, [field]: nextValue });
  }

  return (
    <div className="occupancy-field">
      <button
        type="button"
        className="occupancy-input"
        aria-expanded={open}
        aria-haspopup="dialog"
        onClick={() => setOpen((isOpen) => !isOpen)}
      >
        <span className="occupancy-icon" aria-hidden="true">♙</span>
        <span>
          <strong>Select occupancy</strong>
          <small>{occupancy.adults} adults · {occupancy.children} children · {occupancy.rooms} {occupancy.rooms === 1 ? 'room' : 'rooms'}</small>
        </span>
        <span className="occupancy-chevron" aria-hidden="true">⌄</span>
      </button>
      {open && (
        <div className="occupancy-menu" role="dialog" aria-label="Select occupancy">
          {[
            ['adults', 'Adults', 1],
            ['children', 'Children', 0],
            ['rooms', 'Rooms', 1],
          ].map(([field, label, minimum]) => (
            <div className="occupancy-row" key={field}>
              <span>{label}</span>
              <div className="occupancy-controls">
                <button
                  type="button"
                  aria-label={`Decrease ${label.toLowerCase()}`}
                  disabled={occupancy[field] <= minimum}
                  onClick={() => update(field, -1)}
                >
                  −
                </button>
                <output>{occupancy[field]}</output>
                <button
                  type="button"
                  aria-label={`Increase ${label.toLowerCase()}`}
                  disabled={field === 'rooms' && occupancy[field] >= maxRooms}
                  onClick={() => update(field, 1)}
                >
                  +
                </button>
              </div>
            </div>
          ))}
          <button type="button" className="occupancy-done" onClick={() => setOpen(false)}>Done</button>
        </div>
      )}
    </div>
  );
}
