import { Link, NavLink, Route, Routes } from 'react-router-dom';
import ConfirmationPage from './pages/ConfirmationPage.jsx';
import HomePage from './pages/HomePage.jsx';
import HotelDetailsPage from './pages/HotelDetailsPage.jsx';
import ManageBookingPage from './pages/ManageBookingPage.jsx';
import NotFoundPage from './pages/NotFoundPage.jsx';
import ResultsPage from './pages/ResultsPage.jsx';

export default function App() {
  return (
    <>
      <header className="site-header">
        <div className="container header-inner">
          <Link to="/" className="brand">
            <span className="brand-mark" aria-hidden="true">✦</span>
            <span>stay<span className="brand-accent">well</span></span>
          </Link>
          <nav>
            <NavLink to="/hotels">Hotels</NavLink>
            <NavLink to="/manage" className="nav-cta">Manage booking</NavLink>
          </nav>
        </div>
      </header>

      <main className="container">
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/hotels" element={<ResultsPage />} />
          <Route path="/hotels/:id" element={<HotelDetailsPage />} />
          <Route path="/confirmation" element={<ConfirmationPage />} />
          <Route path="/manage" element={<ManageBookingPage />} />
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </main>

      <footer className="site-footer">
        <div className="container footer-inner">
          <span className="footer-brand">staywell</span>
          <span className="muted">Thoughtful stays, made simple.</span>
        </div>
      </footer>
    </>
  );
}