/**
 * api.js — Centralised API layer for FlowState Payment Processing System
 *
 * All communication with the Spring Boot backend (http://localhost:8080)
 * goes through this file. No fetch() calls are made anywhere else.
 *
 * ─── Backend base URLs ────────────────────────────────────────────────────
 *  Account  :  GET  /account/
 *              GET  /account/{accountId}
 *              GET  /account/number/{accountNumber}
 *              POST /account/
 *              PUT  /account/{accountId}
 *
 *  Payment  :  POST  /payments
 *              GET   /payments/{paymentId}
 *              GET   /payments/reference/{paymentReference}
 *              GET   /payments/account/{accountId}
 *              PATCH /payments/{paymentId}/cancel
 * ─────────────────────────────────────────────────────────────────────────
 *
 * NOTE ON CORS:
 *   If you serve this frontend from a different origin than the backend
 *   (e.g. http://localhost:5500 vs http://localhost:8080) you must add
 *   a @CrossOrigin annotation or a CorsConfigurationSource bean in the
 *   Spring Boot application — do NOT modify anything else.
 *   Example (simplest):  add @CrossOrigin(origins = "*") to each controller.
 */

// ── Configuration ──────────────────────────────────────────────────────────
function resolveApiBase() {
  const explicitBase = window.localStorage.getItem('fs_api_base') || window.__FS_API_BASE__;
  if (explicitBase) {
    const normalizedBase = String(explicitBase).replace(/\/$/, '');
    const isRemoteHost = window.location.hostname && !['localhost', '127.0.0.1'].includes(window.location.hostname);
    const pointsToLocalhost = /https?:\/\/(localhost|127\.0\.0\.1)(:\d+)?$/i.test(normalizedBase);

    // Avoid stale localStorage overrides breaking remote/container deployments.
    if (!(isRemoteHost && pointsToLocalhost)) {
      return normalizedBase;
    }
  }

  // Prefer same-origin so Nginx reverse proxy setups work in Docker without CORS.
  if (window.location.origin && window.location.origin.startsWith('http')) {
    return window.location.origin;
  }

  return 'http://localhost:8080';
}

const API_BASE = resolveApiBase();

// ── Core fetch wrapper ─────────────────────────────────────────────────────
/**
 * Perform a fetch request and return the parsed JSON (or null for 204).
 * Throws an enriched Error object on HTTP errors so callers can display
 * user-friendly messages without writing try/catch boilerplate everywhere.
 *
 * @param {string} path       - e.g. '/account/' or '/payments/3'
 * @param {object} [options]  - standard fetch options (method, body, headers…)
 * @returns {Promise<any>}
 */
async function apiFetch(path, options = {}) {
  const url = `${API_BASE}${path}`;

  const method = String(options.method || 'GET').toUpperCase();
  const hasBody = options.body != null && options.body !== '';

  // Keep GET/HEAD requests "simple" to avoid unnecessary CORS preflights.
  const defaultHeaders = { Accept: 'application/json' };
  if (hasBody && !['GET', 'HEAD'].includes(method)) {
    defaultHeaders['Content-Type'] = 'application/json';
  }
  options.headers = { ...defaultHeaders, ...options.headers };

  let response;
  try {
    response = await fetch(url, options);
  } catch (networkErr) {
    // Browser reports both offline and CORS/preflight failures as network errors.
    const origin = window.location.origin || 'file://';
    const err = new Error(
      `Cannot reach backend at ${API_BASE} from ${origin}. ` +
      'Check backend status and open frontend via http://localhost:5500 (not file:// or 127.0.0.1).'
    );
    err.type = 'network';
    err.method = method;
    err.path = path;
    throw err;
  }

  // 204 No Content — valid success with no body (e.g. cancelPayment)
  if (response.status === 204) return null;

  let body;
  const contentType = response.headers.get('content-type') || '';
  if (contentType.includes('application/json')) {
    body = await response.json();
  } else {
    body = await response.text();
  }

  if (!response.ok) {
    // Backend sends ApiError: { timestamp, status, error, message }
    const message =
      (body && body.message) ||
      (body && body.error)   ||
      `HTTP ${response.status}`;
    const err = new Error(message);
    err.status  = response.status;
    err.body    = body;
    err.type    = response.status >= 500 ? 'server' : 'client';
    err.method  = method;
    err.path    = path;
    throw err;
  }

  return body;
}

