import { Navigate, useLocation } from 'react-router-dom';
import LoginForm from '../components/auth/LoginForm.jsx';
import { SessionRecovery } from '../components/auth/SessionRecovery.jsx';
import { LoadingState } from '../components/common/LoadingState.jsx';
import { useAuth } from '../hooks/useAuth.js';

export function LoginPage() {
  const { user, status } = useAuth();
  const location = useLocation();
  const previousLocation = location.state?.from;
  const destination = previousLocation
    ? `${previousLocation.pathname}${previousLocation.search ?? ''}`
    : '/projects';

  if (status === 'loading') {
    return <LoadingState />;
  }

  if (status === 'error') {
    return <SessionRecovery />;
  }

  if (user) {
    return <Navigate to={destination} replace />;
  }

  return <LoginForm />;
}
