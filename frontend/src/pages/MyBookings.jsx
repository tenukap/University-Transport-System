import { useEffect, useState } from 'react';
import { getBookingsByStudent, cancelBooking } from '../api/api';
import StatusBadge from '../components/StatusBadge';

const STUDENT_ID = 1; // hardcoded for now

const TABS = ['All', 'Pending', 'Confirmed', 'Cancelled'];

const BusIcon = ({ size = 20, color = '#FFFFFF' }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M8 6v6M16 6v6M2 12h19.6M18 18h3s.5-1.7.8-2.8c.1-.4.2-.8.2-1.2V6a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2v8c0 .4.1.8.2 1.2C2.5 16.3 3 18 3 18h3" />
    <circle cx="7" cy="18" r="2" />
    <path d="M9 18h5" />
    <circle cx="16" cy="18" r="2" />
  </svg>
);

const formatDate = (dt) => {
  if (!dt) return '—';
  const d = new Date(dt);
  if (isNaN(d)) return dt;
  return d.toLocaleString(undefined, {
    year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit',
  });
};

export default function MyBookings() {
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [activeTab, setActiveTab] = useState('All');
  const [selected, setSelected] = useState(null);
  const [cancelling, setCancelling] = useState(false);

  const loadBookings = () => {
    setLoading(true);
    getBookingsByStudent(STUDENT_ID)
      .then((data) => {
        setBookings(Array.isArray(data) ? data : []);
        setError('');
      })
      .catch((err) => {
        const msg = err?.response?.data || err?.message || 'Failed to load bookings';
        setError(typeof msg === 'string' ? msg : 'Failed to load bookings');
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadBookings();
  }, []);

  const filtered = bookings.filter((b) => {
    if (activeTab === 'All') return true;
    return (b.status || '').toUpperCase() === activeTab.toUpperCase();
  });

  const handleCancel = async () => {
    if (!selected) return;
    setCancelling(true);
    try {
      await cancelBooking(selected.id);
      setSelected(null);
      loadBookings();
    } catch (err) {
      const msg = err?.response?.data || err?.message || 'Cancel failed';
      alert(typeof msg === 'string' ? msg : 'Cancel failed');
    } finally {
      setCancelling(false);
    }
  };

  const canCancel = (status) => ['PENDING', 'CONFIRMED'].includes((status || '').toUpperCase());

  return (
    <div style={{ maxWidth: '800px', margin: '0 auto' }}>
      {/* Tabs */}
      <div className="flex gap-6" style={{ borderBottom: '1px solid #A6A9D0', marginBottom: '24px' }}>
        {TABS.map((tab) => {
          const active = activeTab === tab;
          return (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              style={{
                background: 'none',
                border: 'none',
                paddingBottom: '10px',
                fontSize: '14px',
                fontWeight: active ? 600 : 400,
                color: active ? '#35627A' : '#8E9A98',
                borderBottom: active ? '2px solid #35627A' : '2px solid transparent',
                marginBottom: '-1px',
                cursor: 'pointer',
              }}
            >
              {tab}
            </button>
          );
        })}
      </div>

      {loading && <p style={{ color: '#8E9A98' }}>Loading...</p>}
      {error && !loading && <p style={{ color: '#B46258' }}>{error}</p>}

      {!loading && !error && filtered.length === 0 && (
        <div className="flex flex-col items-center justify-center" style={{ padding: '64px 0', color: '#8E9A98' }}>
          <div style={{ opacity: 0.5 }}>
            <BusIcon size={48} color="#8E9A98" />
          </div>
          <p style={{ marginTop: '12px' }}>No bookings found</p>
        </div>
      )}

      {/* Booking cards */}
      <div className="space-y-4">
        {!loading && !error && filtered.map((b) => (
          <div key={b.id} className="bg-white shadow" style={{ borderRadius: '8px', padding: '16px' }}>
            <div className="flex items-start gap-4">
              <div
                className="flex items-center justify-center rounded-full shrink-0"
                style={{ width: '44px', height: '44px', backgroundColor: '#35627A' }}
              >
                <BusIcon />
              </div>

              <div className="flex-1">
                <div className="flex items-center justify-between">
                  <span className="font-heading font-bold" style={{ color: '#1E2C33', fontSize: '16px' }}>
                    Trip #{b.tripId}
                  </span>
                  <StatusBadge status={b.status} />
                </div>
                <div style={{ color: '#8E9A98', fontSize: '14px', marginTop: '4px' }}>
                  Pickup Location ID: {b.pickupLocId} → SLIIT
                </div>
                <div style={{ color: '#8E9A98', fontSize: '12px', marginTop: '2px' }}>
                  Date booked: {formatDate(b.createdAt)}
                </div>
                {b.seatNumber != null && (
                  <div style={{ color: '#1E2C33', fontSize: '13px', marginTop: '4px', fontWeight: 500 }}>
                    Seat #{b.seatNumber}
                  </div>
                )}

                <div className="flex justify-end" style={{ marginTop: '12px' }}>
                  <button
                    onClick={() => setSelected(b)}
                    style={{
                      border: '1px solid #35627A',
                      color: '#35627A',
                      borderRadius: '6px',
                      padding: '6px 14px',
                      fontSize: '13px',
                      fontWeight: 500,
                      background: 'none',
                      cursor: 'pointer',
                    }}
                  >
                    View Details
                  </button>
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Detail Modal */}
      {selected && (
        <div
          className="fixed inset-0 flex items-center justify-center"
          style={{ backgroundColor: 'rgba(0,0,0,0.5)', zIndex: 50, padding: '16px' }}
          onClick={() => setSelected(null)}
        >
          <div
            className="bg-white shadow-lg w-full"
            style={{ maxWidth: '500px', borderRadius: '8px', padding: '24px', position: 'relative' }}
            onClick={(e) => e.stopPropagation()}
          >
            <button
              onClick={() => setSelected(null)}
              aria-label="Close"
              style={{ position: 'absolute', top: '16px', right: '16px', background: 'none', border: 'none', cursor: 'pointer', color: '#8E9A98', fontSize: '20px', lineHeight: 1 }}
            >
              ✕
            </button>

            <h2 className="font-heading font-bold" style={{ fontSize: '20px', color: '#1E2C33', margin: 0 }}>
              Booking #{selected.id}
            </h2>

            <div style={{ marginTop: '16px' }} className="space-y-3">
              <Row label="Trip ID" value={`#${selected.tripId}`} />
              <div className="flex items-center justify-between">
                <span style={{ color: '#8E9A98', fontSize: '13px' }}>Status</span>
                <StatusBadge status={selected.status} />
              </div>
              <Row label="Pickup Location ID" value={selected.pickupLocId} />
              <Row label="Dropoff Location ID" value={selected.dropoffLocId} />
              <Row label="Date Booked" value={formatDate(selected.createdAt)} />
              <Row label="Seat Number" value={selected.seatNumber != null ? `#${selected.seatNumber}` : 'Not assigned'} />
            </div>

            {canCancel(selected.status) && (
              <button
                onClick={handleCancel}
                disabled={cancelling}
                className="w-full font-heading"
                style={{
                  marginTop: '24px',
                  backgroundColor: '#B46258',
                  color: '#FFFFFF',
                  fontWeight: 600,
                  padding: '10px',
                  borderRadius: '6px',
                  border: 'none',
                  cursor: cancelling ? 'not-allowed' : 'pointer',
                  opacity: cancelling ? 0.7 : 1,
                }}
              >
                {cancelling ? 'Cancelling...' : 'Cancel Booking'}
              </button>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

function Row({ label, value }) {
  return (
    <div className="flex items-center justify-between">
      <span style={{ color: '#8E9A98', fontSize: '13px' }}>{label}</span>
      <span style={{ color: '#1E2C33', fontSize: '14px', fontWeight: 500 }}>{value}</span>
    </div>
  );
}
