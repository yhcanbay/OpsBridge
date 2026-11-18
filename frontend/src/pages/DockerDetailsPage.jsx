import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useDockerDetails } from '../hooks/useDockerDetails.js'
import './DockerDetailsPage.css'

const stateLabels = {
    running: 'Running',
    exited: 'Exited',
    restarting: 'Restarting',
    created: 'Created',
    paused: 'Paused',
    dead: 'Dead',
    removing: 'Removing'
}

const healthLabels = {
    healthy: 'Healthy',
    unhealthy: 'Unhealthy',
    starting: 'Starting',
    none: 'Kontrol tanımlı değil',
    inactive: 'Kontrol çalışmıyor'
}

const dateFormatter = new Intl.DateTimeFormat('tr-TR', {
    dateStyle: 'medium',
    timeStyle: 'short',
    timeZone: 'Europe/Istanbul'
})

function formatDate(value) {
    return value ? dateFormatter.format(new Date(value)) : '—'
}

function isStopped(container) {
    return container.state === 'exited' || container.state === 'dead'
}

function formatPorts(ports) {
    return ports.length > 0 ? ports.join(', ') : 'Yayında değil'
}

function ContainerDetails({ container }) {
    return (
        <section
            id="container-details"
            className="docker-card docker-details"
            aria-labelledby="container-details-title"
        >
            <h2 id="container-details-title">Container ayrıntıları</h2>
            <div className="docker-selected-heading">
                <strong>{container.name || 'Adı bilinmiyor'}</strong>
                <span className={`docker-badge ${container.state}`}>
                    {stateLabels[container.state] ?? 'Bilinmiyor'}
                </span>
            </div>
            <dl className="docker-detail-list">
                <dt>Servis</dt>
                <dd>{container.serviceName || '—'}</dd>
                <dt>Container ID</dt>
                <dd title={container.id}>
                    <code>{container.id.slice(0, 12)}</code>
                </dd>
                <dt>Sağlık</dt>
                <dd>{healthLabels[container.health] ?? 'Bilinmiyor'}</dd>
                <dt>Image</dt>
                <dd>{container.image || '—'}</dd>
                <dt>Image ID</dt>
                <dd>
                    <code>{container.imageId || '—'}</code>
                </dd>
            </dl>
            <dl className="docker-detail-list">
                <dt>Başlangıç</dt>
                <dd>{formatDate(container.startedAt)}</dd>
                <dt>Bitiş</dt>
                <dd>{formatDate(container.finishedAt)}</dd>
                <dt>Çıkış kodu</dt>
                <dd>{container.exitCode ?? '—'}</dd>
                <dt>Portlar</dt>
                <dd>{formatPorts(container.ports)}</dd>
            </dl>
        </section>
    )
}

