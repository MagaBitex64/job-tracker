// ui.js — reusable UI components and helpers (ES module)

import { createApplication, updateApplication, deleteApplication } from './api.js';

// ─── Toast Notifications ──────────────────────────────────────────────────────

export function showToast(message, type = 'success') {
  const toast = document.createElement('div');
  const color = type === 'success'
    ? 'bg-green-600 text-white'
    : 'bg-red-600 text-white';
  toast.className = `fixed bottom-5 right-5 z-50 px-4 py-3 rounded-lg shadow-lg text-sm font-medium
    transition-all duration-300 opacity-0 ${color}`;
  toast.textContent = message;
  document.body.appendChild(toast);
  // Fade in
  requestAnimationFrame(() => { toast.style.opacity = '1'; });
  // Fade out and remove
  setTimeout(() => {
    toast.style.opacity = '0';
    setTimeout(() => toast.remove(), 300);
  }, 3000);
}

// ─── Status Badge Helper ──────────────────────────────────────────────────────

const STATUS_COLORS = {
  WISHLIST:  'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300',
  APPLIED:   'bg-blue-100 text-blue-700 dark:bg-blue-900 dark:text-blue-200',
  INTERVIEW: 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900 dark:text-yellow-200',
  OFFER:     'bg-green-100 text-green-700 dark:bg-green-900 dark:text-green-200',
  REJECTED:  'bg-red-100 text-red-700 dark:bg-red-900 dark:text-red-200',
};

function statusBadge(status) {
  const cls = STATUS_COLORS[status] ?? 'bg-gray-100 text-gray-600';
  return `<span class="inline-block px-2 py-0.5 rounded text-xs font-semibold ${cls}">${status}</span>`;
}

// ─── Card Renderer ────────────────────────────────────────────────────────────

export function renderCard(app) {
  const appliedDate = app.appliedAt
    ? `<span class="text-xs text-gray-400 dark:text-gray-500">Applied ${app.appliedAt}</span>`
    : '';
  return `
    <div class="bg-white dark:bg-gray-800 rounded-lg shadow-sm hover:shadow-md transition-shadow
      border border-gray-100 dark:border-gray-700 p-3 cursor-grab active:cursor-grabbing select-none"
      draggable="true"
      data-app-id="${app.id}">
      <div class="flex justify-between items-start gap-2">
        <div class="min-w-0">
          <p class="font-semibold text-gray-900 dark:text-gray-100 text-sm truncate">${escHtml(app.company)}</p>
          <p class="text-xs text-gray-500 dark:text-gray-400 truncate mt-0.5">${escHtml(app.position)}</p>
        </div>
        <div class="flex gap-1 shrink-0 mt-0.5">
          <button class="edit-btn p-1 rounded hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-400 hover:text-indigo-600 transition-colors"
            data-action="edit" data-app-id="${app.id}" title="Edit">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
            </svg>
          </button>
          <button class="delete-btn p-1 rounded hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-400 hover:text-red-500 transition-colors"
            data-action="delete" data-app-id="${app.id}" title="Delete">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
            </svg>
          </button>
        </div>
      </div>
      <div class="mt-2 flex items-center justify-between gap-2">
        ${statusBadge(app.status)}
        ${appliedDate}
      </div>
      ${app.link ? `<a href="${escHtml(app.link)}" target="_blank" rel="noopener noreferrer"
        class="block mt-1.5 text-xs text-indigo-500 hover:text-indigo-700 truncate" title="${escHtml(app.link)}">
        View listing ↗</a>` : ''}
    </div>`;
}

export function renderSkeleton() {
  return Array.from({ length: 2 }, () => `
    <div class="bg-white dark:bg-gray-800 rounded-lg border border-gray-100 dark:border-gray-700 p-3 animate-pulse">
      <div class="h-3 bg-gray-200 dark:bg-gray-700 rounded w-3/4 mb-2"></div>
      <div class="h-2.5 bg-gray-100 dark:bg-gray-600 rounded w-1/2 mb-3"></div>
      <div class="h-4 bg-gray-100 dark:bg-gray-600 rounded w-16"></div>
    </div>`).join('');
}

export function renderEmptyState(status) {
  return `
    <div class="text-center py-8 text-gray-400 dark:text-gray-600">
      <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8 mx-auto mb-2 opacity-50" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
          d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
      </svg>
      <p class="text-xs">No ${status.toLowerCase()} applications</p>
    </div>`;
}

// ─── Stats Bar ────────────────────────────────────────────────────────────────

