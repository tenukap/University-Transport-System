import { useEffect, useState } from 'react';
import { getTrips, createBooking } from '../api/api';

const STUDENT_ID = 1; // hardcoded for now

const ClockIcon = () => (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <circle cx="12" cy="12" r="10" />
    <polyline points="12 6 12 12 16 14" />
  </svg>
);

const CalendarIcon = () => (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <rect x="3" y="4" width="18" height="18" rx="2" />
    <line x1="16" y1="2" x2="16" y2="6" />
    <line x1="8" y1="2" x2="8" y2="6" />
    <line x1="3" y1="10" x2="21" y2="10" />
  </svg>
);

const formatTime = (t) => {
  if (!t) return '';
  const [h, m] = t.split(':').map(Number);
  const period = h >= 12 ? 'PM' : 'AM';
  const hour12 = h % 12 === 0 ? 12 : h % 12;
  return `${String(hour12).padStart(2, '0')}:${String(m).padStart(2, '0')} ${period}`;
};

const today = () => new Date().toISOString().slice(0, 10);

export default function BookTrip() {
  const [trips, setTrips] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [selectedTripId, setSelectedTripId] = useState('');
  const [travelDate, setTravelDate] = useState(today());
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    getTrips()
      .then((res) => {
        const data = res.data;
        setTrips(data);
        if (data.length) setSelectedTripId(String(data[0].tripId));
      })
      .catch(() => setError('Failed to load trips'))
      .finally(() => setLoading(false));
  }, []);

  const selectedTrip = trips.find((t) => String(t.tripId) === String(selectedTripId));

  const handleBook = async () => {
    if (!selectedTrip) {
      alert('Please select a trip route.');
      return;
    }
    setSubmitting(true);
    try {
      await createBooking({
        studentId: STUDENT_ID,
        tripId: selectedTrip.tripId,
        pickupLocId: selectedTrip.pickupLocationId,
        dropoffLocId: selectedTrip.dropLocationId,
      });
      alert('Booking confirmed! Seat assigned.');
    } catch (err) {
      const msg = err?.response?.data || err?.message || 'Booking failed';
      alert(typeof msg === 'string' ? msg : 'Booking failed');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div style={{ maxWidth: '600px', margin: '0 auto' }}>
      <div className="bg-white rounded-lg shadow" style={{ borderRadius: '8px', padding: '24px' }}>
        {/* Header */}
        <div className="flex items-start justify-between">
          <h2 className="font-heading font-bold" style={{ fontSize: '24px', color: '#1E2C33', margin: 0 }}>
            Reserve Your Seat
          </h2>
          <span
            style={{
              backgroundColor: '#B46258',
              color: '#FFFFFF',
              borderRadius: '9999px',
              padding: '4px 12px',
              fontSize: '11px',
              fontWeight: 600,
              textTransform: 'uppercase',
              letterSpacing: '0.05em',
              whiteSpace: 'nowrap',
            }}
          >
            Single Pass
          </span>
        </div>
        <p style={{ color: '#8E9A98', marginTop: '4px', fontSize: '14px' }}>
          Select your route, timing, and travel details.
        </p>

        {error && (
          <div style={{ color: '#B46258', marginTop: '16px', fontSize: '14px' }}>{error}</div>
        )}

        {/* Select Trip Route */}
        <div style={{ marginTop: '24px' }}>
          <label className="flex items-center gap-2 font-medium" style={{ color: '#1E2C33', fontSize: '14px', marginBottom: '8px' }}>
            <ClockIcon /> Select Trip Route
          </label>
          <select
            value={selectedTripId}
            onChange={(e) => setSelectedTripId(e.target.value)}
            disabled={loading}
            className="w-full"
            style={{
              border: '1px solid #A6A9D0',
              borderRadius: '6px',
              padding: '10px 12px',
              fontSize: '14px',
              backgroundColor: '#FFFFFF',
              color: '#1E2C33',
              outline: 'none',
            }}
          >
            {loading ? (
              <option>Loading...</option>
            ) : (
              trips.map((t) => (
                <option key={t.tripId} value={t.tripId}>
                  {t.pickupLocationName} → {t.dropLocationName} • {formatTime(t.startTime)}
                </option>
              ))
            )}
          </select>
          <div style={{ color: '#8E9A98', fontSize: '12px', marginTop: '6px' }}>
            Available seats: loading...
          </div>
        </div>

        {/* Travel Date */}
        <div style={{ marginTop: '20px' }}>
          <label className="flex items-center gap-2 font-medium" style={{ color: '#1E2C33', fontSize: '14px', marginBottom: '8px' }}>
            <CalendarIcon /> Travel Date
          </label>
          <input
            type="date"
            value={travelDate}
            onChange={(e) => setTravelDate(e.target.value)}
            className="w-full"
            style={{
              border: '1px solid #A6A9D0',
              borderRadius: '6px',
              padding: '10px 12px',
              fontSize: '14px',
              backgroundColor: '#FFFFFF',
              color: '#1E2C33',
              outline: 'none',
            }}
          />
        </div>

        {/* Info row */}
        <div
          className="flex items-center gap-6"
          style={{ backgroundColor: '#F4F2FF', borderRadius: '8px', padding: '12px', marginTop: '20px', fontSize: '13px', color: '#1E2C33' }}
        >
          <span>~75 mins transit</span>
          <span style={{ color: '#A6A9D0' }}>|</span>
          <span>Student ID required</span>
        </div>

        {/* Book Now */}
        <button
          onClick={handleBook}
          disabled={submitting || loading}
          className="w-full font-heading"
          style={{
            marginTop: '24px',
            backgroundColor: '#35627A',
            color: '#FFFFFF',
            fontWeight: 600,
            padding: '12px',
            borderRadius: '6px',
            border: 'none',
            cursor: submitting || loading ? 'not-allowed' : 'pointer',
            opacity: submitting || loading ? 0.7 : 1,
          }}
        >
          {submitting ? 'Booking...' : 'Book Now'}
        </button>
      </div>
    </div>
  );
}
