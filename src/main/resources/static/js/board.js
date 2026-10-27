// board.js - Kanban board controller for Job Tracker

import { getApplications, updateStatus, getApplicationById } from './api.js';
import { getStats } from './api.js';
import { renderCard, renderSkeleton, renderEmptyState, showToast, showModal, showDeleteConfirm, renderStats } from './ui.js';

const STATUSES = ['WISHLIST', 'APPLIED', 'INTERVIEW', 'OFFER', 'REJECTED'];

// ---------------------------------------------------------------------------
// State
// ---------------------------------------------------------------------------
let allApps = [];
let draggedAppId = null;

// ---------------------------------------------------------------------------
// Public entry point
// ---------------------------------------------------------------------------

/**
 * Initialise the kanban board. Called once from index.html after requireAuth().
 */
export async function initBoard() {
  setupControls();
  await fetchAndRender();
}

// ---------------------------------------------------------------------------
// Data fetching & rendering
// ---------------------------------------------------------------------------

async function fetchAndRender(params = {}) {
  showSkeletons();

  try {
    const [appsResponse, stats] = await Promise.all([
      getApplications({ size: 200, ...params }),
      getStats(),
    ]);

    // Spring Page object → content array; or plain array
    allApps = Array.isArray(appsResponse)
      ? appsResponse
      : (appsResponse.content || []);

    renderBoard(allApps);
    renderStats(stats);
  } catch (err) {
    showToast(err.message || 'Failed to load applications.', 'error');
    STATUSES.forEach((s) => {
      const col = document.getElementById(`col-${s}`);
      if (col) col.innerHTML = renderEmptyState(s);
    });
  }
}

function renderBoard(apps) {
  // Group by status
  const groups = {};
  STATUSES.forEach((s) => (groups[s] = []));
  apps.forEach((app) => {
    if (groups[app.status]) groups[app.status].push(app);
    else groups['WISHLIST'].push(app); // fallback
  });

  STATUSES.forEach((status) => {
    const col = document.getElementById(`col-${status}`);
    if (!col) return;

    const list = groups[status];
    col.innerHTML = list.length
      ? list.map(renderCard).join('')
      : renderEmptyState(status);

    // Update count badge in column header
    const badge = document.getElementById(`badge-${status}`);
    if (badge) badge.textContent = list.length;
  });

  setupDragAndDrop();
  setupCardDelegation();
}

function showSkeletons() {
  STATUSES.forEach((s) => {
    const col = document.getElementById(`col-${s}`);
    if (col) col.innerHTML = renderSkeleton();
  });
}

// ---------------------------------------------------------------------------
// Drag and drop
// ---------------------------------------------------------------------------

function setupDragAndDrop() {
  // Card dragstart (event delegation on document)
  document.querySelectorAll('[data-app-id][draggable="true"]').forEach((card) => {
    card.addEventListener('dragstart', (e) => {
      draggedAppId = card.dataset.appId;
      e.dataTransfer.setData('text/plain', draggedAppId);
      e.dataTransfer.effectAllowed = 'move';
      card.classList.add('opacity-50');
    });
    card.addEventListener('dragend', () => {
      card.classList.remove('opacity-50');
    });
  });

  // Column drop targets
  STATUSES.forEach((status) => {
    const col = document.getElementById(`col-${status}`);
    if (!col) return;

    col.addEventListener('dragover', (e) => {
      e.preventDefault();
      e.dataTransfer.dropEffect = 'move';
      col.classList.add('bg-indigo-50', 'dark:bg-indigo-950', 'ring-2', 'ring-indigo-300');
    });

    col.addEventListener('dragleave', (e) => {
      if (!col.contains(e.relatedTarget)) {
        col.classList.remove('bg-indigo-50', 'dark:bg-indigo-950', 'ring-2', 'ring-indigo-300');
      }
    });

    col.addEventListener('drop', async (e) => {
      e.preventDefault();
      col.classList.remove('bg-indigo-50', 'dark:bg-indigo-950', 'ring-2', 'ring-indigo-300');

      const appId = e.dataTransfer.getData('text/plain') || draggedAppId;
      if (!appId) return;

      const app = allApps.find((a) => String(a.id) === String(appId));
      if (!app || app.status === status) return; // no-op

      // Optimistic update
      const oldStatus = app.status;
      app.status = status;
      renderBoard(allApps);

      try {
        await updateStatus(appId, status);
        // Refresh stats
        const stats = await getStats();
        renderStats(stats);
        showToast(`Moved to ${status.charAt(0) + status.slice(1).toLowerCase()}`);
      } catch (err) {
        // Revert
        app.status = oldStatus;
        renderBoard(allApps);
        showToast(err.message || 'Failed to update status.', 'error');
      }
    });
  });
}

// ---------------------------------------------------------------------------
// Card edit / delete (event delegation)
// ---------------------------------------------------------------------------

function setupCardDelegation() {
  STATUSES.forEach((status) => {
    const col = document.getElementById(`col-${status}`);
    if (!col) return;

    col.addEventListener('click', (e) => {
      const btn = e.target.closest('[data-action]');
      if (!btn) return;

      const appId = btn.dataset.appId;
      const action = btn.dataset.action;

      if (action === 'edit') {
        const app = allApps.find((a) => String(a.id) === String(appId));
        if (!app) return;
        showModal(app, async (saved) => {
          const idx = allApps.findIndex((a) => String(a.id) === String(saved.id));
          if (idx !== -1) allApps[idx] = saved;
          renderBoard(allApps);
          const stats = await getStats();
          renderStats(stats);
          showToast('Application updated.');
        });
      }

      if (action === 'delete') {
        showDeleteConfirm(appId, async () => {
          allApps = allApps.filter((a) => String(a.id) !== String(appId));
          renderBoard(allApps);
          const stats = await getStats();
          renderStats(stats);
          showToast('Application deleted.');
        });
      }
    });
  });
}

// ---------------------------------------------------------------------------
// Controls (search, filter, add)
// ---------------------------------------------------------------------------

function setupControls() {
  let debounceTimer;

  const searchInput = document.getElementById('search-input');
  const statusFilter = document.getElementById('status-filter');
  const addBtn = document.getElementById('add-btn');

  function buildParams() {
    return {
      search: searchInput ? searchInput.value.trim() || undefined : undefined,
      status: statusFilter && statusFilter.value ? statusFilter.value : undefined,
    };
  }

  if (searchInput) {
    searchInput.addEventListener('input', () => {
      clearTimeout(debounceTimer);
      debounceTimer = setTimeout(() => fetchAndRender(buildParams()), 350);
    });
  }

  if (statusFilter) {
    statusFilter.addEventListener('change', () => fetchAndRender(buildParams()));
  }

  if (addBtn) {
    addBtn.addEventListener('click', () => {
      showModal(null, async (saved) => {
        allApps.unshift(saved);
        renderBoard(allApps);
        const stats = await getStats();
        renderStats(stats);
        showToast('Application added!');
      });
    });
  }
}