export function renderStats(stats) {
  const bar = document.getElementById('stats-bar');
  if (!bar) return;
  const items = [
    { label: 'Wishlist', value: stats.wishlist, color: 'text-gray-500' },
    { label: 'Applied',  value: stats.applied,  color: 'text-blue-600' },
    { label: 'Interview',value: stats.interview, color: 'text-yellow-600' },
    { label: 'Offer',    value: stats.offer,     color: 'text-green-600' },
    { label: 'Rejected', value: stats.rejected,  color: 'text-red-500' },
  ];
  bar.innerHTML = items.map(({ label, value, color }) => `
    <div class="flex items-center gap-1.5 text-sm">
      <span class="font-bold text-base ${color}">${value}</span>
      <span class="text-gray-500 dark:text-gray-400">${label}</span>
    </div>`).join('<span class="text-gray-200 dark:text-gray-700">|</span>') +
    `<span class="ml-auto text-sm text-gray-400 dark:text-gray-500">${stats.total} total</span>`;
}

// ─── Modal (Create / Edit) ────────────────────────────────────────────────────

export function showModal(app = null, onSave) {
  hideModal();
  const isEdit = app !== null;
  const overlay = document.createElement('div');
  overlay.id = 'modal-overlay';
  overlay.className = 'fixed inset-0 bg-black/50 dark:bg-black/70 z-40 flex items-center justify-center p-4';

  const statusOptions = ['WISHLIST', 'APPLIED', 'INTERVIEW', 'OFFER', 'REJECTED']
    .map(s => `<option value="${s}" ${isEdit && app.status === s ? 'selected' : ''}>${s}</option>`)
    .join('');

  overlay.innerHTML = `
    <div class="bg-white dark:bg-gray-900 rounded-xl shadow-2xl w-full max-w-lg">
      <div class="flex justify-between items-center px-6 py-4 border-b border-gray-200 dark:border-gray-700">
        <h2 class="text-lg font-semibold text-gray-900 dark:text-gray-100">
          ${isEdit ? 'Edit Application' : 'Add Application'}
        </h2>
        <button id="modal-close" class="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 text-xl leading-none">&times;</button>
      </div>
      <form id="modal-form" novalidate class="px-6 py-5 space-y-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Company <span class="text-red-500">*</span></label>
          <input type="text" id="f-company" maxlength="120" required
            value="${isEdit ? escHtml(app.company) : ''}"
            class="w-full border border-gray-300 dark:border-gray-600 rounded-lg px-3 py-2 text-sm
              bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100
              focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"/>
          <p class="mt-1 text-xs text-red-500 hidden" id="e-company"></p>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Position <span class="text-red-500">*</span></label>
          <input type="text" id="f-position" maxlength="120" required
            value="${isEdit ? escHtml(app.position) : ''}"
            class="w-full border border-gray-300 dark:border-gray-600 rounded-lg px-3 py-2 text-sm
              bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100
              focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"/>
          <p class="mt-1 text-xs text-red-500 hidden" id="e-position"></p>
        </div>
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Status <span class="text-red-500">*</span></label>
            <select id="f-status"
              class="w-full border border-gray-300 dark:border-gray-600 rounded-lg px-3 py-2 text-sm
                bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100
                focus:outline-none focus:ring-2 focus:ring-indigo-500">
              ${statusOptions}
            </select>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Applied Date</label>
            <input type="date" id="f-applied-at"
              value="${isEdit && app.appliedAt ? app.appliedAt : ''}"
              class="w-full border border-gray-300 dark:border-gray-600 rounded-lg px-3 py-2 text-sm
                bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100
                focus:outline-none focus:ring-2 focus:ring-indigo-500"/>
          </div>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Job Listing URL</label>
          <input type="url" id="f-link" maxlength="500"
            value="${isEdit && app.link ? escHtml(app.link) : ''}"
            placeholder="https://..."
            class="w-full border border-gray-300 dark:border-gray-600 rounded-lg px-3 py-2 text-sm
              bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100
              focus:outline-none focus:ring-2 focus:ring-indigo-500"/>
          <p class="mt-1 text-xs text-red-500 hidden" id="e-link"></p>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Notes</label>
          <textarea id="f-notes" maxlength="2000" rows="3"
            class="w-full border border-gray-300 dark:border-gray-600 rounded-lg px-3 py-2 text-sm
              bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100
              focus:outline-none focus:ring-2 focus:ring-indigo-500 resize-none"
          >${isEdit && app.notes ? escHtml(app.notes) : ''}</textarea>
        </div>
        <p class="text-xs text-red-500 hidden" id="modal-error"></p>
        <div class="flex justify-end gap-3 pt-2">
          <button type="button" id="modal-cancel"
            class="px-4 py-2 text-sm rounded-lg border border-gray-300 dark:border-gray-600
              text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors">
            Cancel
          </button>
          <button type="submit"
            class="px-4 py-2 text-sm rounded-lg bg-indigo-600 hover:bg-indigo-700 text-white font-medium transition-colors">
            ${isEdit ? 'Save Changes' : 'Add Application'}
          </button>
        </div>
      </form>
    </div>`;

  document.body.appendChild(overlay);

  document.getElementById('modal-close').onclick = hideModal;
  document.getElementById('modal-cancel').onclick = hideModal;
  overlay.addEventListener('click', e => { if (e.target === overlay) hideModal(); });

  document.getElementById('modal-form').addEventListener('submit', async e => {
    e.preventDefault();
    if (!validateModal()) return;

    const company  = document.getElementById('f-company').value.trim();
    const position = document.getElementById('f-position').value.trim();
    const status   = document.getElementById('f-status').value;
    const link     = document.getElementById('f-link').value.trim() || null;
    const notes    = document.getElementById('f-notes').value.trim() || null;
    const appliedAt= document.getElementById('f-applied-at').value || null;

    const data = { company, position, status, link, notes, appliedAt };

    const btn = e.target.querySelector('[type="submit"]');
    btn.disabled = true;
    btn.textContent = 'Saving…';

    try {
      let saved;
      if (isEdit) {
        saved = await updateApplication(app.id, data);
      } else {
        saved = await createApplication(data);
      }
      hideModal();
      onSave(saved);
    } catch (err) {
      const errEl = document.getElementById('modal-error');
      errEl.textContent = err.message;
      errEl.classList.remove('hidden');
      btn.disabled = false;
      btn.textContent = isEdit ? 'Save Changes' : 'Add Application';
    }
  });
}

