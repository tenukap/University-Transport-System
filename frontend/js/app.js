// Tab Switching Logic
function switchTab(tabId) {
    document.querySelectorAll('.tab-pane').forEach(el => el.classList.remove('active'));
    document.querySelectorAll('.nav-btn').forEach(el => el.classList.remove('active'));
    
    document.getElementById('tab-' + tabId).classList.add('active');
    event.target.classList.add('active');
    
    if(tabId === 'emergency') fetchReports();
    if(tabId === 'incidents') fetchIncidents();
    if(tabId === 'status') fetchStatuses();
    if(tabId === 'location') fetchLocations();
}

// ==========================================
// EMERGENCY REPORTS CRUD
// ==========================================
async function fetchReports() {
    const list = document.getElementById('reports-list');
    try {
        const res = await fetch('http://localhost:8080/api/emergency-reports');
        const data = await res.json();
        list.innerHTML = data.length === 0 ? '<p>No reports found.</p>' : '';
        data.forEach(report => {
            const badgeClass = report.resolutionStatus === 'Resolved' ? 'resolved' : '';
            const html = `
                <div class="data-item">
                    <div class="data-header">
                        <span class="data-title">${report.reportTitle}</span>
                        <div>
                            <span class="badge ${badgeClass}">${report.resolutionStatus || 'Pending'}</span>
                            <button class="btn btn-warning btn-sm" onclick='editReport(${JSON.stringify(report)})'>✏️ Edit</button>
                            <button class="btn btn-success btn-sm" onclick='resolveReport(${JSON.stringify(report)})'>✓ Resolve</button>
                            <button class="btn btn-danger btn-sm" onclick='deleteReport(${report.reportId})'>🗑️ Delete</button>
                        </div>
                    </div>
                    <div style="font-size:0.85rem; color:#7f8c8d;">Type: <strong>${report.emergencyType}</strong></div>
                    <p style="margin-top:10px">${report.description}</p>
                </div>
            `;
            list.innerHTML += html;
        });
    } catch(e) { list.innerHTML = 'Error loading reports.'; }
}

document.getElementById('form-emergency').addEventListener('submit', async (e) => {
    e.preventDefault();
    const id = document.getElementById('reportId').value;
    const payload = {
        reportTitle: document.getElementById('reportTitle').value,
        emergencyType: document.getElementById('emergencyType').value,
        description: document.getElementById('reportDesc').value
    };

    if(id) {
        payload.reportId = id;
        await fetch(`http://localhost:8080/api/emergency-reports/${id}`, {
            method: 'PUT', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(payload)
        });
        cancelReportEdit();
    } else {
        await fetch('http://localhost:8080/api/emergency-reports', {
            method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(payload)
        });
        document.getElementById('form-emergency').reset();
    }
    fetchReports();
});

window.editReport = (report) => {
    document.getElementById('reportId').value = report.reportId;
    document.getElementById('reportTitle').value = report.reportTitle;
    document.getElementById('emergencyType').value = report.emergencyType;
    document.getElementById('reportDesc').value = report.description;
    
    document.getElementById('report-form-title').innerText = '✏️ Edit Emergency Report';
    document.getElementById('report-submit-btn').innerText = 'Update Report';
    document.getElementById('report-cancel-btn').style.display = 'block';
    window.scrollTo({top:0, behavior:'smooth'});
};

window.cancelReportEdit = () => {
    document.getElementById('form-emergency').reset();
    document.getElementById('reportId').value = '';
    document.getElementById('report-form-title').innerText = 'File a New Emergency Report';
    document.getElementById('report-submit-btn').innerText = 'Submit Report';
    document.getElementById('report-cancel-btn').style.display = 'none';
};

window.resolveReport = async (report) => {
    report.resolutionStatus = 'Resolved';
    await fetch(`http://localhost:8080/api/emergency-reports/${report.reportId}`, {
        method: 'PUT', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(report)
    });
    fetchReports();
};

