import { useState } from 'react'
import { useAuth } from '../../hooks/useAuth.js'

function LoginForm() {
    const { login } = useAuth()
    const [error, setError] = useState('')
    const [isLoggingIn, setIsLoggingIn] = useState(false)

    async function handleSubmit(event) {
        event.preventDefault()
        setError('')
        setIsLoggingIn(true)
        const data = new FormData(event.currentTarget)
        const credentials = {
            username: data.get('username'),
            password: data.get('password')
        }

        try {
            await login(credentials)
        } catch (failure) {
            setError(failure.message)
        } finally {
            setIsLoggingIn(false)
        }
    }

    return (
        <main className="login-page">
            <section className="login-intro">
                <div className="brand">
                    <span className="brand-symbol" aria-hidden="true">
                        ob
                    </span>{' '}
                    OpsBridge
                </div>
                <div>
                    <p className="eyebrow">PROJEDEN OPERASYONA</p>
                    <h1>
                        Projeleriniz için
                        <br />
                        ortak bir başlangıç.
                    </h1>
                    <p>
                        Proje profillerinizi tanımlayın. Build, container ve uygulama sağlığı
                        bağlantılarını tek yerde hazırlayın.
                    </p>
                    <div className="connection-path" aria-hidden="true">
                        <span>Proje</span>
                        <i>→</i>
                        <span>Pipeline</span>
                        <i>→</i>
                        <span>Runtime</span>
                    </div>
                </div>
                <p className="intro-footnote">DevOps çalışma alanı · Yerel geliştirme</p>
            </section>
            <section className="login-panel">
                <form className="login-form" onSubmit={handleSubmit}>
                    <p className="eyebrow">YÖNETİCİ ERİŞİMİ</p>
                    <h2>Çalışma alanına giriş</h2>
                    <p className="muted">Devam etmek için yönetici hesabınızı kullanın.</p>
                    <label htmlFor="username">Kullanıcı adı</label>
                    <input
                        id="username"
                        name="username"
                        autoComplete="username"
                        required
                        autoFocus
                    />
                    <label htmlFor="password">Parola</label>
                    <input
                        id="password"
                        name="password"
                        type="password"
                        autoComplete="current-password"
                        required
                    />
                    {error && (
                        <p className="notice error" role="alert">
                            {error}
                        </p>
                    )}
                    <button className="button primary" disabled={isLoggingIn}>
                        {isLoggingIn ? 'Giriş yapılıyor…' : 'Giriş yap →'}
                    </button>
                    <p className="login-help">
                        Hesap bilgileri kurulum sırasında <code>.env</code> dosyasında belirlenir.
                    </p>
                </form>
            </section>
        </main>
    )
}

export default LoginForm
