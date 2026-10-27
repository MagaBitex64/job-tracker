import React, { useEffect } from 'react';
import { CheckCircle2, AlertCircle, X } from 'lucide-react';

export default function Toast({ toast, onClose }) {
  useEffect(() => {
    if (!toast) return;
    const timer = setTimeout(() => {
      onClose();
    }, 3500);
    return () => clearTimeout(timer);
  }, [toast, onClose]);

  if (!toast) return null;

  const isError = toast.type === 'error';

  return (
    <div className="fixed bottom-5 right-5 z-50 animate-in slide-in-from-bottom-5 duration-200">
      <div
        className={`flex items-center gap-3 px-4 py-3 rounded-xl shadow-lg border text-sm font-medium ${
          isError
            ? 'bg-red-50 text-red-900 border-red-200'
            : 'bg-emerald-50 text-emerald-900 border-emerald-200'
        }`}
      >
        {isError ? (
          <AlertCircle size={18} className="text-[#d6001c] shrink-0" />
        ) : (
          <CheckCircle2 size={18} className="text-emerald-600 shrink-0" />
        )}
        <span>{toast.message}</span>
        <button
          onClick={onClose}
          className="ml-2 text-gray-400 hover:text-gray-700 cursor-pointer"
        >
          <X size={14} />
        </button>
      </div>
    </div>
  );
}