window.deleteReport = async (id) => {
    if(confirm('Delete report?')) {
        await fetch(`http://localhost:8080/api/emergency-reports/${id}`, { method: 'DELETE' });
        fetchReports();
    }
};

// ==========================================
// CRASH INCIDENTS CRUD & VALIDATION
// ==========================================
window.validateNumber = (input) => {
    const errorSpan = document.getElementById(input.id + '-error');
    if (isNaN(input.value) && input.value !== '') {
        input.style.borderColor = 'red';
        errorSpan.style.display = 'block';
    } else {
        input.style.borderColor = '#dcdde1';
        errorSpan.style.display = 'none';
    }
};

async function fetchIncidents() {
    const list = document.getElementById('incidents-list');
    try {
        const res = await fetch('http://localhost:8080/api/crash-incidents');
        const data = await res.json();
        list.innerHTML = data.length === 0 ? '<p>No incidents found.</p>' : '';
        data.forEach(inc => {
            list.innerHTML += `
                <div class="data-item">
                    <div class="data-header">
                        <span class="data-title">Bus ${inc.busNo} — Driver ${inc.driverNo}</span>
                        <div>
                            <span class="badge danger">${inc.severityLevel} Severity</span>
                            <button class="btn btn-danger btn-sm" onclick='deleteIncident(${inc.incidentId})'>🗑️</button>
                        </div>
                    </div>
                    <div style="font-size:0.85rem; color:#7f8c8d;">Location: <strong>${inc.locationCoordinates}</strong></div>
                    <p style="margin-top:10px">${inc.description}</p>
                </div>
            `;
        });
    } catch(e) { list.innerHTML = 'Error loading incidents.'; }
}

document.getElementById('form-incident').addEventListener('submit', async (e) => {
    e.preventDefault();
    const busNo = document.getElementById('busNo').value;
    const driverNo = document.getElementById('driverNo').value;
    
    if(isNaN(busNo) || isNaN(driverNo)) {
        alert('Validation Failed: Please enter valid numbers only!');
        return;
    }

    const payload = {
        busNo: busNo,
        driverNo: driverNo,
        locationCoordinates: document.getElementById('incidentLocation').value,
        severityLevel: document.getElementById('severityLevel').value,
        description: document.getElementById('incidentDesc').value
    };

    await fetch('http://localhost:8080/api/crash-incidents', {
        method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(payload)
    });
    document.getElementById('form-incident').reset();
    fetchIncidents();
});

window.deleteIncident = async (id) => {
    if(confirm('Delete incident?')) {
        await fetch(`http://localhost:8080/api/crash-incidents/${id}`, { method: 'DELETE' });
        fetchIncidents();
    }
};

// Initial Load
fetchReports();

// ==========================================
// TRIP STATUS CRUD
// ==========================================
async function fetchStatuses() {
    const list = document.getElementById('statuses-list');
    try {
        const res = await fetch('http://localhost:8080/api/trip-statuses');
        const data = await res.json();
        list.innerHTML = data.length === 0 ? '<p>No live trip statuses found.</p>' : '';
        data.forEach(st => {
            list.innerHTML += `
                <div class="data-item">
                    <div class="data-header">
                        <span class="data-title">Trip #${st.tripId}</span>
                        <div>
                            <span class="badge info" style="background:#3498db">${st.statusType}</span>
                            <button class="btn btn-danger btn-sm" onclick='deleteStatus(${st.statusId})'>🗑️</button>
                        </div>
                    </div>
                    <div style="font-size:0.85rem; color:#7f8c8d;">Last Updated: <strong>${new Date(st.updatedAt).toLocaleTimeString()}</strong></div>
                </div>
            `;
        });
    } catch(e) { list.innerHTML = 'Error loading statuses.'; }
}

document.getElementById('form-status').addEventListener('submit', async (e) => {
    e.preventDefault();
    const payload = {
        tripId: document.getElementById('statusTripId').value,
        statusType: document.getElementById('statusType').value
    };
    await fetch('http://localhost:8080/api/trip-statuses', {
        method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(payload)
    });
    document.getElementById('form-status').reset();
    fetchStatuses();
});

