import React, { useState, useEffect, useCallback } from 'react';
import Navbar from '../components/Navbar';
import StatsBar from '../components/StatsBar';
import KanbanBoard from '../components/KanbanBoard';
import ApplicationModal from '../components/ApplicationModal';
import DeleteConfirmModal from '../components/DeleteConfirmModal';
import Toast from '../components/Toast';
import {
  getApplications,
  getStats,
  createApplication,
  updateApplication,
  updateStatus,
  deleteApplication,
  clearAuth,
} from '../api';
import { Plus, Search, Filter } from 'lucide-react';
import { STATUS_LIST, STATUS_CONFIG } from '../constants';

export default function BoardPage({ user, onLogout }) {
  const [applications, setApplications] = useState([]);
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);

  // Filter & Search states
  const [search, setSearch] = useState('');
  const [selectedStatus, setSelectedStatus] = useState('');

  // Modals state
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingApp, setEditingApp] = useState(null);
  const [deletingId, setDeletingId] = useState(null);
  const [isDeleting, setIsDeleting] = useState(false);

  // Toast
  const [toast, setToast] = useState(null);

  const showToast = (message, type = 'success') => {
    setToast({ message, type });
  };

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      const [appsData, statsData] = await Promise.all([
        getApplications({
          search: search.trim() || undefined,
          status: selectedStatus || undefined,
          size: 200,
        }),
        getStats(),
      ]);

      const items = Array.isArray(appsData) ? appsData : appsData.content || [];
      setApplications(items);
      setStats(statsData);
    } catch (err) {
      showToast(err.message || 'Ошибка загрузки данных', 'error');
    } finally {
      setLoading(false);
    }
  }, [search, selectedStatus]);

  useEffect(() => {
    const timer = setTimeout(() => {
      loadData();
    }, 250);
    return () => clearTimeout(timer);
  }, [loadData]);

  // Drag and drop status update (optimistic UI)
  const handleMoveStatus = async (appId, newStatus) => {
    const target = applications.find((a) => a.id === appId);
    if (!target || target.status === newStatus) return;

    const oldStatus = target.status;

    // Optimistic update
    setApplications((prev) =>
      prev.map((app) => (app.id === appId ? { ...app, status: newStatus } : app))
    );

    try {
      await updateStatus(appId, newStatus);
      const updatedStats = await getStats();
      setStats(updatedStats);
      showToast(`Перемещено: ${STATUS_CONFIG[newStatus].label}`);
    } catch (err) {
      // Revert on error
      setApplications((prev) =>
        prev.map((app) => (app.id === appId ? { ...app, status: oldStatus } : app))
      );
      showToast(err.message || 'Не удалось обновить статус', 'error');
    }
  };

  // Create / Update
  const handleSave = async (payload, id) => {
    if (id) {
      const updated = await updateApplication(id, payload);
      setApplications((prev) => prev.map((a) => (a.id === id ? updated : a)));
      showToast('Вакансия успешно обновлена');
    } else {
      const created = await createApplication(payload);
      setApplications((prev) => [created, ...prev]);
      showToast('Новая вакансия добавлена в список');
    }
    const updatedStats = await getStats();
    setStats(updatedStats);
  };

  // Delete
  const handleConfirmDelete = async () => {
    if (!deletingId) return;
    setIsDeleting(true);
    try {
      await deleteApplication(deletingId);
      setApplications((prev) => prev.filter((a) => a.id !== deletingId));
      const updatedStats = await getStats();
      setStats(updatedStats);
      showToast('Вакансия удалена');
      setDeletingId(null);
    } catch (err) {
      showToast(err.message || 'Не удалось удалить вакансию', 'error');
    } finally {
      setIsDeleting(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#f7f8f9] flex flex-col">
      <Navbar user={user} onLogout={onLogout} />

      {/* Sub-header: Stats & Search Filters */}
      <div className="bg-white border-b border-gray-200 px-4 sm:px-6 lg:px-8 py-3.5 shadow-2xs">
        <div className="max-w-7xl mx-auto flex flex-col md:flex-row md:items-center justify-between gap-3">
          {/* Live stats */}
          <StatsBar stats={stats} />

          {/* Controls: Search, Filter, Add */}
          <div className="flex flex-wrap items-center gap-2.5">
            {/* Search */}
            <div className="relative flex-1 sm:w-64">
              <span className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-gray-400">
                <Search size={15} />
              </span>
              <input
                type="text"
                placeholder="Поиск по компании или должности..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="w-full pl-9 pr-3 py-1.5 text-xs sm:text-sm rounded-lg border border-gray-300 focus:outline-none focus:ring-2 focus:ring-[#d6001c] bg-white transition"
              />
            </div>

            {/* Filter */}
            <div className="relative">
              <select
                value={selectedStatus}
                onChange={(e) => setSelectedStatus(e.target.value)}
                className="pl-3 pr-8 py-1.5 text-xs sm:text-sm rounded-lg border border-gray-300 focus:outline-none focus:ring-2 focus:ring-[#d6001c] bg-white transition cursor-pointer appearance-none font-medium text-gray-700"
              >
                <option value="">Все этапы</option>
                {STATUS_LIST.map((s) => (
                  <option key={s} value={s}>
                    {STATUS_CONFIG[s].label}
                  </option>
                ))}
              </select>
              <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center px-2 text-gray-400">
                <Filter size={13} />
              </div>
            </div>

            {/* Add Button - Signature HH Red */}
            <button
              onClick={() => {
                setEditingApp(null);
                setIsModalOpen(true);
              }}
              className="inline-flex items-center gap-1.5 px-3.5 py-1.5 text-xs sm:text-sm font-bold rounded-lg bg-[#d6001c] hover:bg-[#ba0018] text-white shadow-xs transition cursor-pointer"
            >
              <Plus size={16} />
              <span>Добавить вакансию</span>
            </button>
          </div>
        </div>
      </div>

      {/* Main Board View */}
      <main className="flex-1 max-w-7xl w-full mx-auto p-4 sm:p-6 lg:p-8">
        <KanbanBoard
          applications={applications}
          loading={loading}
          onEdit={(app) => {
            setEditingApp(app);
            setIsModalOpen(true);
          }}
          onDelete={(id) => setDeletingId(id)}
          onMove={handleMoveStatus}
        />
      </main>

      {/* Modal Dialogs */}
      <ApplicationModal
        app={editingApp}
        isOpen={isModalOpen}
        onClose={() => {
          setIsModalOpen(false);
          setEditingApp(null);
        }}
        onSave={handleSave}
      />

      <DeleteConfirmModal
        isOpen={Boolean(deletingId)}
        onClose={() => setDeletingId(null)}
        onConfirm={handleConfirmDelete}
        deleting={isDeleting}
      />

      {/* Toast popup */}
      <Toast toast={toast} onClose={() => setToast(null)} />
    </div>
  );
}
