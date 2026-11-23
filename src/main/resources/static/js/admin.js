/**
 * FamilyCare - Admin Dashboard JavaScript
 * Handles inline SVG Area Chart generation, Donut chart, modals, SOS acknowledge AJAX, and range switcher.
 */

document.addEventListener('DOMContentLoaded', function () {
  initSignupsAreaChart();
  initDonutChart();
  initSosAcknowledgeButtons();
  initStatusModals();
});

/**
 * 1. Area Chart for Daily Sign-ups
 * Reads JSON dataset from data-chart attribute on #signups-chart-container and renders inline SVG.
 */
function initSignupsAreaChart() {
  const container = document.getElementById('signups-chart-container');
  if (!container) return;

  const rawData = container.getAttribute('data-chart');
  let data = [];
  try {
    data = rawData ? JSON.parse(rawData) : [];
  } catch (e) {
    console.error('Failed to parse chart data JSON', e);
  }

  if (!data || data.length === 0) {
    container.innerHTML = '<div style="color:var(--muted); font-size:0.85rem;">No signup data for this period.</div>';
    return;
  }

  const width = 560;
  const height = 200;
  const padding = { top: 20, right: 20, bottom: 35, left: 35 };

  const chartW = width - padding.left - padding.right;
  const chartH = height - padding.top - padding.bottom;

  const maxVal = Math.max(...data.map(d => d.count), 5);
  const minVal = 0;

  const stepX = data.length > 1 ? chartW / (data.length - 1) : chartW / 2;

  // Build coordinate points
  const points = data.map((d, i) => {
    const x = padding.left + (data.length > 1 ? i * stepX : chartW / 2);
    const y = padding.top + chartH - ((d.count - minVal) / (maxVal - minVal)) * chartH;
    return { x, y, date: d.date, count: d.count };
  });

  // Build SVG path
  let pathD = `M ${points[0].x} ${points[0].y}`;
  for (let i = 1; i < points.length; i++) {
    // Smooth bezier or straight line
    const prev = points[i - 1];
    const curr = points[i];
    const cpX = (prev.x + curr.x) / 2;
    pathD += ` C ${cpX} ${prev.y}, ${cpX} ${curr.y}, ${curr.x} ${curr.y}`;
  }

  const areaD = `${pathD} L ${points[points.length - 1].x} ${padding.top + chartH} L ${points[0].x} ${padding.top + chartH} Z`;

  // Grid lines
  let gridLines = '';
  const yTicks = 4;
  for (let t = 0; t <= yTicks; t++) {
    const val = Math.round(minVal + (t / yTicks) * (maxVal - minVal));
    const y = padding.top + chartH - (t / yTicks) * chartH;
    gridLines += `
      <line x1="${padding.left}" y1="${y}" x2="${width - padding.right}" y2="${y}" stroke="var(--border)" stroke-dasharray="3,3" />
      <text x="${padding.left - 8}" y="${y + 4}" font-size="10" fill="var(--muted)" text-anchor="end">${val}</text>
    `;
  }

  // X labels
  let xLabels = '';
  const labelInterval = Math.max(1, Math.floor(points.length / 7));
  points.forEach((p, i) => {
    if (i % labelInterval === 0 || i === points.length - 1) {
      xLabels += `<text x="${p.x}" y="${height - 10}" font-size="10" fill="var(--muted)" text-anchor="middle">${p.date}</text>`;
    }
  });

  // Dots
  let dots = '';
  points.forEach(p => {
    dots += `
      <circle cx="${p.x}" cy="${p.y}" r="3.5" fill="var(--primary)" stroke="var(--surface)" stroke-width="2">
        <title>${p.date}: ${p.count} sign-ups</title>
      </circle>
    `;
  });

  const svg = `
    <svg viewBox="0 0 ${width} ${height}" width="100%" height="100%" style="overflow: visible;" role="img" aria-label="Daily sign-ups line chart">
      <defs>
        <linearGradient id="areaGradient" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stop-color="var(--primary)" stop-opacity="0.32" />
          <stop offset="100%" stop-color="var(--primary)" stop-opacity="0.0" />
        </linearGradient>
      </defs>
      ${gridLines}
      <path d="${areaD}" fill="url(#areaGradient)" />
      <path d="${pathD}" fill="none" stroke="var(--primary)" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" />
      ${dots}
      ${xLabels}
    </svg>
  `;

  container.innerHTML = svg;
}

/**
 * 2. Donut Chart for User Role Distribution
 */