// ── Account APIs ────────────────────────────────────────────────────────────

/** GET /account/ — list all accounts */
async function getAllAccounts() {
  return apiFetch('/account/');
}

/** GET /account/{accountId} — single account by PK */
async function getAccountById(accountId) {
  return apiFetch(`/account/${accountId}`);
}

/** GET /account/number/{accountNumber} — lookup by account number string */
async function getAccountByNumber(accountNumber) {
  return apiFetch(`/account/number/${encodeURIComponent(accountNumber)}`);
}

/** POST /account/ — create a new account */
async function createAccount(accountData) {
  return apiFetch('/account/', {
    method: 'POST',
    body: JSON.stringify(accountData),
  });
}

/** PUT /account/{accountId} — update an existing account */
async function updateAccount(accountId, accountData) {
  return apiFetch(`/account/${accountId}`, {
    method: 'PUT',
    body: JSON.stringify(accountData),
  });
}

/** GET /account/{accountId}/history — account audit trail */
async function getAccountHistory(accountId) {
  return apiFetch(`/account/${accountId}/history`);
}

// ── Payment APIs ────────────────────────────────────────────────────────────

/**
 * POST /payments — create a new payment.
 *
 * The idempotency_key MUST be generated by the caller (frontend) using:
 *   const idempotencyKey = crypto.randomUUID();
 *
 * Request body (field names are fixed by the backend record):
 * {
 *   "source_account_number":      "<string>",
 *   "destination_account_number": "<string>",
 *   "amount":                     <number>,
 *   "currency":                   "<string>",
 *   "idempotency_key":            "<uuid>"
 * }
 */
async function createPayment(paymentRequest) {
  return apiFetch('/payments', {
    method: 'POST',
    body: JSON.stringify(paymentRequest),
  });
}

/** GET /payments/{paymentId} — fetch payment by numeric ID */
async function getPaymentById(paymentId) {
  return apiFetch(`/payments/${paymentId}`);
}

/** GET /payments/reference/{paymentReference} — fetch by reference string */
async function getPaymentByReference(paymentReference) {
  return apiFetch(`/payments/reference/${encodeURIComponent(paymentReference)}`);
}

/** GET /payments/account/{accountId} — all payments for an account */
async function getPaymentsByAccountId(accountId) {
  return apiFetch(`/payments/account/${accountId}`);
}

/**
 * PATCH /payments/{paymentId}/cancel — cancel a payment.
 * Returns null (204 No Content) on success.
 */
async function cancelPayment(paymentId) {
  return apiFetch(`/payments/${paymentId}/cancel`, { method: 'PATCH' });
}

/** GET /payment-history/{paymentId} — lifecycle events for a payment */
async function getPaymentHistory(paymentId) {
  return apiFetch(`/payment-history/${paymentId}`);
}

// ── Helper utilities ────────────────────────────────────────────────────────

/** Format a monetary amount with currency symbol */
function formatCurrency(amount, currency = 'INR') {
  if (amount == null) return '—';
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency, maximumFractionDigits: 2 }).format(amount);
}

/** Format a UTC datetime string to a readable local string */
function formatDate(dateStr) {
  if (!dateStr) return '—';
  return new Date(dateStr).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' });
}

/** Return the CSS class name for a status badge */
function statusBadgeClass(status) {
  if (!status) return 'badge';
  return `badge badge-${status.toLowerCase()}`;
}

/** Get a human-readable error message from a caught error */
function getErrorMessage(err) {
  if (!err) return 'An unknown error occurred.';
  if (err.type === 'network') {
    if (err.method === 'PATCH') {
      return `${err.message} (PATCH request failed; this is often a CORS method restriction).`;
    }
    return err.message;
  }
  if (err.status === 400) return `Bad request: ${err.message}`;
  if (err.status === 404) return `Not found: ${err.message}`;
  if (err.status === 409) return `Conflict: ${err.message}`;
  if (err.status === 503) return `Gateway error: ${err.message}`;
  if (err.status >= 500) return `Server error: ${err.message}`;
  return err.message || 'An error occurred.';
}

