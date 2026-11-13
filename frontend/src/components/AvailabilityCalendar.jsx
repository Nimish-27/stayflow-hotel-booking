import { useMemo, useState } from 'react';

const MONTHS = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December',
];

function parseDate(iso) {
  const [year, month, day] = iso.split('-').map(Number);
  return new Date(year, month - 1, day);
}

function toIso(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

export default function AvailabilityCalendar({
  value, availability, loading, min, max, onChange, label, id, placeholder, open, onToggle,
  disableSoldOut = true,
}) {
  const initial = value ? parseDate(value) : parseDate(min);
  const [month, setMonth] = useState(new Date(initial.getFullYear(), initial.getMonth(), 1));
  const byDate = useMemo(
    () => new Map(availability.map((day) => [day.date, day])),
    [availability],
  );
  const firstDay = new Date(month.getFullYear(), month.getMonth(), 1).getDay();
  const daysInMonth = new Date(month.getFullYear(), month.getMonth() + 1, 0).getDate();
  const cells = [...Array(firstDay).fill(null), ...Array.from({ length: daysInMonth }, (_, i) => i + 1)];
  const canGoPrevious = month > new Date(parseDate(min).getFullYear(), parseDate(min).getMonth(), 1);
  const canGoNext = new Date(month.getFullYear(), month.getMonth() + 1, 1)
    <= new Date(parseDate(max).getFullYear(), parseDate(max).getMonth(), 1);

  return (
    <div className="calendar-field">
      <label htmlFor={id}>{label}</label>
      <button type="button" className="calendar-input" aria-expanded={open} onClick={onToggle}>
        {value || placeholder}
      </button>
      <div className={`availability-calendar ${open ? 'calendar-open' : ''}`}>
        <div className="calendar-header">
          <button type="button" disabled={!canGoPrevious} onClick={() => setMonth(new Date(month.getFullYear(), month.getMonth() - 1, 1))}>‹</button>
          <strong>{MONTHS[month.getMonth()]} {month.getFullYear()}</strong>
          <button type="button" disabled={!canGoNext} onClick={() => setMonth(new Date(month.getFullYear(), month.getMonth() + 1, 1))}>›</button>
        </div>
        <div className="calendar-weekdays">{['Su', 'Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa'].map((day) => <span key={day}>{day}</span>)}</div>
        <div className="calendar-grid">
          {cells.map((day, index) => {
            if (!day) return <span key={`empty-${index}`} />;
            const date = toIso(new Date(month.getFullYear(), month.getMonth(), day));
            const item = byDate.get(date);
            const disabled = date < min || date > max || (disableSoldOut && item?.level === 'sold-out');
            return (
              <button
                type="button"
                key={date}
                className={`calendar-day ${item?.level ?? ''} ${date === value ? 'selected' : ''}`}
                disabled={disabled}
                title={item ? `${item.roomsAvailable} rooms available` : 'Availability unavailable'}
                onClick={() => onChange(date)}
              >
                {day}
              </button>
            );
          })}
        </div>
        <div className="calendar-legend">
          <span><i className="dot available" /> Available</span>
          <span><i className="dot limited" /> Limited</span>
          <span><i className="dot sold-out" /> Sold out</span>
        </div>
        {loading && <small className="muted">Loading availability…</small>}
      </div>
      <input id={id} name={id} type="hidden" value={value} readOnly />
    </div>
  );
}