function initDonutChart() {
  const container = document.getElementById('donut-chart-container');
  if (!container) return;

  const adminPct = parseInt(container.getAttribute('data-admin') || '50', 10);
  const memberPct = parseInt(container.getAttribute('data-member') || '30', 10);
  const caregiverPct = parseInt(container.getAttribute('data-caregiver') || '20', 10);

  // SVGs donut calculation
  const size = 160;
  const strokeWidth = 24;
  const radius = (size - strokeWidth) / 2;
  const circumference = 2 * Math.PI * radius;

  const adminOffset = circumference - (circumference * adminPct) / 100;
  const memberDash = (circumference * memberPct) / 100;
  const caregiverDash = (circumference * caregiverPct) / 100;

  // Render SVG Donut
  const adminStroke = 'var(--primary)';
  const memberStroke = 'var(--secondary)';
  const caregiverStroke = 'var(--warning)';

  const offset1 = 0;
  const offset2 = -(circumference * adminPct) / 100;
  const offset3 = -((circumference * (adminPct + memberPct)) / 100);

  const svg = `
    <svg width="${size}" height="${size}" viewBox="0 0 ${size} ${size}" style="transform: rotate(-90deg);" role="img" aria-label="Role distribution donut chart">
      <circle cx="${size / 2}" cy="${size / 2}" r="${radius}" fill="none" stroke="var(--surface-2)" stroke-width="${strokeWidth}" />
      <circle cx="${size / 2}" cy="${size / 2}" r="${radius}" fill="none" stroke="${adminStroke}" stroke-width="${strokeWidth}"
        stroke-dasharray="${(circumference * adminPct) / 100} ${circumference}" stroke-dashoffset="${offset1}" />
      <circle cx="${size / 2}" cy="${size / 2}" r="${radius}" fill="none" stroke="${memberStroke}" stroke-width="${strokeWidth}"
        stroke-dasharray="${memberDash} ${circumference}" stroke-dashoffset="${offset2}" />
      <circle cx="${size / 2}" cy="${size / 2}" r="${radius}" fill="none" stroke="${caregiverStroke}" stroke-width="${strokeWidth}"
        stroke-dasharray="${caregiverDash} ${circumference}" stroke-dashoffset="${offset3}" />
    </svg>
  `;

  container.innerHTML = svg;
}

/**
 * 3. Emergency SOS Acknowledge Handler
 */
function initSosAcknowledgeButtons() {
  document.querySelectorAll('.btn-ack-sos').forEach(button => {
    button.addEventListener('click', async function (e) {
      e.preventDefault();
      const eventId = this.getAttribute('data-id');
      if (!eventId) return;

      const originalText = this.innerHTML;
      this.innerHTML = 'Acknowledging...';
      this.disabled = true;

      const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
      const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || 'X-CSRF-TOKEN';

      try {
        const response = await fetch(`/admin/sos/${eventId}/ack`, {
          method: 'POST',
          headers: {
            [csrfHeader]: csrfToken || '',
            'X-Requested-With': 'XMLHttpRequest',
            'Accept': 'application/json'
          }
        });

        if (response.ok) {
          this.closest('tr').querySelector('.sos-pulse')?.remove();
          const statusCell = this.closest('tr').querySelector('.sos-status-cell');
          if (statusCell) {
            statusCell.innerHTML = '<span class="badge badge-active">ACKNOWLEDGED</span>';
          }
          this.remove();
        } else {
          this.disabled = false;
          this.innerHTML = originalText;
          alert('Could not acknowledge event. Permission denied or error.');
        }
      } catch (err) {
        this.disabled = false;
        this.innerHTML = originalText;
        console.error('Error acknowledging SOS', err);
      }
    });
  });
}

/**
 * 4. Suspend / Activate Account Reason Modal
 */
function initStatusModals() {
  const modal = document.getElementById('status-modal');
  if (!modal) return;

  const modalTitle = document.getElementById('status-modal-title');
  const modalDesc = document.getElementById('status-modal-desc');
  const modalForm = document.getElementById('status-modal-form');
  const modalActionInput = document.getElementById('status-modal-action');
  const reasonTextarea = document.getElementById('status-reason-input');
  const confirmBtn = document.getElementById('status-modal-confirm-btn');

  document.querySelectorAll('.btn-open-status-modal').forEach(btn => {
    btn.addEventListener('click', function () {
      const action = this.getAttribute('data-action'); // SUSPEND or ACTIVATE
      const userId = this.getAttribute('data-user-id');
      const userName = this.getAttribute('data-user-name');

      modalActionInput.value = action;
      modalForm.action = `/admin/users/${userId}/status`;
      reasonTextarea.value = '';

      if (action === 'SUSPEND') {
        modalTitle.textContent = 'Suspend account';
        modalDesc.textContent = `Are you sure you want to suspend ${userName}? The user will be immediately logged out and unable to access their family portal.`;
        confirmBtn.textContent = 'Confirm suspension';
        confirmBtn.className = 'btn-sm btn-sm-danger';
      } else {
        modalTitle.textContent = 'Reactivate account';
        modalDesc.textContent = `Are you sure you want to reactivate ${userName}? Access will be restored immediately.`;
        confirmBtn.textContent = 'Confirm reactivation';
        confirmBtn.className = 'btn-sm btn-sm-primary';
      }

      modal.classList.add('active');
      modal.setAttribute('aria-hidden', 'false');
      reasonTextarea.focus();
    });
  });

  // Close modal
  document.querySelectorAll('.btn-close-modal').forEach(btn => {
    btn.addEventListener('click', function () {
      modal.classList.remove('active');
      modal.setAttribute('aria-hidden', 'true');
    });
  });

  // Validate minimum 5 characters on submission
  if (modalForm) {
    modalForm.addEventListener('submit', function (e) {
      if (!reasonTextarea.value || reasonTextarea.value.trim().length < 5) {
        e.preventDefault();
        alert('Please enter a reason of at least 5 characters for the audit record.');
        reasonTextarea.focus();
      }
    });
  }
}
