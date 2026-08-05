/**
 * payments.js — Displays payment records.
 *
 * Because the backend has no "get all payments" endpoint, this page:
 *   1. Fetches all accounts → for each account calls
 *      GET /payments/account/{accountId} and de-duplicates results.
 *   2. Also supports direct lookup via:
 *      GET /payments/{paymentId}
 *      GET /payments/account/{accountId}
 */

// ── Render a payments table ──────────────────────────────────────────────────
function renderPaymentsTable(payments, titleText) {
  if (titleText) document.getElementById('results-title').textContent = titleText;

  if (!payments || payments.length === 0) {
    showEmpty('payments-container', 'No payments found', 'Try a different search or create a new payment.');
    return;
  }

  document.getElementById('payments-container').innerHTML = `
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
    </div>
    <div style="padding:10px 0 0;font-size:.8rem;color:var(--gray-400)">
      ${payments.length} payment(s)
    </div>`;
}

// ── Load all payments (sampled across accounts) ──────────────────────────────
async function loadAllPayments() {
  showLoading('payments-container');

  let accounts = [];
  try {
    accounts = await getAllAccounts();
  } catch (err) {
    showError('payments-container', getErrorMessage(err));
    return;
  }

  const seen = new Set();
  const allPayments = [];

  for (const acc of accounts) {
    try {
      const payments = await getPaymentsByAccountId(acc.account_id);
      for (const p of payments) {
        if (!seen.has(p.payment_id)) {
          seen.add(p.payment_id);
          allPayments.push(p);
        }
      }
    } catch (_) {
      // Skip accounts with no payments silently
    }
  }

  // Sort newest first by payment_id
  allPayments.sort((a, b) => b.payment_id - a.payment_id);
  renderPaymentsTable(allPayments, `All Payments (${allPayments.length} found across ${accounts.length} accounts)`);
}

// ── Lookup by payment ID ─────────────────────────────────────────────────────
document.getElementById('lookup-by-id-btn').addEventListener('click', async () => {
  const paymentId = document.getElementById('lookup-payment-id').value.trim();
  if (!paymentId) { showToast('Please enter a payment ID', 'info'); return; }

  showLoading('payments-container');
  try {
    const payment = await getPaymentById(paymentId);
    renderPaymentsTable([payment], `Payment #${paymentId}`);
  } catch (err) {
    showError('payments-container', getErrorMessage(err));
    showToast(getErrorMessage(err), 'error');
  }
});

// ── Lookup by payment reference ──────────────────────────────────────────────
document.getElementById('lookup-by-reference-btn').addEventListener('click', async () => {
  const paymentReference = document.getElementById('lookup-payment-reference').value.trim();
  if (!paymentReference) { showToast('Please enter a payment reference', 'info'); return; }

  showLoading('payments-container');
  try {
    const payment = await getPaymentByReference(paymentReference);
    renderPaymentsTable([payment], `Payment Ref: ${paymentReference}`);
  } catch (err) {
    showError('payments-container', getErrorMessage(err));
    showToast(getErrorMessage(err), 'error');
  }
});

// ── Lookup by account ID ─────────────────────────────────────────────────────
document.getElementById('lookup-by-account-btn').addEventListener('click', async () => {
  const accountId = document.getElementById('lookup-account-id').value.trim();
  if (!accountId) { showToast('Please enter an account ID', 'info'); return; }

  showLoading('payments-container');
  try {
    const payments = await getPaymentsByAccountId(accountId);
    const sorted = [...payments].sort((a, b) => b.payment_id - a.payment_id);
    renderPaymentsTable(sorted, `Payments for Account #${accountId}`);
  } catch (err) {
    showError('payments-container', getErrorMessage(err));
    showToast(getErrorMessage(err), 'error');
  }
});

// ── Refresh ──────────────────────────────────────────────────────────────────
document.getElementById('refresh-btn').addEventListener('click', loadAllPayments);

// ── Enter key support for lookups ────────────────────────────────────────────
document.getElementById('lookup-payment-id').addEventListener('keydown', e => {
  if (e.key === 'Enter') document.getElementById('lookup-by-id-btn').click();
});
document.getElementById('lookup-payment-reference').addEventListener('keydown', e => {
  if (e.key === 'Enter') document.getElementById('lookup-by-reference-btn').click();
});
document.getElementById('lookup-account-id').addEventListener('keydown', e => {
  if (e.key === 'Enter') document.getElementById('lookup-by-account-btn').click();
});

// Initial load
loadAllPayments();