window.deleteStatus = async (id) => {
    if(confirm('Delete status?')) {
        await fetch(`http://localhost:8080/api/trip-statuses/${id}`, { method: 'DELETE' });
        fetchStatuses();
    }
};

// ==========================================
// LOCATION TRACKING & GEOCODING
// ==========================================
async function fetchLocations() {
    const list = document.getElementById('locations-list');
    try {
        const res = await fetch('http://localhost:8080/api/location-updates');
        const data = await res.json();
        list.innerHTML = data.length === 0 ? '<p>No location updates logged.</p>' : '';
        
        for (const loc of data) {
            // Reverse Geocoding
            let placeName = `${loc.latitude}, ${loc.longitude}`;
            try {
                const geoRes = await fetch(`https://nominatim.openstreetmap.org/reverse?format=json&lat=${loc.latitude}&lon=${loc.longitude}`);
                const geoData = await geoRes.json();
                if(geoData && geoData.display_name) {
                    placeName = geoData.display_name.split(',').slice(0, 3).join(',');
                }
            } catch(e) {}

            list.innerHTML += `
                <div class="data-item">
                    <div class="data-header">
                        <span class="data-title">Trip #${loc.tripId}</span>
                        <div>
                            <span class="badge success" style="background:#2ecc71">Active Ping</span>
                            <button class="btn btn-danger btn-sm" onclick='deleteLocation(${loc.locationUpdateId})'>🗑️</button>
                        </div>
                    </div>
                    <div style="font-size:0.85rem; color:#7f8c8d;">Location: <strong>${placeName}</strong></div>
                    <div style="font-size:0.75rem; color:#7f8c8d; margin-top:5px;">Ping Time: ${new Date(loc.recordedAt).toLocaleTimeString()}</div>
                </div>
            `;
        }
    } catch(e) { list.innerHTML = 'Error loading locations.'; }
}

document.getElementById('form-location').addEventListener('submit', async (e) => {
    e.preventDefault();
    const payload = {
        tripId: document.getElementById('locTripId').value,
        latitude: document.getElementById('latitude').value,
        longitude: document.getElementById('longitude').value
    };
    await fetch('http://localhost:8080/api/location-updates', {
        method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(payload)
    });
    document.getElementById('form-location').reset();
    fetchLocations();
});

window.deleteLocation = async (id) => {
    if(confirm('Delete location ping?')) {
        await fetch(`http://localhost:8080/api/location-updates/${id}`, { method: 'DELETE' });
        fetchLocations();
    }
};

// Auto-Fill GPS (Navigator API)
window.autoFillLocation = () => {
    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition((position) => {
            document.getElementById('latitude').value = position.coords.latitude.toFixed(6);
            document.getElementById('longitude').value = position.coords.longitude.toFixed(6);
        }, () => alert('Unable to retrieve location.'));
    } else {
        alert('Geolocation not supported by browser.');
    }
};

// Forward Geocoding (Auto-Suggest)
window.searchLocation = async (query) => {
    const ul = document.getElementById('autoSuggestResults');
    if (query.length < 3) { ul.style.display = 'none'; return; }
    
    try {
        const res = await fetch(`https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(query)}&countrycodes=lk&limit=5`);
        const data = await res.json();
        
        ul.innerHTML = '';
        if(data.length > 0) {
            ul.style.display = 'block';
            data.forEach(r => {
                const li = document.createElement('li');
                li.style.padding = '10px';
                li.style.cursor = 'pointer';
                li.style.borderBottom = '1px solid #eee';
                li.innerText = r.display_name;
                li.onclick = () => {
                    document.getElementById('latitude').value = r.lat;
                    document.getElementById('longitude').value = r.lon;
                    document.getElementById('autoSuggestInput').value = r.display_name;
                    ul.style.display = 'none';
                };
                ul.appendChild(li);
            });
        } else {
            ul.style.display = 'none';
        }
    } catch(e) { console.error(e); }
};
