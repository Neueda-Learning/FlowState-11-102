/**
 * sidebar.js — Injects the shared sidebar into every app page.
 * Include this script BEFORE other page scripts.
 */

(function injectSidebar() {
  const sidebarHTML = `
  <aside class="sidebar" id="sidebar">
    <div class="sidebar-logo">
      <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#2563eb" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
        <rect x="2" y="5" width="20" height="14" rx="3"/>
        <path d="M2 10h20"/>
      </svg>
      <div class="brand">Flow<span>State</span></div>
    </div>

    <nav class="sidebar-nav">
      <div class="nav-section">Main</div>

      <a class="nav-item" href="dashboard.html">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/>
          <rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/>
        </svg>
        Dashboard
      </a>

      <div class="nav-section">Accounts</div>

      <a class="nav-item" href="accounts.html">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/>
          <circle cx="9" cy="7" r="4"/>
          <path d="M23 21v-2a4 4 0 00-3-3.87M16 3.13a4 4 0 010 7.75"/>
        </svg>
        All Accounts
      </a>

      <div class="nav-section">Payments</div>

      <a class="nav-item" href="create-payment.html">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="16"/><line x1="8" y1="12" x2="16" y2="12"/>
        </svg>
        New Payment
      </a>

      <a class="nav-item" href="payments.html">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect x="2" y="5" width="20" height="14" rx="3"/><path d="M2 10h20"/>
        </svg>
        All Payments
      </a>

      <a class="nav-item" href="failed-payments.html">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="10"/>
          <line x1="12" y1="8" x2="12" y2="12"/>
          <line x1="12" y1="16" x2="12.01" y2="16"/>
        </svg>
        Failed Payments
      </a>
    </nav>

    <div class="sidebar-footer">
      FlowState v1.0 &nbsp;·&nbsp;
      <a href="index.html" style="color:var(--gray-500)">Logout</a>
    </div>
  </aside>`;

  // Insert sidebar before the first child of body
  document.body.insertAdjacentHTML('afterbegin', sidebarHTML);

  // Highlight the active nav item based on the current page filename
  const currentPage = location.pathname.split('/').pop() || 'dashboard.html';
  document.querySelectorAll('.nav-item').forEach(link => {
    const href = (link.getAttribute('href') || '');
    if (href === currentPage) link.classList.add('active');
  });
})();

