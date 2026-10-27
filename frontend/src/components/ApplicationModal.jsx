import React, { useState, useEffect } from 'react';
import { STATUS_LIST, STATUS_CONFIG } from '../constants';
import { X } from 'lucide-react';

export default function ApplicationModal({ app, isOpen, onClose, onSave }) {
  const isEdit = Boolean(app);

  const [company, setCompany] = useState('');
  const [position, setPosition] = useState('');
  const [status, setStatus] = useState('WISHLIST');
  const [link, setLink] = useState('');
  const [notes, setNotes] = useState('');
  const [appliedAt, setAppliedAt] = useState('');
  const [errors, setErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (app) {
      setCompany(app.company || '');
      setPosition(app.position || '');
      setStatus(app.status || 'WISHLIST');
      setLink(app.link || '');
      setNotes(app.notes || '');
      setAppliedAt(app.appliedAt || '');
    } else {
      setCompany('');
      setPosition('');
      setStatus('WISHLIST');
      setLink('');
      setNotes('');
      setAppliedAt(new Date().toISOString().split('T')[0]);
    }
    setErrors({});
  }, [app, isOpen]);

  if (!isOpen) return null;

  const validate = () => {
    const errs = {};
    if (!company.trim()) {
      errs.company = 'Укажите название компании';
    } else if (company.trim().length > 120) {
      errs.company = 'Название компании не должно превышать 120 символов';
    }

    if (!position.trim()) {
      errs.position = 'Укажите название должности';
    } else if (position.trim().length > 120) {
      errs.position = 'Название должности не должно превышать 120 символов';
    }

    if (link.trim() && link.trim().length > 500) {
      errs.link = 'Ссылка не должна превышать 500 символов';
    }

    if (notes.trim() && notes.trim().length > 2000) {
      errs.notes = 'Заметки не должны превышать 2000 символов';
    }

    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validate()) return;

    setSubmitting(true);
    try {
      const payload = {
        company: company.trim(),
        position: position.trim(),
        status,
        link: link.trim() || null,
        notes: notes.trim() || null,
        appliedAt: appliedAt || null,
      };
      await onSave(payload, app ? app.id : null);
      onClose();
    } catch (err) {
      setErrors((prev) => ({ ...prev, global: err.message }));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-2xs">
      <div className="bg-white rounded-xl shadow-xl border border-gray-200 w-full max-w-lg overflow-hidden animate-in fade-in zoom-in-95 duration-150">
        {/* Modal Header */}
        <div className="px-6 py-4 border-b border-gray-200 flex items-center justify-between bg-gray-50/70">
          <h3 className="font-bold text-gray-900 text-base">
            {isEdit ? 'Редактировать отклик' : 'Новая вакансия'}
          </h3>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 p-1 rounded-md hover:bg-gray-100 transition-colors cursor-pointer"
          >
            <X size={18} />
          </button>
        </div>

        {/* Modal Form */}
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {errors.global && (
            <div className="p-3 bg-red-50 border border-red-200 rounded-lg text-xs text-[#d6001c] font-medium">
              {errors.global}
            </div>
          )}

          <div>
            <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1">
              Компания <span className="text-[#d6001c]">*</span>
            </label>
            <input
              type="text"
              required
              maxLength={120}
              placeholder="Например, Яндекс или Сбер"
              value={company}
              onChange={(e) => setCompany(e.target.value)}
              className={`w-full px-3 py-2 text-sm rounded-lg border ${
                errors.company ? 'border-[#d6001c]' : 'border-gray-300'
              } focus:outline-none focus:ring-2 focus:ring-[#d6001c] transition`}
            />
            {errors.company && (
              <p className="mt-1 text-xs text-[#d6001c]">{errors.company}</p>
            )}
          </div>

          <div>
            <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1">
              Должность <span className="text-[#d6001c]">*</span>
            </label>
            <input
              type="text"
              required
              maxLength={120}
              placeholder="Например, Java Backend Developer"
              value={position}
              onChange={(e) => setPosition(e.target.value)}
              className={`w-full px-3 py-2 text-sm rounded-lg border ${
                errors.position ? 'border-[#d6001c]' : 'border-gray-300'
              } focus:outline-none focus:ring-2 focus:ring-[#d6001c] transition`}
            />
            {errors.position && (
              <p className="mt-1 text-xs text-[#d6001c]">{errors.position}</p>
            )}
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1">
                Этап отклика
              </label>
              <select
                value={status}
                onChange={(e) => setStatus(e.target.value)}
                className="w-full px-3 py-2 text-sm rounded-lg border border-gray-300 focus:outline-none focus:ring-2 focus:ring-[#d6001c] transition bg-white"
              >
                {STATUS_LIST.map((s) => (
                  <option key={s} value={s}>
                    {STATUS_CONFIG[s].label}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1">
                Дата отклика
              </label>
              <input
                type="date"
                value={appliedAt}
                onChange={(e) => setAppliedAt(e.target.value)}
                className="w-full px-3 py-2 text-sm rounded-lg border border-gray-300 focus:outline-none focus:ring-2 focus:ring-[#d6001c] transition bg-white"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1">
              Ссылка на вакансию (hh.ru / сайт работодателя)
            </label>
            <input
              type="url"
              maxLength={500}
              placeholder="https://hh.ru/vacancy/..."
              value={link}
              onChange={(e) => setLink(e.target.value)}
              className={`w-full px-3 py-2 text-sm rounded-lg border ${
                errors.link ? 'border-[#d6001c]' : 'border-gray-300'
              } focus:outline-none focus:ring-2 focus:ring-[#d6001c] transition`}
            />
            {errors.link && (
              <p className="mt-1 text-xs text-[#d6001c]">{errors.link}</p>
            )}
          </div>

          <div>
            <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1">
              Заметки и комментарии
            </label>
            <textarea
              rows={3}
              maxLength={2000}
              placeholder="Контакты рекрутера, зарплатные ожидания, детали тестового..."
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              className={`w-full px-3 py-2 text-sm rounded-lg border ${
                errors.notes ? 'border-[#d6001c]' : 'border-gray-300'
              } focus:outline-none focus:ring-2 focus:ring-[#d6001c] transition resize-none`}
            />
            {errors.notes && (
              <p className="mt-1 text-xs text-[#d6001c]">{errors.notes}</p>
            )}
          </div>

          {/* Modal Footer Buttons */}
          <div className="pt-3 border-t border-gray-100 flex items-center justify-end gap-3">
            <button
              type="button"
              onClick={onClose}
              disabled={submitting}
              className="px-4 py-2 text-sm font-medium rounded-lg border border-gray-300 text-gray-700 hover:bg-gray-50 transition cursor-pointer"
            >
              Отмена
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="px-5 py-2 text-sm font-bold rounded-lg bg-[#d6001c] hover:bg-[#ba0018] text-white shadow-xs transition cursor-pointer disabled:opacity-50"
            >
              {submitting ? 'Сохранение...' : isEdit ? 'Сохранить изменения' : 'Добавить вакансию'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
