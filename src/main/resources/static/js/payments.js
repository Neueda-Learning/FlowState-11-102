/**
 * payments.js — Displays payment records for one selected account at a time.
 */

const FS_SELECTED_ACCOUNT_KEY = 'fs_selected_account_number';

const paymentsState = {
  accounts: [],
  selectedAccountId: null,
  paymentsByAccount: new Map(),
  activeFilters: { direction: '', status: '' },
};

function getSelectedAccount() {
  return paymentsState.accounts.find(acc => String(acc.account_id) === String(paymentsState.selectedAccountId)) || null;
}

function formatAccountLabel(account) {
  return `${account.account_number} — ${account.account_holder_name} (${formatCurrency(account.balance, account.currency)})`;
}

function sortPaymentsDesc(payments) {
  return [...payments].sort((a, b) => b.payment_id - a.payment_id);
}

function getPaymentDirection(payment, accountId) {
  if (!accountId) return '—';
  if (String(payment.source_account_id) === String(accountId)) return 'Debited';
  if (String(payment.destination_account_id) === String(accountId)) return 'Credited';
  return '—';
}

function shouldHideForSelectedAccount(payment, accountId) {
  const isCreditedForCurrent = String(payment.destination_account_id) === String(accountId);
  const isFailed = String(payment.status || '').toUpperCase() === 'FAILED';
  return isCreditedForCurrent && isFailed;
}

function getAccountNumberFromState(accountId) {
  const acc = paymentsState.accounts.find(a => String(a.account_id) === String(accountId));
  return acc ? acc.account_number : `#${accountId}`;
}

// ── Render a payments table ──────────────────────────────────────────────────
function renderPaymentsTable(payments, titleText, selectedAccountId) {
  if (titleText) document.getElementById('results-title').textContent = titleText;

  if (!payments || payments.length === 0) {
    showEmpty('payments-container', 'No payments found', 'No payments exist for this account yet.');
    return;
  }

  document.getElementById('payments-container').innerHTML = `
    <div class="table-wrapper">
      <table>
        <thead><tr>
          <th>ID</th><th>Reference</th><th>From Acc</th><th>To Acc</th><th>Direction</th>
          <th>Amount</th><th>Currency</th><th>Status</th><th></th>
        </tr></thead>
        <tbody>
          ${payments.map(p => `
            <tr>
              <td>${escapeHtml(String(p.payment_id))}</td>
              <td><code style="font-size:.78rem">${escapeHtml(p.payment_reference || '—')}</code></td>
              <td><a href="account-details.html?id=${p.source_account_id}">${escapeHtml(getAccountNumberFromState(p.source_account_id))}</a></td>
              <td><a href="account-details.html?id=${p.destination_account_id}">${escapeHtml(getAccountNumberFromState(p.destination_account_id))}</a></td>
              <td><span class="badge">${escapeHtml(getPaymentDirection(p, selectedAccountId))}</span></td>
              <td><strong>${formatCurrency(p.amount, p.currency)}</strong></td>
              <td>${escapeHtml(p.currency)}</td>
              <td><span class="${statusBadgeClass(p.status)}">${escapeHtml(p.status)}</span></td>
              <td><a href="payment-details.html?id=${p.payment_id}" class="btn btn-secondary btn-sm">Details</a></td>
            </tr>`).join('')}
        </tbody>
      </table>
    </div>
    <div style="padding:10px 0 0;font-size:.8rem;color:var(--gray-400)">
      ${payments.length} payment(s)
    </div>`;
}

async function loadPaymentsPage() {
  showLoading('payments-container');

  try {
    paymentsState.accounts = await getAllAccounts();
  } catch (err) {
    showError('payments-container', getErrorMessage(err));
    return;
  }

  if (!paymentsState.accounts.length) {
    showEmpty('payments-container', 'No accounts found', 'Create an account first to view payments.');
    return;
  }

  populateAccountSwitcher();
  await loadPaymentsForSelectedAccount();
}

