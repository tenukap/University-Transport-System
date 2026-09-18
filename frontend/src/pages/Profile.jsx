import { useEffect, useState } from 'react';
import { getStudentById, updateStudent } from '../api/api';

const STUDENT_ID = 1; // hardcoded for now

const getInitials = (name) => {
  if (!name) return '?';
  return name
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((w) => w[0].toUpperCase())
    .join('');
};

export default function Profile() {
  const [student, setStudent] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [success, setSuccess] = useState('');
  const [form, setForm] = useState({ fullName: '', phone: '' });

  const loadStudent = () => {
    setLoading(true);
    getStudentById(STUDENT_ID)
      .then((data) => {
        setStudent(data);
        setForm({ fullName: data.fullName || '', phone: data.phone || '' });
        setError('');
      })
      .catch((err) => {
        const msg = err?.response?.data || err?.message || 'Failed to load profile';
        setError(typeof msg === 'string' ? msg : 'Failed to load profile');
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadStudent();
  }, []);

  const handleSave = async () => {
    setSaving(true);
    setSuccess('');
    try {
      const updated = await updateStudent(STUDENT_ID, { fullName: form.fullName, phone: form.phone });
      setStudent(updated);
      setEditing(false);
      setSuccess('Profile updated successfully');
    } catch (err) {
      const msg = err?.response?.data || err?.message || 'Update failed';
      alert(typeof msg === 'string' ? msg : 'Update failed');
    } finally {
      setSaving(false);
    }
  };

  const handleCancel = () => {
    setForm({ fullName: student.fullName || '', phone: student.phone || '' });
    setEditing(false);
  };

  if (loading) return <p style={{ color: '#8E9A98', textAlign: 'center' }}>Loading...</p>;
  if (error) return <p style={{ color: '#B46258', textAlign: 'center' }}>{error}</p>;
  if (!student) return null;

  return (
    <div style={{ maxWidth: '500px', margin: '0 auto' }}>
      <div className="bg-white shadow" style={{ borderRadius: '8px', padding: '24px' }}>
        {/* Top section */}
        <div className="flex flex-col items-center">
          <div
            className="flex items-center justify-center rounded-full font-heading font-bold"
            style={{ width: '84px', height: '84px', backgroundColor: '#35627A', color: '#FFFFFF', fontSize: '28px' }}
          >
            {getInitials(student.fullName)}
          </div>
          <h2 className="font-heading font-bold" style={{ fontSize: '22px', color: '#1E2C33', marginTop: '12px', marginBottom: '8px' }}>
            {student.fullName}
          </h2>
          <span
            style={{
              backgroundColor: '#E5AEA9',
              color: '#35627A',
              borderRadius: '9999px',
              padding: '4px 14px',
              fontSize: '12px',
              fontWeight: 600,
            }}
          >
            Student Transport Pass • Active
          </span>
        </div>

        {success && (
          <div style={{ color: '#35627A', backgroundColor: '#F4F2FF', borderRadius: '6px', padding: '10px', marginTop: '20px', fontSize: '13px', textAlign: 'center' }}>
            {success}
          </div>
        )}

        {/* Fields */}
        <div style={{ marginTop: '24px' }} className="space-y-4">
          <Field
            label="Full Name"
            value={form.fullName}
            editable={editing}
            onChange={(v) => setForm((f) => ({ ...f, fullName: v }))}
            display={student.fullName}
          />
          <Field label="Student Index" value={student.studentIndex} editable={false} display={student.studentIndex} />
          <Field label="Email Address" value={student.email} editable={false} display={student.email} />
          <Field
            label="Phone Number"
            value={form.phone}
            editable={editing}
            onChange={(v) => setForm((f) => ({ ...f, phone: v }))}
            display={student.phone}
          />
        </div>

        {/* Actions */}
        <div className="flex justify-end gap-3" style={{ marginTop: '24px' }}>
          {editing ? (
            <>
              <button
                onClick={handleCancel}
                disabled={saving}
                style={{ border: '1px solid #A6A9D0', color: '#8E9A98', borderRadius: '6px', padding: '8px 16px', fontSize: '13px', fontWeight: 500, background: 'none', cursor: 'pointer' }}
              >
                Cancel
              </button>
              <button
                onClick={handleSave}
                disabled={saving}
                className="font-heading"
                style={{ backgroundColor: '#35627A', color: '#FFFFFF', borderRadius: '6px', padding: '8px 20px', fontSize: '13px', fontWeight: 600, border: 'none', cursor: saving ? 'not-allowed' : 'pointer', opacity: saving ? 0.7 : 1 }}
              >
                {saving ? 'Saving...' : 'Save'}
              </button>
            </>
          ) : (
            <button
              onClick={() => { setEditing(true); setSuccess(''); }}
              style={{ border: '1px solid #35627A', color: '#35627A', borderRadius: '6px', padding: '8px 16px', fontSize: '13px', fontWeight: 500, background: 'none', cursor: 'pointer' }}
            >
              Edit Profile
            </button>
          )}
        </div>
      </div>
    </div>
  );
}

function Field({ label, value, editable, onChange, display }) {
  return (
    <div>
      <label style={{ color: '#8E9A98', fontSize: '11px', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
        {label}
      </label>
      {editable ? (
        <input
          value={value}
          onChange={(e) => onChange(e.target.value)}
          className="w-full"
          style={{ border: '1px solid #A6A9D0', borderRadius: '6px', padding: '8px 12px', fontSize: '14px', color: '#1E2C33', marginTop: '4px', outline: 'none' }}
        />
      ) : (
        <div style={{ color: '#1E2C33', fontSize: '15px', marginTop: '4px' }}>{display || '—'}</div>
      )}
    </div>
  );
}
