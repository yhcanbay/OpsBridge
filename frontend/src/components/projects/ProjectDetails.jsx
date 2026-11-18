import { CAPABILITY_LABELS } from '../../constants/projectProfile.js'
import IntegrationCards from './IntegrationCards.jsx'
import GitHistory from './GitHistory.jsx'

function ProjectDetails({ project, onEdit, onDelete, deleting }) {
    const selectedCapabilities = Object.entries(CAPABILITY_LABELS).filter(([capability]) =>
        project.capabilities.includes(capability)
    )

    return (
        <section className="project-details" aria-labelledby="project-name">
            <header className="detail-header">
                <div>
                    <div className="project-kicker">
                        <span className="environment">{project.environment}</span>
                        <span className={project.enabled ? 'status enabled' : 'status neutral'}>
                            <span className="status-dot" />
                            {project.enabled ? 'Aktif profil' : 'Pasif profil'}
                        </span>
                    </div>
                    <h1 id="project-name">{project.name}</h1>
                    <a
                        className="repository-link"
                        href={project.repositoryUrl}
                        target="_blank"
                        rel="noreferrer"
                    >
                        {project.repositoryUrl} ↗
                    </a>
                </div>
                <button className="button secondary" onClick={onEdit} disabled={deleting}>
                    Profili düzenle
                </button>
            </header>
            <div className="detail-section">
                <div className="section-heading">
                    <h2>Bağlantılar</h2>
                </div>
                <IntegrationCards key={project.id} project={project} />
            </div>
            {project.enabled ? (
                <GitHistory key={`${project.id}:${project.updatedAt}`} projectId={project.id} />
            ) : (
                <div className="detail-section">
                    <h2>GitHub geçmişi</h2>
                    <p className="muted">Proje pasif. GitHub geçmişi okunmuyor.</p>
                </div>
            )}
            <div className="detail-section profile-section">
                <h2>Profil yapılandırması</h2>
                <dl className="profile-values">
                    <div>
                        <dt>Jenkins URL / referansı</dt>
                        <dd>{project.jenkinsBaseUrlRef || 'Tanımlanmadı'}</dd>
                    </div>
                    <div>
                        <dt>Jenkins job yolu</dt>
                        <dd>{project.jenkinsJobPath || 'Tanımlanmadı'}</dd>
                    </div>
                    <div>
                        <dt>Container label seçicisi</dt>
                        <dd>
                            <code>{project.containerLabelSelector || 'Tanımlanmadı'}</code>
                        </dd>
                    </div>
                    <div>
                        <dt>HTTP health URL</dt>
                        <dd>
                            <code>{project.healthUrl || 'Tanımlanmadı'}</code>
                        </dd>
                    </div>
                </dl>
                <h3 className="capabilities-heading">Desteklenen yetenekler</h3>
                <div className="tags">
                    {selectedCapabilities.map(([capability, label]) => (
                        <span className="tag" key={capability}>
                            {label}
                        </span>
                    ))}
                    {selectedCapabilities.length === 0 && (
                        <span className="muted small">Yetenek tanımlanmadı.</span>
                    )}
                </div>
            </div>
            <footer className="detail-footer">
                <span className="small muted">
                    Son profil güncellemesi · {new Date(project.updatedAt).toLocaleString('tr-TR')}
                </span>
                <button className="text-button danger" onClick={onDelete} disabled={deleting}>
                    {deleting ? 'Siliniyor…' : 'Profili sil'}
                </button>
            </footer>
        </section>
    )
}

export default ProjectDetails
