/**
 * account-details.js — Loads a single account via GET /account/{accountId}
 * and its payment history via GET /payments/account/{accountId}.
 *
 * Reads ?id=<accountId> from the URL.
 */

(async function loadAccountDetails() {
  const accountId = getQueryParam('id');

  if (!accountId) {
    showError('account-detail-container', 'No account ID provided. Use ?id=<accountId> in the URL.');
    showEmpty('account-payments-container', 'N/A');
    return;
  }

  document.getElementById('breadcrumb-id').textContent = `Account #${accountId}`;

  // ── Update the "New Payment" link to include fromAccountId ────────────────
  document.getElementById('new-payment-link').href = `create-payment.html?fromAccountId=${encodeURIComponent(accountId)}`;

  // ── Load account ──────────────────────────────────────────────────────────
  let account;
  try {
    account = await getAccountById(accountId);

    if (!account) {
      showError('account-detail-container', `Account #${accountId} not found.`);
      showEmpty('account-payments-container', 'N/A');
      return;
    }

    // Update page title
    document.title = `${account.account_holder_name} — FlowState`;
    document.getElementById('breadcrumb-id').textContent = account.account_holder_name;

    // Status badge
    document.getElementById('account-status-badge').innerHTML =
      `<span class="${statusBadgeClass(account.status)}">${escapeHtml(account.status)}</span>`;

    // Render detail rows
    document.getElementById('account-detail-container').innerHTML = `
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:0">
        ${detailRow('Account ID',       account.account_id)}
        ${detailRow('Account Number',   `<code>${escapeHtml(account.account_number)}</code>`)}
        ${detailRow('Holder Name',      account.account_holder_name)}
        ${detailRow('Email',            account.email || '—')}
        ${detailRow('Phone',            account.phone_number || '—')}
        ${detailRow('Balance',          `<strong>${formatCurrency(account.balance, account.currency)}</strong>`)}
        ${detailRow('Currency',         account.currency)}
        ${detailRow('Status',           `<span class="${statusBadgeClass(account.status)}">${escapeHtml(account.status)}</span>`)}
        ${detailRow('Version',          account.version)}
        ${detailRow('Created At',       formatDate(account.created_at))}
        ${detailRow('Updated At',       formatDate(account.updated_at))}
      </div>`;
  } catch (err) {
    showError('account-detail-container', getErrorMessage(err));
    showEmpty('account-payments-container', 'Cannot load payments without account data.');
    showEmpty('account-audit-container', 'Cannot load audit trail without account data.');
    return;
  }

  // ── Load payments for this account ───────────────────────────────────────
  try {
    const payments = await getPaymentsByAccountId(accountId);

    if (!payments || payments.length === 0) {
      showEmpty('account-payments-container', 'No payments for this account', 'Payments will appear here once they are created.');
    } else {
      // Resolve account numbers for all unique account IDs in this payment list
      const accountNumberMap = new Map();
      const uniqueIds = [...new Set(payments.flatMap(p => [p.source_account_id, p.destination_account_id]))];
      await Promise.all(uniqueIds.map(async id => {
        try {
          const acc = await getAccountById(id);
          accountNumberMap.set(String(id), acc && acc.account_number ? acc.account_number : `#${id}`);
        } catch (_) {
          accountNumberMap.set(String(id), `#${id}`);
        }
      }));

      const getAccNum = id => accountNumberMap.get(String(id)) || `#${id}`;

      // Render filter bar
      const allStatuses = [...new Set(payments.map(p => p.status).filter(Boolean))].sort();
      document.getElementById('account-payments-container').innerHTML = `
        <div style="display:grid;grid-template-columns:1fr 1fr 1fr 1fr auto;gap:10px;margin-bottom:16px;align-items:flex-end">
          <div>
            <label style="font-size:.75rem;font-weight:700;text-transform:uppercase;color:var(--gray-500);letter-spacing:.04em;display:block;margin-bottom:4px">Payment ID</label>
            <input type="number" id="filter-payment-id" placeholder="e.g. 5" min="1" style="width:100%" />
          </div>
          <div>
            <label style="font-size:.75rem;font-weight:700;text-transform:uppercase;color:var(--gray-500);letter-spacing:.04em;display:block;margin-bottom:4px">Reference</label>
            <input type="text" id="filter-reference" placeholder="e.g. PAY000001" style="width:100%" />
          </div>
          <div>
            <label style="font-size:.75rem;font-weight:700;text-transform:uppercase;color:var(--gray-500);letter-spacing:.04em;display:block;margin-bottom:4px">Direction</label>
            <select id="filter-direction" style="width:100%">
              <option value="">All</option>
              <option value="Debited">Debited</option>
              <option value="Credited">Credited</option>
            </select>
          </div>
          <div>
            <label style="font-size:.75rem;font-weight:700;text-transform:uppercase;color:var(--gray-500);letter-spacing:.04em;display:block;margin-bottom:4px">Status</label>
            <select id="filter-status" style="width:100%">
              <option value="">All</option>
              ${allStatuses.map(s => `<option value="${escapeHtml(s)}">${escapeHtml(s)}</option>`).join('')}
            </select>
          </div>
          <button class="btn btn-secondary btn-sm" id="clear-filters-btn" style="align-self:flex-end">Clear</button>
        </div>
        <div id="payments-table-container"></div>`;

      const sorted = [...payments].sort((a, b) => b.payment_id - a.payment_id);
      const visiblePayments = sorted.filter(p => {
        const isCreditedForCurrent = String(p.destination_account_id) === String(accountId);
        const isFailed = String(p.status || '').toUpperCase() === 'FAILED';
        return !(isCreditedForCurrent && isFailed);
      });

      function applyFilters() {
        const idFilter = document.getElementById('filter-payment-id').value.trim();
        const refFilter = document.getElementById('filter-reference').value.trim().toLowerCase();
        const dirFilter = document.getElementById('filter-direction').value;
        const statusFilter = document.getElementById('filter-status').value;

        const filtered = visiblePayments.filter(p => {
          if (idFilter && String(p.payment_id) !== idFilter) return false;
          if (refFilter && !(p.payment_reference || '').toLowerCase().includes(refFilter)) return false;
          if (dirFilter) {
            const dir = String(p.source_account_id) === String(accountId) ? 'Debited'
              : String(p.destination_account_id) === String(accountId) ? 'Credited' : '';
            if (dir !== dirFilter) return false;
          }
          if (statusFilter && p.status !== statusFilter) return false;
          return true;
        });

        const tableContainer = document.getElementById('payments-table-container');
        if (!filtered.length) {
          tableContainer.innerHTML = '<div class="alert alert-info">No payments match your filters.</div>';
          return;
        }

        tableContainer.innerHTML = `
          <div class="table-wrapper">
            <table>
              <thead><tr>
                <th>ID</th><th>Reference</th><th>From Acc</th><th>To Acc</th><th>Direction</th>
                <th>Amount</th><th>Currency</th><th>Status</th><th></th>
              </tr></thead>
              <tbody>
                ${filtered.map(p => {
                  const direction = String(p.source_account_id) === String(accountId) ? 'Debited'
                    : String(p.destination_account_id) === String(accountId) ? 'Credited' : '—';
                  return `
                    <tr>
                      <td>${escapeHtml(String(p.payment_id))}</td>
                      <td><code style="font-size:.78rem">${escapeHtml(p.payment_reference || '—')}</code></td>
                      <td><a href="account-details.html?id=${p.source_account_id}">${escapeHtml(getAccNum(p.source_account_id))}</a></td>
                      <td><a href="account-details.html?id=${p.destination_account_id}">${escapeHtml(getAccNum(p.destination_account_id))}</a></td>
                      <td><span class="badge">${escapeHtml(direction)}</span></td>
                      <td><strong>${formatCurrency(p.amount, p.currency)}</strong></td>
                      <td>${escapeHtml(p.currency)}</td>
                      <td><span class="${statusBadgeClass(p.status)}">${escapeHtml(p.status)}</span></td>
                      <td><a href="payment-details.html?id=${p.payment_id}" class="btn btn-secondary btn-sm">Details</a></td>
                    </tr>`;
                }).join('')}
              </tbody>
            </table>
          </div>
          <div style="padding:10px 0 0;font-size:.8rem;color:var(--gray-400)">${filtered.length} of ${visiblePayments.length} payment(s)</div>`;
      }

      applyFilters();

      ['filter-payment-id', 'filter-reference', 'filter-direction', 'filter-status'].forEach(id => {
        const el = document.getElementById(id);
        el.addEventListener('input', applyFilters);
        el.addEventListener('change', applyFilters);
      });

      document.getElementById('clear-filters-btn').addEventListener('click', () => {
        document.getElementById('filter-payment-id').value = '';
        document.getElementById('filter-reference').value = '';
        document.getElementById('filter-direction').value = '';
        document.getElementById('filter-status').value = '';
        applyFilters();
      });
    }
  } catch (err) {
    document.getElementById('account-payments-container').innerHTML =
      `<div class="alert alert-warning">Could not load payments: ${escapeHtml(getErrorMessage(err))}</div>`;
  }

  // ── Load account audit trail ──────────────────────────────────────────────
  try {
    const auditRecords = await getAccountHistory(accountId);

    if (!auditRecords || auditRecords.length === 0) {
      showEmpty('account-audit-container', 'No audit records yet', 'Account activity will appear here.');
      return;
    }

    document.getElementById('account-audit-container').innerHTML = `
      <div class="table-wrapper">
        <table>
          <thead><tr>
            <th>Time</th><th>Event</th><th>Message</th>
          </tr></thead>
          <tbody>
            ${auditRecords.map(r => `
              <tr>
                <td style="font-size:.82rem;color:var(--gray-500)">${formatDate(r.created_at)}</td>
                <td><span class="badge">${escapeHtml(r.event_type || 'EVENT')}</span></td>
                <td>${escapeHtml(r.message || '—')}</td>
              </tr>`).join('')}
          </tbody>
        </table>
      </div>`;
  } catch (err) {
    document.getElementById('account-audit-container').innerHTML =
      `<div class="alert alert-warning">Could not load audit trail: ${escapeHtml(getErrorMessage(err))}</div>`;
  }
})();

// ── Helper: single detail row ────────────────────────────────────────────────
function detailRow(label, value) {
  return `
    <div class="detail-row" style="grid-column:1/-1">
      <div class="detail-label">${escapeHtml(label)}</div>
      <div class="detail-value">${value}</div>
    </div>`;
}

