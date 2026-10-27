import React from 'react';
import { STATUS_CONFIG } from '../constants';
import { Pencil, Trash2, ExternalLink, Calendar, FileText } from 'lucide-react';

export default function ApplicationCard({ app, onEdit, onDelete, onDragStart, onDragEnd }) {
  const config = STATUS_CONFIG[app.status] || STATUS_CONFIG.WISHLIST;

  return (
    <div
      draggable="true"
      onDragStart={(e) => onDragStart(e, app.id)}
      onDragEnd={onDragEnd}
      className="bg-white rounded-lg border border-gray-200 hover:border-gray-300 shadow-xs hover:shadow-md transition-all p-3.5 cursor-grab active:cursor-grabbing group select-none relative"
      data-app-id={app.id}
    >
      {/* Top row: Company & Action Buttons */}
      <div className="flex items-start justify-between gap-2">
        <div className="min-w-0 flex-1">
          <h4 className="font-bold text-gray-900 text-sm truncate leading-snug">
            {app.company}
          </h4>
          <p className="text-xs text-gray-600 truncate mt-0.5 font-medium">
            {app.position}
          </p>
        </div>

        {/* Edit & Delete actions */}
        <div className="flex items-center gap-1 shrink-0 opacity-80 group-hover:opacity-100 transition-opacity">
          <button
            onClick={() => onEdit(app)}
            className="p-1 text-gray-400 hover:text-gray-700 hover:bg-gray-100 rounded transition-colors cursor-pointer"
            title="Редактировать"
          >
            <Pencil size={13} />
          </button>
          <button
            onClick={() => onDelete(app.id)}
            className="p-1 text-gray-400 hover:text-[#d6001c] hover:bg-red-50 rounded transition-colors cursor-pointer"
            title="Удалить"
          >
            <Trash2 size={13} />
          </button>
        </div>
      </div>

      {/* Applied date or notes */}
      {app.appliedAt && (
        <div className="flex items-center gap-1.5 text-[11px] text-gray-500 mt-2">
          <Calendar size={12} className="text-gray-400" />
          <span>Отклик: {app.appliedAt}</span>
        </div>
      )}

      {app.notes && (
        <div className="mt-2 text-xs text-gray-500 bg-gray-50 p-2 rounded border border-gray-100 line-clamp-2">
          <div className="flex items-start gap-1">
            <FileText size={11} className="text-gray-400 mt-0.5 shrink-0" />
            <span className="text-[11px] text-gray-600">{app.notes}</span>
          </div>
        </div>
      )}

      {/* Footer: Status badge & external link */}
      <div className="mt-3 pt-2.5 border-t border-gray-100 flex items-center justify-between gap-2">
        <span
          className={`inline-flex items-center gap-1 text-[11px] font-semibold px-2 py-0.5 rounded-full border ${config.badgeBg}`}
        >
          <span className={`w-1.5 h-1.5 rounded-full ${config.dotColor}`} />
          {config.label}
        </span>

        {app.link && (
          <a
            href={app.link}
            target="_blank"
            rel="noopener noreferrer"
            onClick={(e) => e.stopPropagation()}
            className="inline-flex items-center gap-1 text-xs text-[#d6001c] hover:text-[#ba0018] font-medium"
            title="Открыть ссылку на вакансию"
          >
            <span>Вакансия</span>
            <ExternalLink size={11} />
          </a>
        )}
      </div>
    </div>
  );
}
