import React, { useState } from 'react';
import { X, Search, Globe, ExternalLink, Check, Loader2, Sparkles, AlertCircle } from 'lucide-react';
import { searchVacancies, importVacancy } from '../api';

export default function VacancySearchModal({ isOpen, onClose, onImportSuccess }) {
  const [query, setQuery] = useState('Java');
  const [source, setSource] = useState('ALL');
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
      const data = await searchVacancies(query.trim(), source, 20);
      setResults(data || []);
      setSearched(true);
    } catch (err) {
      setError(err.message || 'Ошибка при поиске вакансий');
    } finally {
      setLoading(false);
    }
  };

  const handleImport = async (vacancy) => {
    setImportingUrl(vacancy.link || vacancy.position + vacancy.company);
    try {
      const imported = await importVacancy({
        company: vacancy.company,
        position: vacancy.position,
        status: 'WISHLIST',
        link: vacancy.link,
        notes: vacancy.notes ? `${vacancy.notes} [Источник: ${vacancy.source}]` : `[Источник: ${vacancy.source}]`,
      });

      setImportedUrls((prev) => new Set([...prev, vacancy.link || vacancy.position + vacancy.company]));
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
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-2xs">
      <div className="bg-white rounded-xl shadow-2xl border border-gray-200 w-full max-w-3xl flex flex-col max-h-[88vh] overflow-hidden animate-in fade-in zoom-in-95 duration-150">
        
        {/* Modal Header */}
        <div className="px-6 py-4 border-b border-gray-200 flex items-center justify-between bg-gray-50/80">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-[#d6001c]/10 text-[#d6001c] flex items-center justify-center font-bold">
              <Sparkles size={18} />
            </div>
            <div>
              <h3 className="font-bold text-gray-900 text-base leading-tight">
                Автоматический поиск вакансий
              </h3>
              <p className="text-xs text-gray-500">
                Парсинг актуальных предложений с HeadHunter и LinkedIn
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 p-1.5 rounded-lg hover:bg-gray-100 transition-colors cursor-pointer"
          >
            <X size={18} />
          </button>
        </div>

        {/* Search controls */}
        <div className="p-4 sm:p-6 border-b border-gray-100 bg-white">
          <form onSubmit={handleSearch} className="flex flex-col sm:flex-row gap-2.5">
            <div className="relative flex-1">
              <span className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-gray-400">
                <Search size={16} />
              </span>
              <input
                type="text"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="Например: Java Developer, Spring Boot, QA..."
                className="w-full pl-9 pr-3 py-2 text-sm rounded-lg border border-gray-300 focus:outline-none focus:ring-2 focus:ring-[#d6001c] transition bg-white"
                autoFocus
              />
            </div>

            <div className="flex items-center gap-2">
              <select
                value={source}
                onChange={(e) => setSource(e.target.value)}
                className="px-3 py-2 text-sm rounded-lg border border-gray-300 focus:outline-none focus:ring-2 focus:ring-[#d6001c] bg-white transition cursor-pointer font-medium text-gray-700"
              >
                <option value="ALL">Все (HH.ru + LinkedIn)</option>
                <option value="HH">HeadHunter (hh.ru)</option>
                <option value="LINKEDIN">LinkedIn</option>
              </select>

              <button
                type="submit"
                disabled={loading || !query.trim()}
                className="inline-flex items-center gap-2 px-4 py-2 text-sm font-bold rounded-lg bg-[#d6001c] hover:bg-[#ba0018] disabled:opacity-50 text-white shadow-xs transition cursor-pointer whitespace-nowrap"
              >
                {loading ? (
                  <>
                    <Loader2 size={16} className="animate-spin" />
                    <span>Ищем...</span>
                  </>
                ) : (
                  <>
                    <Search size={16} />
                    <span>Найти</span>
                  </>
                )}
              </button>
            </div>
          </form>

          {error && (
            <div className="mt-3 p-3 bg-red-50 border border-red-200 rounded-lg text-xs text-[#d6001c] flex items-center gap-2">
              <AlertCircle size={15} className="shrink-0" />
              <span>{error}</span>
            </div>
          )}
        </div>

        {/* Results List */}
        <div className="flex-1 overflow-y-auto divide-y divide-gray-100 p-2 sm:p-4 bg-gray-50/30">
          {!searched && !loading && (
            <div className="py-12 text-center text-gray-500">
              <div className="w-12 h-12 mx-auto mb-3 rounded-full bg-gray-100 flex items-center justify-center text-gray-400">
                <Globe size={24} />
              </div>
              <p className="font-medium text-sm text-gray-700">Введите ключевое слово для поиска</p>
              <p className="text-xs text-gray-400 mt-1">
                Система автоматически спарсит вакансии и позволит в 1 клик перенести их на вашу доску
              </p>
            </div>
          )}

          {loading && (
            <div className="py-16 text-center text-gray-500">
              <Loader2 size={32} className="mx-auto mb-3 animate-spin text-[#d6001c]" />
              <p className="text-sm font-medium text-gray-700">Ищем свежие вакансии на HH.ru и LinkedIn...</p>
              <p className="text-xs text-gray-400 mt-1">Парсинг данных по запросу "{query}"</p>
            </div>
          )}

          {searched && !loading && results.length === 0 && (
            <div className="py-12 text-center text-gray-500">
              <p className="font-semibold text-gray-700">По запросу "{query}" ничего не найдено</p>
              <p className="text-xs text-gray-400 mt-1">Попробуйте изменить формулировку или выбрать другую платформу</p>
            </div>
          )}

          {searched && !loading && results.map((v, idx) => {
            const itemKey = v.link || `${v.position}-${v.company}-${idx}`;
            const isImported = importedUrls.has(v.link || v.position + v.company);
            const isImporting = importingUrl === (v.link || v.position + v.company);
            const isHH = v.source?.toLowerCase().includes('headhunter');

            return (
              <div
                key={itemKey}
                className="p-3.5 sm:p-4 hover:bg-white rounded-lg transition-colors border border-transparent hover:border-gray-200 flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-white/70 mb-1.5 shadow-2xs"
              >
                <div className="flex-1 min-w-0 pr-2">
                  <div className="flex items-center gap-2 flex-wrap mb-1">
                    <span
                      className={`inline-flex items-center px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider ${
                        isHH
                          ? 'bg-red-50 text-[#d6001c] border border-red-200'
                          : 'bg-blue-50 text-[#0a66c2] border border-blue-200'
                      }`}
                    >
                      {v.source || 'HH'}
                    </span>
                    <span className="text-xs font-semibold text-gray-500">{v.company}</span>
                  </div>

                  <h4 className="text-sm font-bold text-gray-900 leading-snug truncate">
                    {v.position}
                  </h4>

                  {v.notes && (
                    <p className="text-xs text-gray-600 line-clamp-2 mt-1">
                      {v.notes}
                    </p>
                  )}

                  {v.link && (
                    <a
                      href={v.link}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="inline-flex items-center gap-1 text-[11px] text-gray-400 hover:text-[#d6001c] mt-1.5 transition-colors"
                    >
                      <span>Открыть на источнике</span>
                      <ExternalLink size={11} />
                    </a>
                  )}
                </div>

                <div className="shrink-0 flex items-center">
                  {isImported ? (
                    <span className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg text-xs font-bold bg-green-50 text-green-700 border border-green-200">
                      <Check size={14} />
                      <span>В доске</span>
                    </span>
                  ) : (
                    <button
                      type="button"
                      disabled={isImporting}
                      onClick={() => handleImport(v)}
                      className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-bold rounded-lg bg-gray-900 hover:bg-[#d6001c] text-white transition cursor-pointer shadow-2xs disabled:opacity-50"
                    >
                      {isImporting ? (
                        <>
                          <Loader2 size={13} className="animate-spin" />
                          <span>Сохраняем...</span>
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
        <div className="px-6 py-3 border-t border-gray-200 bg-gray-50 flex items-center justify-between text-xs text-gray-500">
          <span>Найдено предложений: {results.length}</span>
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
