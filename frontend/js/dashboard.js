/**
 * dashboard.js — Loads aggregated stats and recent records for the dashboard.
 *
 * Data sources:
 *   GET /account/       → Total accounts + recent account list
 *   GET /payments/account/{id} is too specific; we can't get ALL payments
 *   without knowing every accountId.  Instead we fetch all accounts and
 *   then collect payments per account — or we skip payment totals if there
 *   are no accounts yet.
 *
 * Strategy:
 *   1. Fetch all accounts.
 *   2. For each account fetch their payments (limited to first 3 accounts to
 *      avoid too many requests on the dashboard).
 *   3. Deduplicate payments by payment_id and compute status counts.
 */

(async function loadDashboard() {
  // ── Server status indicator ──────────────────────────────────────────────
  const statusEl = document.getElementById('server-status');

  let accounts = [];
  let allPayments = [];

  // ── 1. Fetch all accounts ────────────────────────────────────────────────
  try {
    accounts = await getAllAccounts();
    statusEl.textContent = 'Online ✓';
    statusEl.style.color = 'var(--success)';
  } catch (err) {
    statusEl.textContent = 'Offline ✗';
    statusEl.style.color = 'var(--danger)';

    const errMsg = getErrorMessage(err);
    // Show error in stats grid
    document.getElementById('stats-grid').innerHTML =
      `<div class="alert alert-danger" style="grid-column:1/-1">${escapeHtml(errMsg)}</div>`;
    document.getElementById('recent-accounts-table').innerHTML =
      `<div class="alert alert-danger">${escapeHtml(errMsg)}</div>`;
    document.getElementById('recent-payments-table').innerHTML =
      `<div class="alert alert-danger">${escapeHtml(errMsg)}</div>`;
    return;
  }

  // ── 2. Fetch payments for up to 5 accounts ───────────────────────────────
  const seen = new Set();
  const sampleAccounts = accounts.slice(0, 5);
  for (const acc of sampleAccounts) {
    try {
      const payments = await getPaymentsByAccountId(acc.account_id);
      for (const p of payments) {
        if (!seen.has(p.payment_id)) {
          seen.add(p.payment_id);
          allPayments.push(p);
        }
      }
    } catch (_) {
      // Payment fetch failure for one account — skip silently
    }
  }

  // ── 3. Compute stats ─────────────────────────────────────────────────────
  const totalAccounts   = accounts.length;
  const totalPayments   = allPayments.length;
  const successPayments = allPayments.filter(p => p.status === 'COMPLETED').length;
  const failedPayments  = allPayments.filter(p => p.status === 'FAILED').length;
  const processingPayments = allPayments.filter(
    p => ['CREATED', 'VALIDATED', 'SENT'].includes(p.status)
  ).length;

  // ── 4. Render stat cards ─────────────────────────────────────────────────
  document.getElementById('stats-grid').innerHTML = `
    ${statCard('blue',   accountsIcon(),   'Total Accounts',     totalAccounts,   'Registered accounts')}
    ${statCard('cyan',   paymentsIcon(),   'Total Payments',     totalPayments,   'Across sampled accounts')}
    ${statCard('green',  checkIcon(),      'Successful',         successPayments, 'COMPLETED payments')}
    ${statCard('red',    alertIcon(),      'Failed',             failedPayments,  'FAILED payments')}
    ${statCard('yellow', clockIcon(),      'Processing',         processingPayments, 'CREATED / VALIDATED / SENT')}
  `;

  // ── 5. Recent accounts table ─────────────────────────────────────────────
  const recentAccounts = accounts.slice(0, 8);
  if (recentAccounts.length === 0) {
    showEmpty('recent-accounts-table', 'No accounts yet', 'Create an account to get started.');
  } else {
    document.getElementById('recent-accounts-table').innerHTML = `
      <table>
        <thead><tr>
          <th>ID</th><th>Account Number</th><th>Holder</th>
          <th>Balance</th><th>Currency</th><th>Status</th><th></th>
        </tr></thead>
        <tbody>
          ${recentAccounts.map(a => `
            <tr>
              <td>${escapeHtml(String(a.account_id))}</td>
              <td><code>${escapeHtml(a.account_number)}</code></td>
              <td>${escapeHtml(a.account_holder_name)}</td>
              <td><strong>${formatCurrency(a.balance, a.currency)}</strong></td>
              <td>${escapeHtml(a.currency)}</td>
              <td><span class="${statusBadgeClass(a.status)}">${escapeHtml(a.status)}</span></td>
              <td><a href="account-details.html?id=${a.account_id}" class="btn btn-secondary btn-sm">Details</a></td>
            </tr>`).join('')}
        </tbody>
      </table>`;
  }

  // ── 6. Recent payments table ─────────────────────────────────────────────
  const recentPayments = allPayments.slice(0, 8);
  if (recentPayments.length === 0) {
    showEmpty('recent-payments-table', 'No payments yet', 'Use "New Payment" to make a transfer.');
  } else {
    document.getElementById('recent-payments-table').innerHTML = `
      <table>
        <thead><tr>
          <th>ID</th><th>Reference</th><th>From</th><th>To</th>
          <th>Amount</th><th>Currency</th><th>Status</th><th></th>
        </tr></thead>
        <tbody>
          ${recentPayments.map(p => `
            <tr>
              <td>${escapeHtml(String(p.payment_id))}</td>
              <td><code style="font-size:.78rem">${escapeHtml(p.payment_reference || '—')}</code></td>
              <td>${escapeHtml(String(p.source_account_id))}</td>
              <td>${escapeHtml(String(p.destination_account_id))}</td>
              <td><strong>${formatCurrency(p.amount, p.currency)}</strong></td>
              <td>${escapeHtml(p.currency)}</td>
              <td><span class="${statusBadgeClass(p.status)}">${escapeHtml(p.status)}</span></td>
              <td><a href="payment-details.html?id=${p.payment_id}" class="btn btn-secondary btn-sm">Details</a></td>
            </tr>`).join('')}
        </tbody>
      </table>`;
  }
})();

// ── Icon helpers ─────────────────────────────────────────────────────────────
function statCard(colorClass, iconSvg, label, value, sub) {
  return `
    <div class="stat-card">
      <div class="stat-icon ${colorClass}">${iconSvg}</div>
      <div class="stat-info">
        <div class="stat-label">${escapeHtml(label)}</div>
        <div class="stat-value">${value}</div>
        <div class="stat-sub">${escapeHtml(sub)}</div>
      </div>
    </div>`;
}
function accountsIcon() { return `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 00-3-3.87M16 3.13a4 4 0 010 7.75"/></svg>`; }
function paymentsIcon() { return `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="5" width="20" height="14" rx="3"/><path d="M2 10h20"/></svg>`; }
function checkIcon()    { return `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>`; }
function alertIcon()    { return `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>`; }
function clockIcon()    { return `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>`; }

