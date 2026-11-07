import { useAuth } from '../../hooks/useAuth.js'

function SessionRecovery() {
    const { error, restoreSession } = useAuth()

    return (
        <main className="empty-state">
            <h1>Oturum kontrol edilemedi</h1>
            <p role="alert">{error}</p>
            <button className="button primary" onClick={restoreSession}>
                Tekrar dene
            </button>
        </main>
    )
}

export default SessionRecovery
