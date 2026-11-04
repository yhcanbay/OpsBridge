import { Link } from 'react-router-dom';

export function NotFoundPage() {
  return (
    <main className="empty-state">
      <h1>Sayfa bulunamadı</h1>
      <p className="muted">Bu adres için bir sayfa tanımlı değil.</p>
      <Link className="button primary" to="/projects">
        Projelere dön
      </Link>
    </main>
  );
}
