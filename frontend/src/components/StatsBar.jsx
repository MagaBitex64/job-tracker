import React from 'react';

export default function StatsBar({ stats }) {
  if (!stats) return null;

  const items = [
    { label: 'Всего', count: stats.total, color: 'text-gray-900', bg: 'bg-gray-100' },
    { label: 'В планах', count: stats.wishlist, color: 'text-gray-700', bg: 'bg-gray-100' },
    { label: 'Отклики', count: stats.applied, color: 'text-blue-700', bg: 'bg-blue-50' },
    { label: 'Интервью', count: stats.interview, color: 'text-amber-700', bg: 'bg-amber-50' },
    { label: 'Офферы', count: stats.offer, color: 'text-emerald-700', bg: 'bg-emerald-50' },
    { label: 'Отказы', count: stats.rejected, color: 'text-rose-700', bg: 'bg-rose-50' },
  ];

  return (
    <div className="flex flex-wrap items-center gap-2 sm:gap-3 py-1">
      {items.map((item, idx) => (
        <div
          key={idx}
          className={`flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-medium ${item.bg} border border-black/5`}
        >
          <span className={`font-bold text-sm ${item.color}`}>{item.count}</span>
          <span className="text-gray-600">{item.label}</span>
        </div>
      ))}
    </div>
  );
}
