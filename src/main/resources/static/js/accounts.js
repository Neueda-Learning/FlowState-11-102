/**
 * accounts.js — Loads and renders all accounts from GET /account/
 * Supports client-side search and status filtering.
 */

let allAccounts = [];

async function loadAccounts() {
  const container = document.getElementById('accounts-container');
  container.innerHTML = '<div class="spinner-wrap"><div class="spinner"></div></div>';

  try {
    allAccounts = await getAllAccounts();
    renderAccounts(allAccounts);
  } catch (err) {
    container.innerHTML = `<div class="alert alert-danger">${escapeHtml(getErrorMessage(err))}</div>`;
  }
}

function renderAccounts(accounts) {
  const container = document.getElementById('accounts-container');

  if (!accounts || accounts.length === 0) {
    showEmpty('accounts-container', 'No accounts found', 'The backend returned an empty list.');
    return;
  }

  container.innerHTML = `
    <div class="table-wrapper">
      <table>
        <thead><tr>
          <th>ID</th>
          <th>Account Number</th>
          <th>Holder Name</th>
          <th>Email</th>
          <th>Phone</th>
          <th>Balance</th>
          <th>Currency</th>
          <th>Status</th>
          <th>Created At</th>
          <th></th>
        </tr></thead>
        <tbody>
          ${accounts.map(a => `
            <tr>
              <td>${escapeHtml(String(a.account_id))}</td>
              <td><code>${escapeHtml(a.account_number)}</code></td>
              <td>${escapeHtml(a.account_holder_name)}</td>
              <td>${escapeHtml(a.email || '—')}</td>
              <td>${escapeHtml(a.phone_number || '—')}</td>
              <td><strong>${formatCurrency(a.balance, a.currency)}</strong></td>
              <td>${escapeHtml(a.currency)}</td>
              <td><span class="${statusBadgeClass(a.status)}">${escapeHtml(a.status)}</span></td>
              <td style="font-size:.8rem;color:var(--gray-400)">${formatDate(a.created_at)}</td>
              <td>
                <a href="account-details.html?id=${a.account_id}" class="btn btn-secondary btn-sm">Details</a>
              </td>
            </tr>`).join('')}
        </tbody>
      </table>
    </div>
    <div style="padding:12px 0 0;font-size:.8rem;color:var(--gray-400)">
      Showing ${accounts.length} of ${allAccounts.length} accounts
    </div>`;
}

// ── Search & filter ──────────────────────────────────────────────────────────
function applyFilters() {
  const query  = document.getElementById('search-input').value.toLowerCase().trim();
  const status = document.getElementById('status-filter').value;

  let filtered = allAccounts;

  if (query) {
    filtered = filtered.filter(a =>
      (a.account_holder_name || '').toLowerCase().includes(query) ||
      (a.account_number      || '').toLowerCase().includes(query) ||
      (a.email               || '').toLowerCase().includes(query) ||
      (a.status              || '').toLowerCase().includes(query)
    );
  }

  if (status) {
    filtered = filtered.filter(a => a.status === status);
  }

  renderAccounts(filtered);
}

document.getElementById('search-input').addEventListener('input', applyFilters);
document.getElementById('status-filter').addEventListener('change', applyFilters);
document.getElementById('refresh-btn').addEventListener('click', loadAccounts);

// Initial load
loadAccounts();