function validateModal() {
  let valid = true;
  const setErr = (id, errId, msg) => {
    const el = document.getElementById(id);
    const errEl = document.getElementById(errId);
    if (!el || !errEl) return;
    if (!el.value.trim()) {
      errEl.textContent = msg;
      errEl.classList.remove('hidden');
      el.classList.add('border-red-400');
      valid = false;
    } else {
      errEl.classList.add('hidden');
      el.classList.remove('border-red-400');
    }
  };

  setErr('f-company',  'e-company',  'Company name is required');
  setErr('f-position', 'e-position', 'Position is required');

  const link = document.getElementById('f-link');
  const linkErr = document.getElementById('e-link');
  if (link && linkErr && link.value.trim() && !isValidUrl(link.value.trim())) {
    linkErr.textContent = 'Please enter a valid URL';
    linkErr.classList.remove('hidden');
    link.classList.add('border-red-400');
    valid = false;
  } else if (linkErr) {
    linkErr.classList.add('hidden');
    link?.classList.remove('border-red-400');
  }

  return valid;
}

function isValidUrl(str) {
  try { new URL(str); return true; } catch { return false; }
}

export function hideModal() {
  document.getElementById('modal-overlay')?.remove();
}

// ─── Delete Confirm ───────────────────────────────────────────────────────────

export function showDeleteConfirm(appId, onConfirm) {
  const overlay = document.createElement('div');
  overlay.className = 'fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4';
  overlay.innerHTML = `
    <div class="bg-white dark:bg-gray-900 rounded-xl shadow-2xl p-6 max-w-sm w-full">
      <h3 class="text-base font-semibold text-gray-900 dark:text-gray-100 mb-2">Delete Application</h3>
      <p class="text-sm text-gray-500 dark:text-gray-400 mb-5">
        Are you sure you want to delete this application? This action cannot be undone.
      </p>
      <div class="flex justify-end gap-3">
        <button id="confirm-cancel"
          class="px-4 py-2 text-sm rounded-lg border border-gray-300 dark:border-gray-600
            text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors">
          Cancel
        </button>
        <button id="confirm-delete"
          class="px-4 py-2 text-sm rounded-lg bg-red-600 hover:bg-red-700 text-white font-medium transition-colors">
          Delete
        </button>
      </div>
    </div>`;
  document.body.appendChild(overlay);

  overlay.querySelector('#confirm-cancel').onclick = () => overlay.remove();
  overlay.querySelector('#confirm-delete').onclick = async () => {
    const btn = overlay.querySelector('#confirm-delete');
    btn.disabled = true;
    btn.textContent = 'Deleting…';
    try {
      await deleteApplication(appId);
      overlay.remove();
      onConfirm();
    } catch (err) {
      overlay.remove();
      showToast(err.message, 'error');
    }
  };
  overlay.addEventListener('click', e => { if (e.target === overlay) overlay.remove(); });
}

// ─── Utility ──────────────────────────────────────────────────────────────────

function escHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}
