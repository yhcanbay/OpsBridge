import { CAPABILITY_LABELS } from '../../constants/projectProfile.js'
import IntegrationCards from './IntegrationCards.jsx'

function ProjectDetails({ project, onEdit, onDelete, deleting }) {
    const selectedCapabilities = Object.entries(CAPABILITY_LABELS).filter(([capability]) =>
        project.capabilities.includes(capability)
    )

    return (
        <section className="project-details" aria-labelledby="project-name">
            <header className="detail-header">
                <div>
                    <div className="project-kicker">
                        PROJE DETAYI <span className="environment">{project.environment}</span>
                    </div>
                    <h2 id="project-name">{project.name}</h2>
                    <a
                        className="repository-link"
                        href={project.repositoryUrl}
                        target="_blank"
                        rel="noreferrer"
                    >
                        {project.repositoryUrl} ↗
                    </a>
                </div>
                <button className="button secondary" onClick={onEdit}>
                    Profili düzenle
                </button>
            </header>
            <div className="detail-section">
                <div className="section-heading">
                    <h3>Bağlantılar</h3>
                    <span className="small muted">Canlı veri henüz alınmıyor</span>
                </div>
                <IntegrationCards key={project.id} project={project} />
            </div>
            <div className="detail-section profile-section">
                <h3>Profil yapılandırması</h3>
                <dl className="profile-values">
                    <div>
                        <dt>Jenkins referansı</dt>
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
                <h4 className="capabilities-heading">Desteklenen yetenekler</h4>
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
                    {deleting ? 'Siliniyor…' : 'Projeyi sil'}
                </button>
            </footer>
        </section>
    )
}

export default ProjectDetails
