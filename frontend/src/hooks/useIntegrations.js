import { useEffect, useState } from 'react'
import { isRequestCancelled } from '../api/httpClient.js'
import { projectService } from '../services/projectService.js'

export function useIntegrations(projectId, profileUpdatedAt) {
    const [items, setItems] = useState([])
    const [isLoading, setIsLoading] = useState(true)
    const [error, setError] = useState('')

    useEffect(() => {
        const controller = new AbortController()
        setItems([])
        setIsLoading(true)
        setError('')

        async function loadIntegrations() {
            try {
                const result = await projectService.getIntegrationStatuses(projectId, {
                    signal: controller.signal
                })
                if (!controller.signal.aborted) {
                    setItems(result)
                }
            } catch (failure) {
                if (!controller.signal.aborted && !isRequestCancelled(failure)) {
                    setError(failure.message)
                }
            } finally {
                if (!controller.signal.aborted) {
                    setIsLoading(false)
                }
            }
        }

        loadIntegrations()

        return () => {
            controller.abort()
        }
    }, [projectId, profileUpdatedAt])

    return { items, isLoading, error }
}
