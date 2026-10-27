// api.js — REST API client (ES module)

import { getToken, clearAuth } from './auth.js';

const BASE = '/api/v1';

async function request(method, path, body = null) {
  const token = getToken();
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const options = { method, headers };
  if (body !== null) options.body = JSON.stringify(body);

  const res = await fetch(`${BASE}${path}`, options);

  if (res.status === 401) {
    clearAuth();
    window.location.href = '/login.html';
    return;
  }

  if (res.status === 204) return null;

  const data = await res.json();
  if (!res.ok) {
    const msg = data.message || data.error || 'Request failed';
    const err = new Error(msg);
    err.data = data;
    throw err;
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

export function getApplications({ status, search, page = 0, size = 200, sort = 'createdAt,desc' } = {}) {
  const params = new URLSearchParams();
  if (status)  params.set('status', status);
  if (search)  params.set('search', search);
  params.set('page', page);
  params.set('size', size);
  params.set('sort', sort);
  return request('GET', `/applications?${params}`);
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
