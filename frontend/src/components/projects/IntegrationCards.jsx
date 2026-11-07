import { INTEGRATION_STATE_LABELS } from '../../constants/projectProfile.js'
import { useIntegrations } from '../../hooks/useIntegrations.js'

function IntegrationCards({ project }) {
    const { items, isLoading, error } = useIntegrations(project.id, project.updatedAt)

    if (isLoading) {
        return (
            <p role="status" className="muted">
                Bağlantı bilgileri yükleniyor…
            </p>
        )
    }

    if (error) {
        return (
            <p className="notice error" role="alert">
                {error}
            </p>
        )
    }

    return (
        <div className="integration-grid">
            {items.map((item, index) => (
                <article className="integration-card" key={item.integration}>
                    <span className="integration-number">0{index + 1}</span>
                    <h4>{item.integration}</h4>
                    <span className="status neutral">
                        <span className="status-dot" />
                        {INTEGRATION_STATE_LABELS[item.state] ?? item.state}
                    </span>
                    <p>{item.message}</p>
                    <small className="muted">
                        Son başarılı veri:{' '}
                        {item.lastSuccessfulFetchAt
                            ? new Date(item.lastSuccessfulFetchAt).toLocaleString('tr-TR')
                            : 'Henüz yok'}
                    </small>
                </article>
            ))}
        </div>
    )
}

export default IntegrationCards
