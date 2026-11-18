import { useEffect, useState } from 'react'
import { ApiError } from '../../api/ApiError.js'
import { projectService } from '../../services/projectService.js'

const HEALTH_STATUSES = {
    UP: { label: 'Sağlıklı', tone: 'success' },
    DOWN: { label: 'Sağlıksız', tone: 'error' },
    OUT_OF_SERVICE: { label: 'Hizmet dışı', tone: 'error' },
    UNKNOWN: { label: 'Durum bilinmiyor', tone: 'neutral' }
}

function HttpHealthStatus() {
    const [health, setHealth] = useState(null)
    const [error, setError] = useState('')

    useEffect(() => {
        const controller = new AbortController()

        async function loadHttpHealth() {
            try {
                const result = await projectService.getHttpHealth({ signal: controller.signal })
                if (!controller.signal.aborted) {
                    setHealth(result)
                }
            } catch (failure) {
                if (!controller.signal.aborted) {
                    setError(
                        failure instanceof ApiError
                            ? 'OpsBridge sağlık uç noktasına ulaşılamadı.'
                            : failure.message
                    )
                }
            }
        }

        loadHttpHealth()

        return () => {
            controller.abort()
        }
    }, [])

    if (error) {
        return <p role="alert">{error}</p>
    }
    if (!health) {
        return <p role="status">Sağlık durumu kontrol ediliyor…</p>
    }

    const status = HEALTH_STATUSES[health.status.toUpperCase()] ?? HEALTH_STATUSES.UNKNOWN

    return (
        <>
            <span className={`status ${status.tone}`} role="status">
                <span className="status-dot" />
                {status.label}
            </span>
            <p>
                OpsBridge backend · HTTP {health.httpStatus} · {health.status}
            </p>
        </>
    )
}

export default HttpHealthStatus
