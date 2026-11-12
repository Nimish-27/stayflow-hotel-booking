import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { searchHotels } from '../api/hotels';
import ErrorBanner from '../components/ErrorBanner.jsx';
import HotelCard from '../components/HotelCard.jsx';
import Loader from '../components/Loader.jsx';
import SearchBar from '../components/SearchBar.jsx';
import { formatDate } from '../utils/format';
import { buildSearch } from '../utils/query';

export default function ResultsPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  // The URL is the single source of truth for the search, so refresh and back-button work.
  const location = searchParams.get('location') ?? '';
  const checkIn = searchParams.get('checkIn') ?? '';
  const checkOut = searchParams.get('checkOut') ?? '';

  const [hotels, setHotels] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let ignore = false; // drops the answer of an outdated request
    setLoading(true);
    setError('');
    searchHotels({ location, checkIn, checkOut })
      .then((data) => {
        if (!ignore) setHotels(data);
      })
      .catch((err) => {
        if (!ignore) {
          setHotels([]);
          setError(err.message);
        }
      })
      .finally(() => {
        if (!ignore) setLoading(false);
      });
    return () => {
      ignore = true;
    };
  }, [location, checkIn, checkOut]);

  function handleSearch(params) {
    navigate(`/hotels${buildSearch(params)}`);
  }

  return (
    <section>
      <SearchBar
        key={searchParams.toString()}
        initialValues={{ location, checkIn, checkOut }}
        onSearch={handleSearch}
      />

      <h1 className="page-title">Hotels{location && ` matching “${location}”`}</h1>
      <p className="muted">
        {checkIn && checkOut
          ? `Prices for ${formatDate(checkIn)} → ${formatDate(checkOut)}`
          : 'Pick dates to see totals and availability.'}
      </p>

      <ErrorBanner message={error} />
      {loading && <Loader label="Searching hotels…" />}
      {!loading && !error && hotels.length === 0 && (
        <p>No hotels match your search. Try another location or clear the filters.</p>
      )}
      <div className="hotel-grid">
        {!loading && hotels.map((hotel) => <HotelCard key={hotel.id} hotel={hotel} />)}
      </div>
    </section>
  );
}