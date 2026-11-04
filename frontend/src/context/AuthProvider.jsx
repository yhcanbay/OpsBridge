import { useCallback, useEffect, useRef, useState } from 'react';
import { AUTH_EXPIRED_EVENT, isRequestCancelled } from '../api/httpClient.js';
import { authService } from '../services/authService.js';
import { AuthContext } from './AuthContext.js';

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [status, setStatus] = useState('loading');
  const [error, setError] = useState('');
  const sessionRequest = useRef(null);

  const restoreSession = useCallback(async () => {
    sessionRequest.current?.abort();
    const controller = new AbortController();
    sessionRequest.current = controller;
    setStatus('loading');
    setError('');

    try {
      const currentUser = await authService.getCurrentUser({ signal: controller.signal });
      if (controller.signal.aborted) {
        return;
      }
      setUser(currentUser);
      setStatus('authenticated');
    } catch (failure) {
      if (isRequestCancelled(failure) || controller.signal.aborted) {
        return;
      }
      setUser(null);

      if (failure.status === 401) {
        setStatus('anonymous');
        return;
      }

      setError(failure.message);
      setStatus('error');
    }
  }, []);

  useEffect(() => {
    let active = true;

    function handleSessionExpired() {
      if (active) {
        setUser(null);
        setStatus('anonymous');
        setError('');
      }
    }

    window.addEventListener(AUTH_EXPIRED_EVENT, handleSessionExpired);
    restoreSession();

    return () => {
      active = false;
      sessionRequest.current?.abort();
      window.removeEventListener(AUTH_EXPIRED_EVENT, handleSessionExpired);
    };
  }, [restoreSession]);

  async function login(credentials) {
    sessionRequest.current?.abort();
    const currentUser = await authService.login(credentials);
    setUser(currentUser);
    setStatus('authenticated');
    setError('');
  }

  async function logout() {
    sessionRequest.current?.abort();
    await authService.logout();
    setUser(null);
    setStatus('anonymous');
    setError('');
  }

  const value = { user, status, error, login, logout, restoreSession };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
