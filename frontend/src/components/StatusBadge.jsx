const STYLES = {
  PENDING: { backgroundColor: '#A6A9D0', color: '#1E2C33' },
  CONFIRMED: { backgroundColor: '#35627A', color: '#FFFFFF' },
  CANCELLED: { backgroundColor: '#B46258', color: '#FFFFFF' },
};

export default function StatusBadge({ status }) {
  const key = (status || '').toUpperCase();
  const colors = STYLES[key] || { backgroundColor: '#A6A9D0', color: '#1E2C33' };

  return (
    <span
      style={{
        ...colors,
        borderRadius: '9999px',
        padding: '4px 10px',
        fontSize: '11px',
        fontWeight: 600,
        textTransform: 'uppercase',
        letterSpacing: '0.05em',
        display: 'inline-block',
        lineHeight: 1.4,
        whiteSpace: 'nowrap',
      }}
    >
      {key || 'UNKNOWN'}
    </span>
  );
}
