document.addEventListener('DOMContentLoaded', () => {
    // 1. Tab Navigation Logic (Single Page App behavior)
    const navButtons = document.querySelectorAll('.nav-btn');
    const tabPanes = document.querySelectorAll('.tab-pane');

    navButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            // Remove active classes
            navButtons.forEach(b => b.classList.remove('active'));
            tabPanes.forEach(p => p.classList.remove('active'));

            // Add active class to clicked button and target tab
            btn.classList.add('active');
            const targetId = btn.getAttribute('data-target');
            document.getElementById(targetId).classList.add('active');
        });
    });

    // 2. Fetch Initial Data immediately on load
    fetchReports();

    // 3. Handle Form Submission to Spring Boot API
    const reportForm = document.getElementById('emergency-form');
    if(reportForm) {
        reportForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            
            const submitBtn = reportForm.querySelector('.submit-btn');
            const originalText = submitBtn.innerText;
            submitBtn.innerText = "Submitting...";
            
            // Build the payload mapping to your Java Entity
            const payload = {
                reportTitle: document.getElementById('reportTitle').value,
                emergencyType: document.getElementById('emergencyType').value,
                description: document.getElementById('description').value
            };

            try {
                // Post to the exact endpoint created in EmergencyReportController
                const response = await fetch('/api/emergency-reports', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                });

                if(response.ok) {
                    reportForm.reset();
                    // Alert and dynamically update the feed at the bottom
                    alert('Emergency Report Logged Successfully!');
                    fetchReports(); 
                } else {
                    alert('Server error while saving report.');
                }
            } catch (error) {
                console.error('Submission failed:', error);
                alert('Network connection error: Ensure the Spring Boot backend is actively running.');
            } finally {
                submitBtn.innerText = originalText;
            }
        });
    }
});

/**
 * Fetches reports from the `/api/emergency-reports` endpoint
 * and automatically injects them into the DOM
 */
async function fetchReports() {
    const listContainer = document.getElementById('reports-list');
    if(!listContainer) return;

    try {
        const response = await fetch('/api/emergency-reports');
        if(!response.ok) throw new Error('API Response was not OK');
        
        const data = await response.json();

        // Clear loading state
        listContainer.innerHTML = '';
        
        if(data.length === 0) {
            listContainer.innerHTML = '<p style="color: var(--text-secondary);">No emergency reports active at the moment. System nominal.</p>';
            return;
        }

        // Render each returned Java object structurally
        data.forEach(report => {
            const div = document.createElement('div');
            div.className = 'report-item';
            div.innerHTML = `
                <div style="display: flex; justify-content: space-between; align-items: flex-start;">
                    <div class="report-title">${report.reportTitle}</div>
                    <span class="badge">${report.resolutionStatus || 'Pending'}</span>
                </div>
                <div class="report-meta">
                    [Type: ${report.emergencyType}] &mdash; Reference ID: #${report.reportId}
                </div>
                <p style="margin-top: 0.75rem; font-size: 0.95rem; line-height: 1.5; color: var(--text-secondary);">
                    ${report.description}
                </p>
            `;
            listContainer.appendChild(div);
        });
    } catch (error) {
        console.error('Fetch error:', error);
        listContainer.innerHTML = `
            <div style="padding: 1rem; border: 1px solid rgba(239, 68, 68, 0.3); border-radius: 0.5rem; background: rgba(239, 68, 68, 0.1);">
                <p style="color: var(--danger); font-weight: 600;">System Offline: Failed to pull real-time data.</p>
                <p style="color: var(--danger); font-size: 0.875rem; margin-top: 0.5rem;">Ensure your backend is running locally at localhost:8080</p>
            </div>
        `;
    }
}
