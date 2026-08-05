/**
 * payment-details.js — Loads a single payment via GET /payments/{paymentId}
 * and provides a cancel button that calls PATCH /payments/{paymentId}/cancel.
 *
 * Reads ?id=<paymentId> from the URL.
 *
 * PaymentResponse fields:
 *   payment_id, payment_reference, source_account_id, destination_account_id,
 *   amount, currency, status, failure_reason, retry_count, message
 */

(async function loadPaymentDetails() {
  const paymentId = getQueryParam('id');

  if (!paymentId) {
    showError('payment-detail-container', 'No payment ID provided. Use ?id=<paymentId> in the URL.');
    showEmpty('payment-history-container', 'N/A', 'A payment ID is required to load the timeline.');
    return;
  }

  document.getElementById('breadcrumb-id').textContent = `Payment #${paymentId}`;

  let payment;
  try {
    payment = await getPaymentById(paymentId);
  } catch (err) {
    showError('payment-detail-container', getErrorMessage(err));
    showEmpty('payment-history-container', 'Timeline unavailable', 'Could not load payment before fetching its history.');
    return;
  }

  if (!payment) {
    showError('payment-detail-container', `Payment #${paymentId} not found.`);
    showEmpty('payment-history-container', 'Timeline unavailable', 'Payment was not found.');
    return;
  }

  // Update page title & breadcrumb
  document.title = `Payment #${payment.payment_id} — FlowState`;
  document.getElementById('breadcrumb-id').textContent = `Payment #${payment.payment_id}`;

  // Status badge
  document.getElementById('payment-status-badge').innerHTML =
    `<span class="${statusBadgeClass(payment.status)}">${escapeHtml(payment.status)}</span>`;

  // ── Render detail rows ─────────────────────────────────────────────────────
  document.getElementById('payment-detail-container').innerHTML = `
    <div style="max-width:680px">
      ${pRow('Payment ID',           payment.payment_id)}
      ${pRow('Payment Reference',    `<code>${escapeHtml(payment.payment_reference || '—')}</code>`)}
      ${pRow('Source Account ID',    `<a href="account-details.html?id=${payment.source_account_id}">#${escapeHtml(String(payment.source_account_id))}</a>`)}
      ${pRow('Destination Account ID', `<a href="account-details.html?id=${payment.destination_account_id}">#${escapeHtml(String(payment.destination_account_id))}</a>`)}
      ${pRow('Amount',               `<strong>${formatCurrency(payment.amount, payment.currency)}</strong>`)}
      ${pRow('Currency',             payment.currency)}
      ${pRow('Status',               `<span class="${statusBadgeClass(payment.status)}">${escapeHtml(payment.status)}</span>`)}
      ${pRow('Failure Reason',       payment.failure_reason
                                       ? `<span style="color:var(--danger)">${escapeHtml(payment.failure_reason)}</span>`
                                       : '—')}
      ${pRow('Retry Count',          payment.retry_count ?? 0)}
      ${pRow('Message',              payment.message ? escapeHtml(payment.message) : '—')}
    </div>`;

  // ── Cancel button (only for cancellable statuses) ─────────────────────────
  const actionsEl = document.getElementById('payment-actions');
  const cancellable = ['CREATED', 'VALIDATED'].includes(payment.status);

  actionsEl.style.display = 'flex';
  actionsEl.innerHTML = `
    <a href="payments.html" class="btn btn-secondary">← All Payments</a>
    ${cancellable ? `<button class="btn btn-danger" id="cancel-btn">Cancel Payment</button>` : ''}
  `;

  if (cancellable) {
    document.getElementById('cancel-btn').addEventListener('click', () => cancelThisPayment(payment.payment_id));
  }

  try {
    const history = await getPaymentHistory(payment.payment_id);
    renderPaymentHistory(history);
  } catch (err) {
    document.getElementById('payment-history-container').innerHTML =
      `<div class="alert alert-warning">Could not load timeline: ${escapeHtml(getErrorMessage(err))}</div>`;
  }
})();

// ── Cancel handler ─────────────────────────────────────────────────────────
async function cancelThisPayment(paymentId) {
  if (!confirm(`Cancel payment #${paymentId}? This cannot be undone.`)) return;

  const btn = document.getElementById('cancel-btn');
  btn.disabled = true;
  btn.textContent = 'Cancelling…';

  try {
    // PATCH /payments/{paymentId}/cancel → 204 No Content
    await cancelPayment(paymentId);
    showToast(`Payment #${paymentId} has been cancelled.`, 'success');

    // Refresh the page to show updated status
    setTimeout(() => location.reload(), 1200);
  } catch (err) {
    showToast(`Cancel failed: ${getErrorMessage(err)}`, 'error');
    btn.disabled = false;
    btn.textContent = 'Cancel Payment';
  }
}

// ── Detail row helper ────────────────────────────────────────────────────────
function pRow(label, value) {
  return `
    <div style="display:grid;grid-template-columns:200px 1fr;gap:16px;padding:11px 0;border-bottom:1px solid var(--gray-100)">
      <div style="font-size:.78rem;font-weight:600;color:var(--gray-500);text-transform:uppercase;letter-spacing:.04em;align-self:center">
        ${escapeHtml(label)}
      </div>
      <div style="color:var(--gray-800);font-size:.9rem;word-break:break-all">${value}</div>
    </div>`;
}

function renderPaymentHistory(history) {
  if (!history || history.length === 0) {
    showEmpty('payment-history-container', 'No timeline events', 'Processing events will appear once available.');
    return;
  }

  document.getElementById('payment-history-container').innerHTML = `
    <div class="table-wrapper">
      <table>
        <thead><tr>
          <th>Time</th><th>Status</th><th>Message</th>
        </tr></thead>
        <tbody>
          ${history.map(event => `
            <tr>
              <td style="font-size:.82rem;color:var(--gray-500)">${formatDate(event.created_at)}</td>
              <td><span class="${statusBadgeClass(event.status)}">${escapeHtml(event.status || 'UNKNOWN')}</span></td>
              <td>${escapeHtml(event.message || '—')}</td>
            </tr>
          `).join('')}
        </tbody>
      </table>
    </div>`;
}

