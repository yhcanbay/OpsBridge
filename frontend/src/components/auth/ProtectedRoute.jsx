import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth.js'
import LoadingState from '../common/LoadingState.jsx'
import SessionRecovery from './SessionRecovery.jsx'

function ProtectedRoute() {
    const { user, status } = useAuth()
    const location = useLocation()

    if (status === 'loading') {
        return <LoadingState />
    }

    if (status === 'error') {
        return <SessionRecovery />
    }

    if (!user) {
        return <Navigate to="/login" replace state={{ from: location }} />
    }

    return <Outlet />
}

export default ProtectedRoute
