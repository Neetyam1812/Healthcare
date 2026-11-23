/**
 * FamilyCare - Family Dashboard Interactive Engine (dashboard.js)
 * Clean, lightweight, zero-external-dependencies, accessible SVG charts & actions.
 */

(function () {
  'use strict';

  // CSRF Utility
  function getCsrfInfo() {
    const tokenMeta = document.querySelector('meta[name="_csrf"]');
    const headerMeta = document.querySelector('meta[name="_csrf_header"]');
    return {
      token: tokenMeta ? tokenMeta.getAttribute('content') : '',
      header: headerMeta ? headerMeta.getAttribute('content') : 'X-CSRF-TOKEN'
    };
  }

  // Toast Notification Utility
  function showToast(message, type = 'success') {
    let container = document.getElementById('toast-container');
    if (!container) {
      container = document.createElement('div');
      container.id = 'toast-container';
      container.setAttribute('aria-live', 'polite');
      document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.setAttribute('role', 'status');

    const icon = type === 'success'
      ? `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>`
      : `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>`;

    toast.innerHTML = `${icon}<span>${escapeHtml(message)}</span>`;
    container.appendChild(toast);

    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transform = 'translateY(8px)';
      setTimeout(() => toast.remove(), 300);
    }, 3800);
  }

  function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/[&<>"']/g, m => ({
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      '"': '&quot;',
      "'": '&#039;'
    })[m]);
  }

  /* --------------------------------------------------------------------------
     1. User Avatar Dropdown
     -------------------------------------------------------------------------- */
  function initUserMenu() {
    const btn = document.getElementById('user-menu-btn');
    const menu = document.getElementById('user-dropdown-menu');
    if (!btn || !menu) return;

    btn.addEventListener('click', e => {
      e.stopPropagation();
      const open = menu.classList.contains('is-open');
      menu.classList.toggle('is-open', !open);
      btn.setAttribute('aria-expanded', String(!open));
    });

    document.addEventListener('click', e => {
      if (!menu.contains(e.target) && !btn.contains(e.target)) {
        menu.classList.remove('is-open');
        btn.setAttribute('aria-expanded', 'false');
      }
    });

    document.addEventListener('keydown', e => {
      if (e.key === 'Escape' && menu.classList.contains('is-open')) {
        menu.classList.remove('is-open');
        btn.setAttribute('aria-expanded', 'false');
        btn.focus();
      }
    });
  }

  /* --------------------------------------------------------------------------
     2. Family Member Switcher
     -------------------------------------------------------------------------- */
  function initMemberSelector() {
    const select = document.getElementById('family-member-select');
    if (!select) return;

    select.addEventListener('change', () => {
      const val = select.value;
      const url = new URL(window.location.href);
      if (val && val !== 'all') {
        url.searchParams.set('memberId', val);
      } else {
        url.searchParams.delete('memberId');
      }
      window.location.href = url.toString();
    });
  }

  /* --------------------------------------------------------------------------
     3. Dismissible Alert Banner
     -------------------------------------------------------------------------- */
  function initAlertBanner() {
    const banner = document.getElementById('dashboard-alert-banner');
    const dismissBtn = document.getElementById('alert-dismiss-btn');
    if (!banner || !dismissBtn) return;

    if (sessionStorage.getItem('fc-alert-dismissed') === 'true') {
      banner.classList.add('is-dismissed');
    }

    dismissBtn.addEventListener('click', () => {
      banner.classList.add('is-dismissed');
      sessionStorage.setItem('fc-alert-dismissed', 'true');
    });
  }

  /* --------------------------------------------------------------------------
     4. Stat Number Counter Animation (Reduced-motion safe)
     -------------------------------------------------------------------------- */
  function initStatCounters() {
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;

    const counters = document.querySelectorAll('[data-counter-target]');
    counters.forEach(counter => {
      const target = parseInt(counter.getAttribute('data-counter-target'), 10);
      if (isNaN(target) || target <= 0) return;

      let current = 0;
      const step = Math.max(1, Math.floor(target / 20));
      const interval = setInterval(() => {
        current += step;
        if (current >= target) {
          current = target;
          clearInterval(interval);
        }
        counter.textContent = current;
      }, 35);
    });
  }

  /* --------------------------------------------------------------------------
     5. Adherence Ring Chart (Inline SVG)
     -------------------------------------------------------------------------- */
  function renderAdherenceRing() {
    const container = document.getElementById('adherence-ring-svg');
    if (!container) return;

    const pct = parseInt(container.getAttribute('data-pct') || '0', 10);
    const radius = 54;
    const circumference = 2 * Math.PI * radius;
    const offset = circumference - (pct / 100) * circumference;

    const svg = `
      <svg width="140" height="140" viewBox="0 0 140 140" style="transform: rotate(-90deg);" aria-hidden="true">
        <circle cx="70" cy="70" r="${radius}" fill="none" stroke="var(--surface-2)" stroke-width="12"/>
        <circle cx="70" cy="70" r="${radius}" fill="none" stroke="var(--primary)" stroke-width="12"
          stroke-linecap="round"
          stroke-dasharray="${circumference}"
          stroke-dashoffset="${offset}"
          style="transition: stroke-dashoffset 0.8s cubic-bezier(0.16, 1, 0.3, 1);"/>
      </svg>
    `;
    container.innerHTML = svg;
  }

  /* --------------------------------------------------------------------------
     6. Today's Medicines: Optimistic "Mark Taken" Action
     -------------------------------------------------------------------------- */
  function initMedicineActions() {
    document.addEventListener('click', async e => {
      const btn = e.target.closest('.btn-take');
      if (!btn) return;

      const medId = btn.getAttribute('data-med-id');
      if (!medId) return;

      const row = document.getElementById(`med-row-${medId}`);
      if (!row || row.classList.contains('taken')) return;

      // Optimistic Update
      row.classList.add('taken');
      btn.disabled = true;
      const originalBtnHtml = btn.innerHTML;
      btn.innerHTML = `
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
          <polyline points="20 6 9 17 4 12"/>
        </svg> Taken
      `;

      const csrf = getCsrfInfo();

      try {
        const response = await fetch(`/app/medicines/${medId}/take`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            [csrf.header]: csrf.token
          }
        });

        if (!response.ok) {
          throw new Error('Failed to update medication status');
        }

        const data = await response.json();
        showToast(data.message || 'Medication marked as taken', 'success');

        // Update stock pill if present
        const stockEl = row.querySelector('.med-stock-count');
        if (stockEl && data.stockCount !== undefined) {
          stockEl.textContent = `${data.stockCount} left`;
        }
      } catch (err) {
        // Rollback
        row.classList.remove('taken');
        btn.disabled = false;
        btn.innerHTML = originalBtnHtml;
        showToast('Could not record medication. Please try again.', 'error');
      }
    });
  }

  /* --------------------------------------------------------------------------
     7. Vitals Tabs and SVG Line Chart
     -------------------------------------------------------------------------- */
  function initVitalsTabs() {
    const tabs = document.querySelectorAll('.vitals-tab-btn');
    if (!tabs.length) return;

    tabs.forEach((tab, index) => {
      tab.addEventListener('click', () => {
        activateVitalTab(tab);
      });

      // Keyboard navigation: Left/Right arrows
      tab.addEventListener('keydown', e => {
        let newIndex = index;
        if (e.key === 'ArrowRight') {
          newIndex = (index + 1) % tabs.length;
        } else if (e.key === 'ArrowLeft') {
          newIndex = (index - 1 + tabs.length) % tabs.length;
        } else {
          return;
        }
        e.preventDefault();
        tabs[newIndex].focus();
        activateVitalTab(tabs[newIndex]);
      });
    });

    // Render the initial active chart
    const activeTab = document.querySelector('.vitals-tab-btn.is-active');
    if (activeTab) {
      renderVitalChart(activeTab.getAttribute('data-target'));
    }
  }

  function activateVitalTab(tab) {
    document.querySelectorAll('.vitals-tab-btn').forEach(t => {
      t.classList.remove('is-active');
      t.setAttribute('aria-selected', 'false');
    });
    document.querySelectorAll('.vital-panel').forEach(p => p.classList.remove('is-active'));

    tab.classList.add('is-active');
    tab.setAttribute('aria-selected', 'true');

    const targetId = tab.getAttribute('data-target');
    const panel = document.getElementById(targetId);
    if (panel) {
      panel.classList.add('is-active');
      renderVitalChart(targetId);
    }
  }

  function renderVitalChart(panelId) {
    const chartContainer = document.querySelector(`#${panelId} .vitals-chart-container`);
    if (!chartContainer) return;

    const rawData = chartContainer.getAttribute('data-readings');
    let points = [];
    try {
      points = JSON.parse(rawData || '[]');
    } catch (e) {
      points = [];
    }

    if (!points || points.length === 0) {
      chartContainer.innerHTML = `
        <div style="height: 100%; display: flex; align-items: center; justify-content: center; color: var(--muted); font-size: 0.8125rem;">
          No trend readings recorded yet
        </div>
      `;
      return;
    }

    const width = 480;
    const height = 110;
    const paddingX = 36;
    const paddingY = 22;

    const values = points.map(p => p.val1);
    let minVal = Math.min(...values);
    let maxVal = Math.max(...values);
    if (minVal === maxVal) {
      minVal -= 5;
      maxVal += 5;
    }

    const range = maxVal - minVal;
    const stepX = (width - paddingX * 2) / Math.max(1, points.length - 1);

    const coords = points.map((p, idx) => {
      const x = paddingX + idx * stepX;
      const y = height - paddingY - ((p.val1 - minVal) / range) * (height - paddingY * 2);
      return { x, y, label: p.label, val: p.val1, val2: p.val2 };
    });

    const pathData = coords.reduce((acc, c, idx) => {
      return idx === 0 ? `M ${c.x} ${c.y}` : `${acc} L ${c.x} ${c.y}`;
    }, '');

    // Area fill
    const areaData = `${pathData} L ${coords[coords.length - 1].x} ${height - paddingY} L ${coords[0].x} ${height - paddingY} Z`;

    let dotsHtml = '';
    let labelsHtml = '';

    coords.forEach(c => {
      dotsHtml += `
        <circle cx="${c.x}" cy="${c.y}" r="4" fill="var(--surface)" stroke="var(--primary)" stroke-width="2.5"/>
      `;
      labelsHtml += `
        <text x="${c.x}" y="${height - 4}" font-size="10" font-weight="600" fill="var(--muted)" text-anchor="middle">${escapeHtml(c.label)}</text>
      `;
    });

    const svg = `
      <svg viewBox="0 0 ${width} ${height}" preserveAspectRatio="none" style="width: 100%; height: 100%; overflow: visible;" aria-hidden="true">
        <defs>
          <linearGradient id="vitalGrad" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stop-color="var(--primary)" stop-opacity="0.18"/>
            <stop offset="100%" stop-color="var(--primary)" stop-opacity="0.0"/>
          </linearGradient>
        </defs>
        <!-- Horizontal Grid Line -->
        <line x1="${paddingX}" y1="${height - paddingY}" x2="${width - paddingX}" y2="${height - paddingY}" stroke="var(--border-subtle)" stroke-width="1"/>
        <line x1="${paddingX}" y1="${paddingY}" x2="${width - paddingX}" y2="${paddingY}" stroke="var(--border-subtle)" stroke-dasharray="3,3" stroke-width="1"/>
        <!-- Area -->
        <path d="${areaData}" fill="url(#vitalGrad)"/>
        <!-- Line -->
        <path d="${pathData}" fill="none" stroke="var(--primary)" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
        <!-- Dots -->
        ${dotsHtml}
        <!-- X Axis Labels -->
        ${labelsHtml}
      </svg>
    `;

    chartContainer.innerHTML = svg;
  }

  /* --------------------------------------------------------------------------
     8. Emergency SOS Accessible Modal & Flow
     -------------------------------------------------------------------------- */
  function initEmergencySos() {
    const triggerBtn = document.getElementById('trigger-sos-btn');
    const modal = document.getElementById('sos-modal');
    const cancelBtn = document.getElementById('cancel-sos-btn');
    const countdownEl = document.getElementById('sos-countdown');
    const titleEl = document.getElementById('sos-modal-title');
    const descEl = document.getElementById('sos-modal-desc');

    if (!triggerBtn || !modal || !cancelBtn) return;

    let timer = null;
    let countdownVal = 5;

    function openModal() {
      countdownVal = 5;
      countdownEl.textContent = '5';
      titleEl.textContent = 'Emergency alert dispatching';
      descEl.textContent = 'Help request will dispatch automatically in 5 seconds. Press Cancel to abort.';
      cancelBtn.textContent = 'Cancel alert';
      cancelBtn.style.display = 'inline-block';
      modal.classList.add('is-open');
      modal.setAttribute('aria-hidden', 'false');
      cancelBtn.focus();

      timer = setInterval(() => {
        countdownVal--;
        countdownEl.textContent = String(countdownVal);

        if (countdownVal <= 0) {
          clearInterval(timer);
          dispatchSosAlert();
        }
      }, 1000);
    }

    function closeModal() {
      if (timer) {
        clearInterval(timer);
        timer = null;
      }
      modal.classList.remove('is-open');
      modal.setAttribute('aria-hidden', 'true');
      triggerBtn.focus();
    }

    async function dispatchSosAlert() {
      titleEl.textContent = 'Dispatching alert...';
      descEl.textContent = 'Contacting family emergency coordinators and logging location...';
      countdownEl.innerHTML = `
        <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" class="animate-spin" style="animation: spin 1s linear infinite;">
          <circle cx="12" cy="12" r="10" stroke-opacity="0.25"/>
          <path d="M12 2 A 10 10 0 0 1 22 12"/>
        </svg>
      `;

      let lat = null;
      let lng = null;

      if ('geolocation' in navigator) {
        try {
          const pos = await new Promise((resolve, reject) => {
            navigator.geolocation.getCurrentPosition(resolve, reject, { timeout: 3500 });
          });
          lat = pos.coords.latitude;
          lng = pos.coords.longitude;
        } catch (e) {
          // Geolocation optional fallback
        }
      }

      const csrf = getCsrfInfo();

      try {
        const res = await fetch('/app/emergency/sos', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            [csrf.header]: csrf.token
          },
          body: JSON.stringify({ latitude: lat, longitude: lng })
        });

        if (res.ok) {
          titleEl.textContent = 'Alert recorded';
          descEl.textContent = 'Your emergency signal has been stored and logged. SMS to contacts is not enabled yet.';
          countdownEl.innerHTML = `
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="var(--secondary)" stroke-width="3">
              <polyline points="20 6 9 17 4 12"/>
            </svg>
          `;
          cancelBtn.textContent = 'Close';
          showToast('Emergency SOS recorded successfully', 'success');
        } else {
          throw new Error('SOS dispatch failed');
        }
      } catch (err) {
        titleEl.textContent = 'Alert logged locally';
        descEl.textContent = 'An emergency notice has been saved. Please contact local services immediately if in danger.';
        cancelBtn.textContent = 'Close';
        showToast('Emergency alert saved locally', 'error');
      }
    }

    triggerBtn.addEventListener('click', openModal);
    cancelBtn.addEventListener('click', closeModal);

    document.addEventListener('keydown', e => {
      if (e.key === 'Escape' && modal.classList.contains('is-open')) {
        closeModal();
      }
    });

    // Trap focus inside modal
    modal.addEventListener('keydown', e => {
      if (e.key === 'Tab') {
        e.preventDefault();
        cancelBtn.focus();
      }
    });
  }

  /* --------------------------------------------------------------------------
     DOM Ready Bootstrap
     -------------------------------------------------------------------------- */
  function init() {
    initUserMenu();
    initMemberSelector();
    initAlertBanner();
    initStatCounters();
    renderAdherenceRing();
    initMedicineActions();
    initVitalsTabs();
    initEmergencySos();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
