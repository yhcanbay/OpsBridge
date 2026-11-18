import { useEffect, useState } from 'react'
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth.js'

function DashboardLayout() {
    const { user, logout } = useAuth()
    const navigate = useNavigate()
    const { pathname } = useLocation()
    const [isLoggingOut, setIsLoggingOut] = useState(false)
    const [error, setError] = useState('')

    useEffect(() => {
        document.scrollingElement?.scrollTo(0, 0)
    }, [pathname])

    async function handleLogout() {
        setIsLoggingOut(true)
        setError('')

        try {
            await logout()
            navigate('/login', { replace: true })
        } catch (failure) {
            setError(failure.message)
        } finally {
            setIsLoggingOut(false)
        }
    }

    return (
        <div className="app-shell">
            <aside className="sidebar">
                <div className="brand">
                    <span className="brand-symbol" aria-hidden="true">
                        ob
                    </span>{' '}
                    OpsBridge
                </div>
                <p className="sidebar-label">ÇALIŞMA ALANI</p>
                <NavLink
                    to="/projects"
                    className={({ isActive }) => (isActive ? 'nav-item active' : 'nav-item')}
                >
                    <span aria-hidden="true">▦</span> Projeler
                </NavLink>
                <div className="account">
                    <span className="avatar">{user.username.slice(0, 1).toUpperCase()}</span>
                    <div>
                        <strong>{user.username}</strong>
                        <small>Yönetici · {user.role}</small>
                    </div>
                    <button
                        className="logout-button"
                        onClick={handleLogout}
                        disabled={isLoggingOut}
                        aria-label="Çıkış yap"
                    >
                        {isLoggingOut ? '…' : '↪'}
                    </button>
                </div>
            </aside>
            <main className="workspace">
                <header className="topbar">
                    <span>
                        Çalışma alanı <span className="breadcrumb-divider">/</span>
                        <strong>Projeler</strong>
                    </span>
                    <span className="local-badge">Yerel geliştirme</span>
                </header>
                {error && (
                    <p className="notice error" role="alert">
                        {error}
                    </p>
                )}
                <Outlet />
            </main>
        </div>
    )
}

export default DashboardLayout
