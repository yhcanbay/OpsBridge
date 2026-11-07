import { useState } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth.js'

function DashboardLayout() {
    const { user, logout } = useAuth()
    const navigate = useNavigate()
    const [isLoggingOut, setIsLoggingOut] = useState(false)
    const [error, setError] = useState('')

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
                <div className="sidebar-note">
                    <span className="note-mark" aria-hidden="true">
                        ↗
                    </span>
                    <strong>Önce sağlam bir temel.</strong>
                    <p>
                        Proje profillerini hazırlayın; operasyon bağlantılarını adım adım ekleyin.
                    </p>
                </div>
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
