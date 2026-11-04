export function LoadingState({ message = 'OpsBridge yükleniyor…' }) {
  return (
    <main className="loading-screen" role="status">
      {message}
    </main>
  );
}
