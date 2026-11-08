import React, { useState } from 'react';
import {
  X,
  Search,
  Globe,
  ExternalLink,
  Check,
  Loader2,
  Sparkles,
  AlertCircle,
  MapPin,
  Briefcase,
  Coins,
  Building2,
  SlidersHorizontal,
} from 'lucide-react';
import { searchVacancies, importVacancy } from '../api';

const QUICK_LOCATIONS = ['Алматы', 'Астана', 'Москва', 'Санкт-Петербург', 'Казахстан', 'Россия'];

const EXPERIENCE_OPTIONS = [
  { value: 'any', label: 'Любой опыт' },
  { value: 'noExperience', label: 'Без опыта (Junior / Стажёр)' },
  { value: 'between1And3', label: 'От 1 года до 3 лет (Junior+ / Middle)' },
  { value: 'between3And6', label: 'От 3 до 6 лет (Senior)' },
  { value: 'moreThan6', label: 'Более 6 лет (Lead / Архитектор)' },
];

export default function VacancySearchModal({ isOpen, onClose, onImportSuccess }) {
  const [query, setQuery] = useState('Java');
  const [source, setSource] = useState('ALL');
  const [experience, setExperience] = useState('noExperience');
  const [location, setLocation] = useState('');
  const [remoteOnly, setRemoteOnly] = useState(false);
  const [showFilters, setShowFilters] = useState(true);

  const [loading, setLoading] = useState(false);
  const [results, setResults] = useState([]);
  const [searched, setSearched] = useState(false);
  const [importedUrls, setImportedUrls] = useState(new Set());
  const [importingUrl, setImportingUrl] = useState(null);
  const [error, setError] = useState(null);

  if (!isOpen) return null;

  const handleSearch = async (e) => {
    if (e) e.preventDefault();
    if (!query.trim()) return;

    setLoading(true);
    setError(null);
    try {
      const data = await searchVacancies({
        query: query.trim(),
        source,
        experience,
        location: location.trim(),
        remoteOnly,
        limit: 25,
      });
      setResults(data || []);
      setSearched(true);
    } catch (err) {
      setError(err.message || 'Ошибка при поиске вакансий');
    } finally {
      setLoading(false);
    }
  };

  const handleImport = async (vacancy) => {
    const itemKey = vacancy.link || vacancy.position + vacancy.company;
    setImportingUrl(itemKey);
    try {
      // Build clean structured notes
      const notesParts = [];
      if (vacancy.location) notesParts.push(`Локация: ${vacancy.location}`);
      if (vacancy.experience) notesParts.push(`Опыт: ${vacancy.experience}`);
      if (vacancy.salary) notesParts.push(`Доход: ${vacancy.salary}`);
      if (vacancy.notes && !vacancy.notes.startsWith('Импортировано')) {
        notesParts.push(vacancy.notes);
      }
      notesParts.push(`[Источник: ${vacancy.source || 'Парсер'}]`);

      const imported = await importVacancy({
        company: vacancy.company,
        position: vacancy.position,
        status: 'WISHLIST',
        link: vacancy.link,
        notes: notesParts.join(' • '),
      });

      setImportedUrls((prev) => new Set([...prev, itemKey]));
      if (onImportSuccess) {
        onImportSuccess(imported);
      }
    } catch (err) {
      setError(err.message || 'Не удалось импортировать вакансию');
    } finally {
      setImportingUrl(null);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-black/40 backdrop-blur-2xs">
      <div className="bg-white rounded-xl shadow-2xl border border-gray-200 w-full max-w-4xl flex flex-col max-h-[90vh] overflow-hidden animate-in fade-in zoom-in-95 duration-150">
        
        {/* Modal Header */}
        <div className="px-5 py-3.5 border-b border-gray-200 flex items-center justify-between bg-gray-50/90">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-[#d6001c]/10 text-[#d6001c] flex items-center justify-center font-bold shrink-0">
              <Sparkles size={18} />
            </div>
            <div>
              <h3 className="font-bold text-gray-900 text-sm sm:text-base leading-tight">
                Автоматический поиск вакансий
              </h3>
              <p className="text-[11px] sm:text-xs text-gray-500">
                Фильтрация по опыту, городу/стране и удалённому формату (HH.ru + LinkedIn)
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 p-1.5 rounded-lg hover:bg-gray-100 transition cursor-pointer"
          >
            <X size={18} />
          </button>
        </div>

        {/* Search & Filters Section */}
        <div className="p-4 sm:p-5 border-b border-gray-100 bg-white">
          <form onSubmit={handleSearch} className="space-y-3">
            
            {/* Primary Search Bar */}
            <div className="flex flex-col sm:flex-row gap-2">
              <div className="relative flex-1">
                <span className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-gray-400">
                  <Search size={16} />
                </span>
                <input
                  type="text"
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                  placeholder="Должность или навык: Java Developer, Spring Boot, QA..."
                  className="w-full pl-9 pr-3 py-2 text-xs sm:text-sm rounded-lg border border-gray-300 focus:outline-none focus:ring-2 focus:ring-[#d6001c] transition bg-white"
                  autoFocus
                />
              </div>

              <div className="flex items-center gap-2">
                <select
                  value={source}
                  onChange={(e) => setSource(e.target.value)}
                  className="px-3 py-2 text-xs sm:text-sm rounded-lg border border-gray-300 focus:outline-none focus:ring-2 focus:ring-[#d6001c] bg-white transition cursor-pointer font-medium text-gray-700"
                >
                  <option value="ALL">Все (HH + LinkedIn)</option>
                  <option value="HH">HeadHunter (hh.ru)</option>
                  <option value="LINKEDIN">LinkedIn</option>
                </select>

                <button
                  type="button"
                  onClick={() => setShowFilters(!showFilters)}
                  className={`p-2 rounded-lg border text-xs sm:text-sm font-semibold transition cursor-pointer flex items-center gap-1.5 ${
                    showFilters || location || remoteOnly || experience !== 'any'
                      ? 'border-[#d6001c] text-[#d6001c] bg-red-50/60'
                      : 'border-gray-300 text-gray-700 hover:bg-gray-50'
                  }`}
                  title="Параметры опыта и локации"
                >
                  <SlidersHorizontal size={15} />
                  <span className="hidden sm:inline">Фильтры</span>
                </button>

                <button
                  type="submit"
                  disabled={loading || !query.trim()}
                  className="inline-flex items-center gap-2 px-4 py-2 text-xs sm:text-sm font-bold rounded-lg bg-[#d6001c] hover:bg-[#ba0018] disabled:opacity-50 text-white shadow-xs transition cursor-pointer whitespace-nowrap"
                >
                  {loading ? (
                    <>
                      <Loader2 size={16} className="animate-spin" />
                      <span>Ищем...</span>
                    </>
                  ) : (
                    <>
                      <Search size={16} />
                      <span>Искать</span>
                    </>
                  )}
                </button>
              </div>
            </div>

            {/* Extended Filters: Experience, City/Country, Remote */}
            {showFilters && (
              <div className="pt-2 border-t border-gray-100 grid grid-cols-1 md:grid-cols-12 gap-3 text-xs sm:text-sm">
                
                {/* Experience selector */}
                <div className="md:col-span-5">
                  <label className="block text-[11px] font-bold text-gray-600 uppercase tracking-wider mb-1">
                    Опыт работы
                  </label>
                  <div className="relative">
                    <select
                      value={experience}
                      onChange={(e) => setExperience(e.target.value)}
                      className="w-full px-3 py-1.5 text-xs sm:text-sm rounded-lg border border-gray-300 focus:outline-none focus:ring-2 focus:ring-[#d6001c] bg-white font-medium text-gray-700 cursor-pointer"
                    >
                      {EXPERIENCE_OPTIONS.map((opt) => (
                        <option key={opt.value} value={opt.value}>
                          {opt.label}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>

                {/* Location / City / Country Input */}
                <div className="md:col-span-4">
                  <label className="block text-[11px] font-bold text-gray-600 uppercase tracking-wider mb-1">
                    Город или страна
                  </label>
                  <div className="relative">
                    <span className="absolute inset-y-0 left-0 pl-2.5 flex items-center pointer-events-none text-gray-400">
                      <MapPin size={13} />
                    </span>
                    <input
                      type="text"
                      value={location}
                      onChange={(e) => setLocation(e.target.value)}
                      placeholder="Напр. Алматы, Москва, Казахстан..."
                      className="w-full pl-8 pr-7 py-1.5 text-xs sm:text-sm rounded-lg border border-gray-300 focus:outline-none focus:ring-2 focus:ring-[#d6001c] bg-white"
                    />
                    {location && (
                      <button
                        type="button"
                        onClick={() => setLocation('')}
                        className="absolute inset-y-0 right-0 pr-2 flex items-center text-gray-400 hover:text-gray-600 cursor-pointer"
                      >
                        <X size={12} />
                      </button>
                    )}
                  </div>
                </div>

                {/* Remote Checkbox */}
                <div className="md:col-span-3 flex items-end">
                  <label className="flex items-center gap-2 py-2 px-3 rounded-lg border border-gray-200 hover:bg-gray-50 transition cursor-pointer w-full select-none bg-white">
                    <input
                      type="checkbox"
                      checked={remoteOnly}
                      onChange={(e) => setRemoteOnly(e.target.checked)}
                      className="w-4 h-4 text-[#d6001c] rounded border-gray-300 focus:ring-[#d6001c] cursor-pointer"
                    />
                    <span className="text-xs font-semibold text-gray-700">Только удалёнка</span>
                  </label>
                </div>

                {/* Quick location pills */}
                <div className="md:col-span-12 flex flex-wrap items-center gap-1.5 pt-1">
                  <span className="text-[11px] text-gray-400 mr-1">Быстрый выбор:</span>
                  {QUICK_LOCATIONS.map((loc) => (
                    <button
                      key={loc}
                      type="button"
                      onClick={() => setLocation(loc)}
                      className={`text-[11px] px-2 py-0.5 rounded-md border transition cursor-pointer ${
                        location.toLowerCase() === loc.toLowerCase()
                          ? 'border-[#d6001c] bg-red-50 text-[#d6001c] font-bold'
                          : 'border-gray-200 bg-gray-50/80 text-gray-600 hover:bg-gray-100 hover:border-gray-300'
                      }`}
                    >
                      {loc}
                    </button>
                  ))}
                  {location && (
                    <button
                      type="button"
                      onClick={() => setLocation('')}
                      className="text-[11px] text-gray-400 hover:text-gray-600 underline ml-1 cursor-pointer"
                    >
                      Сбросить
                    </button>
                  )}
                </div>

              </div>
            )}
          </form>

          {error && (
            <div className="mt-3 p-2.5 bg-red-50 border border-red-200 rounded-lg text-xs text-[#d6001c] flex items-center gap-2">
              <AlertCircle size={15} className="shrink-0" />
              <span>{error}</span>
            </div>
          )}
        </div>

        {/* Results List */}
        <div className="flex-1 overflow-y-auto divide-y divide-gray-100 p-2 sm:p-4 bg-gray-50/40">
          {!searched && !loading && (
            <div className="py-14 text-center text-gray-500">
              <div className="w-12 h-12 mx-auto mb-3 rounded-full bg-gray-100 flex items-center justify-center text-gray-400">
                <Globe size={24} />
              </div>
              <p className="font-semibold text-sm text-gray-700">Настройте критерии и нажмите «Искать»</p>
              <p className="text-xs text-gray-400 mt-1 max-w-md mx-auto">
                Система найдёт только подходящие под ваш опыт и город вакансии с HeadHunter и LinkedIn и исключит нерелевантные позиции.
              </p>
            </div>
          )}

          {loading && (
            <div className="py-16 text-center text-gray-500">
              <Loader2 size={32} className="mx-auto mb-3 animate-spin text-[#d6001c]" />
              <p className="text-sm font-semibold text-gray-800">Парсим вакансии под ваши критерии...</p>
              <p className="text-xs text-gray-400 mt-1">
                Запрос: "{query}" • Опыт: {EXPERIENCE_OPTIONS.find(o => o.value === experience)?.label || experience}
                {location ? ` • Локация: ${location}` : ''}
                {remoteOnly ? ' • Удаленно' : ''}
              </p>
            </div>
          )}

          {searched && !loading && results.length === 0 && (
            <div className="py-14 text-center text-gray-500">
              <Building2 size={28} className="mx-auto mb-2 text-gray-400" />
              <p className="font-semibold text-gray-800 text-sm">Подходящих вакансий не найдено</p>
              <p className="text-xs text-gray-400 mt-1 max-w-md mx-auto">
                Попробуйте расширить опыт (например, выбрать «Любой опыт»), изменить город или снять флаг удалёнки.
              </p>
            </div>
          )}

          {searched && !loading && results.map((v, idx) => {
            const itemKey = v.link || `${v.position}-${v.company}-${idx}`;
            const isImported = importedUrls.has(itemKey);
            const isImporting = importingUrl === itemKey;
            const isHH = v.source?.toLowerCase().includes('headhunter') || v.source === 'HH';

            return (
              <div
                key={itemKey}
                className="p-3.5 sm:p-4 hover:bg-white rounded-lg transition-colors border border-transparent hover:border-gray-200 flex flex-col sm:flex-row sm:items-start justify-between gap-3 bg-white/80 mb-2 shadow-2xs"
              >
                <div className="flex-1 min-w-0 pr-2">
                  
                  {/* Top Badges */}
                  <div className="flex items-center gap-1.5 flex-wrap mb-1.5">
                    <span
                      className={`inline-flex items-center px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider ${
                        isHH
                          ? 'bg-red-50 text-[#d6001c] border border-red-200'
                          : 'bg-blue-50 text-[#0a66c2] border border-blue-200'
                      }`}
                    >
                      {isHH ? 'HH.ru' : 'LinkedIn'}
                    </span>

                    <span className="text-xs font-semibold text-gray-700 flex items-center gap-1">
                      <Building2 size={12} className="text-gray-400" />
                      {v.company}
                    </span>

                    {/* Location Badge */}
                    {v.location && (
                      <span className="inline-flex items-center gap-1 text-[11px] px-2 py-0.5 rounded-full bg-gray-100 text-gray-700 border border-gray-200">
                        <MapPin size={10} className="text-gray-500" />
                        <span>{v.location}</span>
                      </span>
                    )}

                    {/* Experience Badge */}
                    {v.experience && (
                      <span className="inline-flex items-center gap-1 text-[11px] px-2 py-0.5 rounded-full bg-amber-50 text-amber-800 border border-amber-200 font-medium">
                        <Briefcase size={10} className="text-amber-600" />
                        <span>{v.experience}</span>
                      </span>
                    )}

                    {/* Salary Badge */}
                    {v.salary && (
                      <span className="inline-flex items-center gap-1 text-[11px] px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-800 border border-emerald-200 font-semibold">
                        <Coins size={10} className="text-emerald-600" />
                        <span>{v.salary}</span>
                      </span>
                    )}
                  </div>

                  {/* Vacancy Title */}
                  <h4 className="text-sm font-bold text-gray-900 leading-snug">
                    {v.position}
                  </h4>

                  {/* Notes / Description */}
                  {v.notes && (
                    <p className="text-xs text-gray-600 line-clamp-2 mt-1">
                      {v.notes}
                    </p>
                  )}

                  {/* External Link */}
                  {v.link && (
                    <a
                      href={v.link}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="inline-flex items-center gap-1 text-[11px] text-gray-400 hover:text-[#d6001c] mt-2 transition font-medium"
                    >
                      <span>Открыть вакансию на {isHH ? 'hh.ru' : 'LinkedIn'}</span>
                      <ExternalLink size={11} />
                    </a>
                  )}
                </div>

                {/* Import Action */}
                <div className="shrink-0 flex items-center self-end sm:self-center">
                  {isImported ? (
                    <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-bold bg-green-50 text-green-700 border border-green-200">
                      <Check size={14} />
                      <span>В доске</span>
                    </span>
                  ) : (
                    <button
                      type="button"
                      disabled={isImporting}
                      onClick={() => handleImport(v)}
                      className="inline-flex items-center gap-1.5 px-3.5 py-1.5 text-xs font-bold rounded-lg bg-gray-900 hover:bg-[#d6001c] text-white transition cursor-pointer shadow-2xs disabled:opacity-50"
                    >
                      {isImporting ? (
                        <>
                          <Loader2 size={13} className="animate-spin" />
                          <span>Сохранение...</span>
                        </>
                      ) : (
                        <>
                          <Sparkles size={13} />
                          <span>В "В планах"</span>
                        </>
                      )}
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>

        {/* Modal Footer */}
        <div className="px-5 py-3 border-t border-gray-200 bg-gray-50 flex items-center justify-between text-xs text-gray-500">
          <span>Найдено предложений: <strong className="text-gray-800">{results.length}</strong></span>
          <button
            onClick={onClose}
            className="px-3.5 py-1.5 rounded-lg border border-gray-300 font-semibold text-gray-700 hover:bg-white transition cursor-pointer"
          >
            Закрыть
          </button>
        </div>

      </div>
    </div>
  );
}
