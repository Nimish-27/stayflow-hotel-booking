import { formatDate, formatMoney, pluralize } from '../utils/format';
import ErrorBanner from './ErrorBanner.jsx';
import Loader from './Loader.jsx';

export default function AlternativeDates({ alternatives, loading, error, onSelect }) {
  return (
    <section className="card alternatives" aria-live="polite">
      <h3>Not available for these dates</h3>
      <p className="muted">Nearby stays of the same length that are free:</p>

      {loading && <Loader label="Looking for nearby dates…" />}
      <ErrorBanner message={error} />
      {!loading && !error && alternatives.length === 0 && (
        <p>No nearby dates are free. Try another period or another hotel.</p>
      )}

      {!loading && (
        <ul className="alternative-list">
          {alternatives.map((option) => (
            <li key={option.checkIn} className="alternative">
              <div>
                <strong>{formatDate(option.checkIn)} → {formatDate(option.checkOut)}</strong>
                <br />
                <span className="muted">
                  {pluralize(option.nights, 'night')} · {formatMoney(option.totalPrice)}
                </span>
              </div>
              <button type="button" className="button button-secondary" onClick={() => onSelect(option)}>
                Use these dates
              </button>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}