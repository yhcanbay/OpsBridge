import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '../api/ApiError.js';
import { AuthProvider } from '../context/AuthProvider.jsx';
import { authService } from '../services/authService.js';
import App from './App.jsx';

vi.mock('../services/authService.js', () => ({
  authService: {
    getCurrentUser: vi.fn(),
    login: vi.fn(),
    logout: vi.fn(),
  },
}));

vi.mock('../pages/ProjectsPage.jsx', async () => {
  const { useParams } = await import('react-router-dom');
  return {
    ProjectsPage() {
      const { projectId } = useParams();
      return <h1>Proje ekranı: {projectId ?? 'liste'}</h1>;
    },
  };
});

afterEach(cleanup);

function renderRoute(path) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <AuthProvider>
        <App />
      </AuthProvider>
    </MemoryRouter>,
  );
}

describe('authentication and route navigation', () => {
  it('returns to the requested project URL after logging in', async () => {
    authService.getCurrentUser.mockRejectedValue(new ApiError(401, 'Oturum bulunamadı.'));
    authService.login.mockResolvedValue({ username: 'admin', role: 'ADMIN' });
    renderRoute('/projects/reference-project');

    const username = await screen.findByLabelText('Kullanıcı adı');
    fireEvent.change(username, { target: { value: 'admin' } });
    fireEvent.change(screen.getByLabelText('Parola'), { target: { value: 'test-only' } });
    fireEvent.click(screen.getByRole('button', { name: 'Giriş yap →' }));

    expect(
      await screen.findByRole('heading', { name: 'Proje ekranı: reference-project' }),
    ).toBeTruthy();
    expect(authService.login).toHaveBeenCalledWith({ username: 'admin', password: 'test-only' });
  });

  it('offers retry for a connection failure instead of treating it as logout', async () => {
    authService.getCurrentUser.mockRejectedValueOnce(new ApiError(0, 'Backend erişilemiyor.'));
    authService.getCurrentUser.mockResolvedValueOnce({ username: 'admin', role: 'ADMIN' });
    renderRoute('/projects');

    expect(await screen.findByRole('heading', { name: 'Oturum kontrol edilemedi' })).toBeTruthy();
    expect(screen.queryByLabelText('Parola')).toBeNull();
    fireEvent.click(screen.getByRole('button', { name: 'Tekrar dene' }));

    await waitFor(() =>
      expect(screen.getByRole('heading', { name: 'Proje ekranı: liste' })).toBeTruthy(),
    );
  });

  it('renders a missing-page view for unknown routes', async () => {
    authService.getCurrentUser.mockResolvedValue({ username: 'admin', role: 'ADMIN' });
    renderRoute('/unknown');
    expect(await screen.findByRole('heading', { name: 'Sayfa bulunamadı' })).toBeTruthy();
  });
});
