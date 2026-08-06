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

const CANCEL_WINDOW_MS = 10_000;
const POLL_INTERVAL_MS = 1_500;
const TERMINAL_STATUSES = ['COMPLETED', 'FAILED'];

let currentPaymentId = null;
let latestPayment = null;
let pollingTimer = null;
let cancelCountdownTimer = null;
let cancelDeadlineMs = null;
let completionToastShown = false;
const accountNumberCache = new Map();

(async function loadPaymentDetails() {
  currentPaymentId = getQueryParam('id');

  if (!currentPaymentId) {
    showError('payment-detail-container', 'No payment ID provided. Use ?id=<paymentId> in the URL.');
    showEmpty('payment-history-container', 'N/A', 'A payment ID is required to load the timeline.');
    return;
  }

  document.getElementById('breadcrumb-id').textContent = `Payment #${currentPaymentId}`;

  await refreshPaymentAndHistory();
  if (latestPayment) {
    startPollingIfNeeded();
  }
})();

async function refreshPaymentAndHistory() {
  try {
    const payment = await getPaymentById(currentPaymentId);
    if (!payment) {
      showError('payment-detail-container', `Payment #${currentPaymentId} not found.`);
      showEmpty('payment-history-container', 'Timeline unavailable', 'Payment was not found.');
      stopPolling();
      return;
    }

    latestPayment = payment;

    const [sourceAccountNumber, destinationAccountNumber] = await Promise.all([
      resolveAccountNumberById(payment.source_account_id),
      resolveAccountNumberById(payment.destination_account_id),
    ]);

    renderPaymentDetails(payment, sourceAccountNumber, destinationAccountNumber);

    try {
      const history = await getPaymentHistory(payment.payment_id);
      deriveCancelDeadlineFromHistory(history);
      renderPaymentHistory(history);
    } catch (err) {
      document.getElementById('payment-history-container').innerHTML =
        `<div class="alert alert-warning">Could not load timeline: ${escapeHtml(getErrorMessage(err))}</div>`;
    }

    renderPaymentActions(payment);

    if (payment.status === 'COMPLETED' && !completionToastShown) {
      completionToastShown = true;
      showToast(`Payment #${payment.payment_id} completed successfully.`, 'success');
    }

    if (TERMINAL_STATUSES.includes(payment.status)) {
      stopPolling();
    }
  } catch (err) {
    showError('payment-detail-container', getErrorMessage(err));
    showEmpty('payment-history-container', 'Timeline unavailable', 'Could not load payment before fetching its history.');
    stopPolling();
  }
}

function renderPaymentDetails(payment, sourceAccountNumber, destinationAccountNumber) {
  document.title = `Payment #${payment.payment_id} — FlowState`;
  document.getElementById('breadcrumb-id').textContent = `Payment #${payment.payment_id}`;

  document.getElementById('payment-status-badge').innerHTML =
    `<span class="${statusBadgeClass(payment.status)}">${escapeHtml(payment.status)}</span>`;

  document.getElementById('payment-detail-container').innerHTML = `
    <div style="max-width:680px">
      ${pRow('Payment ID', payment.payment_id)}
      ${pRow('Payment Reference', `<code>${escapeHtml(payment.payment_reference || '—')}</code>`)}
      ${pRow('Source Account Number', `<a href="account-details.html?id=${payment.source_account_id}">${escapeHtml(sourceAccountNumber || `#${String(payment.source_account_id)}`)}</a>`)}
      ${pRow('Destination Account Number', `<a href="account-details.html?id=${payment.destination_account_id}">${escapeHtml(destinationAccountNumber || `#${String(payment.destination_account_id)}`)}</a>`)}
      ${pRow('Amount', `<strong>${formatCurrency(payment.amount, payment.currency)}</strong>`)}
      ${pRow('Currency', payment.currency)}
      ${pRow('Status', `<span class="${statusBadgeClass(payment.status)}">${escapeHtml(payment.status)}</span>`)}
      ${pRow('Failure Reason', payment.failure_reason
        ? `<span style="color:var(--danger)">${escapeHtml(payment.failure_reason)}</span>`
        : '—')}
      ${pRow('Retry Count', payment.retry_count ?? 0)}
      ${pRow('Message', payment.message ? escapeHtml(payment.message) : '—')}
    </div>`;
}

async function resolveAccountNumberById(accountId) {
  if (accountId == null) return null;

  if (accountNumberCache.has(accountId)) {
    return accountNumberCache.get(accountId);
  }

  try {
    const account = await getAccountById(accountId);
    const accountNumber = account && account.account_number ? String(account.account_number) : null;
    accountNumberCache.set(accountId, accountNumber);
    return accountNumber;
  } catch (_) {
    accountNumberCache.set(accountId, null);
    return null;
  }
}

