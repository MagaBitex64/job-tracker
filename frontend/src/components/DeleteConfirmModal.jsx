import React from 'react';
import { AlertTriangle } from 'lucide-react';

export default function DeleteConfirmModal({ isOpen, onClose, onConfirm, deleting }) {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-2xs">
      <div className="bg-white rounded-xl shadow-xl border border-gray-200 max-w-sm w-full p-5 animate-in fade-in zoom-in-95 duration-150">
        <div className="flex items-start gap-3.5">
          <div className="w-10 h-10 rounded-full bg-red-100 flex items-center justify-center text-[#d6001c] shrink-0">
            <AlertTriangle size={20} />
          </div>
          <div>
            <h4 className="font-bold text-gray-900 text-sm">
              Удалить запись о вакансии?
            </h4>
            <p className="text-xs text-gray-500 mt-1">
              Это действие нельзя отменить. Вакансия будет удалена из вашей доски навсегда.
            </p>
          </div>
        </div>

        <div className="mt-5 flex items-center justify-end gap-2.5">
          <button
            onClick={onClose}
            disabled={deleting}
            className="px-3.5 py-1.5 text-xs font-semibold rounded-lg border border-gray-300 text-gray-700 hover:bg-gray-50 transition cursor-pointer"
          >
            Отмена
          </button>
          <button
            onClick={onConfirm}
            disabled={deleting}
            className="px-4 py-1.5 text-xs font-bold rounded-lg bg-[#d6001c] hover:bg-[#ba0018] text-white shadow-xs transition cursor-pointer disabled:opacity-50"
          >
            {deleting ? 'Удаление...' : 'Да, удалить'}
          </button>
        </div>
      </div>
    </div>
  );
}
