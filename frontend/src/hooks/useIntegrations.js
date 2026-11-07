import { useEffect, useState } from 'react'
import { isRequestCancelled } from '../api/httpClient.js'
import { projectService } from '../services/projectService.js'

export function useIntegrations(projectId, profileUpdatedAt) {
    const [items, setItems] = useState([])
    const [isLoading, setIsLoading] = useState(true)
    const [error, setError] = useState('')

    useEffect(() => {
        const controller = new AbortController()
        setIsLoading(true)
        setError('')

        async function loadIntegrations() {
            try {
                const result = await projectService.getIntegrationStatuses(projectId, {
                    signal: controller.signal
                })
                setItems(result)
            } catch (failure) {
                if (!isRequestCancelled(failure)) {
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
