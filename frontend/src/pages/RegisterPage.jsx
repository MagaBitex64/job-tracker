import React, { useState } from 'react';
import { register, setAuth } from '../api';

export default function RegisterPage({ onRegisterSuccess, onNavigate }) {
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!fullName.trim() || !email.trim() || !password) {
      setError('Заполните все обязательные поля');
      return;
    }

    if (password.length < 6) {
      setError('Пароль должен содержать минимум 6 символов');
      return;
    }

    setLoading(true);
    try {
      const data = await register(email.trim(), password, fullName.trim());
      setAuth(data.token, data.user);
      onRegisterSuccess(data.user);
    } catch (err) {
      setError(err.message || 'Ошибка при регистрации');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#f7f8f9] flex flex-col justify-center items-center p-4">
      <div className="w-full max-w-md">
        {/* Brand header */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-14 h-14 rounded-full bg-[#d6001c] font-black text-white text-2xl mb-3 shadow-md tracking-tighter">
            hh
          </div>
          <h1 className="text-2xl font-bold text-gray-900 tracking-tight">
            Регистрация соискателя
          </h1>
          <p className="text-sm text-gray-500 mt-1">
            Создайте личный кабинет для отслеживания всех вакансий
          </p>
        </div>

        {/* Card */}
        <div className="bg-white rounded-2xl border border-gray-200 shadow-sm p-8">
          {error && (
            <div className="mb-5 p-3.5 bg-red-50 border border-red-200 rounded-xl text-xs font-semibold text-[#d6001c]">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                Имя и фамилия
              </label>
              <input
                type="text"
                required
                maxLength={120}
                placeholder="Иван Иванов"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                className="w-full px-3.5 py-2.5 text-sm rounded-xl border border-gray-300 focus:outline-none focus:ring-2 focus:ring-[#d6001c] focus:border-transparent transition bg-white"
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                Электронная почта
              </label>
              <input
                type="email"
                required
                autoComplete="email"
                placeholder="ivanov@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full px-3.5 py-2.5 text-sm rounded-xl border border-gray-300 focus:outline-none focus:ring-2 focus:ring-[#d6001c] focus:border-transparent transition bg-white"
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                Пароль (минимум 6 символов)
              </label>
              <input
                type="password"
                required
                minLength={6}
                maxLength={100}
                autoComplete="new-password"
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full px-3.5 py-2.5 text-sm rounded-xl border border-gray-300 focus:outline-none focus:ring-2 focus:ring-[#d6001c] focus:border-transparent transition bg-white"
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full mt-2 py-3 px-4 rounded-xl bg-[#d6001c] hover:bg-[#ba0018] text-white font-bold text-sm shadow-xs transition cursor-pointer disabled:opacity-50"
            >
              {loading ? 'Создание профиля...' : 'Зарегистрироваться'}
            </button>
          </form>

          <div className="mt-6 pt-5 border-t border-gray-100 text-center text-xs text-gray-500">
            Уже есть аккаунт?{' '}
            <button
              type="button"
              onClick={() => onNavigate('/login')}
              className="font-bold text-[#d6001c] hover:underline cursor-pointer"
            >
              Войти в личный кабинет
            </button>
          </div>
        </div>

        {/* Footer info */}
        <p className="text-center text-[11px] text-gray-400 mt-6">
          Портфолио проект: Java 21 · Spring Boot 3 · React · PostgreSQL
        </p>
      </div>
    </div>
  );
}
