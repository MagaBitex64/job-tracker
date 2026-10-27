import React, { useState } from 'react';
import { STATUS_LIST, STATUS_CONFIG } from '../constants';
import ApplicationCard from './ApplicationCard';
import { Inbox } from 'lucide-react';

export default function KanbanBoard({
  applications,
  loading,
  onEdit,
  onDelete,
  onMove,
}) {
  const [activeDropColumn, setActiveDropColumn] = useState(null);
  const [draggedAppId, setDraggedAppId] = useState(null);

  // Group applications by status
  const grouped = STATUS_LIST.reduce((acc, status) => {
    acc[status] = applications.filter((app) => app.status === status);
    return acc;
  }, {});

  const handleDragStart = (e, appId) => {
    setDraggedAppId(appId);
    e.dataTransfer.setData('text/plain', String(appId));
    e.dataTransfer.effectAllowed = 'move';
  };

  const handleDragEnd = () => {
    setDraggedAppId(null);
    setActiveDropColumn(null);
  };

  const handleDragOver = (e, status) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';
    if (activeDropColumn !== status) {
      setActiveDropColumn(status);
    }
  };

  const handleDragLeave = (e, status) => {
    if (e.currentTarget.contains(e.relatedTarget)) return;
    if (activeDropColumn === status) {
      setActiveDropColumn(null);
    }
  };

  const handleDrop = (e, status) => {
    e.preventDefault();
    setActiveDropColumn(null);
    const idStr = e.dataTransfer.getData('text/plain') || draggedAppId;
    if (!idStr) return;
    const appId = Number(idStr);
    onMove(appId, status);
  };

  return (
    <div className="flex gap-4 items-start overflow-x-auto pb-6 custom-scroll min-h-[calc(100vh-14rem)]">
      {STATUS_LIST.map((status) => {
        const config = STATUS_CONFIG[status];
        const items = grouped[status] || [];
        const isDragOver = activeDropColumn === status;

        return (
          <div
            key={status}
            className={`w-[290px] shrink-0 flex flex-col rounded-xl bg-white border border-gray-200 shadow-2xs overflow-hidden transition-all ${
              config.columnBorder
            } ${isDragOver ? 'ring-2 ring-[#d6001c] bg-red-50/10' : ''}`}
            onDragOver={(e) => handleDragOver(e, status)}
            onDragLeave={(e) => handleDragLeave(e, status)}
            onDrop={(e) => handleDrop(e, status)}
          >
            {/* Column Header */}
            <div className={`px-3.5 py-3 border-b border-gray-200 flex items-center justify-between ${config.headerBg}`}>
              <div className="flex items-center gap-2">
                <span className={`w-2.5 h-2.5 rounded-full ${config.dotColor}`} />
                <h3 className="text-xs font-bold text-gray-900 tracking-wide uppercase">
                  {config.label}
                </h3>
              </div>
              <span className="px-2 py-0.5 text-xs font-semibold rounded-full bg-white border border-gray-200 text-gray-700 shadow-2xs">
                {items.length}
              </span>
            </div>

            {/* Cards Container */}
            <div className="p-2.5 flex flex-col gap-2.5 min-h-[220px]">
              {loading ? (
                // Skeleton loading state
                Array.from({ length: 2 }).map((_, i) => (
                  <div key={i} className="bg-gray-50 rounded-lg p-3 border border-gray-100 animate-pulse">
                    <div className="h-3.5 bg-gray-200 rounded w-2/3 mb-2" />
                    <div className="h-2.5 bg-gray-200 rounded w-1/2 mb-3" />
                    <div className="h-4 bg-gray-200 rounded w-16" />
                  </div>
                ))
              ) : items.length > 0 ? (
                items.map((app) => (
                  <ApplicationCard
                    key={app.id}
                    app={app}
                    onEdit={onEdit}
                    onDelete={onDelete}
                    onDragStart={handleDragStart}
                    onDragEnd={handleDragEnd}
                  />
                ))
              ) : (
                // Empty state for column
                <div className="flex flex-col items-center justify-center py-10 px-4 text-center text-gray-400">
                  <Inbox size={26} className="text-gray-300 mb-1.5 stroke-[1.5]" />
                  <p className="text-xs text-gray-500 leading-snug">
                    {config.emptyText}
                  </p>
                </div>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
}
