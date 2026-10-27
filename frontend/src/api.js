// api.js - REST API Client for Job Tracker

const BASE_URL = '/api/v1';

export function getToken() {
  return localStorage.getItem('jt_token');
}

export function getUser() {
  try {
    const raw = localStorage.getItem('jt_user');
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function setAuth(token, user) {
  localStorage.setItem('jt_token', token);
  localStorage.setItem('jt_user', JSON.stringify(user));
}

export function clearAuth() {
  localStorage.removeItem('jt_token');
  localStorage.removeItem('jt_user');
}

async function request(method, path, body = null) {
  const token = getToken();
  const headers = { 'Content-Type': 'application/json' };
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const options = { method, headers };
  if (body !== null) {
    options.body = JSON.stringify(body);
  }

  const res = await fetch(`${BASE_URL}${path}`, options);

  if (res.status === 401) {
    clearAuth();
    if (window.location.pathname !== '/login' && window.location.pathname !== '/register') {
      window.history.pushState({}, '', '/login');
      window.dispatchEvent(new PopStateEvent('popstate'));
    }
    throw new Error('Сессия истекла или требуется авторизация');
  }

  if (res.status === 204) {
    return null;
  }

  const data = await res.json();
  if (!res.ok) {
    let msg = data.message || data.error || 'Ошибка запроса';
    if (data.fieldErrors) {
      const fieldMsgs = Object.values(data.fieldErrors).join(', ');
      msg = `${msg}: ${fieldMsgs}`;
    }
    const error = new Error(msg);
    error.data = data;
    throw error;
  }
  return data;
}

export function login(email, password) {
  return request('POST', '/auth/login', { email, password });
}

export function register(email, password, fullName) {
  return request('POST', '/auth/register', { email, password, fullName });
}

export function getMe() {
  return request('GET', '/auth/me');
}

export function getApplications({ status, search, page = 0, size = 200 } = {}) {
  const params = new URLSearchParams();
  if (status) params.set('status', status);
  if (search) params.set('search', search.trim());
  params.set('page', page);
  params.set('size', size);
  params.set('sort', 'createdAt,desc');
  return request('GET', `/applications?${params.toString()}`);
}

export function getApplicationById(id) {
  return request('GET', `/applications/${id}`);
}

export function createApplication(data) {
  return request('POST', '/applications', data);
}

export function updateApplication(id, data) {
  return request('PUT', `/applications/${id}`, data);
}

export function updateStatus(id, status) {
  return request('PATCH', `/applications/${id}/status`, { status });
}

export function deleteApplication(id) {
  return request('DELETE', `/applications/${id}`);
}

export function getStats() {
  return request('GET', '/applications/stats');
}
