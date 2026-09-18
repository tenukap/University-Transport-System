import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Sidebar from './components/Sidebar';
import TopBar from './components/TopBar';
import BookTrip from './pages/BookTrip';
import MapView from './pages/MapView';
import MyBookings from './pages/MyBookings';
import Profile from './pages/Profile';

export default function App() {
  return (
    <BrowserRouter>
      <div className="flex min-h-screen" style={{ backgroundColor: '#F5F5F5' }}>
        <Sidebar />
        <div className="flex-1 flex flex-col">
          <TopBar />
          <main
            style={{
              marginLeft: '240px',
              marginTop: '64px',
              minHeight: '100vh',
              backgroundColor: '#F5F5F5',
              padding: '32px',
            }}
          >
            <Routes>
              <Route path="/" element={<Navigate to="/book" replace />} />
              <Route path="/book" element={<BookTrip />} />
              <Route path="/map" element={<MapView />} />
              <Route path="/bookings" element={<MyBookings />} />
              <Route path="/profile" element={<Profile />} />
            </Routes>
          </main>
        </div>
      </div>
    </BrowserRouter>
  );
}
