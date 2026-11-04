import { httpClient, loadCsrfToken, resetCsrfToken } from '../api/httpClient.js';

async function getCurrentUser({ signal } = {}) {
  const response = await httpClient.get('/auth/me', { signal });
  return response.data;
}

async function login(credentials) {
  resetCsrfToken();

  const form = new URLSearchParams({
    username: credentials.username,
    password: credentials.password,
  });

  await httpClient.post('/auth/login', form, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
  });

  resetCsrfToken();
  await loadCsrfToken();
  return getCurrentUser();
}

async function logout() {
  await httpClient.post('/auth/logout');
  resetCsrfToken();
}

export const authService = { getCurrentUser, login, logout };
