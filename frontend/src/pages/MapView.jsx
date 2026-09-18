import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { MapContainer, TileLayer, Marker, Popup, Polyline } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';
import { getTrips, getLocations } from '../api/api';

// Fix the default Leaflet marker icon issue in React/bundler setups
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon-2x.png',
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png',
});

// Custom dropoff marker (SLIIT) in secondary rose color
const dropoffIcon = L.divIcon({
  className: '',
  html: '<div style="width:14px;height:14px;background:#B46258;border-radius:50%;border:2px solid white;box-shadow:0 2px 4px rgba(0,0,0,0.3)"></div>',
  iconSize: [14, 14],
  iconAnchor: [7, 7],
});

const SLIIT = [6.9147, 79.9729];

const formatTime = (t) => {
  if (!t) return '';
  const [h, m] = t.split(':').map(Number);
  const period = h >= 12 ? 'PM' : 'AM';
  const hour12 = h % 12 === 0 ? 12 : h % 12;
  return `${String(hour12).padStart(2, '0')}:${String(m).padStart(2, '0')} ${period}`;
};

export default function MapView() {
  const navigate = useNavigate();
  const [trips, setTrips] = useState([]);
  const [locations, setLocations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedTripId, setSelectedTripId] = useState('');

  useEffect(() => {
    Promise.all([getTrips(), getLocations()])
      .then(([tripsRes, locationsRes]) => {
        setTrips(tripsRes.data);
        setLocations(locationsRes.data);
        setLoading(false);
      })
      .catch((err) => {
        console.error('Failed to load map data', err);
        setLoading(false);
      });
  }, []);

  const getCoords = (locationId) => {
    const loc = locations.find((l) => l.locationId === locationId);
    return loc ? [parseFloat(loc.latitude), parseFloat(loc.longitude)] : null;
  };

  const getLocationName = (locationId) => {
    const loc = locations.find((l) => l.locationId === locationId);
    return loc ? loc.locationName : 'Unknown';
  };

  const selected = trips.find((t) => t.tripId === parseInt(selectedTripId, 10));
  const pickupCoords = selected ? getCoords(selected.pickupLocationId) : null;
  const dropoffCoords = selected ? getCoords(selected.dropLocationId) : null;

  return (
    <div style={{ maxWidth: '900px', margin: '0 auto' }}>
      {/* Filter bar */}
      <div className="bg-white shadow" style={{ borderRadius: '8px', padding: '16px', marginBottom: '16px' }}>
        <label className="font-medium" style={{ color: '#1E2C33', fontSize: '14px', marginRight: '12px' }}>
          Select a trip
        </label>
        <select
          value={selectedTripId}
          onChange={(e) => setSelectedTripId(e.target.value)}
          disabled={loading}
          style={{
            border: '1px solid #A6A9D0',
            borderRadius: '6px',
            padding: '8px 12px',
            fontSize: '14px',
            backgroundColor: '#FFFFFF',
            color: '#1E2C33',
            outline: 'none',
            minWidth: '260px',
          }}
        >
          <option value="">All trips</option>
          {trips.map((trip) => (
            <option key={trip.tripId} value={trip.tripId}>
              {getLocationName(trip.pickupLocationId)} → SLIIT • {trip.startTime}
            </option>
          ))}
        </select>
      </div>

      {loading ? (
        <div
          className="bg-white shadow flex items-center justify-center"
          style={{ height: '500px', borderRadius: '8px', color: '#8E9A98', fontSize: '15px' }}
        >
          Loading map data...
        </div>
      ) : (
        <div style={{ borderRadius: '8px', overflow: 'hidden' }} className="shadow">
          <MapContainer center={SLIIT} zoom={13} style={{ height: '500px', width: '100%' }}>
            <TileLayer
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
              attribution="&copy; OpenStreetMap contributors"
            />

            {selected && pickupCoords && dropoffCoords ? (
              <>
                {/* Pickup (default blue marker) */}
                <Marker position={pickupCoords}>
                  <Popup>
                    <strong>{getLocationName(selected.pickupLocationId)}</strong>
                    <br />
                    Pickup Stop
                  </Popup>
                </Marker>

                {/* Dropoff (custom rose marker) */}
                <Marker position={dropoffCoords} icon={dropoffIcon}>
                  <Popup>{getLocationName(selected.dropLocationId)} - Dropoff</Popup>
                </Marker>

                {/* Route */}
                <Polyline
                  positions={[pickupCoords, dropoffCoords]}
                  pathOptions={{ color: '#35627A', weight: 4, dashArray: '8 4' }}
                />
              </>
            ) : (
              // Default state: show all pickup location markers
              trips.map((trip) => {
                const coords = getCoords(trip.pickupLocationId);
                if (!coords) return null;
                return (
                  <Marker key={trip.tripId} position={coords}>
                    <Popup>
                      <strong>{getLocationName(trip.pickupLocationId)}</strong>
                      <br />
                      Pickup Stop • {formatTime(trip.startTime)}
                    </Popup>
                  </Marker>
                );
              })
            )}
          </MapContainer>
        </div>
      )}

      {/* Trip info card */}
      {selected ? (
        <div className="bg-white shadow" style={{ borderRadius: '8px', padding: '24px', marginTop: '16px' }}>
          <div className="font-heading font-bold" style={{ color: '#35627A', fontSize: '18px' }}>
            {getLocationName(selected.pickupLocationId)} → {getLocationName(selected.dropLocationId)}
          </div>
          <div style={{ color: '#8E9A98', fontSize: '14px', marginTop: '8px' }}>
            Departure: <span style={{ color: '#1E2C33', fontWeight: 500 }}>{formatTime(selected.startTime)}</span>
            {selected.eta && (
              <> &nbsp;•&nbsp; ETA: <span style={{ color: '#1E2C33', fontWeight: 500 }}>{formatTime(selected.eta)}</span></>
            )}
          </div>
          <div style={{ color: '#8E9A98', fontSize: '14px', marginTop: '4px' }}>
            9 seats left
          </div>
          <button
            onClick={() => navigate('/book')}
            className="font-heading"
            style={{
              marginTop: '16px',
              backgroundColor: '#35627A',
              color: '#FFFFFF',
              fontWeight: 600,
              padding: '10px 20px',
              borderRadius: '6px',
              border: 'none',
              cursor: 'pointer',
            }}
          >
            Book This Trip
          </button>
        </div>
      ) : (
        !loading && (
          <div
            className="bg-white shadow"
            style={{ borderRadius: '8px', padding: '24px', marginTop: '16px', textAlign: 'center', color: '#8E9A98', fontSize: '14px' }}
          >
            Select a trip above to see the route
          </div>
        )
      )}
    </div>
  );
}
