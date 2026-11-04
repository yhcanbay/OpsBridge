import { useState } from 'react';
import { useAuth } from '../../hooks/useAuth.js';

export default function LoginForm() {
  const { login } = useAuth();
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function submit(event) {
    event.preventDefault();
    setError('');
    setBusy(true);
    const data = new FormData(event.currentTarget);
    try {
      await login({ username: data.get('username'), password: data.get('password') });
    } catch (failure) {
      setError(failure.message);
    } finally {
      setBusy(false);
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
            Proje profillerinizi tanımlayın. Build, container ve uygulama sağlığı bağlantılarını tek
            yerde hazırlayın.
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
        <form className="login-form" onSubmit={submit}>
          <p className="eyebrow">YÖNETİCİ ERİŞİMİ</p>
          <h2>Çalışma alanına giriş</h2>
          <p className="muted">Devam etmek için yönetici hesabınızı kullanın.</p>
          <label htmlFor="username">Kullanıcı adı</label>
          <input id="username" name="username" autoComplete="username" required autoFocus />
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
          <button className="button primary" disabled={busy}>
            {busy ? 'Giriş yapılıyor…' : 'Giriş yap →'}
          </button>
          <p className="login-help">
            Hesap bilgileri kurulum sırasında <code>.env</code> dosyasında belirlenir.
          </p>
        </form>
      </section>
    </main>
  );
}
