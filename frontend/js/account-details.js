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
      return;
    }

    document.getElementById('account-payments-container').innerHTML = `
      <div class="table-wrapper">
        <table>
          <thead><tr>
            <th>ID</th><th>Reference</th><th>From Acc</th><th>To Acc</th>
            <th>Amount</th><th>Currency</th><th>Status</th><th>Retries</th><th></th>
          </tr></thead>
          <tbody>
            ${payments.map(p => `
              <tr>
                <td>${escapeHtml(String(p.payment_id))}</td>
                <td><code style="font-size:.78rem">${escapeHtml(p.payment_reference || '—')}</code></td>
                <td>${escapeHtml(String(p.source_account_id))}</td>
                <td>${escapeHtml(String(p.destination_account_id))}</td>
                <td><strong>${formatCurrency(p.amount, p.currency)}</strong></td>
                <td>${escapeHtml(p.currency)}</td>
                <td><span class="${statusBadgeClass(p.status)}">${escapeHtml(p.status)}</span></td>
                <td>${escapeHtml(String(p.retry_count ?? 0))}</td>
                <td><a href="payment-details.html?id=${p.payment_id}" class="btn btn-secondary btn-sm">Details</a></td>
              </tr>`).join('')}
          </tbody>
        </table>
      </div>`;
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

