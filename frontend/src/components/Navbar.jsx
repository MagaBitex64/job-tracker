import React from 'react';
import { LogOut, ExternalLink, Briefcase } from 'lucide-react';

export default function Navbar({ user, onLogout }) {
  return (
    <header className="bg-white border-b border-gray-200 sticky top-0 z-30 shadow-xs">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        {/* Brand Logo - HeadHunter Red Identity */}
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-full bg-[#d6001c] flex items-center justify-center font-bold text-white text-base tracking-tighter shadow-sm select-none">
            hh
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="text-lg font-bold tracking-tight text-gray-900">
                Мои отклики
              </span>
              <span className="hidden sm:inline-block px-2 py-0.5 text-xs font-semibold rounded-full bg-red-50 text-[#d6001c] border border-red-100">
                Job Tracker
              </span>
            </div>
            <p className="text-[11px] text-gray-400 hidden sm:block">
              Канбан-доска для поиска работы
            </p>
          </div>
        </div>

        {/* User Info & Actions */}
        <div className="flex items-center gap-4">
          <a
            href="/swagger-ui.html"
            target="_blank"
            rel="noopener noreferrer"
            className="hidden md:inline-flex items-center gap-1.5 text-xs text-gray-500 hover:text-[#d6001c] font-medium transition-colors"
          >
            <span>Swagger API</span>
            <ExternalLink size={13} />
          </a>

          <div className="h-4 w-px bg-gray-200 hidden md:block" />

          {user && (
            <div className="text-right hidden sm:block">
              <div className="text-sm font-semibold text-gray-900 truncate max-w-[180px]">
                {user.fullName || user.email}
              </div>
              <div className="text-xs text-gray-500 truncate max-w-[180px]">
                {user.email}
              </div>
            </div>
          )}

          <button
            onClick={onLogout}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-lg border border-gray-300 hover:border-gray-400 hover:bg-gray-50 text-gray-700 transition-colors cursor-pointer"
            title="Выйти из аккаунта"
          >
            <LogOut size={14} className="text-gray-500" />
            <span className="hidden sm:inline">Выйти</span>
          </button>
        </div>
      </div>
    </header>
  );
}
