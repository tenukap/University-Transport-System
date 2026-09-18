import { useLocation } from 'react-router-dom';

const TITLES = {
  '/book': 'Book a Trip',
  '/map': 'Live Trip Map',
  '/bookings': 'My Bookings',
  '/profile': 'My Profile',
};

export default function TopBar() {
  const { pathname } = useLocation();
  const title = TITLES[pathname] || 'UTMS';

  return (
    <header
      className="fixed top-0 right-0 flex items-center justify-between bg-white px-6"
      style={{ height: '64px', left: '240px', borderBottom: '1px solid #A6A9D0', zIndex: 10 }}
    >
      <h1 className="font-heading" style={{ color: '#35627A', fontSize: '16px', fontWeight: 600, margin: 0 }}>
        {title}
      </h1>

      <div className="flex items-center gap-4">
        {/* Notification bell */}
        <button className="relative text-text-muted hover:text-primary transition-colors" aria-label="Notifications">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9" />
            <path d="M10.3 21a1.94 1.94 0 0 0 3.4 0" />
          </svg>
        </button>

        {/* Avatar */}
        <div
          className="flex items-center justify-center rounded-full font-heading font-semibold"
          style={{ width: '36px', height: '36px', backgroundColor: '#35627A', color: '#FFFFFF', fontSize: '13px' }}
        >
          JS
        </div>
      </div>
    </header>
  );
}
