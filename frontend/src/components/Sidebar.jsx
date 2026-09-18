import { NavLink } from 'react-router-dom';
import { Calendar, Map, BookOpen, User, Bus } from 'lucide-react';

const navItems = [
  { to: '/book', label: 'Book a Trip', Icon: Calendar },
  { to: '/map', label: 'Live Trip Map', Icon: Map },
  { to: '/bookings', label: 'My Bookings', Icon: BookOpen },
  { to: '/profile', label: 'My Profile', Icon: User },
];

const linkClass = ({ isActive }) =>
  [
    'flex items-center gap-3 py-3 pr-5 border-l-[3px] transition-colors',
    isActive
      ? 'bg-white/15 border-[#E5AEA9] text-white font-semibold pl-[17px]'
      : 'border-transparent text-white/70 hover:bg-white/10 pl-5',
  ].join(' ');

export default function Sidebar() {
  return (
    <aside
      className="fixed left-0 top-0 h-screen flex flex-col text-white"
      style={{ width: '240px', backgroundColor: '#35627A' }}
    >
      {/* Logo */}
      <div className="flex items-center gap-2 px-5" style={{ height: '64px' }}>
        <Bus width={18} height={18} className="text-white" />
        <span className="font-heading font-bold text-white" style={{ fontSize: '18px' }}>
          UTMS
        </span>
      </div>

      {/* Nav */}
      <nav className="flex-1 py-4">
        {navItems.map(({ to, label, Icon }) => (
          <NavLink key={to} to={to} className={linkClass}>
            <Icon width={18} height={18} />
            <span className="text-sm">{label}</span>
          </NavLink>
        ))}
      </nav>

      {/* Bottom user section */}
      <div
        className="px-5 py-4 flex items-center gap-3"
        style={{ borderTop: '1px solid rgba(255,255,255,0.15)' }}
      >
        <div
          className="flex items-center justify-center rounded-full font-heading font-semibold shrink-0"
          style={{ width: '40px', height: '40px', backgroundColor: '#E5AEA9', color: '#35627A' }}
        >
          JS
        </div>
        <div className="leading-tight">
          <div className="text-white text-sm font-semibold">John Silva</div>
          <div style={{ color: 'rgba(255,255,255,0.7)', fontSize: '12px' }}>IT21001</div>
        </div>
      </div>
    </aside>
  );
}
