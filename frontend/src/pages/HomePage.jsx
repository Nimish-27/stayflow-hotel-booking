import { Link, useNavigate } from 'react-router-dom';
import SearchBar from '../components/SearchBar.jsx';
import { buildSearch } from '../utils/query';

export default function HomePage() {
  const navigate = useNavigate();

  function handleSearch(params) {
    navigate(`/hotels${buildSearch(params)}`);
  }

  return (
    <section className="home-page">
      <div className="hero">
        <div className="hero-copy">
          <p className="eyebrow">YOUR NEXT ESCAPE STARTS HERE</p>
          <h1>Make room for<br /><em>something beautiful.</em></h1>
          <p className="hero-description">
            Discover considered stays in the places you want to be.
            Simple searching, honest prices, no surprises.
          </p>
        </div>
        <div className="hero-orb hero-orb-one" aria-hidden="true" />
        <div className="hero-orb hero-orb-two" aria-hidden="true" />
      </div>
      <div className="search-panel">
        <div className="search-panel-heading">
          <span className="search-icon" aria-hidden="true">⌕</span>
          <div>
            <strong>Where will you go?</strong>
            <span className="muted small">Find a stay that feels like yours</span>
          </div>
        </div>
        <SearchBar onSearch={handleSearch} />
      </div>
      <div className="home-footer-row">
        <div className="trust-points">
          <span><b>✦</b> Curated stays</span>
          <span><b>✓</b> Transparent pricing</span>
          <span><b>♡</b> Easy booking</span>
        </div>
        <Link className="browse-link" to="/hotels">Browse all stays <span>→</span></Link>
      </div>
    </section>
  );
}