function populateAccountSwitcher() {
  const switcher = document.getElementById('account-switcher');

  switcher.innerHTML = '';
  paymentsState.accounts.forEach(account => {
    const option = new Option(formatAccountLabel(account), String(account.account_id));
    switcher.add(option);
  });

  if (!paymentsState.selectedAccountId) {
    // Restore from localStorage by matching account number → account ID
    const savedNumber = localStorage.getItem(FS_SELECTED_ACCOUNT_KEY);
    if (savedNumber) {
      const savedAccount = paymentsState.accounts.find(a => a.account_number === savedNumber);
      if (savedAccount) paymentsState.selectedAccountId = String(savedAccount.account_id);
    }
    if (!paymentsState.selectedAccountId) {
      paymentsState.selectedAccountId = String(paymentsState.accounts[0].account_id);
    }
  }

  switcher.value = String(paymentsState.selectedAccountId);

  switcher.onchange = async () => {
    paymentsState.selectedAccountId = switcher.value;
    // Save account number to localStorage so new-payment page stays in sync
    const account = paymentsState.accounts.find(a => String(a.account_id) === switcher.value);
    if (account) localStorage.setItem(FS_SELECTED_ACCOUNT_KEY, account.account_number);
    await loadPaymentsForSelectedAccount();
  };
}

async function loadPaymentsForSelectedAccount(forceReload = false) {
  const account = getSelectedAccount();
  if (!account) {
    showError('payments-container', 'Selected account is not available.');
    return;
  }

  showLoading('payments-container');
  const accountId = String(account.account_id);

  if (forceReload || !paymentsState.paymentsByAccount.has(accountId)) {
    try {
      const payments = await getPaymentsByAccountId(accountId);
      paymentsState.paymentsByAccount.set(accountId, payments || []);
    } catch (err) {
      showError('payments-container', getErrorMessage(err));
      showToast(getErrorMessage(err), 'error');
      return;
    }
  }

  applyFiltersAndRender();
}

function applyFiltersAndRender() {
  const account = getSelectedAccount();
  if (!account) return;

  const accountId = String(account.account_id);
  const allPayments = sortPaymentsDesc(paymentsState.paymentsByAccount.get(accountId) || []);
  const visiblePayments = allPayments.filter(p => !shouldHideForSelectedAccount(p, accountId));

  const directionFilter = paymentsState.activeFilters.direction;
  const statusFilter = paymentsState.activeFilters.status;

  const filtered = visiblePayments.filter(p => {
    if (directionFilter) {
      const dir = getPaymentDirection(p, accountId);
      if (dir !== directionFilter) return false;
    }
    if (statusFilter && p.status !== statusFilter) return false;
    return true;
  });

  const filterSuffix = (directionFilter || statusFilter)
    ? ` — filtered ${filtered.length} of ${visiblePayments.length}`
    : '';

  renderPaymentsTable(
    filtered,
    `Payments for ${account.account_number}${filterSuffix}`,
    accountId
  );
}

// ── Lookup by payment ID ─────────────────────────────────────────────────────
document.getElementById('lookup-by-id-btn').addEventListener('click', async () => {
  const paymentId = document.getElementById('lookup-payment-id').value.trim();
  if (!paymentId) { showToast('Please enter a payment ID', 'info'); return; }

  showLoading('payments-container');
  try {
    const payment = await getPaymentById(paymentId);
    if (shouldHideForSelectedAccount(payment, paymentsState.selectedAccountId)) {
      showEmpty('payments-container', 'No payments found', 'This credited failed payment is hidden for the selected account.');
      return;
    }
    renderPaymentsTable([payment], `Payment #${paymentId}`, paymentsState.selectedAccountId);
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
    if (shouldHideForSelectedAccount(payment, paymentsState.selectedAccountId)) {
      showEmpty('payments-container', 'No payments found', 'This credited failed payment is hidden for the selected account.');
      return;
    }
    renderPaymentsTable([payment], `Payment Ref: ${paymentReference}`, paymentsState.selectedAccountId);
  } catch (err) {
    showError('payments-container', getErrorMessage(err));
    showToast(getErrorMessage(err), 'error');
  }
});

// ── Refresh ──────────────────────────────────────────────────────────────────
document.getElementById('refresh-btn').addEventListener('click', async () => {
  await loadPaymentsForSelectedAccount(true);
});

// ── Direction & Status filters ───────────────────────────────────────────────
document.getElementById('filter-direction').addEventListener('change', () => {
  paymentsState.activeFilters.direction = document.getElementById('filter-direction').value;
  applyFiltersAndRender();
});

document.getElementById('filter-status').addEventListener('change', () => {
  paymentsState.activeFilters.status = document.getElementById('filter-status').value;
  applyFiltersAndRender();
});

// ── Enter key support for lookups ────────────────────────────────────────────
document.getElementById('lookup-payment-id').addEventListener('keydown', e => {
  if (e.key === 'Enter') document.getElementById('lookup-by-id-btn').click();
});
document.getElementById('lookup-payment-reference').addEventListener('keydown', e => {
  if (e.key === 'Enter') document.getElementById('lookup-by-reference-btn').click();
});

// Initial load
loadPaymentsPage();