function DockerDetailsPage() {
    const { projectId } = useParams()
    const { result, isLoading, refresh } = useDockerDetails(projectId)
    const [stateFilter, setStateFilter] = useState('all')
    const [selectedId, setSelectedId] = useState(null)
    const containers = result?.containers ?? []
    const visibleContainers = containers.filter(
        (container) => stateFilter === 'all' || stateFilter === container.state
    )
    const selectedContainer =
        visibleContainers.find((container) => container.id === selectedId) ??
        visibleContainers.find(isStopped) ??
        visibleContainers[0]
    const running = containers.filter((container) => container.state === 'running')
    const stopped = containers.filter(isStopped)
    const healthy = running.filter((container) => container.health === 'healthy')
    const problem =
        stopped[0] ??
        containers.find(
            (container) => container.health === 'unhealthy' || container.state === 'restarting'
        )

    let overview = `${running.length} / ${containers.length} container çalışıyor`
    let problemDetail = ''
    if (stopped.length > 0) {
        overview = `${stopped.length} container durmuş`
        if (problem.exitCode != null) problemDetail = ` · Çıkış kodu ${problem.exitCode}`
    } else if (problem) {
        overview =
            problem.state === 'restarting'
                ? 'Container yeniden başlıyor'
                : 'Sağlık kontrolü başarısız'
    }

    function handleFilterChange(event) {
        setStateFilter(event.target.value)
    }

    function handleContainerSelect(containerId) {
        setSelectedId(containerId)
    }

    return (
        <div className="workspace-content docker-page">
            <nav className="docker-breadcrumb" aria-label="Docker içerik yolu">
                <Link to={`/projects/${projectId}`}>← Projeye dön</Link>
                {result?.project && <span>{result.project.name} / Docker</span>}
            </nav>
            <header className="page-heading docker-heading">
                <div className="docker-title">
                    <span className="docker-symbol" aria-hidden="true">
                        ⬡
                    </span>
                    <div>
                        <p className="eyebrow">PROJE ENTEGRASYONU</p>
                        <h1>Docker</h1>
                    </div>
                </div>
                <button className="button secondary" onClick={refresh} disabled={isLoading}>
                    <span aria-hidden="true">⟳</span> Yenile
                </button>
            </header>

            {isLoading && (
                <p className="docker-card docker-message" role="status">
                    Docker bilgileri yükleniyor…
                </p>
            )}
            {result?.error && (
                <p className="notice error" role="alert">
                    {result.error}
                </p>
            )}

            {result?.project && (
                <>
                    <div className="docker-connection">
                        <span className="docker-connection-status">
                            <span className="status-dot" aria-hidden="true" />
                            Docker bağlı
                        </span>
                        <code>{result.project.containerLabelSelector?.split('=')[1]}</code>
                        <span>Son güncelleme {formatDate(result.fetchedAt)} TRT</span>
                    </div>
                    {containers.length === 0 ? (
                        <section className="docker-card docker-message">
                            <h2>Container bulunamadı</h2>
                            <p>Bu Docker Compose proje etiketiyle eşleşen container yok.</p>
                        </section>
                    ) : (
                        <>
                            <div
                                className={
                                    problem ? 'docker-overview has-alert' : 'docker-overview'
                                }
                            >
                                <span className="docker-overview-icon" aria-hidden="true">
                                    {problem ? '!' : '✓'}
                                </span>
                                <div>
                                    <strong>{overview}</strong>
                                    {problem && (
                                        <p>
                                            {problem.name}
                                            {problemDetail}
                                        </p>
                                    )}
                                </div>
                            </div>
                            <dl className="docker-card docker-summary" aria-label="Container özeti">
                                <div>
                                    <dt>Toplam container</dt>
                                    <dd>{containers.length}</dd>
                                </div>
                                <div>
                                    <dt>Çalışan</dt>
                                    <dd>{running.length}</dd>
                                </div>
                                <div>
                                    <dt>Duran</dt>
                                    <dd>{stopped.length}</dd>
                                </div>
                                <div>
                                    <dt>Sağlıklı</dt>
                                    <dd>{healthy.length}</dd>
                                </div>
                            </dl>
                            <div className="docker-grid">
                                <section
                                    className="docker-card"
                                    aria-labelledby="container-list-title"
                                >
                                    <div className="docker-list-header">
                                        <h2 id="container-list-title">Projenin container’ları</h2>
                                        <select
                                            aria-label="Container çalışma durumu"
                                            value={stateFilter}
                                            onChange={handleFilterChange}
                                        >
                                            <option value="all">Tüm durumlar</option>
                                            {Object.entries(stateLabels).map(([value, label]) => (
                                                <option key={value} value={value}>
                                                    {label}
                                                </option>
                                            ))}
                                        </select>
                                    </div>
                                    {visibleContainers.length === 0 ? (
                                        <p className="docker-message">
                                            Seçilen durumda container yok.
                                        </p>
                                    ) : (
                                        <div
                                            className="docker-table-scroll"
                                            role="region"
                                            aria-label="Container tablosu"
                                            tabIndex={0}
                                        >
                                            <table className="docker-table">
                                                <thead>
                                                    <tr>
                                                        <th scope="col">Servis / Container</th>
                                                        <th scope="col">Durum</th>
                                                        <th scope="col">Sağlık</th>
                                                        <th scope="col">Image</th>
                                                        <th scope="col">Sunucu → Container</th>
                                                    </tr>
                                                </thead>
                                                <tbody>
                                                    {visibleContainers.map((container) => {
                                                        const isSelected =
                                                            container.id === selectedContainer?.id
                                                        return (
                                                            <tr
                                                                key={container.id}
                                                                className={
                                                                    isSelected
                                                                        ? 'docker-row-selected'
                                                                        : ''
                                                                }
                                                            >
                                                                <td>
                                                                    <button
                                                                        className="docker-container-select"
                                                                        aria-pressed={isSelected}
                                                                        aria-controls="container-details"
                                                                        onClick={() =>
                                                                            handleContainerSelect(
                                                                                container.id
                                                                            )
                                                                        }
                                                                    >
                                                                        <strong>
                                                                            {container.serviceName ||
                                                                                'Servis bilinmiyor'}
                                                                        </strong>
                                                                        <span>
                                                                            {container.name ||
                                                                                'Adı bilinmiyor'}
                                                                        </span>
                                                                        <code>
                                                                            {container.id.slice(
                                                                                0,
                                                                                12
                                                                            )}
                                                                        </code>
                                                                    </button>
                                                                </td>
                                                                <td>
                                                                    <span
                                                                        className={`docker-badge ${container.state}`}
                                                                    >
                                                                        {stateLabels[
                                                                            container.state
                                                                        ] ?? 'Bilinmiyor'}
                                                                    </span>
                                                                </td>
                                                                <td>
                                                                    <span
                                                                        className={`docker-badge ${container.health}`}
                                                                    >
                                                                        {healthLabels[
                                                                            container.health
                                                                        ] ?? 'Bilinmiyor'}
                                                                    </span>
                                                                </td>
                                                                <td>{container.image || '—'}</td>
                                                                <td>
                                                                    {formatPorts(container.ports)}
                                                                </td>
                                                            </tr>
                                                        )
                                                    })}
                                                </tbody>
                                            </table>
                                        </div>
                                    )}
                                </section>
                                {selectedContainer && (
                                    <ContainerDetails container={selectedContainer} />
                                )}
                            </div>
                        </>
                    )}
                </>
            )}
        </div>
    )
}

export default DockerDetailsPage
