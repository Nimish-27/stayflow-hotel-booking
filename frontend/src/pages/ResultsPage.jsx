import { useEffect, useMemo, useState } from 'react';
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
  const adults = searchParams.get('adults') ?? '2';
  const children = searchParams.get('children') ?? '0';
  const rooms = searchParams.get('rooms') ?? '1';

  const [hotels, setHotels] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [sort, setSort] = useState('recommended');
  const [priceLimit, setPriceLimit] = useState('');
  const [amenity, setAmenity] = useState('');
  const [locationFilter, setLocationFilter] = useState('');
  const [availability, setAvailability] = useState('all');

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

  const locations = useMemo(
    () => [...new Set(hotels.map((hotel) => hotel.location))].sort(),
    [hotels],
  );
  const amenities = useMemo(
    () => [...new Set(hotels.flatMap((hotel) => hotel.amenities))].sort(),
    [hotels],
  );
  const filteredHotels = useMemo(() => {
    const result = hotels.filter((hotel) => {
      const soldOut = hotel.quote && hotel.quote.roomsAvailable === 0;
      const matchesPrice = !priceLimit || hotel.pricePerNight <= Number(priceLimit);
      const matchesAmenity = !amenity || hotel.amenities.includes(amenity);
      const matchesLocation = !locationFilter || hotel.location === locationFilter;
      const matchesAvailability = availability === 'all'
        || (availability === 'available' && !soldOut)
        || (availability === 'sold-out' && soldOut);
      return matchesPrice && matchesAmenity && matchesLocation && matchesAvailability;
    });

    if (sort === 'recommended') {
      return result;
    }

    const priceForStay = (hotel) => hotel.quote?.totalPrice ?? hotel.pricePerNight;
    return result.sort((first, second) => {
      const difference = priceForStay(first) - priceForStay(second);
      if (difference !== 0) {
        return sort === 'price-low' ? difference : -difference;
      }
      return first.name.localeCompare(second.name);
    });
  }, [amenity, availability, hotels, locationFilter, priceLimit, sort]);

  function clearFilters() {
    setSort('recommended');
    setPriceLimit('');
    setAmenity('');
    setLocationFilter('');
    setAvailability('all');
  }

  function handleSearch(params) {
    navigate(`/hotels${buildSearch(params)}`);
  }

  return (
    <section>
      <SearchBar
        key={searchParams.toString()}
        initialValues={{ location, checkIn, checkOut, adults, children, rooms }}
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
      {!loading && !error && hotels.length > 0 && (
        <div className="results-toolbar">
          <div>
            <strong>{filteredHotels.length} {filteredHotels.length === 1 ? 'stay' : 'stays'}</strong>
            <span className="muted small"> available for your search</span>
          </div>
          <div className="filter-actions">
            <label className="select-field">
              <span>Sort by</span>
              <select value={sort} onChange={(event) => setSort(event.target.value)}>
                <option value="recommended">Recommended</option>
                <option value="price-low">Price: low to high</option>
                <option value="price-high">Price: high to low</option>
              </select>
            </label>
            <button type="button" className="clear-filters" onClick={clearFilters}>
              Clear filters
            </button>
          </div>
        </div>
      )}
      {!loading && !error && hotels.length > 0 && (
        <div className="filter-panel card">
          <label className="select-field">
            <span>Location</span>
            <select value={locationFilter} onChange={(event) => setLocationFilter(event.target.value)}>
              <option value="">All locations</option>
              {locations.map((item) => <option key={item} value={item}>{item}</option>)}
            </select>
          </label>
          <label className="select-field">
            <span>Max price / night</span>
            <select value={priceLimit} onChange={(event) => setPriceLimit(event.target.value)}>
              <option value="">Any price</option>
              <option value="4000">Up to ₹4,000</option>
              <option value="6000">Up to ₹6,000</option>
              <option value="8000">Up to ₹8,000</option>
              <option value="10000">Up to ₹10,000</option>
            </select>
          </label>
          <label className="select-field">
            <span>Amenity</span>
            <select value={amenity} onChange={(event) => setAmenity(event.target.value)}>
              <option value="">Any amenity</option>
              {amenities.map((item) => <option key={item} value={item}>{item}</option>)}
            </select>
          </label>
          <label className="select-field">
            <span>Availability</span>
            <select value={availability} onChange={(event) => setAvailability(event.target.value)}>
              <option value="all">Show all</option>
              <option value="available">Available only</option>
              <option value="sold-out">Sold out only</option>
            </select>
          </label>
        </div>
      )}
      {!loading && !error && hotels.length === 0 && (
        <p>No hotels match your search. Try another location or clear the search.</p>
      )}
      {!loading && !error && hotels.length > 0 && filteredHotels.length === 0 && (
        <p className="empty-state">No stays match these filters. Try clearing one or more filters.</p>
      )}
      <div className="hotel-grid">
        {!loading && !error && filteredHotels.map((hotel) => (
          <HotelCard
            key={hotel.id}
            hotel={hotel}
            occupancy={{ adults, children, rooms }}
          />
        ))}
      </div>
    </section>
  );
}