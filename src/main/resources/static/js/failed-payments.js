/**
 * failed-payments.js — Shows only payments with status === 'FAILED'.
 *
 * Strategy: Fetch all accounts, then collect payments per account,
 * de-duplicate by payment_id, and filter to status === 'FAILED'.
 *
 * Displayed fields: payment_id, payment_reference, source_account_id,
 *   destination_account_id, amount, currency, failure_reason, retry_count
 */

async function loadFailedPayments() {
  showLoading('failed-container');
  document.getElementById('failed-title').textContent = 'Loading…';

  let accounts = [];
  try {
    accounts = await getAllAccounts();
  } catch (err) {
    showError('failed-container', getErrorMessage(err));
    document.getElementById('failed-title').textContent = 'Error';
    return;
  }

  const seen    = new Set();
  const all     = [];

  for (const acc of accounts) {
    try {
      const payments = await getPaymentsByAccountId(acc.account_id);
      for (const p of payments) {
        if (!seen.has(p.payment_id)) {
          seen.add(p.payment_id);
          all.push(p);
        }
      }
    } catch (_) { /* skip */ }
  }

  const failed = all.filter(p => p.status === 'FAILED');
  failed.sort((a, b) => b.payment_id - a.payment_id);

  document.getElementById('failed-title').textContent =
    `Failed Payments (${failed.length} of ${all.length} total)`;

  if (failed.length === 0) {
    showEmpty('failed-container', 'No failed payments 🎉', 'All payments have processed without errors.');
    return;
  }

  document.getElementById('failed-container').innerHTML = `
    <div class="table-wrapper">
      <table>
        <thead><tr>
          <th>ID</th><th>Reference</th><th>From Acc</th><th>To Acc</th>
          <th>Amount</th><th>Currency</th><th>Failure Reason</th><th>Retries</th><th></th>
        </tr></thead>
        <tbody>
          ${failed.map(p => `
            <tr>
              <td>${escapeHtml(String(p.payment_id))}</td>
              <td><code style="font-size:.78rem">${escapeHtml(p.payment_reference || '—')}</code></td>
              <td>${escapeHtml(String(p.source_account_id))}</td>
              <td>${escapeHtml(String(p.destination_account_id))}</td>
              <td><strong>${formatCurrency(p.amount, p.currency)}</strong></td>
              <td>${escapeHtml(p.currency)}</td>
              <td style="color:var(--danger);font-size:.82rem;max-width:260px">
                ${escapeHtml(p.failure_reason || '—')}
              </td>
              <td>
                <span style="background:var(--danger-light);color:var(--danger);
                  padding:2px 8px;border-radius:99px;font-size:.78rem;font-weight:600">
                  ${escapeHtml(String(p.retry_count ?? 0))}
                </span>
              </td>
              <td>
                <a href="payment-details.html?id=${p.payment_id}" class="btn btn-secondary btn-sm">Details</a>
              </td>
            </tr>`).join('')}
        </tbody>
      </table>
    </div>
    <div style="padding:10px 0 0;font-size:.8rem;color:var(--gray-400)">
      ${failed.length} failed payment(s) displayed
    </div>`;
}

document.getElementById('refresh-btn').addEventListener('click', loadFailedPayments);

loadFailedPayments();

