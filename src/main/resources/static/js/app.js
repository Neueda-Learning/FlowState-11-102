/**
 * app.js — Shared UI helpers used across all pages
 *  - Toast notifications
 *  - Sidebar active link highlighting
 *  - Common render helpers
 */

// ── Toast Notifications ────────────────────────────────────────────────────
(function initToastContainer() {
  if (!document.getElementById('toast-container')) {
    const el = document.createElement('div');
    el.id = 'toast-container';
    document.body.appendChild(el);
  }
})();

/**
 * Show a brief toast notification.
 * @param {string} message
 * @param {'success'|'error'|'info'} type
 * @param {number} [duration=4000] milliseconds
 */
function showToast(message, type = 'info', duration = 4000) {
  const container = document.getElementById('toast-container');
  const toast = document.createElement('div');
  toast.className = `toast ${type}`;

  const icons = {
    success: '✓',
    error:   '✕',
    info:    'ℹ',
  };

  toast.innerHTML = `<span style="font-size:1.1rem;font-weight:700">${icons[type] || 'ℹ'}</span><span>${escapeHtml(message)}</span>`;
  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transition = 'opacity .3s';
    setTimeout(() => toast.remove(), 320);
  }, duration);
}

// ── Loading helpers ────────────────────────────────────────────────────────
function showLoading(containerId) {
  const el = document.getElementById(containerId);
  if (el) el.innerHTML = '<div class="spinner-wrap"><div class="spinner"></div></div>';
}

function showError(containerId, message) {
  const el = document.getElementById(containerId);
  if (el) el.innerHTML = `<div class="alert alert-danger">${escapeHtml(message)}</div>`;
}

function showEmpty(containerId, title = 'No records found', subtitle = '') {
  const el = document.getElementById(containerId);
  if (el) el.innerHTML = `
    <div class="empty-state">
      <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0H4"/></svg>
      <h3>${escapeHtml(title)}</h3>
      <p>${escapeHtml(subtitle)}</p>
    </div>`;
}

// ── Sanitiser ─────────────────────────────────────────────────────────────
function escapeHtml(str) {
  if (str == null) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

// ── Sidebar active state ───────────────────────────────────────────────────
(function highlightActiveNav() {
  const page = location.pathname.split('/').pop() || 'index.html';
  document.querySelectorAll('.nav-item').forEach(link => {
    const href = link.getAttribute('href') || '';
    if (href && href.includes(page)) link.classList.add('active');
  });
})();

// ── URL query param helper ─────────────────────────────────────────────────
function getQueryParam(name) {
  return new URLSearchParams(location.search).get(name);
}