function renderPaymentActions(payment) {
  const actionsEl = document.getElementById('payment-actions');
  const cancellableStatus = ['CREATED', 'VALIDATED'].includes(payment.status);
  const canRetry = payment.status === 'FAILED';
  const remainingMs = getCancelRemainingMs();
  const cancelAllowed = cancellableStatus && remainingMs > 0;

  actionsEl.style.display = 'flex';
  actionsEl.innerHTML = `
    <a href="payments.html" class="btn btn-secondary">← All Payments</a>
    ${canRetry ? `<a href="${buildRetryPaymentUrl(payment)}" class="btn btn-primary">Retry Payment</a>` : ''}
    ${cancellableStatus ? `<button class="btn btn-danger" id="cancel-btn" ${cancelAllowed ? '' : 'disabled'}></button>` : ''}
  `;

  if (!cancellableStatus) {
    stopCancelCountdown();
    return;
  }

  const btn = document.getElementById('cancel-btn');
  updateCancelButtonLabel(btn, remainingMs);

  if (cancelAllowed) {
    btn.addEventListener('click', () => cancelThisPayment(payment.payment_id));
    startCancelCountdown();
  } else {
    stopCancelCountdown();
  }
}

function deriveCancelDeadlineFromHistory(history) {
  if (cancelDeadlineMs != null) return;

  const createdEvent = (history || []).find(event => event.status === 'CREATED' && event.created_at);
  if (createdEvent && createdEvent.created_at) {
    const createdAtMs = new Date(createdEvent.created_at).getTime();
    if (!Number.isNaN(createdAtMs)) {
      cancelDeadlineMs = createdAtMs + CANCEL_WINDOW_MS;
      return;
    }
  }

  cancelDeadlineMs = Date.now() + CANCEL_WINDOW_MS;
}

function getCancelRemainingMs() {
  if (cancelDeadlineMs == null) {
    cancelDeadlineMs = Date.now() + CANCEL_WINDOW_MS;
  }
  return Math.max(0, cancelDeadlineMs - Date.now());
}

function updateCancelButtonLabel(btn, remainingMs) {
  if (!btn) return;

  if (remainingMs <= 0) {
    btn.textContent = 'Cancel Window Expired';
    btn.disabled = true;
    return;
  }

  const secondsLeft = Math.ceil(remainingMs / 1000);
  btn.textContent = `Cancel Payment (${secondsLeft}s)`;
}

function startCancelCountdown() {
  if (cancelCountdownTimer != null) return;

  cancelCountdownTimer = window.setInterval(() => {
    if (!latestPayment || !['CREATED', 'VALIDATED'].includes(latestPayment.status)) {
      stopCancelCountdown();
      return;
    }

    const btn = document.getElementById('cancel-btn');
    if (!btn) {
      stopCancelCountdown();
      return;
    }

    const remainingMs = getCancelRemainingMs();
    updateCancelButtonLabel(btn, remainingMs);
    if (remainingMs <= 0) {
      stopCancelCountdown();
      showToast('Cancel window has expired for this payment.', 'info');
    }
  }, 500);
}

function stopCancelCountdown() {
  if (cancelCountdownTimer != null) {
    window.clearInterval(cancelCountdownTimer);
    cancelCountdownTimer = null;
  }
}

function startPollingIfNeeded() {
  if (!latestPayment || TERMINAL_STATUSES.includes(latestPayment.status) || pollingTimer != null) {
    return;
  }

  pollingTimer = window.setInterval(async () => {
    try {
      await refreshPaymentAndHistory();
    } catch (_) {
      // Non-blocking; next polling tick retries.
    }
  }, POLL_INTERVAL_MS);
}

function stopPolling() {
  if (pollingTimer != null) {
    window.clearInterval(pollingTimer);
    pollingTimer = null;
  }
}

function buildRetryPaymentUrl(payment) {
  const params = new URLSearchParams({
    retryOf: String(payment.payment_id),
    sourceAccountId: String(payment.source_account_id),
    destinationAccountId: String(payment.destination_account_id),
    amount: String(payment.amount ?? ''),
    currency: String(payment.currency ?? ''),
  });

  return `create-payment.html?${params.toString()}`;
}

// ── Cancel handler ─────────────────────────────────────────────────────────
async function cancelThisPayment(paymentId) {
  if (!confirm(`Cancel payment #${paymentId}? This cannot be undone.`)) return;

  const btn = document.getElementById('cancel-btn');
  if (!btn || btn.disabled) return;

  btn.disabled = true;
  btn.textContent = 'Cancelling…';

  try {
    // PATCH /payments/{paymentId}/cancel → 204 No Content
    await cancelPayment(paymentId);
    showToast(`Payment #${paymentId} has been cancelled.`, 'success');
    await refreshPaymentAndHistory();
  } catch (err) {
    showToast(`Cancel failed: ${getErrorMessage(err)}`, 'error');
    if (getCancelRemainingMs() > 0 && latestPayment && ['CREATED', 'VALIDATED'].includes(latestPayment.status)) {
      btn.disabled = false;
      updateCancelButtonLabel(btn, getCancelRemainingMs());
    } else {
      updateCancelButtonLabel(btn, 0);
    }
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

